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
import java.security.KeyPair;
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
import com.ibm.ws.security.acme.AcmeCaException;
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
			throw new AcmeCaException("The ACME CA's directory URI must be a valid URI.");
		}
		validateKeyFilePath(accountKeyFilePath, "account");
		validateKeyFilePath(domainKeyFilePath, "domain");

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
				throw new AcmeCaException("Error triggering challenge to ACME server.", e);
			}

			/*
			 * Poll for the challenge to complete.
			 */
			int attempts = challengeRetries + 1;
			while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
				// Did the authorization fail?
				if (challenge.getStatus() == Status.INVALID) {
					String msg = "ACME CA responded that the challenge failed for domain '"
							+ authorization.getIdentifier().getDomain() + "' with status " + Status.INVALID.toString()
							+ ".";
					Tr.error(tc, msg);
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
					throw new AcmeCaException("Error updating challenge status from ACME server.", e);
				}
			}

			/*
			 * All re-attempts are used up and there is still no valid
			 * authorization?
			 */
			if (challenge.getStatus() != Status.VALID) {
				String msg = "Timed out waiting for successful challenge for domain '"
						+ authorization.getIdentifier().getDomain() + "'. Status is "
						+ challenge.getStatus().toString();
				Tr.error(tc, msg);
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
	public AcmeCertificate fetchCertificate(CSROptions csrOptions) throws AcmeCaException {

		/*
		 * Load the account key file. If there is no key file, create a new one.
		 */
		KeyPair accountKeyPair = loadOrCreateAccountKeyPair();

		/*
		 * Create a session to the ACME CA directory service.
		 */
		Session session = new Session(this.directoryURI);

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
			throw new AcmeCaException("Error creating order for ACME server.", e);
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
			throw new AcmeCaException("Error signing the certificate signing request.", e);
		}
		Tr.debug(tc, "Certificate Signing Request: " + csrb.toString());

		/*
		 * Order the certificate
		 */
		try {
			order.execute(csrb.getEncoded());
		} catch (AcmeException e) {
			throw new AcmeCaException("Error ordering the certificate.", e);
		} catch (IOException e) {
			throw new AcmeCaException("Error getting the encoded certificate signing request.", e);
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
				String msg = "ACME CA responded that the challenge failed for domains " + csrOptions.getDomains()
						+ " with status " + Status.INVALID.toString() + ".";
				Tr.error(tc, msg);
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
				throw new AcmeCaException("Error updating the order status from the ACME server.", e);
			}
		}

		/*
		 * All re-attempts are used up and there is still no valid order?
		 */
		if (order.getStatus() != Status.VALID) {
			String msg = "Timed out waiting for successful order for domains " + csrOptions.getDomains()
					+ ". Status is " + order.getStatus().toString();
			Tr.error(tc, msg);
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
	private Account findExistingAccount(Session session, KeyPair accountKey) throws AcmeCaException {
		try {
			return new AccountBuilder().useKeyPair(accountKey).onlyExisting().create(session);
		} catch (AcmeServerException e) {
			return null;
		} catch (AcmeException e) {
			throw new AcmeCaException("Error requesting an existing account from ACME server.", e);
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
				throw new AcmeCaException("Error retrieving the terms of service from the ACME server.", e);
			}

			/*
			 * If the server provides terms of service, the account must accept
			 * them.
			 */
			if (tosURI != null && !termsOfServiceAgreed) {
				String msg = "The account must accept the terms of service by setting the ACME CA configuration to have a value of \"true\" for '"
						+ AcmeConstants.ACCEPT_TERMS
						+ "' in the server.xml. The terms of service can be found at the following URI: " + tosURI;
				Tr.error(tc, msg);
				throw new AcmeCaException(msg);
			} else if (tosURI != null) {
				/*
				 * Log that we are accepting the terms of the CA on behalf of
				 * the account.
				 */
				Tr.info(tc,
						"Accepted the ACME CA's terms of service on behalf of the account. See the terms of service here: "
								+ tosURI);
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
				throw new AcmeCaException("Error creating account on ACME server.", e);
			}
		}

		Tr.info(tc, "Fetched account from ACME CA. URL: " + account.getLocation());
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
				throw new AcmeCaException("Error reading domain key file.", e);
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
				throw new AcmeCaException("Error reading domain key pair file.", e);
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
			}
			if (accountKeyFile != null) {
				try (FileWriter fw = new FileWriter(accountKeyFile)) {
					KeyPairUtils.writeKeyPair(accountKeyPair, fw);
				} catch (IOException e) {
					throw new AcmeCaException("Error writing account key pair file.", e);
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
			}
			if (domainKeyFile != null) {
				try (FileWriter fw = new FileWriter(domainKeyFile)) {
					KeyPairUtils.writeKeyPair(domainKeyPair, fw);
				} catch (IOException e) {
					throw new AcmeCaException("Error writing account key pair file.", e);
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
		 * Find a single http-01 challenge
		 */
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
		if (challenge == null) {
			String msg = "Didn't find a " + Http01Challenge.TYPE + " challenge type.";
			Tr.error(tc, msg);
			throw new AcmeCaException(msg);
		}

		/*
		 * Save away the challenge.
		 */
		httpTokenToAuthzMap.put(challenge.getToken(), challenge.getAuthorization());

		Tr.debug(tc, "Prepared the following challenge with token '" + challenge.getToken() + "' and authorization '"
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
	public void revoke(X509Certificate certificate) throws AcmeCaException {

		/*
		 * Load the account key file.
		 */
		KeyPair accountKeyPair = loadAccountKeyPair();
		if (accountKeyPair == null) {
			throw new AcmeCaException(
					"Could not load account KeyPair. Revoking a certificate requires a valid account key pair.");
		}

		/*
		 * Create a session to the ACME CA directory service.
		 */
		Session session = new Session(this.directoryURI);

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
				throw new AcmeCaException("Error revoking certificate.", e);
			}
		} else {
			throw new AcmeCaException(
					"Unable to find account to revoke certificate.  Revoking a certificate requires a valid account.");
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
			throw new AcmeCaException("The " + type + " key file path must be valid.");
		}

		File file = new File(path);
		if (file.exists() && !file.canRead()) {
			throw new AcmeCaException("Cannot read existing " + type + " key file.");
		}
		if (file.exists() && !file.canWrite()) {
			throw new AcmeCaException("Cannot write to specified " + type + " key file location.");
		}
	}
}
