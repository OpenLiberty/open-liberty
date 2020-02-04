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
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeProvider;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;

/**
 * ACME 2.0 support component service.
 */
@Component(immediate = true, configurationPolicy = REQUIRE, configurationPid = ACME_CONFIG_PID, property = "service.vendor=IBM")
public class AcmeProviderImpl implements AcmeProvider, ServletContextListener, ServletContainerInitializer {

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

	@Activate
	public void activate(ComponentContext context, Map<String, Object> properties) throws AcmeCaException {
		initialize(properties);
	}

	@Modified
	public void modify(Map<String, Object> properties) throws AcmeCaException {
		initialize(properties);
	}

	public void initialize(Map<String, Object> configProps) throws AcmeCaException {
		directoryURI = getStringValue(configProps, AcmeConstants.DIR_URI);
		domains = getStringList(configProps, AcmeConstants.DOMAIN);
		validFor = getLongValue(configProps, AcmeConstants.VALID_FOR);
		country = getStringValue(configProps, AcmeConstants.COUNTRY);
		locality = getStringValue(configProps, AcmeConstants.LOCALITY);
		state = getStringValue(configProps, AcmeConstants.STATE);
		organization = getStringValue(configProps, AcmeConstants.ORG);
		organizationalUnit = getStringValue(configProps, AcmeConstants.OU);
		challengeRetries = getIntegerValue(configProps, AcmeConstants.CHALL_RETRIES);
		challengeRetryWaitMs = getLongValue(configProps, AcmeConstants.CHALL_RETRY_WAIT);
		orderRetries = getIntegerValue(configProps, AcmeConstants.ORDER_RETRIES);
		orderRetryWaitMs = getLongValue(configProps, AcmeConstants.ORDER_RETRY_WAIT);
		accountKeyFile = getStringValue(configProps, AcmeConstants.ACCOUNT_KEY_FILE);
		accountContact = getStringList(configProps, AcmeConstants.ACCOUNT_CONTACT);
		acceptTermsOfService = getBooleanValue(configProps, AcmeConstants.ACCEPT_TERMS);
		domainKeyFile = getStringValue(configProps, AcmeConstants.DOMAIN_KEY_FILE);

		acmeClient = new AcmeClient(directoryURI, accountKeyFile, domainKeyFile, accountContact);
		acmeClient.setAcceptTos(acceptTermsOfService);
		acmeClient.setChallengeRetries(challengeRetries);
		acmeClient.setChallengeRetryWait(challengeRetryWaitMs);
		acmeClient.setOrderRetries(orderRetries);
		acmeClient.setOrderRetryWait(orderRetryWaitMs);
	}

	@Deactivate
	public void deactivate(ComponentContext context, int reason) {
		// TODO Do nothing?
	}

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
	}

	@Override
	public void contextDestroyed(ServletContextEvent cte) {
		// AcmeProviderServiceImpl.moduleStopped(appmodname);
	}

	@Override
	public void contextInitialized(ServletContextEvent cte) {
	}

	@Override
	public List<X509Certificate> checkAndRetrieveCertificate() throws AcmeCaException {

		/*
		 * TODO Default this to false when certificate check is completed.
		 */
		boolean requestCertificate = true;

		/**
		 * TODO Check to see if we need to fetch a new certificate. Reasons may
		 * include:
		 * 
		 * <pre>
		 * 1. Certificate has not been fetched (does not exist)
		 * 2. Certificate is expired.
		 * 3. Certificate has been revoked.
		 * 4. Certificate is about to expire.
		 * 5. Certificate exists, but is for the wrong domain, or a new domain has been added.
		 * 6. More?
		 * </pre>
		 */

		/*
		 * If we don't need to request a certificate, we are done. There is
		 * nothing to do.
		 */
		if (!requestCertificate) {
			return null;
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

		return getAcmeClient().fetchCertificate(csrOptions).getCertificateChain();
	}

	@Override
	public String getHttp01Authorization(String token) throws AcmeCaException {
		return getAcmeClient().getHttp01Authorization(token);
	}

	@Override
	public void revoke(X509Certificate certificate) throws AcmeCaException {
		getAcmeClient().revoke(certificate);
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
}
