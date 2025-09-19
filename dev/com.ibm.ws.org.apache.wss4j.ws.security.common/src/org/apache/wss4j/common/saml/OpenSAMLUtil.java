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

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.ParserPool;

import org.apache.wss4j.common.crypto.WSProviderConfig;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.provider.MapBasedConfiguration;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLConfigurationException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.config.SAMLConfiguration;
import org.opensaml.xmlsec.config.DecryptionParserPool;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.opensaml.xmlsec.signature.support.SignerProvider;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * Class OpenSAMLUtil provides static helper methods for the OpenSaml library
 */
public final class OpenSAMLUtil {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(OpenSAMLUtil.class);

    private static XMLObjectProviderRegistry providerRegistry;
    private static XMLObjectBuilderFactory builderFactory;
    private static MarshallerFactory marshallerFactory;
    private static UnmarshallerFactory unmarshallerFactory;
    private static boolean samlEngineInitialized = false;

    private OpenSAMLUtil() {
        // Complete
    }

    /**
     * Initialise the SAML library
     */
    public static synchronized void initSamlEngine() {
        initSamlEngine(true);
    }

    public static synchronized void initSamlEngine(boolean includeXacml) {
        if (!samlEngineInitialized) {
            LOG.debug("Initializing the opensaml2 library...");
            WSProviderConfig.init();

            Configuration configuration = new MapBasedConfiguration();
            ConfigurationService.setConfiguration(configuration);

            providerRegistry = new XMLObjectProviderRegistry();
            configuration.register(XMLObjectProviderRegistry.class, providerRegistry,
                                   ConfigurationService.DEFAULT_PARTITION_NAME);

            try {
                OpenSAMLBootstrap.bootstrap(includeXacml);

                SAMLConfiguration samlConfiguration = new SAMLConfiguration();

                configuration.register(SAMLConfiguration.class, samlConfiguration,
                                       ConfigurationService.DEFAULT_PARTITION_NAME);

                builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
                marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
                unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();

                try {
                    configureParserPool();

                    // used by org.opensaml.saml.saml2.encryption.Decrypter
                    configuration.register(DecryptionParserPool.class, new DecryptionParserPool(getParserPool()),
                        ConfigurationService.DEFAULT_PARTITION_NAME);
                } catch (Throwable t) {
                    LOG.warn("Unable to bootstrap the parser pool part of the opensaml library "
                             + "- some SAML operations may fail", t);
                }

                samlEngineInitialized = true;
                LOG.debug("opensaml3 library bootstrap complete");
            } catch (XMLConfigurationException ex) {
                LOG.error("Unable to bootstrap the opensaml3 library - all SAML operations will fail", ex);
            }
        }
    }

    private static void configureParserPool() throws Throwable {
        BasicParserPool pp = new BasicParserPool();
        pp.setMaxPoolSize(50);
        pp.initialize();
        providerRegistry.setParserPool(pp);
    }

    /**
     * Get the configured ParserPool.
     *
     * @return the configured ParserPool
     */
    public static ParserPool getParserPool() {
        return providerRegistry.getParserPool();
    }

