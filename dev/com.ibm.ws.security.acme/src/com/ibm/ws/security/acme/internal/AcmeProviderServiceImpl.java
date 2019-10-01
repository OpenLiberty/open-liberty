/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.internal;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.security.KeyPair;
import java.security.Security;
import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.URI;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.util.concurrent.ExecutorService;

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

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;
import com.ibm.ws.security.acme.config.AcmeConfig;
import com.ibm.ws.security.acme.config.AcmeService;
import com.ibm.ws.security.acme.web.AcmeAuthorizationServices;

import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 * ACME certificate management support.
 */
@Component(service = { AcmeConfig.class, ServletContainerInitializer.class,
                       ServletContextListener.class }, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.security.acme.config", property = "service.vendor=IBM")
public class AcmeProviderServiceImpl implements AcmeConfig, ServletContextListener, ServletContainerInitializer {

    private final TraceComponent tc = Tr.register(AcmeProviderServiceImpl.class);
    private final String parameter1 = "JM-acme-parm1";
    private final String parameter2 = "JM-acme-parm2";

    private AcmeService serviceProvider;

    /**
     * The properties class that contain the attributes defined
     * by inside of server.xml.
     */
    private final Properties sessionProperties = new Properties();
    /**
     * Strings used to access the various attributes that are
     * defined in the <mailSession> and that are subsequently
     * extracted from the ComponentContext to be placed in the Properties
     * object
     */
    public static final String PARM1 = "configParm1";
    public static final String PARM2 = "configParm2";

    private final String propertiesArray[] = { PARM1, PARM2 };

    private final HashMap<String, Set<String>> appModules = new HashMap<String, Set<String>>();

    // File name of the User Key Pair
    private static final File USER_KEY_FILE = new File("user.key");

    // File name of the Domain Key Pair
    private static final File DOMAIN_KEY_FILE = new File("domain.key");

    // File name of the CSR
    private static final File DOMAIN_CSR_FILE = new File("domain.csr");

    // File name of the signed certificate
    private static final File DOMAIN_CHAIN_FILE = new File("domain-chain.crt");

    //Challenge type to be used
    private static final ChallengeType CHALLENGE_TYPE = ChallengeType.HTTP;

    // RSA key size of generated key pairs
    private static final int KEY_SIZE = 2048;

    private enum ChallengeType {
        HTTP, DNS
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        Tr.info(tc, "******* JTM ******* AcmeProviderServiceImpl: inside activate() method. Display input properties: ");

    }

    @Modified
    protected void modify(Map<String, Object> properties) {
        Tr.info(tc, " ******* JTM ******* AcmeProviderServiceImpl: inside modified () method");
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        Tr.info(tc, " ******* JTM ******* AcmeProviderServiceImpl: inside deactivate() method");
    }

    /** {@inheritDoc} */
    @Override
    public void onStartup(java.util.Set<java.lang.Class<?>> c, ServletContext ctx) throws ServletException {
        Tr.info(tc, " ******* JTM ******* AcmeProviderServiceImpl: entered ServletContext onStartup() method");
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent cte) {
        Tr.info(tc, "**** JTM **** AcmeProviderServiceImpl: entered ServletContextListener contextDestroyed() for application: " + cte.getServletContext().getServletContextName());
        // AcmeProviderServiceImpl.moduleStopped(appmodname);
    }

    /** {@inheritDoc} */
    @Override
    public void contextInitialized(ServletContextEvent cte) {
        Tr.info(tc, "******* JTM ******* AcmeProviderServiceImpl: entered ServletContextListener contextInitialized() for application: "
                    + cte.getServletContext().getServletContextName());
    }

    @Override
    public String getParameter1() {
        return parameter1;
    }

    @Override
    public String getParameter2() {
        return parameter2;
    }

    private final ConcurrentServiceReferenceMap<String, AcmeAuthorizationServices> acmeAuthServiceRef = new ConcurrentServiceReferenceMap<String, AcmeAuthorizationServices>("acmeAuthService");

    @Reference(service = AcmeAuthorizationServices.class, name = "com.ibm.ws.acme.web.AcmeAuthorizationServices", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)

    protected void setAcmeAuthService(ServiceReference<AcmeAuthorizationServices> ref) {
        synchronized (acmeAuthServiceRef) {
            Tr.info(tc, "AcmeProviderImpl: setAcmeAuth() Setting reference for " + ref.getProperty("acmeAuthID"));
            acmeAuthServiceRef.putReference((String) ref.getProperty("acmeAuthID"), ref);
        }
    }

    protected void unsetAcmeAuthService(ServiceReference<AcmeAuthorizationServices> ref) {
        synchronized (acmeAuthServiceRef) {
            Tr.info(tc, "AcmeProviderImpl: unsetAcmeAuth() Unsetting reference for " + ref.getProperty("acmeAuthID"));
            acmeAuthServiceRef.removeReference((String) ref.getProperty("acmeAuthID"), ref);
        }
    }

}
