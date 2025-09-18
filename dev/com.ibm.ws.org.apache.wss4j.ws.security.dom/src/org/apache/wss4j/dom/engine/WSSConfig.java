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

package org.apache.wss4j.dom.engine;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.apache.wss4j.dom.action.Action;
import org.apache.wss4j.common.crypto.WSProviderConfig;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.WSCurrentTimeSource;
import org.apache.wss4j.common.util.WSTimeSource;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.resolvers.ResolverAttachment;
import org.apache.wss4j.dom.transform.AttachmentCiphertextTransform;
import org.apache.wss4j.dom.transform.AttachmentCompleteSignatureTransformProvider;
import org.apache.wss4j.dom.transform.AttachmentContentSignatureTransformProvider;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.utils.resolver.ResourceResolver;

/**
 * WSSConfig <p/> Carries configuration data so the WSS4J spec compliance can be
 * modified in runtime. Configure an instance of this object only if you need
 * WSS4J to emulate certain industry clients or previous OASIS specifications
 * for WS-Security interoperability testing purposes. <p/> The default settings
 * follow the latest OASIS and changing anything might violate the OASIS specs.
 * <p/> <b>WARNING: changing the default settings will break the compliance with
 * the latest specs. Do this only if you know what you are doing.</b> <p/>
 */
public final class WSSConfig {

