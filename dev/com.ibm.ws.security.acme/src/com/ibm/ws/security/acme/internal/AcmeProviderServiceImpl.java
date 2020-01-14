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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.acme.config.AcmeConfig;
import com.ibm.ws.security.acme.config.AcmeService;
import com.ibm.ws.security.acme.util.AcmeConstants;
import com.ibm.ws.security.acme.web.AcmeAuthorizationServices;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * ACME certificate management support.
 */
@Component(service = { AcmeConfig.class, ServletContainerInitializer.class,
		ServletContextListener.class }, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = AcmeConstants.ACME_PID, property = "service.vendor=IBM")
public class AcmeProviderServiceImpl implements AcmeConfig, ServletContextListener, ServletContainerInitializer {

    private final TraceComponent tc = Tr.register(AcmeProviderServiceImpl.class);

    private AcmeService serviceProvider;
    
	private String directoryURI;
	private String domain;
	private int validFor;
	private String country;
	private String locality;
	private String state;
	private String organization;
	private String organizationalUnit;

	// Challenge and order related fields.
	private int challengeRetries;
	private int challengeRetryWait;
	private int orderRetries;
	private int orderRetryWait;

	// ACME account related fields.
	private String accountKeyFile;
	private Set<String> accountContact;
	private boolean acceptTermsOfService;
	private String domainKeyFile;

	private AcmeClient acmeClient;

    private final HashMap<String, Set<String>> appModules = new HashMap<String, Set<String>>();

    @Activate
    public void activate(ComponentContext context, Map<String, Object> properties) {
		initialize(properties);
    }

    @Modified
    public void modify(Map<String, Object> properties) {
		initialize(properties);
    }

	public void initialize(Map<String, Object> configProps) {
		directoryURI = (String) configProps.get(AcmeConstants.DIR_URI);
		domain = (String) configProps.get(AcmeConstants.DOMAIN);
		validFor = ((Integer) configProps.get(AcmeConstants.VALID_FOR)).intValue();
		country = (String) configProps.get(AcmeConstants.COUNTRY);
		locality = (String) configProps.get(AcmeConstants.LOCALITY);
		state = (String) configProps.get(AcmeConstants.STATE);
		organization = (String) configProps.get(AcmeConstants.ORG);
		organizationalUnit = (String) configProps.get(AcmeConstants.OU);
		challengeRetries = ((Integer) configProps.get(AcmeConstants.VALID_FOR)).intValue();
		challengeRetryWait = ((Integer) configProps.get(AcmeConstants.VALID_FOR)).intValue();
		orderRetries = ((Integer) configProps.get(AcmeConstants.VALID_FOR)).intValue();
		orderRetryWait = ((Integer) configProps.get(AcmeConstants.VALID_FOR)).intValue();
		accountKeyFile = (String) configProps.get(AcmeConstants.ACCOUNT_KEY_FILE);
		accountContact = new HashSet<String>();
		accountContact.add((String) configProps.get(AcmeConstants.ACCOUNT_CONTACT));
		acceptTermsOfService = ((Boolean) configProps.get(AcmeConstants.ACCEPT_TERMS)).booleanValue();
		domainKeyFile = (String) configProps.get(AcmeConstants.DOMAIN_KEY_FILE);

		acmeClient = new AcmeClient(directoryURI, accountKeyFile, domainKeyFile, accountContact);
		acmeClient.setAcceptTos(acceptTermsOfService);
		acmeClient.setChallengeRetries(challengeRetries);
		acmeClient.setChallengeRetryWait(challengeRetryWait);
		acmeClient.setOrderRetries(orderRetries);
		acmeClient.setOrderRetryWait(orderRetryWait);

	}

    @Deactivate
    public void deactivate(ComponentContext context, int reason) {
    }

    /** {@inheritDoc} */
    @Override
    public void onStartup(java.util.Set<java.lang.Class<?>> c, ServletContext ctx) throws ServletException {
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent cte) {
        // AcmeProviderServiceImpl.moduleStopped(appmodname);
    }

    /** {@inheritDoc} */
    @Override
    public void contextInitialized(ServletContextEvent cte) {
    }

    private final ConcurrentServiceReferenceMap<String, AcmeAuthorizationServices> acmeAuthServiceRef = new ConcurrentServiceReferenceMap<String, AcmeAuthorizationServices>("acmeAuthService");

    @Reference(service = AcmeAuthorizationServices.class, name = "com.ibm.ws.security.acme.web.AcmeAuthorizationServices", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)

    protected void setAcmeAuthService(ServiceReference<AcmeAuthorizationServices> ref) {
        synchronized (acmeAuthServiceRef) {
			// Tr.info(tc, "AcmeProviderImpl: setAcmeAuth() Setting reference for " +
			// ref.getProperty("acmeAuthID"));
            acmeAuthServiceRef.putReference((String) ref.getProperty("acmeAuthID"), ref);
        }
    }

    protected void unsetAcmeAuthService(ServiceReference<AcmeAuthorizationServices> ref) {
        synchronized (acmeAuthServiceRef) {
			// Tr.info(tc, "AcmeProviderImpl: unsetAcmeAuth() Unsetting reference for " +
			// ref.getProperty("acmeAuthID"));
            acmeAuthServiceRef.removeReference((String) ref.getProperty("acmeAuthID"), ref);
        }
    }

	@Override
	public String getDirectoryURI() {
		return directoryURI;
	}

	@Override
	public String getDomain() {
		return domain;
	}

	@Override
	public int getValidFor() {
		return validFor;
	}

	@Override
	public String getCountry() {
		return country;
	}

	@Override
	public String getLocality() {
		return locality;
	}

	@Override
	public String getState() {
		return state;
	}

	@Override
	public String getOrganization() {
		return organization;
	}

	@Override
	public String getOrganizationalUnit() {
		return organizationalUnit;
	}

	@Override
	public int getChallengeRetries() {
		return challengeRetries;
	}

	@Override
	public int getChallengeRetryWait() {
		return challengeRetryWait;
	}

	@Override
	public int getOrderRetries() {
		return orderRetries;
	}

	@Override
	public int getOrderRetryWait() {
		return orderRetryWait;
	}

	@Override
	public String getAccountKeyFile() {
		return accountKeyFile;
	}

	@Override
	public Set<String> getAccountContact() {
		return accountContact;
	}

	@Override
	public boolean getAcceptTermsOfService() {
		return acceptTermsOfService;
	}

	@Override
	public String getDomainKeyFile() {
		return domainKeyFile;
	}

}
