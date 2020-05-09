/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Configuration for the acmeCA-2.0 feature.
 */
public class AcmeConfig {
	private static final TraceComponent tc = Tr.register(AcmeConfig.class);

	private String directoryURI = null;
	private List<String> domains = null;
	private Long validForMs = null;
	private List<Rdn> subjectDN = new ArrayList<Rdn>();

	// Challenge and order related fields.
	private Long challengePollTimeoutMs = AcmeConstants.CHALLENGE_POLL_DEFAULT;
	private Long orderPollTimeoutMs = AcmeConstants.ORDER_POLL_DEFAULT;

	// ACME account related fields.
	private String accountKeyFile = null;
	private List<String> accountContacts = null;
	private String domainKeyFile = null;

	// Transport related fields.
	private String protocol = null;
	private String trustStore = null;
	private SerializableProtectedString trustStorePassword = null;
	private String trustStoreType = null;

	// Renew configuration options
	private Long renewBeforeExpirationMs = AcmeConstants.RENEW_DEFAULT_MS;
	private boolean autoRenewOnExpiration = true;

	// Revocation checker related fields.
	private URI ocspResponderUrl = null;
	private Boolean revocationCheckerEnabled = null;
	private Boolean preferCRLs = false;
	private Boolean disableFallback = false;

	// Certificate checker configuration options, currently intended to be internal only
	private Long certCheckerScheduler = AcmeConstants.SCHEDULER_MS;
	private Long certCheckerErrorScheduler = AcmeConstants.SCHEDULER_ERROR_MS;

	// Allow back to back renew requests, currently intended to be internal use only
	private boolean disableMinRenewWindow = false;

	// Disable certificate renewal when the acmeca-history file does not yet exist
	private boolean disableRenewOnNewHistory = false;
	
	/**
	 * Create a new {@link AcmeConfig} instance.
	 * 
	 * @param properties
	 *            The configuration properties passed in from declarative
	 *            services.
	 * @throws AcmeCaException
	 *             if there is a configuration error
	 */
	public AcmeConfig(Map<String, Object> properties) throws AcmeCaException {
		/*
		 * Directory URI must be valid.
		 */
		directoryURI = getStringValue(properties, AcmeConstants.DIR_URI);
		if (directoryURI == null || directoryURI.trim().isEmpty()) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2008E", directoryURI));
		}

