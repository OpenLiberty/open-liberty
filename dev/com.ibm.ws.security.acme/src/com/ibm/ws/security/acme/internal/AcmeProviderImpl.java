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

import static com.ibm.ws.security.acme.internal.util.AcmeConstants.ACME_CONFIG_PID;
import static com.ibm.ws.security.acme.internal.util.AcmeConstants.DEFAULT_ALIAS;
import static com.ibm.ws.security.acme.internal.util.AcmeConstants.DEFAULT_KEY_STORE;
import static com.ibm.ws.security.acme.internal.util.AcmeConstants.KEY_KEYSTORE_SERVICE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.bouncycastle.asn1.x509.GeneralName;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeCertificate;
import com.ibm.ws.security.acme.AcmeProvider;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.security.acme.internal.web.AcmeAuthorizationServlet;
import com.ibm.ws.ssl.KeyStoreService;

/**
 * ACME 2.0 support component service.
 */
@Component(immediate = true, configurationPolicy = REQUIRE, configurationPid = ACME_CONFIG_PID, property = {
		"service.vendor=IBM", "includeAppsWithoutConfig=true" })
public class AcmeProviderImpl implements AcmeProvider, ApplicationStateListener {

	private final TraceComponent tc = Tr.register(AcmeProviderImpl.class);

	private final AtomicReference<KeyStoreService> keyStoreServiceRef = new AtomicReference<KeyStoreService>();

	private String directoryURI = null;
	private List<String> domains = null;
	private Long validFor = null;
	private String country = null;
	private String locality = null;
	private String state = null;
	private String organization = null;
	private String organizationalUnit = null;

	// Challenge and order related fields.
	private Integer challengeRetries = null;
	private Long challengeRetryWaitMs = null;
	private Integer orderRetries = null;
	private Long orderRetryWaitMs = null;

	// ACME account related fields.
	private String accountKeyFile = null;
	private List<String> accountContact = null;
	private Boolean acceptTermsOfService = null;
	private String domainKeyFile = null;

	private AcmeClient acmeClient = null;

	private final Lock lock = new ReentrantLock();
	private final Condition appStartedCondition = lock.newCondition();
	private boolean isAppStarted = false;

	@Activate
	public void activate(ComponentContext context, Map<String, Object> properties) throws AcmeCaException {
		final String methodName = "activate(ComponentContext, Map<String,Object>)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, context, properties);
		}

