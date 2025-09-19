/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.common.saml;

import java.io.IOException;
import java.io.InputStream;

import org.opensaml.core.config.Configuration;
import org.opensaml.core.xml.config.XMLConfigurationException;
import org.opensaml.core.xml.config.XMLConfigurator;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;

/**
 * This class intializes the Opensaml library.
 */
public final class OpenSAMLBootstrap {

    /** List of default configuration files. */
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
        "/saml2-metadata-attr-config.xml",
        "/saml2-metadata-config.xml",
        "/saml2-metadata-idp-discovery-config.xml",
        "/saml2-metadata-query-config.xml",
        "/saml2-metadata-reqinit-config.xml",
        "/saml2-metadata-ui-config.xml",
        "/saml2-metadata-rpi-config.xml",
        "/saml2-protocol-config.xml",
        "/saml2-protocol-thirdparty-config.xml",
        "/saml2-protocol-aslo-config.xml",
        "/saml2-channel-binding-config.xml",
        "/saml-ec-gss-config.xml",
        "/signature-config.xml",
        "/wss4j-signature-config.xml",  // Override the default Base64 Binary Unmarshaller for X.509 Certificates
        "/encryption-config.xml",
        "/xacml20-context-config.xml",
        "/xacml20-policy-config.xml",
        "/xacml10-saml2-profile-config.xml",
        "/xacml11-saml2-profile-config.xml",
        "/xacml2-saml2-profile-config.xml",
        "/xacml3-saml2-profile-config.xml",
        "/saml2-xacml2-profile.xml",
    };

    private OpenSAMLBootstrap() {
        // complete
    }

    /**
     * Initializes the OpenSAML library, loading default configurations.
     *
     * @throws XMLConfigurationException thrown if there is a problem initializing the OpenSAML library
     */
    public static synchronized void bootstrap() throws XMLConfigurationException {
        bootstrap(true);
    }

    public static synchronized void bootstrap(boolean includeXacml) throws XMLConfigurationException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            XMLConfigurator configurator = new XMLConfigurator();

            Thread.currentThread().setContextClassLoader(XMLObjectProviderRegistrySupport.class.getClassLoader());

            for (String config : XML_CONFIGS) {
                if (includeXacml || !config.contains("xacml")) {
                    //most are found in the Configuration.class classloader
                    InputStream ins = Configuration.class.getResourceAsStream(config);  //NOPMD
                    if (ins == null) {
                        //some are from us
                        ins = OpenSAMLBootstrap.class.getResourceAsStream(config);
                    }
                    if (ins != null) {
                        configurator.load(ins);
                        try {
                            ins.close();
                        } catch (IOException ex) { //NOPMD
                            // Do nothing
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

}