		/*
		 * Validate the domains. We must have some valid domains.
		 */
		domains = new ArrayList<String>();
		List<String> tempDomains = getStringList(properties, AcmeConstants.DOMAIN);
		if (tempDomains != null && !tempDomains.isEmpty()) {
			for (String domain : tempDomains) {
				if (domain != null && !domain.trim().isEmpty()) {
					domains.add(domain);
				}
			}
		}
		if (domains.isEmpty()) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2037E"));
		}

		setValidFor(getLongValue(properties, AcmeConstants.VALID_FOR));
		processSubjectDN(getStringValue(properties, AcmeConstants.SUBJECT_DN));
		Long temp = getLongValue(properties, AcmeConstants.CHALL_POLL_TIMEOUT);
		challengePollTimeoutMs = Math.max(0, (temp == null) ? AcmeConstants.CHALLENGE_POLL_DEFAULT : temp);
		temp = getLongValue(properties, AcmeConstants.ORDER_POLL_TIMEOUT);
		orderPollTimeoutMs = Math.max(0, (temp == null) ? AcmeConstants.ORDER_POLL_DEFAULT : temp);
		accountContacts = getStringList(properties, AcmeConstants.ACCOUNT_CONTACT);
		setCertCheckerScheduler(getLongValue(properties, AcmeConstants.CERT_CHECKER_SCHEDULE));
		setCertCheckerErrorScheduler(getLongValue(properties, AcmeConstants.CERT_CHECKER_ERROR_SCHEDULE));

		/*
		 * Validate key file paths.
		 */
		accountKeyFile = getStringValue(properties, AcmeConstants.ACCOUNT_KEY_FILE);
		validateKeyFilePath(accountKeyFile, AcmeConstants.ACCOUNT_TYPE);

		domainKeyFile = getStringValue(properties, AcmeConstants.DOMAIN_KEY_FILE);
		validateKeyFilePath(domainKeyFile, AcmeConstants.DOMAIN_TYPE);

		/*
		 * Get transport configuration.
		 */
		List<Map<String, Object>> transportConfig = Nester.nest(AcmeConstants.TRANSPORT_CONFIG, properties);
		if (!transportConfig.isEmpty()) {
			Map<String, Object> transportProps = transportConfig.get(0);

			protocol = getStringValue(transportProps, AcmeConstants.TRANSPORT_PROTOCOL);
			trustStore = getStringValue(transportProps, AcmeConstants.TRANSPORT_TRUST_STORE);
			trustStorePassword = getSerializableProtectedStringValue(transportProps,
					AcmeConstants.TRANSPORT_TRUST_STORE_PASSWORD);
			trustStoreType = getStringValue(transportProps, AcmeConstants.TRANSPORT_TRUST_STORE_TYPE);
		}

		setRenewBeforeExpirationMs(getLongValue(properties, AcmeConstants.RENEW_BEFORE_EXPIRATION), true);
		disableMinRenewWindow = getBooleanValue(properties, AcmeConstants.DISABLE_MIN_RENEW_WINDOW, false);
		disableRenewOnNewHistory = getBooleanValue(properties, AcmeConstants.DISABLE_RENEW_ON_NEW_HISTORY, false);

		/*
		 * Get revocation checker configuration.
		 */
		List<Map<String, Object>> revocationChecker = Nester.nest(AcmeConstants.REVOCATION_CHECKER, properties);
		if (!revocationChecker.isEmpty()) {
			Map<String, Object> revocationProps = revocationChecker.get(0);

			/*
			 * The responder URL must be a valid URI.
			 */
			String url = getStringValue(revocationProps, AcmeConstants.REVOCATION_OCSP_RESPONDER_URL);
			if (url != null) {
				try {
					ocspResponderUrl = URI.create(url);
				} catch (IllegalArgumentException e) {
					throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2062E", url));
				}
			}

			revocationCheckerEnabled = getBooleanValue(revocationProps, AcmeConstants.REVOCATION_CHECKER_ENABLED);
			preferCRLs = getBooleanValue(revocationProps, AcmeConstants.REVOCATION_PREFER_CRLS);
			disableFallback = getBooleanValue(revocationProps, AcmeConstants.REVOCATION_DISABLE_FALLBACK);
		}
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
	@Trivial
	private static Boolean getBooleanValue(Map<String, Object> configProps, String property, boolean outcomeOnNull) {
		Object value = configProps.get(property);
		if (value == null) {
			return outcomeOnNull;
		}
		return (Boolean) value;
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
	@Trivial
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
	@Trivial
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
	@Trivial
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
	@Trivial
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
	@Trivial
	private static SerializableProtectedString getSerializableProtectedStringValue(Map<String, Object> configProps,
			String property) {
		Object value = configProps.get(property);
		if (value == null) {
			return null;
		}
		return (SerializableProtectedString) value;
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
	@Trivial
	private static String getStringValue(Map<String, Object> configProps, String property) {
		Object value = configProps.get(property);
		if (value == null) {
			return null;
		}
		return (String) value;
	}

	/**
	 * @return the directoryURI
	 */
	public String getDirectoryURI() {
		return directoryURI;
	}

	/**
	 * @return the disableFallback
	 */
	public Boolean isDisableFallback() {
		return (disableFallback == null) ? false : disableFallback;
	}

	/**
	 * @return the domains
	 */
	public List<String> getDomains() {
		return domains;
	}

	/**
	 * @return the validForMs
	 */
	public Long getValidForMs() {
		return validForMs;
	}

	/**
	 * @return the challengePollTimeoutMs
	 */
	public Long getChallengePollTimeoutMs() {
		return challengePollTimeoutMs;
	}

	/**
	 * @return the ocspResponderUrl
	 */
	public URI getOcspResponderUrl() {
		return ocspResponderUrl;
	}

	/**
	 * @return the orderPollTimeoutMs
	 */
	public Long getOrderPollTimeoutMs() {
		return orderPollTimeoutMs;
	}

	/**
	 * @return the preferCRLs
	 */
	public Boolean isPreferCrls() {
		return (preferCRLs == null) ? false : preferCRLs;
	}

	/**
	 * @return the accountKeyFile
	 */
	public String getAccountKeyFile() {
		return accountKeyFile;
	}

	/**
	 * @return the accountContacts
	 */
	public List<String> getAccountContacts() {
		return accountContacts;
	}

	/**
	 * @return the domainKeyFile
	 */
	public String getDomainKeyFile() {
		return domainKeyFile;
	}

	/**
	 * @return the revocationCheckerEnabled
	 */
	public Boolean isRevocationCheckerEnabled() {
		return (revocationCheckerEnabled == null) ? true : revocationCheckerEnabled;
	}

	/**
	 * Get the {@link SSLConfig} object that contains the user-specified SSL
	 * configuration.
	 * 
	 * @return The {@link SSLConfig}.
	 */
	public SSLConfig getSSLConfig() {
		SSLConfig sslConfig = new SSLConfig();

		/*
		 * Set any configured SSL properties into the SSLConfig instance.
		 */
		if (protocol != null) {
			sslConfig.setProperty(Constants.SSLPROP_PROTOCOL, protocol);
		}
		if (trustStore != null) {
			sslConfig.setProperty(Constants.SSLPROP_TRUST_STORE, trustStore);
		}
		if (trustStorePassword != null) {
			sslConfig.setProperty(Constants.SSLPROP_TRUST_STORE_PASSWORD,
					String.valueOf(trustStorePassword.getChars()));
		}
		if (trustStoreType != null) {
			sslConfig.setProperty(Constants.SSLPROP_TRUST_STORE_TYPE, trustStoreType);
		}

		/*
		 * Always allow default certificates (CACERTS).
		 */
		sslConfig.setProperty(Constants.SSLPROP_USE_DEFAULTCERTS, "true");

		return sslConfig;
	}

	/**
	 * @return the subjectDN
	 */
	public List<Rdn> getSubjectDN() {
		return subjectDN;
	}

	/**
	 * Process the subject DN and by breaking it apart into an ordered list of
	 * RDNs while validating the format.
	 * 
	 * @param subjectDN
	 *            The subject distinguished name.
	 * @throws AcmeCaException
	 *             If there was an error processing the subject DN.
	 */
	@FFDCIgnore(InvalidNameException.class)
	private void processSubjectDN(String subjectDN) throws AcmeCaException {

		if (subjectDN != null && !subjectDN.isEmpty()) {

			/*
			 * Parse the subject DN.
			 */
			try {
				this.subjectDN.addAll(new LdapName(subjectDN).getRdns());
				Collections.reverse(this.subjectDN);

				for (int idx = 0; idx < this.subjectDN.size(); idx++) {
					Rdn rdn = this.subjectDN.get(idx);
					switch (rdn.getType().toLowerCase()) {
					case "cn":
						if (idx == 0) {
							/*
							 * The CN RDN value must match one of the domains.
							 */
							String cnValue = (String) rdn.getValue();
							int dIdx = -1;
							for (int jdx = 0; jdx < domains.size(); jdx++) {
								if (domains.get(jdx).equalsIgnoreCase(cnValue)) {
									dIdx = jdx;
									break;
								}
							}
							if (dIdx == -1) {
								throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2039E", subjectDN, cnValue));
							}

							/*
							 * Put the CN domain first in the list of domains so
							 * the CSR uses it as the CN for the certificate.
							 */
							String domain = domains.remove(dIdx);
							domains.add(0, domain);
						} else {
							/*
							 * If defined, the CN RDN must be the first RDN.
							 */
							throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2040E", (Object[]) null));
						}
					case "o":
					case "ou":
					case "c":
					case "st":
					case "l":
						break;
					default:
						/*
						 * Invalid RDN type.
						 */
						throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2041E", rdn.getType()));
					}
				}
			} catch (InvalidNameException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2042E", subjectDN, e.getMessage()), e);
			}
		}

		/*
		 * Prepend the 'cn' if it is not already included.
		 */
		if (this.subjectDN.isEmpty() || !"cn".equalsIgnoreCase(this.subjectDN.get(0).getType())) {
			try {
				this.subjectDN.add(0, new Rdn("cn", domains.get(0)));
			} catch (InvalidNameException e) {
				throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2043E", "cn=" + domains.get(0), e.getMessage()),
						e);
			}
		}
	}

	/**
	 * Set the amount of time before certificate expiration to renew the
	 * certificate
	 * 
	 * @param retries
	 *            The number of time to try to update a challenge.
	 */
	@Trivial
	protected void setRenewBeforeExpirationMs(Long ms, boolean printWarning) {
		autoRenewOnExpiration = true;
		if (ms != null) {
			if (ms <= 0) {
				/*
				 * disable auto renew
				 */
				this.renewBeforeExpirationMs = 0L;
				autoRenewOnExpiration = false;
				if (tc.isDebugEnabled()) {
					Tr.debug(tc,
							"Auto renewal of the certificate is disabled, renewBeforeExpirationMs was configured to "
									+ ms);
				}
			} else if (ms < AcmeConstants.RENEW_CERT_MIN) {
				/*
				 * too low of a timeout, reset to the min rewew allowed
				 */
				this.renewBeforeExpirationMs = AcmeConstants.RENEW_CERT_MIN;
				Tr.warning(tc, "CWPKI2051W", ms + "ms", AcmeConstants.RENEW_CERT_MIN + "ms");
			} else {
				this.renewBeforeExpirationMs = ms;

				if (printWarning) {
					if (ms < AcmeConstants.RENEW_CERT_MIN_WARN_LEVEL) {
						/*
						 * we have a really low time configured. Allow it, but
						 * print a general warning.
						 */
						Tr.warning(tc, "CWPKI2055W", renewBeforeExpirationMs + "ms");
					}
				}
			}
		}
	}

	/**
	 * Set the amount of time, in milliseconds, the certificate should be valid
	 * for.
	 * 
	 * @param validForMs
	 *            The time the certificate should be valid for.
	 */
	@Trivial
	private void setValidFor(Long validForMs) {
		if (validForMs != null && validForMs >= 0) {
			this.validForMs = validForMs;
		}
	}

	/**
	 * @return the renewBeforeExpirationMs
	 */
	public Long getRenewBeforeExpirationMs() {
		return renewBeforeExpirationMs;
	}

	/**
	 * If renewBeforeExpiration is set to zero or less, automatic renewal on
	 * certificate expiration is disabled.
	 */
	public boolean isAutoRenewOnExpiration() {
		return autoRenewOnExpiration;
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
			String messageId = AcmeConstants.DOMAIN_TYPE.equals(type) ? "CWPKI2020E" : "CWPKI2021E";
			String cause = Tr.formatMessage(tc, "FILE_NOT_READABLE");
			throw new AcmeCaException(Tr.formatMessage(tc, messageId, path, cause));
		}

		if (!file.exists()) {
			File parentFile = file;
			while ((parentFile = parentFile.getParentFile()) != null) {
				if (parentFile.exists() && !parentFile.canWrite()) {
					String messageId = AcmeConstants.DOMAIN_TYPE.equals(type) ? "CWPKI2022E" : "CWPKI2023E";
					String cause = Tr.formatMessage(tc, "FILE_NOT_WRITABLE");
					throw new AcmeCaException(Tr.formatMessage(tc, messageId, path, cause));
				} else if (parentFile.exists()) {
					break;
				}
			}
		}
	}
	
	/**
	 * 
	 * @return the certCheckerScheduler
	 */
	@Trivial
	public Long getCertCheckerScheduler() {
		return certCheckerScheduler;
	}

	/**
	 * Sets the certCheckerScheduler. If set to 0 or less, the certificate
	 * checker is considered disabled. If set below the min renew time, reset to the
	 * min renew time.
	 * @param certCheckerScheduler
	 */
	public void setCertCheckerScheduler(Long certCheckerScheduler) {		
		if (certCheckerScheduler != null) {
			if (certCheckerScheduler <= 0) {
				/*
				 * Cert Checker is disabled
				 */
				Tr.info(tc, "CWPKI2069I");
				this.certCheckerScheduler = 0L;
			} else if (certCheckerScheduler < AcmeConstants.RENEW_CERT_MIN) {
				/*
				 * Too low of a timeout, reset to the min renew allowed
				 */
				this.certCheckerScheduler = AcmeConstants.RENEW_CERT_MIN;
				Tr.warning(tc, "CWPKI2070W", certCheckerScheduler, this.certCheckerScheduler + "ms");
			} else { 
				this.certCheckerScheduler = certCheckerScheduler;
			}
		}
	}

	
	/**
	 * Get the certCheckerErrorScheduler
	 * @return certCheckerErrorScheduler
	 */
	@Trivial
	public Long getCertCheckerErrorScheduler() {
		return certCheckerErrorScheduler;
	}

	/**
	 * Set the certCheckerErrorScheduler
	 * If it is set below the min renewal amount, reset to the min renewal amount
	 *
	 * @param certCheckerErrorScheduler
	 */
	public void setCertCheckerErrorScheduler(Long certCheckerErrorScheduler) {
		if (certCheckerErrorScheduler != null) {
			if (certCheckerErrorScheduler < AcmeConstants.RENEW_CERT_MIN) {
				/*
				 * Too low of a timeout, reset to the min renew allowed
				 */
				this.certCheckerErrorScheduler = AcmeConstants.RENEW_CERT_MIN;
				Tr.warning(tc, "CWPKI2071W", certCheckerErrorScheduler, this.certCheckerErrorScheduler + "ms");
			} else { 
				this.certCheckerErrorScheduler = certCheckerErrorScheduler;
			}
		}
	}

	/**
	 * 
	 * @return disableMinRenewWindow
	 */
	@Trivial
	public boolean isDisableMinRenewWindow() {
		return disableMinRenewWindow;
	}
	
	/**
	 * 
	 * @return disableRenewOnNewHistory
	 */
	@Trivial
	public boolean isDisableRenewOnNewHistory() {
		return disableRenewOnNewHistory;
	}

}