		initialize(context, properties);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	@Deactivate
	public void deactivate(ComponentContext context, int reason) {
		final String methodName = "deactivate(ComponentContext, Map<String,Object>)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, context, reason);
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	@Modified
	public void modify(ComponentContext context, Map<String, Object> properties) throws AcmeCaException {
		final String methodName = "modify(ComponentContext, Map<String,Object>)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, context, properties);
		}

		/*
		 * TODO We need to determine which configuration changes will result in
		 * requiring a certificate to be refreshed. Some that might trigger a
		 * refresh: validFor, directoryURI, country, locality, state,
		 * organization, organizationUnit
		 * 
		 * We can't necessarily just check the certificate, b/c they don't
		 * always honor them.
		 */

		initialize(context, properties);
		checkAndInstallCertificate(false, null, null, null);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	/**
	 * Initialize the {@link AcmeClient} instance that will be used to
	 * communicate with the ACME CA server.
	 * 
	 * @param context
	 *            The context passed in from DS.
	 * @param properties
	 *            Configuration properties.
	 * @throws AcmeCaException
	 *             If there was an issue initializing the {@link AcmeClient}.
	 */
	public void initialize(ComponentContext context, Map<String, Object> properties) throws AcmeCaException {
		final String methodName = "initialize(ComponentContext, Map<String,Object>)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, context, properties);
		}

		/*
		 * Retrieve the configuration.
		 */
		directoryURI = getStringValue(properties, AcmeConstants.DIR_URI);
		domains = getStringList(properties, AcmeConstants.DOMAIN);
		validFor = getLongValue(properties, AcmeConstants.VALID_FOR);
		country = getStringValue(properties, AcmeConstants.COUNTRY);
		locality = getStringValue(properties, AcmeConstants.LOCALITY);
		state = getStringValue(properties, AcmeConstants.STATE);
		organization = getStringValue(properties, AcmeConstants.ORG);
		organizationalUnit = getStringValue(properties, AcmeConstants.OU);
		challengeRetries = getIntegerValue(properties, AcmeConstants.CHALL_RETRIES);
		challengeRetryWaitMs = getLongValue(properties, AcmeConstants.CHALL_RETRY_WAIT);
		orderRetries = getIntegerValue(properties, AcmeConstants.ORDER_RETRIES);
		orderRetryWaitMs = getLongValue(properties, AcmeConstants.ORDER_RETRY_WAIT);
		accountKeyFile = getStringValue(properties, AcmeConstants.ACCOUNT_KEY_FILE);
		accountContact = getStringList(properties, AcmeConstants.ACCOUNT_CONTACT);
		acceptTermsOfService = getBooleanValue(properties, AcmeConstants.ACCEPT_TERMS);
		domainKeyFile = getStringValue(properties, AcmeConstants.DOMAIN_KEY_FILE);

		/*
		 * Construct a new ACME client.
		 */
		acmeClient = new AcmeClient(directoryURI, accountKeyFile, domainKeyFile, accountContact);
		acmeClient.setAcceptTos(acceptTermsOfService);
		acmeClient.setChallengeRetries(challengeRetries);
		acmeClient.setChallengeRetryWait(challengeRetryWaitMs);
		acmeClient.setOrderRetries(orderRetries);
		acmeClient.setOrderRetryWait(orderRetryWaitMs);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	@Override
	public void refreshCertificate() throws AcmeCaException {
		checkAndInstallCertificate(true, null, null, null);
	}

	/**
	 * Check the certificate, and install a new certificate generated by the
	 * ACME CA if the certificate needs to be replaced.
	 * 
	 * <p/>
	 * If <code>keyStore</code> is non-null, then both <code>keyStoreFile</code>
	 * and <code>password</code> should also be non-null. When these are
	 * non-null the method will use the certificate installed under the
	 * "default" alias in the input {@link KeyStore} as the currently installed
	 * certificate.
	 * 
	 * <p/>
	 * If the <code>keyStore</code> is null, the method will look up the
	 * certificate from the SSL configuration.
	 * 
	 * @param forceRefresh
	 *            Force refreshing of the certificate. Skip any checks used to
	 *            determine whether the certificate should be replaced.
	 * @param keyStore
	 *            {@link KeyStore} that contains the certificate under the
	 *            "default" alias.
	 * @param keyStoreFile
	 *            {@link KeyStore} file to update.
	 * @param password
	 *            The password for the {@link KeyStore}.
	 * @throws AcmeCaException
	 *             If there was an issue checking or updating the certificate.
	 */
	private void checkAndInstallCertificate(boolean forceRefresh, KeyStore keyStore, File keyStoreFile, String password)
			throws AcmeCaException {
		final String methodName = "checkAndInstallCertificate(KeyStore,File,String)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, keyStore, keyStoreFile, "******");
		}

		/*
		 * Wait until the ACME authorization web application is available. At
		 * this point, it always should be, but check just in case.
		 */
		waitUntilWebAppAvailable();

		/*
		 * Keep a reference to the existing certificate that we will replace so
		 * we can revoke it.
		 */
		X509Certificate existingCertificate = null;
		if (keyStore == null) {
			existingCertificate = getConfiguredDefaultCertificate();
		} else {
			try {
				existingCertificate = (X509Certificate) keyStore.getCertificate(DEFAULT_ALIAS);
			} catch (KeyStoreException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2029E", keyStoreFile, DEFAULT_ALIAS, e.getMessage()), e);
			}
		}

		/*
		 * Check whether we need a new certificate.
		 */
		AcmeCertificate acmeCertificate = checkAndRetrieveCertificate(existingCertificate, forceRefresh);

		if (acmeCertificate != null) {
			/*
			 * Convert the certificate chain to an array from a list.
			 */
			Certificate[] chainArr = convertChainToArray(acmeCertificate.getCertificateChain());

			/*
			 * Store the certificate chain for the default alias in the default
			 * keystore.
			 */
			try {
				if (keyStore == null) {
					getKeystoreService().setKeyEntryToKeyStore(DEFAULT_KEY_STORE, DEFAULT_ALIAS,
							acmeCertificate.getKeyPair().getPrivate(), chainArr);
				} else {
					keyStore.setKeyEntry(DEFAULT_ALIAS, acmeCertificate.getKeyPair().getPrivate(),
							password.toCharArray(), chainArr);
					keyStore.store(new FileOutputStream(keyStoreFile), password.toCharArray());
				}
			} catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException ex) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2030E", DEFAULT_ALIAS, DEFAULT_KEY_STORE, ex.getMessage()), ex);
			}

