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

import static com.ibm.ws.security.acme.internal.util.AcmeConstants.DEFAULT_ALIAS;
import static com.ibm.ws.security.acme.internal.util.AcmeConstants.DEFAULT_KEY_STORE;
import static com.ibm.ws.security.acme.internal.util.AcmeConstants.KEY_KEYSTORE_SERVICE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.bouncycastle.asn1.x509.GeneralName;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeCertificate;
import com.ibm.ws.security.acme.AcmeProvider;
import com.ibm.ws.security.acme.internal.AcmeClient.AcmeAccount;
import com.ibm.ws.security.acme.internal.exceptions.CertificateRenewRequestBlockedException;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.ssl.JSSEProviderFactory;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * ACME 2.0 support component service.
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class AcmeProviderImpl implements AcmeProvider {

	private static final TraceComponent tc = Tr.register(AcmeProviderImpl.class);

	/** KeyStoreService to retrieve configured KeyStores from. */
	private final static AtomicReference<KeyStoreService> keyStoreServiceRef = new AtomicReference<KeyStoreService>();

	/**
	 * An {@link ApplicationStateListener} used to validate whether the ACME
	 * authorization web application has started.
	 */
	private final static AtomicReference<AcmeApplicationStateListener> applicationStateListenerRef = new AtomicReference<AcmeApplicationStateListener>();

	/**
	 * A scheduler service used to scheduler the {@link AcmeCertCheckerTask} for checking
	 * whether the certificate is expiring or revoked
	 */
	private final AtomicServiceReference<ScheduledExecutorService> scheduledExecutorServiceRef = new AtomicServiceReference<ScheduledExecutorService>("scheduledExecutorService");

	/** Client used to communicate with the ACME CA server. */
	private static AcmeClient acmeClient;

	/** Configuration for the ACME client. */
	private static AcmeConfig acmeConfig;

	/** Scheduled thread to check if the certificate is reaching expiration or is revoked **/
	private AcmeCertCheckerTask acmeCertChecker = null;
	
	/** Read/Write lock to prevent multiple processes from renewing the certificate at the same or similar time. **/
	private final ReadWriteLock rwRenewCertLock = new ReentrantReadWriteLock();
	
	/** The last time the certificate was renewed **/
	private long lastCertificateRenewalTimestamp = -1;
	
	private AcmeHistory acmeHistory = new AcmeHistory();
	
	/** Activate for the scheduler ref **/
	public void activate(ComponentContext cc) {
		scheduledExecutorServiceRef.activate(cc);
	}

	@Reference
	private WsLocationAdmin wslocation;
	
	@Override
	public void renewAccountKeyPair() throws AcmeCaException {
		acmeClient.renewAccountKeyPair();
	}

	@Override
	public void renewCertificate() throws AcmeCaException {
		checkAndInstallCertificate(true, null, null, null);
	}

	@Override
	public void revokeCertificate(String reason) throws AcmeCaException {
		revoke(getConfiguredDefaultCertificateChain(), reason);
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
	@FFDCIgnore({ AcmeCaException.class })
	private void checkAndInstallCertificate(boolean forceRefresh, KeyStore keyStore, File keyStoreFile,
			@Sensitive String password) throws AcmeCaException {
		
		acquireWriteLock();
		try {
		/*
		 * Wait until the ACME authorization web application is available. At
		 * this point, it always should be, but check just in case.
		 */
		applicationStateListenerRef.get().waitUntilWebAppAvailable();

		/*
		 * Keep a reference to the existing certificate chain that we will
		 * replace so we can revoke it.
		 */
		List<X509Certificate> existingCertChain = null;
		if (keyStore == null) {
			existingCertChain = getConfiguredDefaultCertificateChain();
		} else {
			try {
				existingCertChain = convertToX509CertChain(keyStore.getCertificateChain(DEFAULT_ALIAS));
			} catch (KeyStoreException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2029E", keyStoreFile, DEFAULT_ALIAS, e.getMessage()), e);
			}
		}

		/*
		 * Check whether we need a new certificate.
		 */
		AcmeCertificate acmeCertificate = checkAndRetrieveCertificate(existingCertChain, forceRefresh);

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
					keyStoreServiceRef.get().setKeyEntryToKeyStore(DEFAULT_KEY_STORE, DEFAULT_ALIAS,
							acmeCertificate.getKeyPair().getPrivate(), chainArr);
				} else {
					keyStore.setKeyEntry(DEFAULT_ALIAS, acmeCertificate.getKeyPair().getPrivate(),
							password.toCharArray(), chainArr);
					FileOutputStream fos = new FileOutputStream(keyStoreFile);
					try {
						keyStore.store(fos, password.toCharArray());
					} finally {
						fos.close();
					}
				}
			} catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException ex) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2030E", DEFAULT_ALIAS, DEFAULT_KEY_STORE, ex.getMessage()), ex);
			}
			
			/*
			 * Mark the timestamp for this renew request
			 */
			if (!acmeConfig.isDisableMinRenewWindow()) {
				lastCertificateRenewalTimestamp = System.currentTimeMillis();
			}

			/*
			 * Revoke the old certificate, which has now been replaced in the
			 * keystore.
			 */
			if (existingCertChain != null) {
				try {
					revoke(existingCertChain, "SUPERSEDED");
				} catch (AcmeCaException e) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, "Failed to revoke the certificate.", existingCertChain, e);
					}
				}
			}

			/*
			 * Finally, log a message indicate the new certificate has been
			 * installed.
			 */
			Tr.audit(tc, "CWPKI2007I", acmeCertificate.getCertificate().getSerialNumber().toString(16),
					acmeConfig.getDirectoryURI(),
					acmeCertificate.getCertificate().getNotAfter().toInstant().toString());
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Previous certificate requested from ACME CA server is still valid.");
			}
		}

		/*
		 * Start the certificate checker task, will cancel any existing tasks and restart
		 */
		acmeCertChecker.startCertificateChecker(getScheduledExecutorService());

		} finally {
			releaseWriteLock();
		}
	}

	@Override
	public String getHttp01Authorization(String token) throws AcmeCaException {
		return getAcmeClient().getHttp01Authorization(token);
	}

	/**
	 * Revoke a certificate using an existing account on the ACME server. If the
	 * account key pair cannot be found, we will fail.
	 * 
	 * @param certificateChain
	 *            The certificate chain with the leaf certificate to revoke.
	 * @param reason
	 *            The reason the certificate is being revoked. The following
	 *            reason are supported: UNSPECIFIED, KEY_COMPROMISE,
	 *            CA_COMPROMISE, AFFILIATION_CHANGED, SUPERSEDED,
	 *            CESSATION_OF_OPERATIONS, CERTIFICATE_HOLD, REMOVE_FROM_CRL,
	 *            PRIVILEGE_WITHDRAWN and AA_COMPROMISE. If null, the reason
	 *            "UNSPECIFIED" will be used.
	 * @throws AcmeCaException
	 *             If there was an error revoking the certificate.
	 */
	public void revoke(List<X509Certificate> certificateChain, String reason) throws AcmeCaException {
		acquireWriteLock();
		try {
			X509Certificate certificate = getLeafCertificate(certificateChain);
			if (certificate == null) {
				return;
			}
			//Check to see if the certificate is in the history file - it should be unless we are transitioning from self-signed to ACME. 
			//If the certificate isn't in the history file, use the configured directory URI.
			String directoryURI = acmeHistory.getDirectoryURI(certificate.getSerialNumber().toString(16));
			if (directoryURI == null) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "The certificate was not found in the AcmeHistory file. Use the configured directory URI to revoke.");
				}
				directoryURI = acmeConfig.getDirectoryURI();
			}
			getAcmeClient().revoke(certificate, reason, directoryURI);
		} finally {
			releaseWriteLock();
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
	 * Convenience method that will retrieve the {@link AcmeConfig}
	 * 
	 * @return The {@link AcmeConfig} instance to use.
	 * 
	 */
	@Trivial
	protected AcmeConfig getAcmeConfig() {
		return acmeConfig;
	}

	/**
	 * Set the {@link KeyStoreService} instance.
	 * 
	 * @param keyStoreService
	 *            {@link KeyStoreService} instance.
	 */
	@Reference(name = KEY_KEYSTORE_SERVICE, service = KeyStoreService.class, cardinality = ReferenceCardinality.MANDATORY)
	protected void setKeyStoreService(KeyStoreService keyStoreService) {
		keyStoreServiceRef.set(keyStoreService);
	}

	protected void unsetKeyStoreService(KeyStoreService keyStoreService) {
		keyStoreServiceRef.compareAndSet(keyStoreService, null);
	}

	/**
	 * Check the existing certificate and determine whether a new certificate is
	 * required.
	 * 
	 * @param existingCertChain
	 *            the existing certificate chain.
	 * @return true if a new certificate should be requested, false if the
	 *         existing certificate is still valid.
	 * @throws AcmeCaException
	 *             If there was an issue checking the existing certificate.
	 */
	private boolean isCertificateRequired(List<X509Certificate> existingCertChain) throws AcmeCaException {
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
		 * 
		 * Check revocation last, as it is the most expensive call since it will
		 * make call outs to the OCSP responder and CRL distribution points.
		 */
		boolean isExpired = false;
		if (isExpired(existingCertChain)) {
			X509Certificate x590Certificate = existingCertChain.get(0);
			if (acmeConfig.isAutoRenewOnExpiration()) {
				isExpired = true;
				Tr.info(tc, "CWPKI2052I", x590Certificate.getSerialNumber().toString(16),
						x590Certificate.getNotAfter().toInstant().toString(), acmeConfig.getDirectoryURI());
			} else {
				// log that the certificate is expired, even if we can't renew it
				Tr.warning(tc, "CWPKI2053W", x590Certificate.getSerialNumber().toString(16),
						x590Certificate.getNotAfter().toInstant().toString());

			}
		}

		return existingCertChain == null || existingCertChain.isEmpty() || isExpired
				|| hasWrongDomains(existingCertChain) || hasWrongSubjectRDNs(existingCertChain)
                                || isRevoked(existingCertChain);
	}

	/**
	 * Check if a new certificate is required and retrieve it if so.
	 * 
	 * @param existingCertChain
	 *            the existing certificate chain.
	 * @param forceRefresh
	 *            Force a refresh of the certificate.
	 * @return The {@link AcmeCertificate} containing the new certificate.
	 * @throws AcmeCaException
	 *             If there was an issue checking or retrieving the certificate.
	 */
	private AcmeCertificate checkAndRetrieveCertificate(List<X509Certificate> existingCertChain, boolean forceRefresh)
			throws AcmeCaException {
		/*
		 * Check if we need to get a new certificate.
		 */
		acquireWriteLock();
		try {
			if (forceRefresh || isCertificateRequired(existingCertChain)) {
				return fetchCertificate();
			}
		} finally {
			releaseWriteLock();
		}

		return null;
	}

	/**
	 * Convert an array of {@link Certificate} to a {@link List} of
	 * {@link X509Certificate}.
	 * 
	 * @param certChain
	 *            The certificate chain array to convert.
	 * @return The {@link List} of {@link X509Certificate}s.
	 * @throws AcmeCaException
	 *             If any of the certificates were not an instance of
	 *             {@link X509Certificate}.
	 */
	private List<X509Certificate> convertToX509CertChain(Certificate[] certChain) throws AcmeCaException {
		List<X509Certificate> x509Chain = new ArrayList<X509Certificate>();
		if (certChain != null) {
			for (Certificate cert : certChain) {
				if (cert instanceof X509Certificate) {
					x509Chain.add((X509Certificate) cert);
				} else {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2044E", cert.getType()));
				}
			}
		}
		return x509Chain;
	}

	/**
	 * Determine if the leaf certificate has domains that no longer match the
	 * domains configured for the ACME feature. We will check that the
	 * certificate subjects common name (CN) and that the subject alternative
	 * DNSNames match.
	 * 
	 * @param certificateChain
	 *            The certificate chain to check.
	 * @return True if the leaf certificate's domains do not match those that
	 *         are configured, false otherwise.
	 * @throws AcmeCaException
	 *             If there was an issue checking the leaf certificate's
	 *             domains.
	 */
	private boolean hasWrongDomains(List<X509Certificate> certificateChain) throws AcmeCaException {
		String methodName = "hasWrongDomains(List<X509Certificate>)";
		boolean hasWrongDomains = false;

		X509Certificate certificate = getLeafCertificate(certificateChain);
		if (certificate == null) {
			return false;
		}

		/*
		 * The common name better match one of the domains.
		 */
		try {
			LdapName dn = new LdapName(certificate.getSubjectX500Principal().getName());

			boolean cnMatches = false;
			for (Rdn rdn : dn.getRdns()) {
				if ("cn".equalsIgnoreCase(rdn.getType())) {
					for (String domain : acmeConfig.getDomains()) {
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
							certificate.getSerialNumber().toString(16), e.getMessage()),
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
				if (!dnsNames.containsAll(acmeConfig.getDomains())) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, methodName,
								"The certificate subject alternative names do not contain all of the configured domains.");
					}
					hasWrongDomains = true;
				}

			} catch (CertificateParsingException e) {
				throw new AcmeCaException(
						Tr.formatMessage(tc, "CWPKI2032E", certificate.getSerialNumber().toString(16), e.getMessage()),
						e);
			}
		}
		return hasWrongDomains;
	}

	/**
	 * Check if the leaf certificate's subject RDNs match the configured
	 * subjectDN.
	 * 
	 * <p/>
	 * Note that this isn't the best check. It is possible that the ACME CA
	 * server may not even honor the RDN's (beside the required CN).
	 * 
	 * @param certificateChain
	 *            The certificate chain to check.
	 * @return whether the leaf certificate's subject RDNs match the configured
	 *         subjectDN.
	 * @throws AcmeCaException
	 */
	private boolean hasWrongSubjectRDNs(List<X509Certificate> certificateChain) throws AcmeCaException {
		String methodName = "hasWrongSubjectRDNs(List<X509Certificate>)";
		boolean hasWrongSubjectRDNs = false;

		X509Certificate certificate = getLeafCertificate(certificateChain);
		if (certificate == null) {
			return false;
		}

		List<Rdn> configuredRdns = acmeConfig.getSubjectDN();
		List<Rdn> certRdns;
		try {
			certRdns = new LdapName(certificate.getSubjectX500Principal().getName()).getRdns();

		} catch (InvalidNameException e) {
			throw new AcmeCaException(
					Tr.formatMessage(tc, "CWPKI2031E", certificate.getSubjectX500Principal().getName(),
							certificate.getSerialNumber().toString(16), e.getMessage()),
					e);
		}

		/*
		 * Determine if the RDNs match between the configuration and the
		 * certificate.
		 */
		boolean rdnsMatch = true;
		if (certRdns.size() == 1) {
			/*
			 * If the certificate only has a single RDN, assume that the CA
			 * doesn't honor other RDNs besides CN.
			 */
			rdnsMatch = certRdns.get(0).equals(configuredRdns.get(0));
		} else if (certRdns.size() == configuredRdns.size()) {
			/*
			 * More than 1 RDN for both the configured subjedDN and the cert?
			 * Make sure they all match.
			 */
			for (int idx = 0; idx < certRdns.size(); idx++) {
				if (!certRdns.get(idx).equals(configuredRdns.get(idx))) {
					rdnsMatch = false;
					break;
				}
			}
		} else {
			rdnsMatch = false;
		}

		if (!rdnsMatch) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, methodName, "The certificate subject's RDNs do not match the configuration.");
			}
			hasWrongSubjectRDNs = true;
		}

		return hasWrongSubjectRDNs;
	}

	/**
	 * Is the existing leaf certificate expired or nearly expired?
	 * 
	 * @param certificateChain
	 *            The certificate chain to check.
	 * @return true if the leaf certificate is expired or nearly expiring.
	 */
	protected boolean isExpired(List<X509Certificate> certificateChain) {
		X509Certificate certificate = getLeafCertificate(certificateChain);
		if (certificate == null) {
			return false;
		}

		/*
		 * Certificate's not after date.
		 */
		Date notAfter = certificate.getNotAfter();

		/*
		 * Get current date.
		 */
		Calendar cal = Calendar.getInstance();
		Date now = cal.getTime();

		/*
		 * Get a date where we want to refresh the certificate.
		 * Convert to milliseconds. The cal.add method requires an int and we can overflow int
		 * with the getRenewBeforeExpirationMs
		 */
		long refreshTime = notAfter.getTime() - acmeConfig.getRenewBeforeExpirationMs();
		cal.setTimeInMillis(refreshTime);
		Date refreshDate = cal.getTime();

		if (tc.isDebugEnabled()) {
			Tr.debug(tc,
					"isExpired: notAfter: " + notAfter + ", calculated renew Date: " + refreshDate + ", compared to now: " + now);
		}
		/*
		 * Consider the certificate expired if the refresh date has elapsed.
		 */
		return now.compareTo(refreshDate) >= 0;
	}

	/**
	 * Has the certificate been revoked?
	 * 
	 * @param certificateChain
	 *            The certificate chain to check.
	 * @return True if the certificate has been revoked, false otherwise.
	 * @throws AcmeCaException
	 */
	protected boolean isRevoked(List<X509Certificate> certificateChain) throws AcmeCaException {
		CertificateRevocationChecker checker = new CertificateRevocationChecker(acmeConfig);
		return checker.isRevoked(certificateChain);
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
		/*
		 * Request a new certificate.
		 */
		return getAcmeClient().fetchCertificate(false);
	}

	/**
	 * Convert a certificate chain that is in the form of a {@link List} into an
	 * array of {@link Certificate}s.
	 * 
	 * @param chainList
	 *            The {@link List} of certificates.
	 * @return An array of the same certificates.
	 */
	@Trivial
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

	@Override
	public AcmeAccount getAccount() throws AcmeCaException {
		return acmeClient.getAccount();
	}

	/**
	 * Get the leaf certificate from the certificate chain.
	 * 
	 * @param certificateChain
	 *            The certificate chain.
	 * @return The leaf certificate.
	 */
	public static X509Certificate getLeafCertificate(List<X509Certificate> certificateChain) {
		if (certificateChain != null && !certificateChain.isEmpty()) {
			return certificateChain.get(0);
		}
		return null;
	}

	/**
	 * Get the current certificate for the default alias from the default
	 * keystore.
	 * 
	 * @return The {@link X509Certificate} chain that is stored under the
	 *         default alias in the default keystore or null if it does not
	 *         exist.
	 * @throws AcmeCaException
	 *             if there was an error getting the configured default cert
	 *             chain
	 */
	@FFDCIgnore({ CertificateException.class })
	protected List<X509Certificate> getConfiguredDefaultCertificateChain() throws AcmeCaException {
		/*
		 * Get our existing certificate.
		 */
		try {
			Certificate[] certChain = keyStoreServiceRef.get().getCertificateChainFromKeyStore(DEFAULT_KEY_STORE,
					DEFAULT_ALIAS);
			return convertToX509CertChain(certChain);
		} catch (KeyStoreException | CertificateException e) {
			return null;
		}
	}

	@Override
	public File createDefaultSSLCertificate(String filePath, @Sensitive String password, String keyStoreType,
			String keyStoreProvider) throws CertificateException {
		/*
		 * If we make it in here, Liberty is asking us to generate the default
		 * certificate. We need to not only generate the certificate but also
		 * the keystore itself.
		 * 
		 * First wait until the ACME authorization web application is available.
		 */
		try {
			applicationStateListenerRef.get().waitUntilWebAppAvailable();
		} catch (AcmeCaException e) {
			throw new CertificateException(e.getMessage(), e);
		}

		try {

			/*
			 * Get a new certificate.
			 */
			AcmeCertificate acmeCertificate = fetchCertificate();

			/*
			 * Create a new keystore instance.
			 */
			File file = createKeyStore(filePath, acmeCertificate, password, keyStoreType, keyStoreProvider);

			/*
			 * Mark the timestamp for this creation request
			 */
			if (!acmeConfig.isDisableMinRenewWindow()) {
				lastCertificateRenewalTimestamp = System.currentTimeMillis();
			}
			
			/*
			 * Create the acme file which holds certificate information and a record
			 * of directoryURIs. We use this to determine if the directoryURI has
			 * been updated. If so, we need to refresh the certificate.
			 */
			acmeHistory.updateAcmeFile(acmeCertificate, null, acmeConfig.getDirectoryURI(), acmeClient.getAccount().getLocation().toString(), wslocation);

			/*
			 * Finally, log a message indicate the new certificate has been
			 * installed and return the file.
			 */
			Tr.audit(tc, "CWPKI2007I", acmeCertificate.getCertificate().getSerialNumber().toString(16),
					acmeConfig.getDirectoryURI(),
					acmeCertificate.getCertificate().getNotAfter().toInstant().toString());

			/*
			 * Start the certificate checker task, will cancel any existing tasks and restart
			 */
			acmeCertChecker.startCertificateChecker(getScheduledExecutorService());

			return file;
		} catch (AcmeCaException ace) {
			createKeyStore(filePath, null, password, keyStoreType, keyStoreProvider);

			throw new CertificateException(ace.getMessage(), ace);
		} catch (Exception e) {
			/*
			 * Process an FFDC before we flow back to WSKeystore
			 */
			throw e;
		}
	}

	/**
	 * Create the keystore instance and return a file that points to the
	 * keystore.
	 * 
	 * @param filePath
	 *            The path to the keystore to create.
	 * @param acmeCertificate
	 *            The {@link AcmeCertificate} instance to insert into the
	 *            keystore. If null, the keystore will be empty.
	 * @param password
	 *            The passsword for the keystore.
	 * @param type
	 *            The keystore type.
	 * @param provider
	 *            The keystore provider.
	 * @return The keystore file.
	 * @throws CertificateException
	 *             If there was an issue creating the keystore.
	 */
	private File createKeyStore(String filePath, AcmeCertificate acmeCertificate, @Sensitive String password,
			String type, String provider) throws CertificateException {
		/*
		 * Create a new keystore instance.
		 */
		KeyStore keyStore;
		try {
			keyStore = JSSEProviderFactory.getInstance().getKeyStoreInstance(type, provider);
			keyStore.load(null, password.toCharArray());
			if (acmeCertificate != null) {
				keyStore.setKeyEntry(DEFAULT_ALIAS, acmeCertificate.getKeyPair().getPrivate(), password.toCharArray(),
						convertChainToArray(acmeCertificate.getCertificateChain()));
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | IOException | NoSuchProviderException ee) {
			throw new CertificateException(Tr.formatMessage(tc, "CWPKI2034E", ee.getMessage()), ee);
		}

		/*
		 * Write the store to a file.
		 */
		File file = new File(filePath);
		try {
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			FileOutputStream fos = new FileOutputStream(file);
			try {
				keyStore.store(fos, password.toCharArray());
			} finally {
				fos.close();
			}

		} catch (KeyStoreException | NoSuchAlgorithmException | IOException e) {
			throw new CertificateException(Tr.formatMessage(tc, "CWPKI2035E", file.getName(), e.getMessage()), e);
		}
		return file;
	}

	/*
	 * This method will be called during keystore initialization when the
	 * default keystore exists.
	 */
	@Override
	public void updateDefaultSSLCertificate(KeyStore keyStore, File keyStoreFile, @Sensitive String password)
			throws CertificateException {
		try {
			boolean dirURIChanged = acmeHistory.directoryURIChanged(acmeConfig.getDirectoryURI(), wslocation, acmeConfig.isDisableRenewOnNewHistory());
			checkAndInstallCertificate(dirURIChanged, keyStore, keyStoreFile, password);
			/*
			 * Update the acme file with the new directoryURI and certificate information.
			 * This only needs to be done if the URI has changed.
			 */
			if (dirURIChanged) {
				List<X509Certificate> existingCertChain = null;
				if (keyStore == null) {
					existingCertChain = getConfiguredDefaultCertificateChain();
				} else {
					try {
						existingCertChain = convertToX509CertChain(keyStore.getCertificateChain(DEFAULT_ALIAS));
					} catch (KeyStoreException e) {
						throw new AcmeCaException(
								Tr.formatMessage(tc, "CWPKI2029E", keyStoreFile, DEFAULT_ALIAS, e.getMessage()), e);
					}
				}
				acmeHistory.updateAcmeFile(getLeafCertificate(existingCertChain), acmeConfig.getDirectoryURI(), acmeClient.getAccount().getLocation().toString(), wslocation);
			}

		} catch (AcmeCaException e) {
			throw new CertificateException(e.getMessage(), e);
		} catch (Exception e) {
			/*
			 * Process an FFDC before we flow back to WSKeystore
			 */
			throw e;
		}
	}

	/**
	 * Get the {@link SSLConfig} object that contains the user-specified SSL
	 * configuration.
	 * 
	 * @return The {@link SSLConfig}.
	 */
	public static SSLConfig getSSLConfig() {
		return acmeConfig.getSSLConfig();
	}

	/**
	 * This method will receive the initial configuration from the
	 * {@link AcmeConfigService} and will behave much like the activate method
	 * would on a regular OSGi component.
	 * 
	 * @param acmeConfigService
	 *            The {@link AcmeConfigService} instance.
	 * @param properties
	 *            The initial properties.
	 */
	@Reference(cardinality = ReferenceCardinality.MANDATORY, updated = "updateAcmeConfigService")
	public void setAcmeConfigService(AcmeConfigService acmeConfigService, Map<String, Object> properties) {
		try {
			acmeConfig = new AcmeConfig(properties);
			acmeClient = new AcmeClient(acmeConfig);

			/*
			 * Update the account.
			 */
			acmeClient.updateAccount();

			/*
			 * Create a new certificate checker
			 */
			acmeCertChecker = new AcmeCertCheckerTask(this);

		} catch (AcmeCaException e) {
			Tr.error(tc, e.getMessage()); // AcmeCaExceptions are localized.
		}
	}

	/**
	 * Unset the {@link AcmeConfigService} instance.
	 * 
	 * @param acmeConfigService
	 *            the {@link AcmeConfigService} instance to unet.
	 */
	protected void unsetAcmeConfigService(AcmeConfigService acmeConfigService) {
		if (acmeCertChecker != null) {
			acmeCertChecker.stop();
		}
		acmeConfig = null;
		acmeClient = null;
	}

	/**
	 * This method will receive updated configuration from the
	 * {@link AcmeConfigService} and will behave much like the modified method
	 * would on a regular OSGi component.
	 * 
	 * @param acmeConfigService
	 *            The {@link AcmeConfigService} instance.
	 * @param properties
	 *            The updated properties.
	 */
	protected void updateAcmeConfigService(AcmeConfigService acmeConfigService, Map<String, Object> properties) {
		try {

			if (acmeCertChecker == null) {
				acmeCertChecker = new AcmeCertCheckerTask(this);
			}

			acmeConfig = new AcmeConfig(properties);
			acmeClient = new AcmeClient(acmeConfig);

			boolean dirURIChanged = acmeHistory.directoryURIChanged(acmeConfig.getDirectoryURI(), wslocation, acmeConfig.isDisableRenewOnNewHistory());
			checkAndInstallCertificate(dirURIChanged, null, null, null);
			
			/*
			 * Update the acme file with the new directoryURI and certificate information.
			 * This only needs to be done if the URI has changed.
			 */
			if (dirURIChanged) {
				acmeHistory.updateAcmeFile(getLeafCertificate(getConfiguredDefaultCertificateChain()), acmeConfig.getDirectoryURI(), acmeClient.getAccount().getLocation().toString(), wslocation);
			}

			/*
			 * Update the account.
			 */
			acmeClient.updateAccount();

		} catch (AcmeCaException e) {
			Tr.error(tc, e.getMessage()); // AcmeCaExceptions are localized.
		}
	}

	/**
	 * Set the {@link AcmeApplicationStateListener} reference.
	 * 
	 * @param acmeApplicationStateListener
	 *            the {@link AcmeApplicationStateListener} instance.
	 */
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	public void setAcmeApplicationStateListener(AcmeApplicationStateListener acmeApplicationStateListener) {
		applicationStateListenerRef.set(acmeApplicationStateListener);
	}
	
	/**
	 * Set the Scheduler service ref
	 */
	@Reference(name = "scheduledExecutorService", service = ScheduledExecutorService.class, target = "(deferrable=false)")
	protected void setScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
		scheduledExecutorServiceRef.setReference(ref);
	}

	/**
	 * Unset the scheduler ref and stop the certificate checker
	 */
	protected void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> ref) {
		if (acmeCertChecker != null) {
			acmeCertChecker.stop();
		}
		scheduledExecutorServiceRef.unsetReference(ref);
	}

	/**
	 * Get the scheduler ref
	 */
	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorServiceRef.getService();
	}

    /**
     * Acquire the writer lock. To be used to prevent concurrent certificate
     * renew or revoke requests. Must be used with releaseWriteLock
     */
    @Trivial
    void acquireWriteLock() {
		rwRenewCertLock.writeLock().lock();
    }
    
    /**
     * Release the writer lock. To be used to prevent concurrent certificate
     * renew or revoke requests. Must be used with acquireWriteLock
     */
    @Trivial
    void releaseWriteLock() {
    	rwRenewCertLock.writeLock().unlock();
    }
    
	/**
	 * Checks whether certificate renewal is allowed. It is allowed if:
	 * <li>Certificate renewal checking is disabled</li>
	 * <li>This is the first certificate request</li>
	 * <li>Enough time has passed since the last renewal</li>
	 * 
	 * If certificate renewal is not allowed, an exception is thrown.
	 */
	@Override
	public void checkCertificateRenewAllowed() throws CertificateRenewRequestBlockedException {
		long timeDiff = System.currentTimeMillis() - lastCertificateRenewalTimestamp;
		if (acmeConfig.isDisableMinRenewWindow() || lastCertificateRenewalTimestamp == -1
				|| (timeDiff >= acmeConfig.getRenewCertMin())) {
			return;
		}

		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Too soon to renew, last certificate renewal was " + lastCertificateRenewalTimestamp);
		}
		CertificateRenewRequestBlockedException cr = new CertificateRenewRequestBlockedException(
				"Too soon to renew, last certificate renewal was " + lastCertificateRenewalTimestamp,
				acmeConfig.getRenewCertMin() - timeDiff);
		throw cr;
	}
}
