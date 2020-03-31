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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.shredzone.acme4j.Account;
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

	/*
	 * TODO Support external account binding?
	 */

	/**
	 * The class' trace component.
	 */
	private static final TraceComponent tc = Tr.register(AcmeClient.class);

	/**
	 * A collection of account contacts.
	 */
	private Collection<String> accountContacts;

	/**
	 * File name of the account key pair.
	 */
	private String accountKeyFilePath = null;

	/**
	 * The number of times to retry updating the status of a challenge.
	 */
	private int challengeRetries = 10;

	/**
	 * How long to wait (in ms) before retrying to update the status of a
	 * challenge.
	 */
	private long challengeRetryWaitMs = 5000L;

	/**
	 * The URI of ACME CA server's directory.
	 */
	private String directoryURI = null;

	/**
	 * File name of the domain key pair.
	 */
	private String domainKeyFilePath = null;

	/**
	 * This is a map of HTTP-01 challenge tokens to authorizations. The
	 * challenge token and the authorization are received from the ACME CA
	 * server during the HTTP-01 challenge process. We need to save it so we can
	 * respond back with the authorization when the CA requests it from the
	 * domain the certificate is being generated for.
	 */
	private final Map<String, String> httpTokenToAuthzMap = new HashMap<String, String>();

	/**
	 * The number of times to retry updating the status of an order.
	 */
	private int orderRetries = 10;

	/**
	 * How long to wait (in ms) before retrying to update the status of an
	 * order.
	 */
	private long orderRetryWaitMs = 3000L;

	/**
	 * Whether the account accepts any terms of service.
	 */
	private boolean termsOfServiceAgreed = false;

	/**
	 * Create a new {@link AcmeClient} instance.
	 * 
	 * @param directoryURI
	 *            The URI of the ACME CA's directory service. Must be non-null
	 *            and non-empty.
	 * @param accountKeyFilePath
	 *            The path to the account account key file. This path must be
	 *            readable if it exists and writable if it does not exist.
	 * @param domainKeyFilePath
	 *            The path to the account domain key file. This path must be
	 *            readable if it exists and writable if it does not exist.
	 * @param accountContacts
	 *            A collection of account contacts.
	 * @throws AcmeCaException
	 *             if the parameters passed in were invalid.
	 */
	public AcmeClient(String directoryURI, String accountKeyFilePath, String domainKeyFilePath,
			Collection<String> accountContacts) throws AcmeCaException {

		if (directoryURI == null || directoryURI.trim().isEmpty()) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2008E", directoryURI));
		}
		validateKeyFilePath(accountKeyFilePath, AcmeConstants.ACCOUNT_TYPE);
		validateKeyFilePath(domainKeyFilePath, AcmeConstants.DOMAIN_TYPE);

		this.directoryURI = directoryURI;
		this.accountKeyFilePath = accountKeyFilePath;
		this.domainKeyFilePath = domainKeyFilePath;
		this.accountContacts = accountContacts;
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
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2009E", directoryURI, e.getMessage()), e);
			}

			/*
			 * Poll for the challenge to complete.
			 */
			int attempts = challengeRetries + 1;
			while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
				// Did the authorization fail?
				if (challenge.getStatus() == Status.INVALID) {
					String msg = Tr.formatMessage(tc, "CWPKI2001E", directoryURI,
							authorization.getIdentifier().getDomain(), challenge.getStatus().toString(),
							challenge.getError().toString());
					//Tr.error(tc, msg);
					throw new AcmeCaException(msg);
				}

				/*
				 * Wait to update the status.
				 */
				sleep(challengeRetryWaitMs);

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
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2010E", directoryURI, e.getMessage()), e);
				}
			}

			/*
			 * All re-attempts are used up and there is still no valid
			 * authorization?
			 */
			if (challenge.getStatus() != Status.VALID) {
				String msg = Tr.formatMessage(tc, "CWPKI2002E", directoryURI, authorization.getIdentifier().getDomain(),
						challenge.getStatus().toString(), (challengeRetries * challengeRetryWaitMs) + "ms");
				//Tr.error(tc, msg); TODO -- Do we want to log and throw?
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
	 * @param options
	 *            The certificate signing request options.
	 * @return The {@link X509Certificate} returned from the certificate
	 *         authority.
	 * @throws AcmeCaException
	 *             if there was an issue fetching the certificate.
	 */
	@FFDCIgnore({ IOException.class, AcmeException.class, AcmeRetryAfterException.class })
	public AcmeCertificate fetchCertificate(CSROptions csrOptions) throws AcmeCaException {

		/*
		 * Load the account key file. If there is no key file, create a new one.
		 */
		KeyPair accountKeyPair = loadOrCreateAccountKeyPair();

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
		KeyPair domainKeyPair = loadOrCreateDomainKeyPair();

		/*
		 * Order the certificate
		 */
		OrderBuilder orderBuilder = acct.newOrder();
		orderBuilder.domains(csrOptions.getDomains());
		if (csrOptions.getValidForMs() != null && csrOptions.getValidForMs() > 0) {
			orderBuilder.notAfter(Instant.now().plusMillis(csrOptions.getValidForMs()));
		}
		Order order;
		try {
			order = orderBuilder.create();
		} catch (AcmeException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2011E", directoryURI, e.getMessage()), e);
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
		csrb.addDomains(csrOptions.getDomains());

		/*
		 * Some CA's ignore these options, but set them anyway.
		 */
		if (csrOptions.getCountry() != null) {
			csrb.setCountry(csrOptions.getCountry());
		}
		if (csrOptions.getState() != null) {
			csrb.setState(csrOptions.getState());
		}
		if (csrOptions.getLocality() != null) {
			csrb.setLocality(csrOptions.getLocality());
		}
		if (csrOptions.getOrganization() != null) {
			csrb.setOrganization(csrOptions.getOrganization());
		}
		if (csrOptions.getOrganizationalUnit() != null) {
			csrb.setOrganizationalUnit(csrOptions.getOrganizationalUnit());
		}

		/*
		 * Sign the certificate signing request.
		 */
		try {
			csrb.sign(domainKeyPair);
		} catch (IOException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2012E", directoryURI, e.getMessage()), e);
		}
		Tr.debug(tc, "Certificate Signing Request: " + csrb.toString());

		/*
		 * Order the certificate
		 */
		try {
			order.execute(csrb.getEncoded());
		} catch (AcmeException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2013E", directoryURI, e.getMessage()), e);
		} catch (IOException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2014E", directoryURI, e.getMessage()), e);
		}

		/*
		 * Wait for the order to complete
		 */
		int attempts = orderRetries + 1;
		while (order.getStatus() != Status.VALID && attempts-- > 0) {
			/*
			 * Did the order fail?
			 */
			if (order.getStatus() == Status.INVALID) {
				String msg = Tr.formatMessage(tc, "CWPKI2001E", directoryURI,
						csrOptions.getDomains(), order.getStatus().toString(), order.getError().toString());
				throw new AcmeCaException(msg);
			}

			/*
			 * Wait to update the status.
			 */
			sleep(orderRetryWaitMs);

			/*
			 * Then update the status
			 */
			try {
				order.update();
			} catch (AcmeRetryAfterException e) {
				// TODO Wait until the moment defined in the returned Instant.
				// Instant when = e.getRetryAfter();
			} catch (AcmeException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2015E", directoryURI, e.getMessage()), e);
			}
		}

		/*
		 * All re-attempts are used up and there is still no valid order?
		 */
		if (order.getStatus() != Status.VALID) {
			String msg = Tr.formatMessage(tc, "CWPKI2004E", directoryURI, csrOptions.getDomains(),
					order.getStatus().toString(), (orderRetries * orderRetryWaitMs) + "ms");
			//Tr.error(tc, msg); TODO -- Do we want to log and throw?
			throw new AcmeCaException(msg);
		}

		/*
		 * Package up the pieces and parts needed to generate the personal
		 * certificate.
		 */
		Certificate certificate = order.getCertificate();
		return new AcmeCertificate(domainKeyPair, certificate.getCertificate(), certificate.getCertificateChain());
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
	private Account findExistingAccount(Session session, KeyPair accountKey) throws AcmeCaException {
		try {
			return new AccountBuilder().useKeyPair(accountKey).onlyExisting().create(session);
		} catch (AcmeServerException e) {
			return null;
		} catch (Exception e) {
			/*
			 * We want FFDC here as this will usually be the first communication
			 * we try with the ACME server. We want to capture why we failed.
			 */
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2016E", directoryURI, e.getMessage()), e);
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
		Account account = findExistingAccount(session, accountKey);

		/*
		 * If there is no existing account, create one.
		 */
		if (account == null) {
			/*
			 * Get the terms of service from the ACME server.
			 */
			URI tosURI = null;
			try {
				tosURI = session.getMetadata().getTermsOfService();
			} catch (AcmeException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2017E", directoryURI, e.getMessage()), e);
			}

			/*
			 * If the server provides terms of service, the account must accept
			 * them.
			 */
			if (tosURI == null) {
				Tr.debug(tc, "No terms of service provided");
			} else {
				Tr.audit(tc, "CWPKI2006I", directoryURI, tosURI);
			}

			/*
			 * Create the account using the account account key pair.
			 */
			AccountBuilder accountBuilder = new AccountBuilder().agreeToTermsOfService().useKeyPair(accountKey);

			/*
			 * Add any account contacts.
			 */
			if (accountContacts != null && !accountContacts.isEmpty()) {
				for (String contact : accountContacts) {
					accountBuilder.addContact(contact);
				}
			}

			/*
			 * Finally, create the account.
			 */
			try {
				account = accountBuilder.create(session);
			} catch (AcmeException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2018E", directoryURI, e.getMessage()), e);
			}
		}

		Tr.audit(tc, "CWPKI2019I", directoryURI, account.getLocation());
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

		File accountKeyFile = null;
		if (accountKeyFilePath != null) {
			accountKeyFile = new File(accountKeyFilePath);
		}

		if (accountKeyFile != null && accountKeyFile.exists()) {
			/*
			 * If there is a key file, read it
			 */
			try (FileReader fr = new FileReader(accountKeyFile)) {
				return KeyPairUtils.readKeyPair(fr);
			} catch (IOException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2021E", directoryURI, accountKeyFile, e.getMessage()), e);
			}
		}

		return null;
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
		if (domainKeyFilePath != null) {
			domainKeyFile = new File(domainKeyFilePath);
		}

		if (domainKeyFile != null && domainKeyFile.exists()) {
			/*
			 * If there is a key file, read it
			 */
			try (FileReader fr = new FileReader(domainKeyFile)) {
				return KeyPairUtils.readKeyPair(fr);
			} catch (IOException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2020E", directoryURI, domainKeyFile, e.getMessage()), e);
			}
		}

		return null;
	}

	/**
	 * Loads a account key pair from the user account key file. If the file does
	 * not exist, a new key pair is generated and saved.
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
	private KeyPair loadOrCreateAccountKeyPair() throws AcmeCaException {

		/*
		 * See if we have an account KeyPair already.
		 */
		KeyPair accountKeyPair = loadAccountKeyPair();

		/*
		 * If we don't have an account KeyPair already, generate one.
		 */
		if (accountKeyPair == null) {
			/*
			 * If there is none, create a new key pair and save it
			 */
			accountKeyPair = KeyPairUtils.createKeyPair(AcmeConstants.KEY_SIZE);

			File accountKeyFile = null;
			if (accountKeyFilePath != null) {
				accountKeyFile = new File(accountKeyFilePath);

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
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2023E",
							directoryURI, accountKeyFile, e.getMessage()), e);
				}
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
	private KeyPair loadOrCreateDomainKeyPair() throws AcmeCaException {

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
			if (domainKeyFilePath != null) {
				domainKeyFile = new File(domainKeyFilePath);

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
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2022E",
							directoryURI, domainKeyFile, e.getMessage()), e);
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
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2005E",
					directoryURI, Http01Challenge.TYPE));
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

		/*
		 * Load the account key file.
		 */
		KeyPair accountKeyPair = loadAccountKeyPair();
		if (accountKeyPair == null) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2025W", directoryURI));
		}

		/*
		 * Create a session to the ACME CA directory service.
		 */
		Session session = getNewSession();

		/*
		 * Get the Account.
		 */
		Account acct = findExistingAccount(session, accountKeyPair);

		if (acct != null) {
			/*
			 * Login and revoke the certificate.
			 */
			Login login = new Login(acct.getLocation(), accountKeyPair, session);
			try {
				Certificate.revoke(login, certificate, RevocationReason.UNSPECIFIED);
			} catch (AcmeException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2024E", directoryURI, e.getMessage()), e);
			}
		} else {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2026W", directoryURI));
		}
	}

	/**
	 * Set the accept terms of service response. If the ACME CA service has
	 * terms of service, this needs to be set to 'true'.
	 * 
	 * @param acceptTos
	 *            Whether to accept any terms of service issued by the ACME CA.
	 */
	public void setAcceptTos(Boolean acceptTos) {
		if (acceptTos != null) {
			this.termsOfServiceAgreed = acceptTos;
		}
	}

	/**
	 * Set the number of times to try to update a challenge before failing.
	 * 
	 * @param retries
	 *            The number of time to try to update a challenge.
	 */
	public void setChallengeRetries(Integer retries) {
		if (retries != null && retries >= 0) {
			this.challengeRetries = retries;
		}
	}

	/**
	 * Set the amount of time, in milliseconds, to wait to retry updating the
	 * challenge.
	 * 
	 * @param retryWaitMs
	 *            The time to wait before re-trying to update a challenge.
	 */
	public void setChallengeRetryWait(Long retryWaitMs) {
		if (retryWaitMs != null && retryWaitMs >= 0) {
			this.challengeRetryWaitMs = retryWaitMs;
		}
	}

	/**
	 * Set the number of times to try to update an order before failing.
	 * 
	 * @param retries
	 *            The number of time to try to update an order.
	 */
	public void setOrderRetries(Integer retries) {
		if (retries != null && retries >= 0) {
			this.orderRetries = retries;
		}
	}

	/**
	 * Set the amount of time, in milliseconds, to wait to retry updating the
	 * order.
	 * 
	 * @param retryWaitMs
	 *            The time to wait before re-trying to update an order.
	 */
	public void setOrderRetryWait(Long retryWaitMs) {
		if (retryWaitMs != null && retryWaitMs >= 0) {
			this.orderRetryWaitMs = retryWaitMs;
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
	 * Validate the key file path is usable.
	 * 
	 * @param path
	 *            The file path to verify.
	 * @param type
	 *            The key file type (account or domain). For logging only.
	 * @throws AcmeCaException
	 *             if the file path exists and is not readable or if the file
	 *             path does not exist and is not writable.
	 */
	private static void validateKeyFilePath(String path, String type) throws AcmeCaException {
		if (path == null || path.trim().isEmpty()) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2027E", type, path));
		}

		File file = new File(path);
		if (file.exists() && !file.canRead()) {
			if (type.equals(AcmeConstants.DOMAIN_TYPE)) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2020E", type, path));
			} else {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2021E", type, path));
			}
		}
		if (file.exists() && !file.canWrite()) {
			if (type.equals(AcmeConstants.DOMAIN_TYPE)) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2022E", type, path));
			} else {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2023E", type, path));
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
	@FFDCIgnore({ PrivilegedActionException.class })
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

						return new Session(AcmeClient.this.directoryURI);
					} finally {
						Thread.currentThread().setContextClassLoader(origLoader);
					}
				}
			});
		} catch (PrivilegedActionException e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Getting a new session failed for " + directoryURI + ", full stack trace is", e);
			}
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2028E", directoryURI), e.getCause()); // during test,
																										// the getCause
																										// provided
																										// better
																										// information
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
}