			/*
			 * Revoke the old certificate, which has now been replaced in the
			 * keystore.
			 */
			if (existingCertificate != null) {
				try {
					revoke(existingCertificate);
				} catch (AcmeCaException e) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, "Failed to revoke the certificate.", existingCertificate);
					}
				}
			}

			/*
			 * Finally, log a message indicate the new certificate has been
			 * installed.
			 * 
			 * TODO Use CWPKI0803A?
			 */
			Tr.audit(tc, "CWPKI2007I", directoryURI, acmeCertificate.getCertificate().getNotAfter());
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Previous certificate requested from ACME CA server is still valid.");
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	@Override
	public String getHttp01Authorization(String token) throws AcmeCaException {
		String methodName = "getHttp01Authorization(String)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, token);
		}

		String authorization = getAcmeClient().getHttp01Authorization(token);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName, authorization);
		}
		return authorization;
	}

	/**
	 * Revoke a certificate using an existing account on the ACME server. If the
	 * account key pair cannot be found, we will fail.
	 * 
	 * @param certificate
	 *            The certificate to revoke.
	 * @throws AcmeCaException
	 *             If there was an error revoking the certificate.
	 */
	public void revoke(X509Certificate certificate) throws AcmeCaException {
		String methodName = "revoke(X509Certificate)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, certificate);
		}

		getAcmeClient().revoke(certificate);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	/**
	 * Convenience method that will retrieve the {@link AcmeClient} instance or
	 * throw an {@link AcmeCaException} if the {@link AcmeClient} is null.
	 * 
	 * @return The {@link AcmeClient} instance to use.
	 * @throws AcmeCaException
	 *             If the {@link AcmeClient} instance is null.
	 */
	@Trivial
	private AcmeClient getAcmeClient() throws AcmeCaException {
		if (acmeClient == null) {
			throw new AcmeCaException("Internal error. ACME client was not initialized.");
		}
		return acmeClient;
	}

	/**
	 * Convenience method that will retrieve the {@link KeyStoreService}
	 * instance or throw an {@link AcmeCaException} if the
	 * {@link KeyStoreService} is null.
	 * 
	 * @return The {@link KeyStoreService} instance to use.
	 * @throws AcmeCaException
	 *             If the {@link KeyStoreService} instance is null.
	 */
	@Trivial
	private KeyStoreService getKeystoreService() throws AcmeCaException {
		if (keyStoreServiceRef.get() == null) {
			throw new AcmeCaException("Internal error. KeyStoreService was not registered.");
		}
		return keyStoreServiceRef.get();
	}

	/**
	 * Get a {@link Boolean} value from the config properties.
	 * 
	 * @param configProps
	 *            The configuration properties passed in by declarative
	 *            services.
	 * @param property
	 *            The property to lookup.
	 * @return The {@link Boolean} value, or null if it doesn't exist.
	 */
	private static Boolean getBooleanValue(Map<String, Object> configProps, String property) {
		Object value = configProps.get(property);
		if (value == null) {
			return null;
		}
		return (Boolean) value;
	}

	/**
	 * Get a {@link Integer} value from the config properties.
	 * 
	 * @param configProps
	 *            The configuration properties passed in by declarative
	 *            services.
	 * @param property
	 *            The property to lookup.
	 * @return The {@link Integer} value, or null if it doesn't exist.
	 */
	private static Integer getIntegerValue(Map<String, Object> configProps, String property) {
		Object value = configProps.get(property);
		if (value == null) {
			return null;
		}
		return (Integer) value;
	}

	/**
	 * Get a {@link Long} value from the config properties.
	 * 
	 * @param configProps
	 *            The configuration properties passed in by declarative
	 *            services.
	 * @param property
	 *            The property to lookup.
	 * @return The {@link Long} value, or null if it doesn't exist.
	 */
	private static Long getLongValue(Map<String, Object> configProps, String property) {
		Object value = configProps.get(property);
		if (value == null) {
			return null;
		}
		return (Long) value;
	}

	/**
	 * Get a {@link List} of values from an array stored in the config
	 * properties.
	 * 
	 * @param configProps
	 *            The configuration properties passed in by declarative
	 *            services.
	 * @param property
	 *            The property to lookup.
	 * @return The {@link List} value, or null if it doesn't exist.
	 */
	private static List<String> getStringList(Map<String, Object> configProps, String property) {

		Object value = configProps.get(property);
		if (value == null) {
			return null;
		}

		if (!(value instanceof String[])) {
			return null;
		}

		String[] array = (String[]) value;
		if (array.length == 0) {
			return null;
		}

		List<String> values = null;
		for (String item : array) {
			if (item != null && !item.trim().isEmpty()) {
				if (values == null) {
					values = new ArrayList<String>();
				}
				values.add(item);
			}
		}

		return values;
	}

	/**
	 * Get a {@link String} value from the config properties.
	 * 
	 * @param configProps
	 *            The configuration properties passed in by declarative
	 *            services.
	 * @param property
	 *            The property to lookup.
	 * @return The {@link String} value, or null if it doesn't exist.
	 */
	private static String getStringValue(Map<String, Object> configProps, String property) {
		Object value = configProps.get(property);
		if (value == null) {
			return null;
		}
		return (String) value;
	}

	@Reference(name = KEY_KEYSTORE_SERVICE, service = KeyStoreService.class, cardinality = ReferenceCardinality.MANDATORY)
	protected void setKeyStoreService(KeyStoreService keyStoreService) {
		final String methodName = "setKeyStoreService(KeyStoreService)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, keyStoreService);
		}

		keyStoreServiceRef.set(keyStoreService);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	protected void unsetKeyStoreService(KeyStoreService keyStoreService) {
		final String methodName = "unsetKeyStoreService(KeyStoreService)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, keyStoreService);
		}

		keyStoreServiceRef.compareAndSet(keyStoreService, null);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	/**
	 * Check the existing certificate and determine whether a new certificate is
	 * required.
	 * 
	 * @param existingCertificate
	 *            the existing certificate.
	 * @return true if a new certificate should be requested, false if the
	 *         existing certificate is still valid.
	 * @throws AcmeCaException
	 *             If there was an issue checking the existing certificate.
	 */
	private boolean isCertificateRequired(X509Certificate existingCertificate) throws AcmeCaException {
		final String methodName = "isCertificateRequired()";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName);
		}

		/**
		 * Check to see if we need to fetch a new certificate. Reasons may
		 * include:
		 * 
		 * <pre>
		 * 1. Certificate has not been fetched (does not exist)
		 * 2. Certificate is expired or about to expire.
		 * 3. Certificate has been revoked.
		 * 4. Certificate exists, but is for the wrong domain, or a new domain has been added.
		 * 5. TODO More?
		 * </pre>
		 */
		boolean certificateRequired = false;
		if (isCertificateExpired(existingCertificate)) {
			certificateRequired = true;
		} else if (isCertificateRevoked(existingCertificate)) {
			certificateRequired = true;
		} else if (hasWrongDomains(existingCertificate)) {
			certificateRequired = true;
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName, certificateRequired);
		}
		return certificateRequired;
	}

	/**
	 * Check if a new certificate is required and retrieve it if so.
	 * 
	 * @param existingCertificate
	 *            the existing certificate.
	 * @param forceRefresh
	 *            Force a refresh of the certificate.
	 * @return The {@link AcmeCertificate} containing the new certificate.
	 * @throws AcmeCaException
	 *             If there was an issue checking or retrieving the certificate.
	 */
	private AcmeCertificate checkAndRetrieveCertificate(X509Certificate existingCertificate, boolean forceRefresh)
			throws AcmeCaException {
		final String methodName = "checkAndRetrieveCertificate()";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName);
		}

		AcmeCertificate certificate = null;

		/*
		 * Check if we need to get a new certificate.
		 */
		if (forceRefresh || isCertificateRequired(existingCertificate)) {
			certificate = fetchCertificate();
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName, certificate);
		}
		return certificate;
	}

	/**
	 * Determine if the certificate has domains that no longer match the domains
	 * configured for the ACME feature. We will check that the certificate
	 * subjects common name (CN) and that the subject alternative DNSNames
	 * match.
	 * 
	 * @param certificate
	 *            The certificate to check.
	 * @return True if the certificate's domains do not match those that are
	 *         configured, false otherwise.
	 * @throws AcmeCaException
	 *             If there was an issue checking the certificate's domains.
	 */
	private boolean hasWrongDomains(X509Certificate certificate) throws AcmeCaException {
		String methodName = "hasWrongDomains(Certificate)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, certificate);
		}

		boolean hasWrongDomains = false;

		/*
		 * The common name better match one of the domains.
		 */
		try {
			LdapName dn = new LdapName(certificate.getSubjectX500Principal().getName());

			boolean cnMatches = false;
			for (Rdn rdn : dn.getRdns()) {
				if ("cn".equalsIgnoreCase(rdn.getType())) {
					for (String domain : domains) {
						if (domain.equalsIgnoreCase((String) rdn.getValue())) {
							cnMatches = true;
							break;
						}
					}
					break;
				}
			}

			if (!cnMatches) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, methodName,
							"The certificate subject's common name does not match any of the domains.");
				}
				hasWrongDomains = true;
			}
		} catch (InvalidNameException e) {
			throw new AcmeCaException(
					Tr.formatMessage(tc, "CWPKI2031E", certificate.getSubjectX500Principal().getName(),
							certificate.getSerialNumber(), e.getMessage()),
					e);
		}

		/*
		 * Check the subject alternative names for all of our domains. We are OK
		 * if it has more, but we will need to request a new certificate if it
		 * doesn't contain all of the domains.
		 */
		if (!hasWrongDomains) {
			try {
				Collection<List<?>> altNames = certificate.getSubjectAlternativeNames();
				Set<String> dnsNames = new HashSet<String>();
				if (altNames != null) {
					for (List<?> altName : altNames) {
						if (altName.size() < 2) {
							continue;
						}
						switch ((Integer) altName.get(0)) {
						case GeneralName.dNSName:
							Object data = altName.get(1);
							if (data instanceof String) {
								dnsNames.add((String) data);
							}
							break;
						default:
						}
					}
				}

				/*
				 * Check the configured domains against those retrieved from the
				 * certificate.
				 */
				if (!dnsNames.containsAll(domains)) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, methodName,
								"The certificate subject alternative names do not contain all of the configured domains.");
					}
					hasWrongDomains = true;
				}

			} catch (CertificateParsingException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2032E", certificate.getSerialNumber(), e.getMessage()), e);
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName, hasWrongDomains);
		}
		return hasWrongDomains;
	}

	/**
	 * Is the existing certificate expired or nearly expired?
	 * 
	 * @param certificate
	 *            The certificate to check.
	 * @return true if the certificate is expired or nearly expiring.
	 */
	private boolean isCertificateExpired(X509Certificate certificate) {
		String methodName = "isExpired(Certificate)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, certificate);
		}

		boolean isExpired = false;

		/*
		 * Certificates not after date.
		 */
		Date notAfter = certificate.getNotAfter();

		/*
		 * Get current date.
		 */
		Calendar cal = Calendar.getInstance();
		Date now = cal.getTime();

		/*
		 * Get a date where we want to refresh the certificate.
		 */
		cal.setTime(notAfter);
		cal.add(Calendar.DAY_OF_MONTH, -7); // TODO Hard-coded
		Date refreshDate = cal.getTime();

		/*
		 * Consider the certificate expired if the refresh date has elapsed.
		 */
		isExpired = now.compareTo(refreshDate) >= 0;

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName, isExpired);
		}
		return isExpired;
	}

	/**
	 * Has the certificate been revoked?
	 * 
	 * @param certificate
	 *            The certificate to check.
	 * @return True if the certificate has been revoked, false otherwise.
	 */
	private boolean isCertificateRevoked(X509Certificate certificate) {
		String methodName = "isRevoked(Certificate)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, certificate);
		}

		boolean isRevoked = false;

		// TODO Check CRLs and OSCPs

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName, isRevoked);
		}
		return isRevoked;
	}

	/**
	 * Fetch the certificate from the ACME CA server using the current
	 * configuration.
	 * 
	 * @return The {@link AcmeCertificate}, which contains the certificate chain
	 *         as well as the public and private keys used to sign the CSR
	 *         request.
	 * @throws AcmeCaException
	 */
	private AcmeCertificate fetchCertificate() throws AcmeCaException {

		String methodName = "fetchCertificate()";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName);
		}

		/*
		 * If we need to request a new certificate, generate a new certificate
		 * signing request options instance.
		 */
		CSROptions csrOptions = new CSROptions(domains);
		csrOptions.setCountry(country);
		csrOptions.setLocality(locality);
		csrOptions.setOrganization(organization);
		csrOptions.setOrganizationalUnit(organizationalUnit);
		csrOptions.setState(state);
		csrOptions.setValidForMs(validFor);

		/*
		 * Return the ACME certificate.
		 */
		AcmeCertificate certificate = getAcmeClient().fetchCertificate(csrOptions);
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName, certificate);
		}
		return certificate;
	}

	/**
	 * Convert a certificate chain that is in the form of a {@link List} into an
	 * array of {@link Certificate}s.
	 * 
	 * @param chainList
	 *            The {@link List} of certificates.
	 * @return An array of the same certificates.
	 */
	private static Certificate[] convertChainToArray(List<X509Certificate> chainList) {
		/*
		 * Convert the certificate chain to an array from a list.
		 */
		Certificate[] chainArray = new X509Certificate[chainList.size()];
		int idx = 0;
		for (Certificate x509cert : chainList) {
			chainArray[idx++] = x509cert;
		}
		return chainArray;
	}

	/**
	 * Get the current certificate for the default alias from the default
	 * keystore.
	 * 
	 * @return The {@link X509Certificate} that is stored under the default
	 *         alias in the default keystore.
	 * @throws AcmeCaException
	 */
	private X509Certificate getConfiguredDefaultCertificate() throws AcmeCaException {
		/*
		 * Get our existing certificate.
		 */
		try {
			return getKeystoreService().getX509CertificateFromKeyStore(DEFAULT_KEY_STORE, DEFAULT_ALIAS);
		} catch (KeyStoreException | CertificateException e) {
			throw new AcmeCaException(
					Tr.formatMessage(tc, "CWPKI2033E", DEFAULT_ALIAS, DEFAULT_KEY_STORE, e.getMessage()), e);
		}
	}

	/**
	 * Create the default keystore and populate the default alias with a
	 * certificate requested from the ACME CA server.
	 * 
	 * @param filePath
	 *            The path to generate the new keystore.
	 * @param password
	 *            The password for the generated keystore and certificate.
	 */
	@Override
	public File createDefaultSSLCertificate(String filePath, @Sensitive String password) throws CertificateException {

		String methodName = "createDefaultSSLCertificate(String,String)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, filePath, "******");
		}

		/*
		 * If we make it in here, Liberty is asking us to generate the default
		 * certificate. We need to not only generate the certificate but also
		 * the keystore itself.
		 * 
		 * First wait until the ACME authorization web application is available.
		 */
		try {
			waitUntilWebAppAvailable();
		} catch (AcmeCaException e) {
			throw new CertificateException(e.getMessage(), e);
		}

		/*
		 * Determine the keystore type we will use. This is the same behavior
		 * that the self-signed certificate generation uses.
		 *
		 * TODO Update the interface to take store type and remove this code.
		 */
		String setKeyStoreType = null;
		if (filePath.lastIndexOf(".") != -1) {
			setKeyStoreType = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());
		}
		if (setKeyStoreType == null || setKeyStoreType.equalsIgnoreCase("p12")) {
			setKeyStoreType = DefaultSSLCertificateCreator.DEFAULT_KEYSTORE_TYPE;
		}

		try {

			/*
			 * Get a new certificate.
			 */
			AcmeCertificate acmeCertificate = fetchCertificate();

			/*
			 * Create a new keystore instance.
			 */
			KeyStore keyStore = null;
			try {
				keyStore = KeyStore.getInstance(setKeyStoreType);
				keyStore.load(null, password.toCharArray());
				keyStore.setKeyEntry(DEFAULT_ALIAS, acmeCertificate.getKeyPair().getPrivate(), password.toCharArray(),
						convertChainToArray(acmeCertificate.getCertificateChain()));
			} catch (KeyStoreException | NoSuchAlgorithmException | IOException ee) {
				throw new CertificateException(
						Tr.formatMessage(tc, "CWPKI2034E", directoryURI, filePath, ee.getMessage()), ee);
			}

			File file = new File(filePath);

			try {
				/*
				 * Write the store to a file.
				 */

				if (file.getParentFile() != null && !file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				FileOutputStream fos = new FileOutputStream(file);
				keyStore.store(fos, password.toCharArray());

				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.exit(tc, methodName, file);
				}

			} catch (KeyStoreException | NoSuchAlgorithmException | IOException e) {
				throw new CertificateException(
						Tr.formatMessage(tc, "CWPKI2035E", directoryURI, file.getName(), e.getMessage()), e);
			}
			return file;
		} catch (AcmeCaException ace) {
			throw new CertificateException(ace.getMessage(), ace);
		}
	}

	/**
	 * Wait until the ACME authorization web application is available for
	 * service at /.well-known/acme-authorization.
	 * 
	 * @throws AcmeCaException
	 *             If the application is not available within the expected time.
	 */
	private void waitUntilWebAppAvailable() throws AcmeCaException {
		final String methodName = "waitUntilWebAppAvailable()";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName);
		}

		try {
			lock.lock();
			if (!isAppStarted) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.debug(tc, methodName + ": ACME authorization web application has not started - waiting.");
				}

				boolean signalled = false, keepWaiting = true;
				Calendar cal = Calendar.getInstance();
				int timeToWait = 2;
				cal.add(Calendar.MINUTE, timeToWait); // Wait 2 minutes, maximum
				while (keepWaiting) {
					try {
						keepWaiting = false;
						signalled = appStartedCondition.awaitUntil(cal.getTime());
					} catch (InterruptedException e) {
						keepWaiting = true;
					}
				}
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.debug(tc, methodName + ": Finished waiting.");
				}

				/*
				 * If the wait above expired and we weren't signaled by the
				 * applicationStarted(...) method, the ACME authorization web
				 * application did not start, we can't proceed.
				 */
				if (!signalled) {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2036E", timeToWait));
				} else if (!isAppStarted) {
					/*
					 * This should never happen, but throw an exception if it
					 * does.
					 */
					throw new AcmeCaException("ACME authorization web application did not start.");
				}

			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
					Tr.debug(tc, methodName + ": ACME authorization web application already started - not waiting.");
				}
			}
		} finally {
			lock.unlock();
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	@Override
	public void applicationStarting(ApplicationInfo appInfo) {
		final String methodName = "applicationStarting(ApplicationInfo)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, appInfo);
			Tr.exit(tc, methodName);
		}
		/*
		 * Ignore. The service and web application cannot be up without one
		 * another, so no need to do anything here.
		 */
	}

	@Override
	public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
		final String methodName = "applicationStarted(ApplicationInfo)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, appInfo);
		}

		if (AcmeAuthorizationServlet.APP_NAME.equals(appInfo.getName())) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
				Tr.event(tc,
						methodName + ": ACME authorization web application has started and is available for requests.");
			}

			lock.lock();
			try {
				isAppStarted = true;
				appStartedCondition.signalAll();
			} finally {
				lock.unlock();
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	@Override
	public void applicationStopping(ApplicationInfo appInfo) {
		final String methodName = "applicationStopping(ApplicationInfo)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, appInfo);
			Tr.exit(tc, methodName);
		}
		/*
		 * Ignore. The service and web application cannot be up without one
		 * another, so no need to do anything here.
		 */
	}

	@Override
	public void applicationStopped(ApplicationInfo appInfo) {
		final String methodName = "applicationStopped(ApplicationInfo)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, appInfo);
			Tr.exit(tc, methodName);
		}
		/*
		 * Ignore. The service and servlet cannot be up without one another, so
		 * no need to do anything here.
		 */
	}

	/*
	 * This method will be called during keystore initialization when the
	 * default keystore exists.
	 */
	@Override
	public void updateDefaultSSLCertificate(KeyStore keyStore, File keyStoreFile, @Sensitive String password)
			throws CertificateException {
		String methodName = "updateDefaultSSLCertificate(KeyStore,File,String)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, keyStore, keyStoreFile, "******");
		}

		try {
			checkAndInstallCertificate(false, keyStore, keyStoreFile, password);
		} catch (AcmeCaException e) {
			throw new CertificateException(e.getMessage(), e);
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}
}
