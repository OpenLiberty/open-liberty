/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.impl;

import java.security.Provider;
import java.security.Security;

import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.config.provider.MapBasedConfiguration;
import org.opensaml.core.xml.config.XMLConfigurator;
import org.opensaml.core.xml.config.XMLObjectProviderInitializer;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.saml.config.SAMLConfiguration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

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
    private static String[] providerConfigs = {
                                                "/default-config.xml",
                                                "/schema-config.xml",
                                                "/signature-config.xml",
                                                "/signature-validation-config.xml",
                                                "/encryption-config.xml",
                                                "/encryption-validation-config.xml",
                                                "/soap11-config.xml",
                                                "/wsfed11-protocol-config.xml",
                                                "/saml1-assertion-config.xml",
                                                "/saml1-protocol-config.xml",
                                                "/saml1-core-validation-config.xml",
                                                "/saml2-assertion-config.xml",
                                                "/saml2-protocol-config.xml",
                                                "/saml2-core-validation-config.xml",
                                                "/saml1-metadata-config.xml",
                                                "/saml2-metadata-config.xml",
                                                "/saml2-metadata-validation-config.xml",
                                                "/saml2-metadata-idp-discovery-config.xml",
                                                "/saml2-protocol-thirdparty-config.xml",
                                                "/saml2-metadata-query-config.xml",
                                                "/saml2-assertion-delegation-restriction-config.xml",
                                                "/saml2-ecp-config.xml",
                                                "/xacml10-saml2-profile-config.xml",
                                                "/xacml11-saml2-profile-config.xml",
                                                "/xacml20-context-config.xml",
                                                "/xacml20-policy-config.xml",
                                                "/xacml2-saml2-profile-config.xml",
                                                "/xacml3-saml2-profile-config.xml",
                                                "/wsaddressing-config.xml",
                                                "/wssecurity-config.xml",
    };

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
                                                  /* "/saml2-metadata-attr-config.xml", */
                                                  "/saml2-metadata-config.xml",
                                                  "/saml2-metadata-idp-discovery-config.xml",
                                                  "/saml2-metadata-query-config.xml",
                                                  /*"/saml2-metadata-reqinit-config.xml",
                                                  "/saml2-metadata-ui-config.xml",
                                                  "/saml2-metadata-rpi-config.xml",*/
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
                    /* "/saml2-xacml2-profile.xml", */
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
//            
//            Thread thread = Thread.currentThread();
//            ClassLoader loader = thread.getContextClassLoader();
//            thread.setContextClassLoader(InitializationService.class.getClassLoader());try {
//               InitializationService.initialize();
//            } finally {
//               thread.setContextClassLoader(loader);
//            }
//            
            
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
                //for (String config : providerConfigs) { //@AV999
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

                //Classloading
//                BundleWiring bundleWiring = ctx.getBundle().adapt(BundleWiring.class);
//                ClassLoader loader = bundleWiring.getClassLoader();
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Thread thread = Thread.currentThread();
                thread.setContextClassLoader(InitializationService.class.getClassLoader());
                try {
                    InitializationService.initialize();
                    //org.opensaml.core.xmlsec.config.XMLObjectProviderInitializer init1 = 
                    //new org.opensaml.saml.config.XMLObjectProviderInitializer().init();
//                    XMLObjectProviderInitializer init1 = new XMLObjectProviderInitializer();
//                    init1.init();
//                    new org.opensaml.saml.config.impl.XMLObjectProviderInitializer().init();
//
//                    new org.opensaml.xmlsec.config.impl.XMLObjectProviderInitializer().init();
                } finally {
                    thread.setContextClassLoader(loader);
                }

                // We do not need to initialize the ArtifactBuilder until we want to build SAML output
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Initializing SAML20 Artifact builder when we have SAML output");
                }
                // Configuration.setSAML1ArtifactBuilderFactory(new SAML1ArtifactBuilderFactory());
                // Configuration.setSAML2ArtifactBuilderFactory(new SAML2ArtifactBuilderFactory());

//                SAMLConfiguration samlConfiguration = new SAMLConfiguration();
//                configuration.register(SAMLConfiguration.class, samlConfiguration, ConfigurationService.DEFAULT_PARTITION_NAME);
                for (Provider jceProvider : Security.getProviders()) { //logger.info(jceProvider.getInfo());
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "jce provider = ", jceProvider.getInfo());
                    }
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "initialize Parser pool");
                }

//                StaticBasicParserPool sbpp = new StaticBasicParserPool();
//                sbpp.setNamespaceAware(true);
//                sbpp.setMaxPoolSize(50); // TODO size of XMLParser: configurable?
//                try {
//                    sbpp.initialize();
//                } catch (XMLParserException e) {
//                    throw new ConfigurationException("Error initializing parser pool", e);
//                }
//                Configuration.setParserPool(sbpp);
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
        //BasicSecurityConfiguration basicSecurityConfiguration = DefaultSecurityConfigurationBootstrap.buildDefaultConfig();
        //basicSecurityConfiguration.setSignatureReferenceDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256");//SignatureConstants.ALGO_ID_DIGEST_SHA1);
        //Configuration.setGlobalSecurityConfiguration(basicSecurityConfiguration);
    }

}
