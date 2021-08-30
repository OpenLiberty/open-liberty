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
package com.ibm.ws.wssecurity.cxf.interceptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wssecurity.cxf.validator.UsernameTokenValidator;
import com.ibm.ws.wssecurity.cxf.validator.Utils;
import com.ibm.ws.wssecurity.cxf.validator.WssSamlAssertionValidator;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.ws.wssecurity.signature.SignatureAlgorithms;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class WSSecurityLibertyPluginInterceptor extends AbstractSoapInterceptor {

    private static final Map<String, Object> providerConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());
    //new HashMap<String, Object>();
    private static final Map<String, Object> clientConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());
    //new HashMap<String, Object>();
    private static final TraceComponent tc = Tr.register(WSSecurityLibertyPluginInterceptor.class,
                                                         WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

    private static final String SIGNATURE_METHOD = "signatureAlgorithm";
    private static Map<String, Object> samlTokenConfigMap = null; //unmodifiableMap
    private static boolean signatureConfigChanged = false;
    private static boolean clientSignatureConfigChanged = false;

    public WSSecurityLibertyPluginInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addBefore(PolicyBasedWSS4JInInterceptor.class.getName());
        addBefore(PolicyBasedWSS4JOutInterceptor.class.getName());
    }

    public static void setBindingsConfiguration(Map<String, Object> map) {
        signatureConfigChanged = true;
        if (map != null) {
            if (!providerConfigMap.isEmpty()) {
                providerConfigMap.clear();
            }
            providerConfigMap.putAll(map);
        } else {
            providerConfigMap.clear();
        }
    }

    public static void setClientBindingsConfiguration(Map<String, Object> map) {
        clientSignatureConfigChanged = true;
        if (map != null) {
            if (!clientConfigMap.isEmpty()) {
                clientConfigMap.clear();
            }
            clientConfigMap.putAll(map);
        } else {
            clientConfigMap.clear();
        }
    }

    /**
     * @param samlTokenConfigMap -- unmodifiableMap
     */
    public static void setSamlTokenConfiguration(Map<String, Object> tmpSamlTokenConfigMap) {
        samlTokenConfigMap = tmpSamlTokenConfigMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void handleMessage(@Sensitive SoapMessage message) throws Fault {
        if (message == null) {
            return;
        }
        boolean isReq = MessageUtils.isRequestor(message);
        //boolean isOut = MessageUtils.isOutbound(message);

        if (isReq) { //client -
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "client side message = ", message);
            }
            //checkConfigMap(clientConfigMap);
            Set<String> client_config_keys = clientConfigMap.keySet();
            Iterator<String> keyIt = client_config_keys.iterator();
            //check whether user name is specified via request context
            boolean user_id_exists = false;
            if (message.getContextualProperty(WSSecurityConstants.CXF_USER_NAME) != null || message.getContextualProperty(WSSecurityConstants.SEC_USER_NAME) != null) {
                user_id_exists = true;
//                message.setContextualProperty(WSSecurityConstants.UPDATED_CXF_USER_NAME, message.getContextualProperty(WSSecurityConstants.CXF_USER_NAME));
//                message.setContextualProperty(WSSecurityConstants.UPDATED_CXF_USER_PASSWORD, message.getContextualProperty(WSSecurityConstants.CXF_USER_PASSWORD));
//            } else if(message.getContextualProperty(WSSecurityConstants.UPDATED_CXF_USER_NAME) != null) {
//                user_id_exists = true;
            }

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                if (message.getContextualProperty(key) == null) {
                    //check whether config has password
                    if ((WSSecurityConstants.CXF_SIG_PROPS.equals(key) || WSSecurityConstants.SEC_SIG_PROPS.equals(key)) && 
                                    message.getContextualProperty(WSSecurityConstants.SEC_SIG_PROPS) == null ) {
                        Map<String, Object> tempMap = (Map<String, Object>) clientConfigMap.
                                        get(WSSecurityConstants.SEC_SIG_PROPS); //v3
                        if (tempMap == null) {
                            tempMap = (Map<String, Object>) clientConfigMap.
                                            get(WSSecurityConstants.CXF_SIG_PROPS); //v3
                        }
                        if (tempMap != null) {
                            Map<String, Object> sigPropsMap = new HashMap<String, Object>(tempMap);
                            Utils.modifyConfigMap(sigPropsMap);
                            Properties sigProps = new Properties();
                            sigProps.putAll(sigPropsMap);
                            message.setContextualProperty(key, sigProps);
                            if (clientSignatureConfigChanged) {
                                //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, null);
                                message.setContextualProperty(WSSecurityConstants.SEC_SIG_CRYPTO, null);
                                clientSignatureConfigChanged = false;
                            }
                            //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, Utils.getCrypto(sigProps)); //@2020 TODO
                            SignatureAlgorithms.setAlgorithm(message, (String) tempMap.get(SIGNATURE_METHOD));
                        }
                    } else if ((WSSecurityConstants.CXF_ENC_PROPS.equals(key) || WSSecurityConstants.SEC_ENC_PROPS.equals(key)) && 
                                    message.getContextualProperty(WSSecurityConstants.SEC_ENC_PROPS) == null ) {
                        Map<String, Object> tempMap = (Map<String, Object>) clientConfigMap.
                                        get(WSSecurityConstants.SEC_ENC_PROPS); //v3
                        if (tempMap == null) {
                            tempMap = (Map<String, Object>) clientConfigMap.
                                            get(WSSecurityConstants.CXF_ENC_PROPS); //v3
                        }
                        if (tempMap != null) {
                            Map<String, Object> encPropsMap = new HashMap<String, Object>(tempMap);
                            Utils.modifyConfigMap(encPropsMap);
                            Properties encProps = new Properties();
                            encProps.putAll(encPropsMap);
                            message.setContextualProperty(key, encProps);
                            //message.setContextualProperty(WSSecurityConstants.CXF_ENC_CRYPTO, Utils.getCrypto(encProps)); //@2020 TODO
                        }
                    } else if (WSSecurityConstants.CXF_USER_PASSWORD.equals(key) || WSSecurityConstants.SEC_USER_PASSWORD.equals(key)) { //v3
                        //if user is specified via request context, 
                        //then don't bother checking for password in the server.xml
                        if (!user_id_exists) {
                            String pwd = null;
                            if (clientConfigMap.get(WSSecurityConstants.SEC_USER_PASSWORD) != null) {
                                pwd = Utils.changePasswordType
                                                ((SerializableProtectedString) clientConfigMap.get(WSSecurityConstants.SEC_USER_PASSWORD));
                            } else if (clientConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD) != null) {
                                pwd = Utils.changePasswordType
                                                ((SerializableProtectedString) clientConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD));
                            }
//                            String pwd = Utils.changePasswordType
//                                            ((SerializableProtectedString) clientConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD));
                            message.setContextualProperty(key, pwd);
                        }

                    } else {
                        message.setContextualProperty(key, clientConfigMap.get(key));
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Client Config attribute is set on message = ",
                                 key, ", value = ", clientConfigMap.get(key));
                    }
                }
                // set default samlCallbackHandler if none is set
                String samlCallbackHandler = (String) message.getContextualProperty(WSSecurityConstants.CXF_SAML_CALLBACK_HANDLER);
                if (samlCallbackHandler == null || samlCallbackHandler.isEmpty()) {
                    message.setContextualProperty(WSSecurityConstants.CXF_SAML_CALLBACK_HANDLER,
                                                  WSSecurityConstants.DEFAULT_SAML_CALLBACK_HANDLER);
                }
            }

        } else { //provider
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "provider side message = ", message);
            }
            // Handle UserNameTokenValidator
            Object validator =
                            message.getContextualProperty(SecurityConstants.USERNAME_TOKEN_VALIDATOR);
            if (validator == null) {
                message.put(SecurityConstants.USERNAME_TOKEN_VALIDATOR, new UsernameTokenValidator());
            }

            // Handle saml assertion validator. We only handle saml20 
            validator = message.getContextualProperty(SecurityConstants.SAML2_TOKEN_VALIDATOR);
            if (validator == null) {
                // override SamlTokenValidator
                message.put(SecurityConstants.SAML2_TOKEN_VALIDATOR, new WssSamlAssertionValidator(samlTokenConfigMap));
                if (samlTokenConfigMap != null) {
                    String[] restrictions = (String[]) samlTokenConfigMap.get(WSSecurityConstants.KEY_audienceRestrictions);
                    if (restrictions == null || restrictions.length < 1) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "set audience restriction validation to false");
                        }
                        message.put(SecurityConstants.AUDIENCE_RESTRICTION_VALIDATION, false); //v3
                    }
                }
                
            }

            Set<String> provider_config_keys = providerConfigMap.keySet();
            Iterator<String> keyIt = provider_config_keys.iterator();

            while (keyIt.hasNext()) {
                String key = keyIt.next();

                //check whether config has password
                if ((WSSecurityConstants.CXF_SIG_PROPS.equals(key) || WSSecurityConstants.SEC_SIG_PROPS.equals(key)) && 
                                message.getContextualProperty(WSSecurityConstants.SEC_SIG_PROPS) == null ) {
                    Map<String, Object> tempMap = (Map<String, Object>) providerConfigMap.
                                    get(WSSecurityConstants.SEC_SIG_PROPS); //v3
                    if (tempMap == null) {
                        tempMap = (Map<String, Object>) providerConfigMap.
                                        get(WSSecurityConstants.CXF_SIG_PROPS); //v3
                    }
                    if (tempMap != null) {
                        Map<String, Object> sigPropsMap = new HashMap<String, Object>(tempMap);
                        Utils.modifyConfigMap(sigPropsMap);
                        Properties sigProps = new Properties();
                        sigProps.putAll(sigPropsMap);
                        message.setContextualProperty(key, sigProps);
                        if (signatureConfigChanged) {
                            //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, null);
                            message.setContextualProperty(WSSecurityConstants.SEC_SIG_CRYPTO, null);
                            signatureConfigChanged = false;
                        }
                        //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, Utils.getCrypto(sigProps)); //@2020 TODO
                        SignatureAlgorithms.setAlgorithm(message, (String) tempMap.get(SIGNATURE_METHOD));
                    }
                } else if ((WSSecurityConstants.CXF_ENC_PROPS.equals(key) || WSSecurityConstants.SEC_ENC_PROPS.equals(key)) && 
                                message.getContextualProperty(WSSecurityConstants.SEC_ENC_PROPS) == null) {
                    Map<String, Object> tempMap = (Map<String, Object>) providerConfigMap.
                                    get(WSSecurityConstants.SEC_ENC_PROPS); //v3
                    if (tempMap == null) {
                        tempMap = (Map<String, Object>) providerConfigMap.
                                        get(WSSecurityConstants.CXF_ENC_PROPS); //v3
                    }
                    if (tempMap != null) {
                        Map<String, Object> encPropsMap = new HashMap<String, Object>(tempMap);
                        Utils.modifyConfigMap(encPropsMap);
                        Properties encProps = new Properties();
                        encProps.putAll(encPropsMap);
                        message.setContextualProperty(key, encProps);
                        //message.setContextualProperty(WSSecurityConstants.CXF_ENC_CRYPTO, Utils.getCrypto(encProps)); //@2020 TODO
                    }
                } /*else if (WSSecurityConstants.CXF_USER_PASSWORD.equals(key)) {
                    String pwd = Utils.changePasswordType
                                    ((SerializableProtectedString) providerConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD));
                    message.setContextualProperty(key, pwd);
                }*/ else {
                    //handle ws-security.cache.config.file property
                    if (WSSecurityConstants.CXF_NONCE_CACHE_CONFIG_FILE.equals(key)) {
                        //System.out.println("gkuo Liberty:get ws-security.cache.config.file:" + providerConfigMap.get(key));
                    }
                    message.setContextualProperty(key, providerConfigMap.get(key));
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Provider Config attribute is set on message = ",
                             key, ", value = ", providerConfigMap.get(key));
                }
            }
        }
    }
}
