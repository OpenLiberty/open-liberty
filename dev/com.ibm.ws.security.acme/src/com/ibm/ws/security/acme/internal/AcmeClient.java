/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.ldap.Rdn;

import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.Account.EditableAccount;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.OrderBuilder;
import org.shredzone.acme4j.RevocationReason;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.exception.AcmeProtocolException;
import org.shredzone.acme4j.exception.AcmeRetryAfterException;
import org.shredzone.acme4j.exception.AcmeServerException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeCertificate;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;

/**
 * The {@link AcmeClient} class is the gateway for all interactions with the
 * ACME CA server.
 */
public class AcmeClient {

	private static final TraceComponent tc = Tr.register(AcmeClient.class);

	/**
	 * Acme configuration for this client.
	 */
	private final AcmeConfig acmeConfig;

	/**
	 * This is a map of HTTP-01 challenge tokens to authorizations. The
	 * challenge token and the authorization are received from the ACME CA
	 * server during the HTTP-01 challenge process. We need to save it so we can
	 * respond back with the authorization when the CA requests it from the
	 * domain the certificate is being generated for.
	 */
	private final Map<String, String> httpTokenToAuthzMap = new HashMap<String, String>();

	/**
	 * Read / write lock for the account key file. This is used to ensure
	 * multiple threads are not trying to update the file at the same time, or
	 * that we are not updating the file and reading from the file at the same
	 * time. Currently, we don't use the same mechanism for the domain key as
	 * the domain key doesn't currently have a REST endpoint that allows the it
	 * to be regenerated.
	 */
	private final static ReadWriteLock accountKeyPairFileRWLock = new ReentrantReadWriteLock();

	/**
	 * Create a new {@link AcmeClient} instance.
	 * 
	 * @param acmeConfig
	 *            The {@link AcmeConfig} object to create the {@link AcmeClient}
	 *            from.
	 * @throws AcmeCaException
	 *             if the parameters passed in were invalid.
	 */
	public AcmeClient(AcmeConfig acmeConfig) throws AcmeCaException {
		this.acmeConfig = acmeConfig;
	}

