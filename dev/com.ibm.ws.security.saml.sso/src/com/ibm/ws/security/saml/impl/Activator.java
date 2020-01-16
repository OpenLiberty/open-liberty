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

import org.opensaml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLConfigurator;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.BasicSecurityConfiguration;
import org.opensaml.xml.security.DefaultSecurityConfigurationBootstrap;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;

public class Activator implements BundleActivator {
    // Trace classes are in the spi, See com.ibm.ws.logging
    private static TraceComponent tc = Tr.register(Activator.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

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

    static boolean bInit = false;

    /**
     * see org/opensaml/DefaultBootstrap.java
     */
    @Override
    public void start(BundleContext ctx) throws Exception {
        // Currently, we initialize it when the static boolean is not true
        if (!bInit) {
            try {
                // We are not using org.apache.santuario.xmlsec
                // We will need to initialize the xmlsec with our own security

                Class<Configuration> clazz = Configuration.class;
                XMLConfigurator configurator = new XMLConfigurator();
                for (String config : providerConfigs) {
                    try {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "handling config :" + config);
                        }
                        configurator.load(clazz.getResourceAsStream(config));
                    } catch (Exception e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "ERR handling config :" + config);
                        }
                    }
                }
                // OpenSAMLUtil.initSamlEngine();

                // We do not need to initialize the ArtifactBuilder until we want to build SAML output
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Initializing SAML20 Artifact builder when we have SAML output");
                }
                // Configuration.setSAML1ArtifactBuilderFactory(new SAML1ArtifactBuilderFactory());
                // Configuration.setSAML2ArtifactBuilderFactory(new SAML2ArtifactBuilderFactory());

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "initialize Parser pool");
                }

                StaticBasicParserPool sbpp = new StaticBasicParserPool();
                sbpp.setNamespaceAware(true);
                sbpp.setMaxPoolSize(50); // TODO size of XMLParser: configurable?
                try {
                    sbpp.initialize();
                } catch (XMLParserException e) {
                    throw new ConfigurationException("Error initializing parser pool", e);
                }
                Configuration.setParserPool(sbpp);

                initializeGlobalSecurityConfiguration();

            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "handling config Exception:" + e);
                }
            }
            bInit = true;
        }
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
    }

    /**
     * Initializes the default global security configuration.
     */
    protected static void initializeGlobalSecurityConfiguration() {
        BasicSecurityConfiguration basicSecurityConfiguration = DefaultSecurityConfigurationBootstrap.buildDefaultConfig();
        basicSecurityConfiguration.setSignatureReferenceDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256");//SignatureConstants.ALGO_ID_DIGEST_SHA1);
        Configuration.setGlobalSecurityConfiguration(basicSecurityConfiguration);
    }

}
