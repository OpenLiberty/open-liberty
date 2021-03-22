/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.impl;


import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.config.provider.MapBasedConfiguration;
import org.opensaml.core.xml.config.XMLConfigurator;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;

import net.shibboleth.utilities.java.support.xml.BasicParserPool;

public class Activator implements BundleActivator {
    // Trace classes are in the spi, See com.ibm.ws.logging
    private static TraceComponent tc = Tr.register(Activator.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    private static XMLObjectProviderRegistry providerRegistry;

    /** List of default configuration files */
    private static final String[] XML_CONFIGS = {
                                                  "/default-config.xml",
                                                  "/schema-config.xml",
                                                  "/saml1-assertion-config.xml",
                                                  "/saml1-metadata-config.xml",
                                                  "/saml1-protocol-config.xml",
                                                  "/saml2-assertion-config.xml",
                                                  "/saml2-assertion-delegation-restriction-config.xml",
                                                  "/saml2-ecp-config.xml",
                                                  "/saml2-metadata-algorithm-config.xml",
                                                  "/saml2-metadata-config.xml",
                                                  "/saml2-metadata-idp-discovery-config.xml",
                                                  "/saml2-metadata-query-config.xml",
                                                  "/saml2-protocol-config.xml",
                                                  "/saml2-protocol-thirdparty-config.xml",
                                                  "/saml2-protocol-aslo-config.xml",
                                                  "/saml2-channel-binding-config.xml",
                                                  "/saml-ec-gss-config.xml",
                                                  "/signature-config.xml",
                                                  "/encryption-config.xml",
                                                  "/xacml20-context-config.xml",
                                                  "/xacml20-policy-config.xml",
                                                  "/xacml10-saml2-profile-config.xml",
                                                  "/xacml11-saml2-profile-config.xml",
                                                  "/xacml2-saml2-profile-config.xml",
                                                  "/xacml3-saml2-profile-config.xml"
    };

    static boolean bInit = false;

    /**
     * see org/opensaml/DefaultBootstrap.java
     */
    @Override
    public void start(BundleContext ctx) throws Exception {
        // Currently, we initialize it when the static boolean is not true
        if (!bInit) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Initializing the opensaml3 library...");
            }         
            Configuration configuration = new MapBasedConfiguration();
            ConfigurationService.setConfiguration(configuration);

            providerRegistry = new XMLObjectProviderRegistry();
            configuration.register(XMLObjectProviderRegistry.class, providerRegistry,
                                   ConfigurationService.DEFAULT_PARTITION_NAME);
            try {
                // We are not using org.apache.santuario.xmlsec
                // We will need to initialize the xmlsec with our own security

                Class<Configuration> clazz = Configuration.class;
                XMLConfigurator configurator = new XMLConfigurator();
                for (String config : XML_CONFIGS) {
                    try {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "handling config :" + config);
                        }
                        configurator.load(clazz.getResourceAsStream(config));
                    } catch (Exception e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "ERR handling config :" + config + e.getMessage());
                        }
                    }
                }
                // OpenSAMLUtil.initSamlEngine();

                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Thread thread = Thread.currentThread();
                thread.setContextClassLoader(InitializationService.class.getClassLoader());
                try {
                    InitializationService.initialize();
                } finally {
                    thread.setContextClassLoader(loader);
                }

                // We do not need to initialize the ArtifactBuilder until we want to build SAML output
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Initializing SAML20 Artifact builder when we have SAML output");
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "initialize Parser pool");
                }

                BasicParserPool pp = new BasicParserPool();
                pp.setNamespaceAware(true);
                pp.setMaxPoolSize(50);
                pp.initialize();
                providerRegistry.setParserPool(pp);

                //TODO:
                initializeGlobalSecurityConfiguration();

            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "unable to bootstrap the opensaml3 library - config Exception:" + e);
                }
            }
            bInit = true;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "opensaml3 library bootstrap complete");
            }
        }
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {}

    /**
     * Initializes the default global security configuration.
     */
    protected static void initializeGlobalSecurityConfiguration() {

    }

}
