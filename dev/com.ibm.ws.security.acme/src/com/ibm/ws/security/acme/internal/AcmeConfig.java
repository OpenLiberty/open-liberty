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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.config.xml.internal.nester.Nester;
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
	private Long validFor = null;
	private String country = null;
	private String locality = null;
	private String state = null;
	private String organization = null;
	private String organizationalUnit = null;

	// Challenge and order related fields.
	private Integer challengeRetries = 10;
	private Long challengeRetryWaitMs = 5000L;
	private Integer orderRetries = 10;
	private Long orderRetryWaitMs = 3000L;

	// ACME account related fields.
	private String accountKeyFile = null;
	private List<String> accountContacts = null;
	private String domainKeyFile = null;

	// Transport related fields.
	private String protocol = null;
	private String trustStore = null;
	private SerializableProtectedString trustStorePassword = null;
	private String trustStoreType = null;

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

		validFor = getLongValue(properties, AcmeConstants.VALID_FOR);
		country = getStringValue(properties, AcmeConstants.COUNTRY);
		locality = getStringValue(properties, AcmeConstants.LOCALITY);
		state = getStringValue(properties, AcmeConstants.STATE);
		organization = getStringValue(properties, AcmeConstants.ORG);
		organizationalUnit = getStringValue(properties, AcmeConstants.OU);
		setChallengeRetries(getIntegerValue(properties, AcmeConstants.CHALL_RETRIES));
		setChallengeRetryWait(getLongValue(properties, AcmeConstants.CHALL_RETRY_WAIT));
		setOrderRetries(getIntegerValue(properties, AcmeConstants.ORDER_RETRIES));
		setOrderRetryWait(getLongValue(properties, AcmeConstants.ORDER_RETRY_WAIT));
		accountContacts = getStringList(properties, AcmeConstants.ACCOUNT_CONTACT);

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
	 * @return the domains
	 */
	public List<String> getDomains() {
		return domains;
	}

	/**
	 * @return the validFor
	 */
	public Long getValidFor() {
		return validFor;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @return the locality
	 */
	public String getLocality() {
		return locality;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @return the organization
	 */
	public String getOrganization() {
		return organization;
	}

	/**
	 * @return the organizationalUnit
	 */
	public String getOrganizationalUnit() {
		return organizationalUnit;
	}

	/**
	 * @return the challengeRetries
	 */
	public Integer getChallengeRetries() {
		return challengeRetries;
	}

	/**
	 * @return the challengeRetryWaitMs
	 */
	public Long getChallengeRetryWaitMs() {
		return challengeRetryWaitMs;
	}

	/**
	 * @return the orderRetries
	 */
	public Integer getOrderRetries() {
		return orderRetries;
	}

	/**
	 * @return the orderRetryWaitMs
	 */
	public Long getOrderRetryWaitMs() {
		return orderRetryWaitMs;
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
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
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
	 * @return the trustStore
	 */
	public String getTrustStore() {
		return trustStore;
	}

	/**
	 * @return the trustStorePassword
	 */
	public SerializableProtectedString getTrustStorePassword() {
		return trustStorePassword;
	}

	/**
	 * @return the trustStoreType
	 */
	public String getTrustStoreType() {
		return trustStoreType;
	}

	/**
	 * Set the number of times to try to update a challenge before failing.
	 * 
	 * @param retries
	 *            The number of time to try to update a challenge.
	 */
	@Trivial
	private void setChallengeRetries(Integer retries) {
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
	@Trivial
	private void setChallengeRetryWait(Long retryWaitMs) {
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
	@Trivial
	private void setOrderRetries(Integer retries) {
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
	@Trivial
	private void setOrderRetryWait(Long retryWaitMs) {
		if (retryWaitMs != null && retryWaitMs >= 0) {
			this.orderRetryWaitMs = retryWaitMs;
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
			String messageId = AcmeConstants.DOMAIN_TYPE.equals(type) ? "CWPKI2020E" : "CWPKI2021E";
			String cause = Tr.formatMessage(tc, "FILE_NOT_READABLE");
			throw new AcmeCaException(Tr.formatMessage(tc, messageId, path, cause));
		}
		if (file.exists() && !file.canWrite()) {
			String messageId = AcmeConstants.DOMAIN_TYPE.equals(type) ? "CWPKI2022E" : "CWPKI2023E";
			String cause = Tr.formatMessage(tc, "FILE_NOT_WRITABLE");
			throw new AcmeCaException(Tr.formatMessage(tc, messageId, path, cause));
		}
	}
}