    /**
     * Convert a SAML Assertion from a DOM Element to an XMLObject
     *
     * @param root of type Element
     * @return XMLObject
     * @throws WSSecurityException
     */
    public static XMLObject fromDom(Element root) throws WSSecurityException {
        if (root == null) {
            LOG.debug("Attempting to unmarshal a null element!");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"Error unmarshalling a SAML assertion"});
        }
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(root);
        if (unmarshaller == null) {
            LOG.debug("Unable to find an unmarshaller for element: " + root.getLocalName());
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"Error unmarshalling a SAML assertion"});
        }
        try {
            return unmarshaller.unmarshall(root);
        } catch (UnmarshallingException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "empty",
                                          new Object[] {"Error unmarshalling a SAML assertion"});
        }
    }

    /**
     * Convert a SAML Assertion from a XMLObject to a DOM Element
     *
     * @param xmlObject of type XMLObject
     * @param doc of type Document
     * @return Element
     * @throws WSSecurityException
     */
    public static Element toDom(
        XMLObject xmlObject,
        Document doc
    ) throws WSSecurityException {
        return toDom(xmlObject, doc, true);
    }

    /**
     * Convert a SAML Assertion from a XMLObject to a DOM Element
     *
     * @param xmlObject of type XMLObject
     * @param doc of type Document
     * @param signObject whether to sign the XMLObject during marshalling
     * @return Element
     * @throws WSSecurityException
     */
    public static Element toDom(
        XMLObject xmlObject,
        Document doc,
        boolean signObject
    ) throws WSSecurityException {
        Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
        Element element = null;
        DocumentFragment frag = doc == null ? null : doc.createDocumentFragment();
        try {
            if (frag != null) {
                while (doc.getFirstChild() != null) {
                    frag.appendChild(doc.removeChild(doc.getFirstChild()));
                }
            }
            try {
                if (doc == null) {
                    element = marshaller.marshall(xmlObject);
                } else {
                    element = marshaller.marshall(xmlObject, doc);
                }
            } catch (MarshallingException ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "empty",
                                              new Object[] {"Error marshalling a SAML assertion"});
            }

            if (signObject) {
                signXMLObject(xmlObject);
            }
        } finally {
            if (frag != null) {
                while (doc.getFirstChild() != null) {
                    doc.removeChild(doc.getFirstChild());
                }
                doc.appendChild(frag);
            }
        }
        return element;
    }

    private static void signXMLObject(XMLObject xmlObject) throws WSSecurityException {
        if (xmlObject instanceof org.opensaml.saml.saml1.core.Response) {
            org.opensaml.saml.saml1.core.Response response =
                    (org.opensaml.saml.saml1.core.Response)xmlObject;

            // Sign any Assertions
            if (response.getAssertions() != null) {
                for (org.opensaml.saml.saml1.core.Assertion assertion : response.getAssertions()) {
                    signObject(assertion.getSignature());
                }
            }

            signObject(response.getSignature());
        } else if (xmlObject instanceof org.opensaml.saml.saml2.core.Response) {
            org.opensaml.saml.saml2.core.Response response =
                    (org.opensaml.saml.saml2.core.Response)xmlObject;

            // Sign any Assertions
            if (response.getAssertions() != null) {
                for (org.opensaml.saml.saml2.core.Assertion assertion : response.getAssertions()) {
                    signObject(assertion.getSignature());
                }
            }

            signObject(response.getSignature());
        } else if (xmlObject instanceof SignableSAMLObject) {
            signObject(((SignableSAMLObject)xmlObject).getSignature());
        }
    }

    private static void signObject(Signature signature) throws WSSecurityException {
        if (signature != null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(SignerProvider.class.getClassLoader());
                Signer.signObject(signature);
            } catch (SignatureException ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "empty",
                                              new Object[] {"Error signing a SAML assertion"});
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }
    }

    /**
     * Method buildSignature ...
     *
     * @return Signature
     */
    @SuppressWarnings("unchecked")
    public static Signature buildSignature() {
        QName qName = Signature.DEFAULT_ELEMENT_NAME;
        XMLObjectBuilder<Signature> builder =
            (XMLObjectBuilder<Signature>)builderFactory.getBuilder(qName);
        if (builder == null) {
            LOG.error(
                "Unable to retrieve builder for object QName "
                + qName
            );
            return null;
        }
        return
            builder.buildObject(
                 qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix()
             );
    }

    /**
     * Method isMethodSenderVouches ...
     *
     * @param confirmMethod of type String
     * @return boolean
     */
    public static boolean isMethodSenderVouches(String confirmMethod) {
        return
            confirmMethod != null && confirmMethod.startsWith("urn:oasis:names:tc:SAML:")
                && confirmMethod.endsWith(":cm:sender-vouches");
    }

    /**
     * Method isMethodHolderOfKey ...
     *
     * @param confirmMethod of type String
     * @return boolean
     */
    public static boolean isMethodHolderOfKey(String confirmMethod) {
        return
            confirmMethod != null && confirmMethod.startsWith("urn:oasis:names:tc:SAML:")
                && confirmMethod.endsWith(":cm:holder-of-key");
    }

}