    public static final DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSSConfig.class);

    /**
     * The default collection of actions supported by the toolkit.
     */
    private static final Map<Integer, Class<?>> DEFAULT_ACTIONS;
    static {
        final Map<Integer, Class<?>> tmp = new HashMap<>();
        try {
            tmp.put(
                WSConstants.UT,
                org.apache.wss4j.dom.action.UsernameTokenAction.class
            );
            tmp.put(
                WSConstants.UT_NOPASSWORD,
                org.apache.wss4j.dom.action.UsernameTokenAction.class
            );
            tmp.put(
                WSConstants.ENCR,
                org.apache.wss4j.dom.action.EncryptionAction.class
            );
            tmp.put(
                WSConstants.SIGN,
                org.apache.wss4j.dom.action.SignatureAction.class
            );
            tmp.put(
                WSConstants.DKT_SIGN,
                org.apache.wss4j.dom.action.SignatureDerivedAction.class
            );
            tmp.put(
                WSConstants.DKT_ENCR,
                org.apache.wss4j.dom.action.EncryptionDerivedAction.class
            );
            tmp.put(
                WSConstants.ST_SIGNED,
                org.apache.wss4j.dom.action.SAMLTokenSignedAction.class
            );
            tmp.put(
                WSConstants.ST_UNSIGNED,
                org.apache.wss4j.dom.action.SAMLTokenUnsignedAction.class
            );
            tmp.put(
                WSConstants.TS,
                org.apache.wss4j.dom.action.TimestampAction.class
            );
            tmp.put(
                WSConstants.UT_SIGN,
                org.apache.wss4j.dom.action.UsernameTokenSignedAction.class
            );
            tmp.put(
                WSConstants.SC,
                org.apache.wss4j.dom.action.SignatureConfirmationAction.class
            );
            tmp.put(
                WSConstants.CUSTOM_TOKEN,
                org.apache.wss4j.dom.action.CustomTokenAction.class
            );
        } catch (final Exception ex) {
            LOG.debug(ex.getMessage(), ex);
        }
        DEFAULT_ACTIONS = java.util.Collections.unmodifiableMap(tmp);
    }

    /**
     * The default collection of processors supported by the toolkit
     */
    private static final Map<QName, Class<?>> DEFAULT_PROCESSORS;
    static {
        final Map<QName, Class<?>> tmp = new HashMap<>();
        try {
            tmp.put(
                WSConstants.SAML_TOKEN,
                org.apache.wss4j.dom.processor.SAMLTokenProcessor.class
            );
            tmp.put(
                WSConstants.SAML2_TOKEN,
                org.apache.wss4j.dom.processor.SAMLTokenProcessor.class
            );
            tmp.put(
                WSConstants.ENCRYPTED_ASSERTION,
                org.apache.wss4j.dom.processor.EncryptedAssertionProcessor.class
            );
            tmp.put(
                WSConstants.ENCRYPTED_KEY,
                org.apache.wss4j.dom.processor.EncryptedKeyProcessor.class
            );
            tmp.put(
                WSConstants.SIGNATURE,
                org.apache.wss4j.dom.processor.SignatureProcessor.class
            );
            tmp.put(
                WSConstants.TIMESTAMP,
                org.apache.wss4j.dom.processor.TimestampProcessor.class
            );
            tmp.put(
                WSConstants.USERNAME_TOKEN,
                org.apache.wss4j.dom.processor.UsernameTokenProcessor.class
            );
            tmp.put(
                WSConstants.REFERENCE_LIST,
                org.apache.wss4j.dom.processor.ReferenceListProcessor.class
            );
            tmp.put(
                WSConstants.SIGNATURE_CONFIRMATION,
                org.apache.wss4j.dom.processor.SignatureConfirmationProcessor.class
            );
            tmp.put(
                WSConstants.DERIVED_KEY_TOKEN_05_02,
                org.apache.wss4j.dom.processor.DerivedKeyTokenProcessor.class
            );
            tmp.put(
                WSConstants.DERIVED_KEY_TOKEN_05_12,
                tmp.get(WSConstants.DERIVED_KEY_TOKEN_05_02)
            );
            tmp.put(
                WSConstants.SECURITY_CONTEXT_TOKEN_05_02,
                org.apache.wss4j.dom.processor.SecurityContextTokenProcessor.class
            );
            tmp.put(
                WSConstants.SECURITY_CONTEXT_TOKEN_05_12,
                tmp.get(WSConstants.SECURITY_CONTEXT_TOKEN_05_02)
            );
            tmp.put(
                WSConstants.BINARY_TOKEN,
                org.apache.wss4j.dom.processor.BinarySecurityTokenProcessor.class
            );
            tmp.put(
                WSConstants.ENCRYPTED_DATA,
                org.apache.wss4j.dom.processor.EncryptedDataProcessor.class
            );
        } catch (final Exception ex) {
            LOG.debug(ex.getMessage(), ex);
        }
        DEFAULT_PROCESSORS = java.util.Collections.unmodifiableMap(tmp);
    }

    /**
     * The default collection of validators supported by the toolkit
     */
    private static final Map<QName, Class<?>> DEFAULT_VALIDATORS;
    static {
        final Map<QName, Class<?>> tmp = new HashMap<>();
        try {
            tmp.put(
                WSConstants.SAML_TOKEN,
                org.apache.wss4j.dom.validate.SamlAssertionValidator.class
            );
            tmp.put(
                WSConstants.SAML2_TOKEN,
                org.apache.wss4j.dom.validate.SamlAssertionValidator.class
            );
            tmp.put(
                WSConstants.SIGNATURE,
                org.apache.wss4j.dom.validate.SignatureTrustValidator.class
            );
            tmp.put(
                WSConstants.TIMESTAMP,
                org.apache.wss4j.dom.validate.TimestampValidator.class
            );
            tmp.put(
                WSConstants.USERNAME_TOKEN,
                org.apache.wss4j.dom.validate.UsernameTokenValidator.class
            );
        } catch (final Exception ex) {
            LOG.debug(ex.getMessage(), ex);
        }
        DEFAULT_VALIDATORS = java.util.Collections.unmodifiableMap(tmp);
    }

    /**
     * a static boolean flag that determines whether default JCE providers
     * should be added at the time of construction.
     *
     * These providers, and the order in which they are added, can interfere
     * with some JVMs (such as IBMs).
     */
    private static boolean addJceProviders = true;

    /**
     * a boolean flag to record whether we have already been statically
     * initialized.  This flag prevents repeated and unnecessary calls
     * to static initialization code at construction time.
     */
    private static boolean staticallyInitialized = false;

    /**
     * This allows the user to specify a different time than that of the current System time.
     */
    private WSTimeSource currentTime;

    public static final WsuIdAllocator DEFAULT_ID_ALLOCATOR = new WsuIdAllocator() {

        public String createId(String prefix, Object o) {
            if (prefix == null) {
                return IDGenerator.generateID("_");
            }

            return IDGenerator.generateID(prefix);
        }

        public String createSecureId(String prefix, Object o) {
            return IDGenerator.generateID(prefix);
        }
    };
    protected WsuIdAllocator idAllocator = DEFAULT_ID_ALLOCATOR;

    /**
     * The known actions. This map is of the form <Integer, Class<?>> or
     * <Integer, Action>.
     * The known actions are initialized from a set of defaults,
     * but the list may be modified via the setAction operations.
     */
    private final Map<Integer, Object> actionMap = new HashMap<>(DEFAULT_ACTIONS);

    /**
     * The known processors. This map is of the form <QName, Class<?>> or
     * <QName, Processor>.
     * The known processors are initialized from a set of defaults,
     * but the list may be modified via the setProcessor operations.
     */
    private final Map<QName, Object> processorMap = new HashMap<>(DEFAULT_PROCESSORS);

    /**
     * The known validators. This map is of the form <QName, Class<?>> or
     * <QName, Validator>.
     * The known validators are initialized from a set of defaults,
     * but the list may be modified via the setValidator operations.
     */
    private final Map<QName, Object> validatorMap = new HashMap<>(DEFAULT_VALIDATORS);

    static {
        try {
            Transform.register(WSConstants.SWA_ATTACHMENT_CIPHERTEXT_TRANS,
                    AttachmentCiphertextTransform.class);
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        }

        ResourceResolver.register(new ResolverAttachment(), false);
    }

    private WSSConfig() {
        // complete
    }

    public static synchronized void init() {
        if (!staticallyInitialized) {
            if (addJceProviders) {
                AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        Security.removeProvider("STRTransform");
                        WSProviderConfig.appendJceProvider(
                            "STRTransform",
                            new org.apache.wss4j.dom.transform.STRTransformProvider()
                        );

                        Security.removeProvider("AttachmentContentSignatureTransform");
                        WSProviderConfig.appendJceProvider(
                                "AttachmentContentSignatureTransform",
                                new AttachmentContentSignatureTransformProvider()
                        );

                        Security.removeProvider("AttachmentCompleteSignatureTransform");
                        WSProviderConfig.appendJceProvider(
                                "AttachmentCompleteSignatureTransform",
                                new AttachmentCompleteSignatureTransformProvider()
                        );

                        return true;
                    }
                });
            }
            WSProviderConfig.init();
            staticallyInitialized = true;
        }
    }

    public static synchronized void cleanUp() {
        if (staticallyInitialized) {
            if (addJceProviders) {
                Security.removeProvider("STRTransform");
                Security.removeProvider("AttachmentContentSignatureTransform");
                Security.removeProvider("AttachmentCompleteSignatureTransform");
            }
            WSProviderConfig.cleanUp();

            staticallyInitialized = false;
        }
    }

    /**
     * @return a new WSSConfig instance configured with the default values
     */
    public static WSSConfig getNewInstance() {
        init();
        return new WSSConfig();
    }

    /**
     * @return Returns the WsuIdAllocator used to generate wsu:Id attributes
     */
    public WsuIdAllocator getIdAllocator() {
        return idAllocator;
    }

    public void setIdAllocator(WsuIdAllocator idAllocator) {
        this.idAllocator = idAllocator;
    }

    /**
     * Associate an action instance with a specific action code.
     *
     * This operation allows applications to supply their own
     * actions for well-known operations.
     *
     * Please note that the Action object does NOT get class-loaded per invocation, and so
     * it is up to the implementing class to ensure that it is thread-safe.
     */
    public Class<?> setAction(int code, Action action) {
        Object result = actionMap.put(code, action);
        if (result instanceof Class<?>) {
            return (Class<?>)result;
        } else if (result instanceof Action) {
            return result.getClass();
        }
        return null;
    }

    /**
     * Associate an action instance with a specific action code.
     *
     * This operation allows applications to supply their own
     * actions for well-known operations.
     */
    public Class<?> setAction(int code, Class<?> clazz) {
        Object result = actionMap.put(code, clazz);
        if (result instanceof Class<?>) {
            return (Class<?>)result;
        } else if (result instanceof Action) {
            return result.getClass();
        }
        return null;
    }

    /**
     * Lookup action
     *
     * @param action
     * @return An action class to create a security token
     * @throws WSSecurityException
     */
    public Action getAction(int action) throws WSSecurityException {
        final Object actionObject = actionMap.get(action);

        if (actionObject instanceof Class<?>) {
            try {
                return (Action)((Class<?>)actionObject).newInstance();
            } catch (Exception ex) {
                LOG.debug(ex.getMessage(), ex);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex,
                        "unableToLoadClass", new Object[] {((Class<?>)actionObject).getName()});
            }
        } else if (actionObject instanceof Action) {
            return (Action)actionObject;
        }
        return null;
    }

    /**
     * Associate a SOAP processor name with a specified SOAP Security header
     * element QName.  Processors registered under this QName will be
     * called when processing header elements with the specified type.
     *
     * Please note that the Processor object does NOT get class-loaded per invocation, and so
     * it is up to the implementing class to ensure that it is thread-safe.
     */
    public Class<?> setProcessor(QName el, Processor processor) {
        Object result = processorMap.put(el, processor);
        if (result instanceof Class<?>) {
            return (Class<?>)result;
        } else if (result instanceof Processor) {
            return result.getClass();
        }
        return null;
    }

    /**
     * Associate a SOAP processor name with a specified SOAP Security header
     * element QName.  Processors registered under this QName will be
     * called when processing header elements with the specified type.
     */
    public Class<?> setProcessor(QName el, Class<?> clazz) {
        Object result = processorMap.put(el, clazz);
        if (result instanceof Class<?>) {
            return (Class<?>)result;
        } else if (result instanceof Processor) {
            return result.getClass();
        }
        return null;
    }

    /**
     * Associate a SOAP validator name with a specified SOAP Security header
     * element QName.  Validators registered under this QName will be
     * called when processing header elements with the specified type.
     *
     * Please note that the Validator object does NOT get class-loaded per invocation, and so
     * it is up to the implementing class to ensure that it is thread-safe.
     */
    public Class<?> setValidator(QName el, Validator validator) {
        Object result = validatorMap.put(el, validator);
        if (result instanceof Class<?>) {
            return (Class<?>)result;
        } else if (result instanceof Validator) {
            return result.getClass();
        }
        return null;
    }

    /**
     * Associate a SOAP validator name with a specified SOAP Security header
     * element QName.  validator registered under this QName will be
     * called when processing header elements with the specified type.
     */
    public Class<?> setValidator(QName el, Class<?> clazz) {
        Object result = validatorMap.put(el, clazz);
        if (result instanceof Class<?>) {
            return (Class<?>)result;
        } else if (result instanceof Validator) {
            return result.getClass();
        }
        return null;
    }

    /**
     * @return      the SOAP Validator associated with the specified
     *              QName.  The QName is intended to refer to an element
     *              in a SOAP security header.  This operation returns
     *              null if there is no Validator associated with the
     *              specified QName.
     */
    public Validator getValidator(QName el) throws WSSecurityException {
        final Object validatorObject = validatorMap.get(el);

        if (validatorObject instanceof Class<?>) {
            try {
                return (Validator)((Class<?>)validatorObject).newInstance();
            } catch (Exception ex) {
                LOG.debug(ex.getMessage(), ex);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex,
                    "unableToLoadClass", new Object[] {((Class<?>)validatorObject).getName()});
            }
        } else if (validatorObject instanceof Validator) {
            return (Validator)validatorObject;
        }
        return null;
    }

    /**
     * @return      the SOAP processor associated with the specified
     *              QName.  The QName is intended to refer to an element
     *              in a SOAP security header.  This operation returns
     *              null if there is no processor associated with the
     *              specified QName.
     */
    public Processor getProcessor(QName el) throws WSSecurityException {
        final Object processorObject = processorMap.get(el);

        if (processorObject instanceof Class<?>) {
            try {
                return (Processor)((Class<?>)processorObject).newInstance();
            } catch (Exception ex) {
                LOG.debug(ex.getMessage(), ex);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex,
                        "unableToLoadClass", new Object[] {((Class<?>)processorObject).getName()});
            }
        } else if (processorObject instanceof Processor) {
            return (Processor)processorObject;
        }
        return null;
    }

    public WSTimeSource getCurrentTime() {
        if (currentTime != null) {
            return currentTime;
        }
        return new WSCurrentTimeSource();
    }

    public void setCurrentTime(WSTimeSource currentTime) {
        this.currentTime = currentTime;
    }


    public static boolean isAddJceProviders() {
        return addJceProviders;
    }

    public static void setAddJceProviders(boolean addJceProviders) {
        WSSConfig.addJceProviders = addJceProviders;
        WSProviderConfig.setAddJceProviders(addJceProviders);
    }
}