	/**
	 * Authorize a domain. It will be associated with your account, so you will
	 * be able to retrieve a signed certificate for the domain later.
	 *
	 * @param authorization
	 *            {@link Authorization} to perform
	 * @throws AcmeCaException
	 *             if there was an issue authorizing the domain.
	 */
	@FFDCIgnore({ AcmeException.class, AcmeRetryAfterException.class })
	private void authorize(Authorization authorization) throws AcmeCaException {
		/*
		 * The authorization is already valid. No need to process a challenge.
		 */
		if (authorization.getStatus() == Status.VALID) {
			return;
		}

		/*
		 * Prepare the HTTP-01 challenge.
		 */
		Challenge challenge = prepareHttpChallenge(authorization);

		try {
			/*
			 * If the challenge is already verified, there's no need to execute
			 * it again.
			 */
			if (challenge.getStatus() == Status.VALID) {
				return;
			}

			/*
			 * Now trigger the challenge.
			 */
			try {
				challenge.trigger();
			} catch (AcmeException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2009E", acmeConfig.getDirectoryURI(), e.getMessage()), e);
			}

			/*
			 * Poll for the challenge to complete.
			 */
			int attempts = acmeConfig.getChallengeRetries() + 1;
			while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
				// Did the authorization fail?
				if (challenge.getStatus() == Status.INVALID) {
					String msg = Tr.formatMessage(tc, "CWPKI2001E", acmeConfig.getDirectoryURI(),
							authorization.getIdentifier().getDomain(), challenge.getStatus().toString(),
							challenge.getError().toString());
					throw new AcmeCaException(msg);
				}

				/*
				 * Wait to update the status.
				 */
				sleep(acmeConfig.getChallengeRetryWaitMs());

				/*
				 * Then update the status
				 */
				try {
					challenge.update();
				} catch (AcmeRetryAfterException e) {
					// TODO Wait until the moment defined in the returned
					// Instant.
					// Instant when = e.getRetryAfter();
				} catch (AcmeException e) {
					throw new AcmeCaException(
							Tr.formatMessage(tc, "CWPKI2010E", acmeConfig.getDirectoryURI(), e.getMessage()), e);
				}
			}

			/*
			 * All re-attempts are used up and there is still no valid
			 * authorization?
			 */
			if (challenge.getStatus() != Status.VALID) {
				String msg = Tr.formatMessage(tc, "CWPKI2002E", acmeConfig.getDirectoryURI(),
						authorization.getIdentifier().getDomain(), challenge.getStatus().toString(),
						(acmeConfig.getChallengeRetries() * acmeConfig.getChallengeRetryWaitMs()) + "ms");
				throw new AcmeCaException(msg);
			}

		} finally {
			/*
			 * We are done with the authorization, so clear it from the map.
			 */
			if (challenge instanceof Http01Challenge) {
				httpTokenToAuthzMap.remove(((Http01Challenge) challenge).getToken());
			}
		}
	}

	/**
	 * Generates a certificate for the CSR options. Also takes care for the
	 * registration process.
	 *
	 * @param dryRun
	 *            Whether this should be a dry run to report any errors. The no
	 *            order will be made with the ACME CA server and no certificate
	 *            will be retrieved.
	 * @return The {@link X509Certificate} returned from the certificate
	 *         authority.
	 * @throws AcmeCaException
	 *             if there was an issue fetching the certificate.
	 */
	@FFDCIgnore({ IOException.class, AcmeException.class, AcmeRetryAfterException.class })
	public AcmeCertificate fetchCertificate(boolean dryRun) throws AcmeCaException {

		/*
		 * Load the account key file. If there is no key file, create a new one.
		 */
		KeyPair accountKeyPair = getAccountKeyPair(false);

		/*
		 * Create a session to the ACME CA directory service.
		 */
		Session session = getNewSession();

		/*
		 * Get the Account. If there is no account yet, create a new one.
		 */
		Account acct = findOrRegisterAccount(session, accountKeyPair);

		/*
		 * Create a key pair for the domains.
		 */
		KeyPair domainKeyPair = getDomainKeyPair();

		/*
		 * Stop now if this is a dry run.
		 */
		if (dryRun) {
			return null;
		}

		/*
		 * Order the certificate
		 */
		OrderBuilder orderBuilder = acct.newOrder();
		orderBuilder.domains(acmeConfig.getDomains());
		if (acmeConfig.getValidForMs() != null && acmeConfig.getValidForMs() > 0) {
			orderBuilder.notAfter(Instant.now().plusMillis(acmeConfig.getValidForMs()));
		}
		Order order;
		try {
			order = orderBuilder.create();
		} catch (AcmeException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2011E", acmeConfig.getDirectoryURI(), e.getMessage()),
					e);
		}

		/*
		 * Perform all required authorizations
		 */
		for (Authorization auth : order.getAuthorizations()) {
			authorize(auth);
		}

		/*
		 * Generate a CSR for all of the domains, and sign it with the domain
		 * key pair.
		 */
		CSRBuilder csrb = new CSRBuilder();
		csrb.addDomains(acmeConfig.getDomains());

		/*
		 * Add the RDN's for the subjectDN in order.
		 */
		for (Rdn rdn : acmeConfig.getSubjectDN()) {
			switch (rdn.getType().toLowerCase()) {
			case "o":
				csrb.setOrganization((String) rdn.getValue());
				break;
			case "ou":
				csrb.setOrganizationalUnit((String) rdn.getValue());
				break;
			case "c":
				csrb.setCountry((String) rdn.getValue());
				break;
			case "st":
				csrb.setState((String) rdn.getValue());
				break;
			case "l":
				csrb.setLocality((String) rdn.getValue());
				break;
			}
		}

		/*
		 * Sign the certificate signing request.
		 */
		try {
			csrb.sign(domainKeyPair);
		} catch (IOException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2012E", acmeConfig.getDirectoryURI(), e.getMessage()),
					e);
		}
		Tr.debug(tc, "Certificate Signing Request: " + csrb.toString());

		/*
		 * Order the certificate
		 */
		try {
			order.execute(csrb.getEncoded());
		} catch (AcmeException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2013E", acmeConfig.getDirectoryURI(), e.getMessage()),
					e);
		} catch (IOException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2014E", acmeConfig.getDirectoryURI(), e.getMessage()),
					e);
		}

		/*
		 * Wait for the order to complete
		 */
		int attempts = acmeConfig.getOrderRetries() + 1;
		while (order.getStatus() != Status.VALID && attempts-- > 0) {
			/*
			 * Did the order fail?
			 */
			if (order.getStatus() == Status.INVALID) {
				String msg = Tr.formatMessage(tc, "CWPKI2001E", acmeConfig.getDirectoryURI(), acmeConfig.getDomains(),
						order.getStatus().toString(), order.getError().toString());
				throw new AcmeCaException(msg);
			}

			/*
			 * Wait to update the status.
			 */
			sleep(acmeConfig.getOrderRetryWaitMs());

			/*
			 * Then update the status
			 */
			try {
				order.update();
			} catch (AcmeRetryAfterException e) {
				// TODO Wait until the moment defined in the returned Instant.
				// Instant when = e.getRetryAfter();
			} catch (AcmeException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2015E", acmeConfig.getDirectoryURI(), e.getMessage()), e);
			}
		}

		/*
		 * All re-attempts are used up and there is still no valid order?
		 */
		if (order.getStatus() != Status.VALID) {
			String msg = Tr.formatMessage(tc, "CWPKI2004E", acmeConfig.getDirectoryURI(), acmeConfig.getDomains(),
					order.getStatus().toString(),
					(acmeConfig.getOrderRetries() * acmeConfig.getOrderRetryWaitMs()) + "ms");
			throw new AcmeCaException(msg);
		}

		/*
		 * Package up the pieces and parts needed to generate the personal
		 * certificate.
		 */
		Certificate certificate = order.getCertificate();

		/*
		 * Check whether the notBefore time is in the future. This might happen if the
		 * time on the local system is off.
		 */
		X509Certificate x509Cert = certificate.getCertificate();
		if (x509Cert.getNotBefore().after(Calendar.getInstance().getTime())) {
			Tr.warning(tc, "CWPKI2045W", x509Cert.getSerialNumber().toString(16), acmeConfig.getDirectoryURI(),
					x509Cert.getNotBefore().toInstant().toString());
		}
		return new AcmeCertificate(domainKeyPair, x509Cert, certificate.getCertificateChain());
	}

	/**
	 * Get the account that is configured for this client. This is the account
	 * that is bound to the configured account key.
	 * 
	 * @return The account or null if it was not found.
	 * @throws AcmeCaException
	 *             if there was an error requesting the account.
	 */
	public AcmeAccount getAccount() throws AcmeCaException {
		return new AcmeAccount(getAccount(null));
	}

	/**
	 * Get the account that is configured for this client. This is the account
	 * that is bound to the configured account key.
	 * 
	 * @param session
	 *            The session to use to request the account from the ACME CA
	 *            server. If null, a new session will be created.
	 * @return The account or null if it was not found.
	 * @throws AcmeCaException
	 *             if there was an error requesting the account.
	 */
	private Account getAccount(Session session) throws AcmeCaException {
		/*
		 * Load the account key file.
		 */
		KeyPair accountKeyPair = loadAccountKeyPair();
		if (accountKeyPair == null) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2025W", acmeConfig.getDirectoryURI()));
		}

		/*
		 * Create a session to the ACME CA directory service.
		 */
		if (session == null) {
			session = getNewSession();
		}

		/*
		 * Get the Account.
		 */
		return getExistingAccount(session, accountKeyPair);
	}

	/**
	 * Find an existing account on the ACME CA server.
	 * 
	 * @param session
	 *            Session with the ACME CA server.
	 * @param accountKey
	 *            The account key to lookup the account for.
	 * @return The account or null if it was not found.
	 * @throws AcmeCaException
	 *             if there was an issue requesting an existing account.
	 */
	@FFDCIgnore({ AcmeServerException.class })
	private Account getExistingAccount(Session session, KeyPair accountKey) throws AcmeCaException {
		try {
			return new AccountBuilder().useKeyPair(accountKey).onlyExisting().create(session);
		} catch (AcmeServerException e) {
			return null;
		} catch (Exception e) {
			/*
			 * We want FFDC here as this will usually be the first communication
			 * we try with the ACME server. We want to capture why we failed.
			 */
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2016E",
					new Object[] { acmeConfig.getDirectoryURI(), getRootCauseMessage(e) }), e);
		}
	}

	/**
	 * Finds your {@link Account} at the ACME server. It will be found by your
	 * account's public key. If your key is not known to the server yet, a new
	 * account will be created.
	 *
	 * @param session
	 *            {@link Session} to bind with
	 * @return {@link Login} that is connected to your account
	 * @throws AcmeCaException
	 *             if there was an issue finding or registering an account.
	 */
	@FFDCIgnore(AcmeException.class)
	private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeCaException {

		/*
		 * Find an existing account.
		 */
		Account account = getExistingAccount(session, accountKey);

		/*
		 * If there is no existing account, create one.
		 */
		if (account == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "An existing account was not found, requesting terms of service.");
			}
			/*
			 * Get the terms of service from the ACME server.
			 */
			URI tosURI = null;
			try {
				tosURI = session.getMetadata().getTermsOfService();
			} catch (AcmeException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2017E", acmeConfig.getDirectoryURI(), e.getMessage()), e);
			}

			/*
			 * If the server provides terms of service, the account must accept
			 * them.
			 */
			if (tosURI == null) {
				if (tc.isDebugEnabled()) {
					Tr.debug(tc, "No terms of service provided");
				}
			} else {
				Tr.audit(tc, "CWPKI2006I", acmeConfig.getDirectoryURI(), tosURI);
			}

			/*
			 * Create the account using the account account key pair.
			 */
			AccountBuilder accountBuilder = new AccountBuilder().agreeToTermsOfService().useKeyPair(accountKey);

			/*
			 * Add any account contacts.
			 */
			if (acmeConfig.getAccountContacts() != null && !acmeConfig.getAccountContacts().isEmpty()) {
				for (String contact : acmeConfig.getAccountContacts()) {
					accountBuilder.addContact(contact);
				}
			}

			/*
			 * Finally, create the account.
			 */
			try {
				account = accountBuilder.create(session);
			} catch (AcmeException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2018E", acmeConfig.getDirectoryURI(), e.getMessage()), e);
			}
		}

		Tr.audit(tc, "CWPKI2019I", acmeConfig.getDirectoryURI(), account.getLocation());
		return account;
	}

	/**
	 * Get the HTTP-01 challenge authorization for the specified challenge
	 * token. Both the challenge token and the challenge authorization are
	 * generated by the ACME CA server.
	 * 
	 * @param token
	 *            The HTTP-01 challenge token to get the authorization for.
	 * @return The HTTP-01 challenge authorization.
	 */
	public String getHttp01Authorization(String token) {
		return httpTokenToAuthzMap.get(token);
	}

	/**
	 * Loads a account key pair from the account key file.
	 *
	 * @return Account's {@link KeyPair} or null if it does not exist.
	 * @throws AcmeCaException
	 *             if there was an issue loading the account key pair.
	 */
	@FFDCIgnore(IOException.class)
	private KeyPair loadAccountKeyPair() throws AcmeCaException {
		/*
		 * Obtain the read lock. If another thread holds the write lock, we will
		 * wait until it is done.
		 */
		accountKeyPairFileRWLock.readLock().lock();

		try {
			File accountKeyFile = null;
			if (acmeConfig.getAccountKeyFile() != null) {
				accountKeyFile = new File(acmeConfig.getAccountKeyFile());
			}

			if (accountKeyFile != null && accountKeyFile.exists()) {
				/*
				 * If there is a key file, read it
				 */
				try (FileReader fr = new FileReader(accountKeyFile)) {
					return KeyPairUtils.readKeyPair(fr);
				} catch (IOException e) {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2021E", accountKeyFile, e.getMessage()), e);
				}
			}

			return null;
		} finally {
			/*
			 * Always release the read lock.
			 */
			accountKeyPairFileRWLock.readLock().unlock();
		}
	}

	/**
	 * Loads a domain key pair from the domain key file.
	 *
	 * @return Domain's {@link KeyPair} or null if it does not exist.
	 * @throws AcmeCaException
	 *             if there was an issue loading the domain key pair.
	 */
	@FFDCIgnore(IOException.class)
	private KeyPair loadDomainKeyPair() throws AcmeCaException {

		File domainKeyFile = null;
		if (acmeConfig.getDomainKeyFile() != null) {
			domainKeyFile = new File(acmeConfig.getDomainKeyFile());
		}

		if (domainKeyFile != null && domainKeyFile.exists()) {
			/*
			 * If there is a key file, read it
			 */
			try (FileReader fr = new FileReader(domainKeyFile)) {
				return KeyPairUtils.readKeyPair(fr);
			} catch (IOException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2020E", domainKeyFile, e.getMessage()), e);
			}
		}

		return null;
	}

	/**
	 * Loads a account key pair from the user account key file. If the file does
	 * not exist, a new key pair is generated and saved.
	 * <p>
	 * If <code>force</code> is set to true, a new key pair is generated and
	 * saved.
	 * <p>
	 * Keep this key pair in a safe place! In a production environment, you will
	 * not be able to access your account again if you should lose the key pair.
	 *
	 * @param force
	 *            force generation of a new key pair.
	 * @return Account's {@link KeyPair}.
	 * @throws AcmeCaException
	 *             if there was an error reading or writing the account key pair
	 *             file.
	 */
	@FFDCIgnore(IOException.class)
	private KeyPair getAccountKeyPair(boolean force) throws AcmeCaException {

		KeyPair accountKeyPair = null;
		if (!force) {
			/*
			 * See if we have an account KeyPair already.
			 */
			accountKeyPair = loadAccountKeyPair();
		}

		/*
		 * If we don't have an account KeyPair already, generate one.
		 */
		if (accountKeyPair == null) {
			/*
			 * Obtain the write lock.
			 */
			accountKeyPairFileRWLock.writeLock().lock();

			try {
				/*
				 * If there is none, create a new key pair and save it
				 */
				accountKeyPair = KeyPairUtils.createKeyPair(AcmeConstants.KEY_SIZE);

				File accountKeyFile = null;
				if (acmeConfig.getAccountKeyFile() != null) {
					accountKeyFile = new File(acmeConfig.getAccountKeyFile());

					/*
					 * Create parent directories if the abstract path contains
					 * parent directories.
					 */
					if (accountKeyFile.getParentFile() != null) {
						accountKeyFile.getParentFile().mkdirs();
					}
				}
				if (accountKeyFile != null) {
					try (FileWriter fw = new FileWriter(accountKeyFile)) {
						KeyPairUtils.writeKeyPair(accountKeyPair, fw);
					} catch (IOException e) {
						throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2023E", acmeConfig.getDirectoryURI(),
								accountKeyFile, e.getMessage()), e);
					}
				}
			} finally {
				/*
				 * Always release the write lock.
				 */
				accountKeyPairFileRWLock.writeLock().unlock();
			}
		}

		return accountKeyPair;

	}

	/**
	 * Loads a domain key pair from the domain key file. If the file does not
	 * exist, a new key pair is generated and saved.
	 * <p>
	 * Keep this key pair in a safe place! In a production environment, you will
	 * not be able to access your account again if you should lose the key pair.
	 *
	 * @return Account's {@link KeyPair}.
	 * @throws AcmeCaException
	 *             if there was an error reading or writing the account key pair
	 *             file.
	 */
	@FFDCIgnore(IOException.class)
	private KeyPair getDomainKeyPair() throws AcmeCaException {

		/*
		 * See if we have an domain KeyPair already.
		 */
		KeyPair domainKeyPair = loadDomainKeyPair();

		/*
		 * If we don't have an domain KeyPair already, generate one.
		 */
		if (domainKeyPair == null) {
			/*
			 * If there is none, create a new key pair and save it
			 */
			domainKeyPair = KeyPairUtils.createKeyPair(AcmeConstants.KEY_SIZE);

			File domainKeyFile = null;
			if (acmeConfig.getDomainKeyFile() != null) {
				domainKeyFile = new File(acmeConfig.getDomainKeyFile());

				/*
				 * Create parent directories if the abstract path contains
				 * parent directories.
				 */
				if (domainKeyFile.getParentFile() != null) {
					domainKeyFile.getParentFile().mkdirs();
				}
			}
			if (domainKeyFile != null) {
				try (FileWriter fw = new FileWriter(domainKeyFile)) {
					KeyPairUtils.writeKeyPair(domainKeyPair, fw);
				} catch (IOException e) {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2022E", acmeConfig.getDirectoryURI(),
							domainKeyFile, e.getMessage()), e);
				}
			}
		}

		return domainKeyPair;
	}

	/**
	 * Prepares an HTTP-01 challenge.
	 * <p>
	 * The verification of this challenge expects a file with a certain content
	 * to be reachable at a given path under the domain to be tested.
	 * <p>
	 * This example outputs instructions that need to be executed manually. In a
	 * production environment, you would rather generate this file
	 * automatically, or maybe use a servlet that returns
	 * {@link Http01Challenge#getAuthorization()}.
	 *
	 * @param auth
	 *            {@link Authorization} to find the challenge in
	 * @return {@link Challenge} to verify
	 * @throws AcmeCaException
	 *             if there was an error preparing the HTTP-01 challenge.
	 */
	public Challenge prepareHttpChallenge(Authorization auth) throws AcmeCaException {
		/*
		 * Find a single HTTP-01 challenge
		 */
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
		if (challenge == null) {
			throw new AcmeCaException(
					Tr.formatMessage(tc, "CWPKI2005E", acmeConfig.getDirectoryURI(), Http01Challenge.TYPE));
		}

		/*
		 * Save away the challenge.
		 */
		httpTokenToAuthzMap.put(challenge.getToken(), challenge.getAuthorization());

		Tr.debug(tc, "Prepared the HTTP-01 challenge with token '" + challenge.getToken() + "' and authorization '"
				+ challenge.getAuthorization() + "'.");
		return challenge;
	}

	/**
	 * Revoke a certificate using an existing account on the ACME server. If the
	 * account key pair cannot be found, we will fail.
	 * 
	 * @param certificate
	 *            The certificate to revoke.
	 * @throws AcmeCaException
	 *             if there was an issue revoking the certificate.
	 */
	@FFDCIgnore(AcmeException.class)
	public void revoke(X509Certificate certificate) throws AcmeCaException {

		if (certificate == null) {
			return;
		}

		/*
		 * Load the account key file.
		 */
		KeyPair accountKeyPair = loadAccountKeyPair();
		if (accountKeyPair == null) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2025W", acmeConfig.getDirectoryURI()));
		}

		/*
		 * Create a session to the ACME CA directory service.
		 */
		Session session = getNewSession();

		/*
		 * Get the Account.
		 */
		Account acct = getExistingAccount(session, accountKeyPair);

		if (acct != null) {
			/*
			 * Login and revoke the certificate.
			 */
			Login login = new Login(acct.getLocation(), accountKeyPair, session);
			try {
				Certificate.revoke(login, certificate, RevocationReason.SUPERSEDED);
			} catch (AcmeException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2024E", acmeConfig.getDirectoryURI(),
						certificate.getSerialNumber().toString(16), e.getMessage()), e);
			}
		} else {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2026W", acmeConfig.getDirectoryURI()));
		}
	}

	/**
	 * Call {@link Thread#sleep(long)} while handling interrupts.
	 * 
	 * @param sleepMs
	 *            The number of milliseconds to sleep.
	 */
	@FFDCIgnore(InterruptedException.class)
	private static void sleep(long sleepMs) {

		long current, terminate = System.currentTimeMillis() + sleepMs;
		while ((current = System.currentTimeMillis()) < terminate) {
			try {
				Thread.sleep(terminate - current);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Create a new session with the ACME CA server.
	 * 
	 * @return the session
	 * @throws AcmeCaException
	 *             If there was an error trying to create the new session.
	 */
	@FFDCIgnore({ Exception.class })
	private Session getNewSession() throws AcmeCaException {

		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Session>() {

				@Override
				public Session run() throws Exception {
					ClassLoader origLoader = null;
					try {
						/*
						 * Acme4J tries to load the providers using
						 * ServiceLoader, which requires the ClassLoader is for
						 * this bundle. If it isn't, calls through our HTTP
						 * end-points will cause Acme4J to fail to load the
						 * providers.
						 */
						origLoader = Thread.currentThread().getContextClassLoader();
						Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

						return new Session(AcmeClient.this.acmeConfig.getDirectoryURI());
					} finally {
						Thread.currentThread().setContextClassLoader(origLoader);
					}
				}
			});
		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc,
						"Getting a new session failed for " + acmeConfig.getDirectoryURI() + ", full stack trace is",
						e);
			}

			/*
			 * Get the cause. PrivilegedActionException ONLY wraps checked
			 * exceptions.
			 */
			Throwable cause;
			if (e instanceof PrivilegedActionException) {
				cause = ((PrivilegedActionException) e).getException();
			} else {
				cause = e;
			}

			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2028E", acmeConfig.getDirectoryURI()), cause);
		}
	}

	/**
	 * Get the root cause message. The acme4j library has a tendency to
	 * genericize the exception messages which makes it hard to determine root
	 * cause.
	 * 
	 * @param t
	 *            Throwable to get the root cause's message from.
	 * @return The message from the lowest level cause that isn't empty or null.
	 */
	@Trivial
	private static String getRootCauseMessage(Throwable t) {
		Throwable cause;
		String rootMessage = t.getMessage();

		for (cause = t; cause != null; cause = cause.getCause()) {
			String msg = cause.getMessage();
			if (msg != null && !msg.trim().isEmpty()) {
				rootMessage = msg;
			}
		}

		return rootMessage;
	}

	/**
	 * Renew the account key pair and back up the existing key pair to disk.
	 * 
	 * @throws AcmeCaException
	 *             if there was an error replacing the account key pair.
	 */
	public void renewAccountKeyPair() throws AcmeCaException {

		/*
		 * Obtain the write lock. We will hold this the entire length of the
		 * operation.
		 */
		accountKeyPairFileRWLock.writeLock().lock();

		try {
			/*
			 * Get the account.
			 */
			Account acct = getAccount(null);
			if (acct != null) {
				/*
				 * Copy the existing account key pair.
				 */
				File backupFile = null;
				try {
					String datestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
					File existingFile = new File(acmeConfig.getAccountKeyFile());
					String backupFilename = existingFile.getParent() + File.separatorChar + datestamp + "-"
							+ existingFile.getName();
					copyFile(acmeConfig.getAccountKeyFile(), backupFilename);

					/*
					 * Indicate that we have copied the file by setting the
					 * copied file to non-null.
					 */
					backupFile = new File(backupFilename);
				} catch (IOException e) {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2050E", e.getMessage()), e);
				}

				/*
				 * Force creation of a new account key pair. This will write it
				 * to the existing key pair file.
				 */
				KeyPair keyPair = null;
				try {
					keyPair = getAccountKeyPair(true);
				} catch (AcmeCaException e) {
					/*
					 * We failed to generate a new file. Remove the backup file.
					 */
					deleteFile(backupFile);
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2047E", e.getMessage()), e);
				}

				/*
				 * Associate the new key pair to the account.
				 */
				try {
					acct.changeKey(keyPair);
				} catch (AcmeException e) {
					/*
					 * The ACME CA server refused to update the key pair.
					 * Restore the previous key pair file.
					 */
					try {
						copyFile(backupFile.getAbsolutePath(), acmeConfig.getAccountKeyFile());
						deleteFile(backupFile);
					} catch (IOException e1) {
						/*
						 * The user is going to need to manually replace the
						 * file.
						 */
						Tr.error(tc, "CWPKI2049E", acmeConfig.getAccountKeyFile(), backupFile.getAbsolutePath());
					}
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2047E", e.getMessage()), e);
				}

				Tr.info(tc, Tr.formatMessage(tc, "CWPKI2048I", backupFile.getAbsolutePath()));

			} else {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2026W", acmeConfig.getDirectoryURI()));
			}

		} finally {
			/*
			 * Always unlock the write lock.
			 */
			accountKeyPairFileRWLock.writeLock().unlock();
		}
	}

	/**
	 * Delete the specified file.
	 * 
	 * @param file
	 *            file to delete.
	 */
	private static void deleteFile(File file) {
		AccessController.doPrivileged(new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				file.delete();
				return null;
			}

		});
	}

	/**
	 * Copy the specified source file to the specified destination.
	 * 
	 * @param source
	 *            The source file path.
	 * @param destination
	 *            The destination file path.
	 * @throws IOException
	 *             if there was an error copying the file.
	 */
	private static void copyFile(String source, String destination) throws IOException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

				@Override
				public Void run() throws IOException {
					OutputStream out = new FileOutputStream(new File(destination));

					try {
						Files.copy(new File(source).toPath(), out);
					} finally {
						out.close();
					}
					return null;
				}

			});
		} catch (PrivilegedActionException e) {
			throw (IOException) e.getException();
		}
	}

	/**
	 * Update the account with the latest account information.
	 * 
	 * @throws AcmeCaException
	 */
	@FFDCIgnore(AcmeException.class)
	public void updateAccount() throws AcmeCaException {

		/*
		 * Get the account.
		 */
		Account acct = getAccount(null);

		if (acct != null) {
			List<String> configContacts = (acmeConfig.getAccountContacts() == null) ? Collections.emptyList()
					: acmeConfig.getAccountContacts();
			List<URI> acctContacts = (acct.getContacts() == null) ? Collections.emptyList() : acct.getContacts();
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Existing account contacts: " + acctContacts);
			}

			/*
			 * Do we require an update?
			 */
			boolean requiresUpdate = acctContacts.size() != configContacts.size();
			if (!requiresUpdate) {
				/* Size is the same, but are the contents? */
				for (String contact : configContacts) {
					if (!acctContacts.contains(URI.create(contact))) {
						requiresUpdate = true;
						break;
					}
				}
			}

			if (requiresUpdate) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Account requires updating.");
				}

				/*
				 * Login and update the contacts.
				 */
				try {
					EditableAccount editableAcct = acct.modify();

					/*
					 * Clear the current contacts on the account.
					 */
					List<URI> editableContacts = editableAcct.getContacts();
					if (editableContacts != null) {
						editableContacts.clear();
					}

					/*
					 * Add the configured contacts to the account.
					 */
					if (configContacts != null) {
						for (String contact : configContacts) {
							editableAcct.addContact(URI.create(contact));
						}
					}

					editableAcct.commit();
				} catch (AcmeException e) {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2033E", acct.getLocation(),
							acmeConfig.getDirectoryURI(), e.getMessage()), e);
				}
			}
		} else {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2026W", acmeConfig.getDirectoryURI()));
		}
	}

	/**
	 * Internal class to wrap an {@link Account} object from Acme4J. This will
	 * keep Acme4J import solely in this class.
	 */
	@Trivial
	public class AcmeAccount {
		private final Account account;

		/**
		 * Create a new {@link AcmeAccount} instance.
		 * 
		 * @param account
		 *            The account to wrap.
		 */
		private AcmeAccount(Account account) {
			this.account = account;
		}

		/**
		 * Get the account contacts.
		 * 
		 * @return The list of contact {@link URI}s.
		 */
		public List<URI> getContacts() {
			return account.getContacts();
		}

		/**
		 * Get the account location.
		 * 
		 * @return account location.
		 */
		public URL getLocation() {
			return account.getLocation();
		}

		/**
		 * Get the list of account orders.
		 * 
		 * @return list of account orders.
		 */
		@FFDCIgnore(AcmeProtocolException.class)
		public List<String> getOrders() {
			Iterator<Order> orders;
			try {
				orders = account.getOrders();
				if (orders == null) {
					return Collections.emptyList();
				}
			} catch (AcmeProtocolException e) {
				/* Can happens if there are no orders. */
				return Collections.emptyList();
			}

			List<String> ordersList = new ArrayList<String>();
			while (orders.hasNext()) {
				Order order = orders.next();
				ordersList.add(order.getJSON().toString());
			}
			return ordersList;
		}

		/**
		 * Get the account status.
		 * 
		 * @return account status.
		 */
		public String getStatus() {
			return account.getStatus().toString();
		}

		/**
		 * Get whether the account has agreed to the terms of service.
		 * 
		 * @return whether the account has agreed to the terms of service
		 */
		public Boolean getTermsOfServiceAgreed() {
			return account.getTermsOfServiceAgreed();
		}

		@Override
		public String toString() {
			return super.toString() + "{" + account.getLocation() + "}";
		}
	}
}
