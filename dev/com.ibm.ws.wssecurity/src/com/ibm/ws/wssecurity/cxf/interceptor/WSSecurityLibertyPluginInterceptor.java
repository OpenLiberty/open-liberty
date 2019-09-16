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
package com.ibm.ws.wssecurity.cxf.interceptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
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

    final static Map<String, Object> providerConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());
    //new HashMap<String, Object>();
    final static Map<String, Object> clientConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());
    //new HashMap<String, Object>();
    private static final TraceComponent tc = Tr.register(WSSecurityLibertyPluginInterceptor.class,
                                                         WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

    private static final String SIGNATURE_METHOD = "signatureAlgorithm";
    static Map<String, Object> samlTokenConfigMap = null; //unmodifiableMap

    public WSSecurityLibertyPluginInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addBefore(PolicyBasedWSS4JInInterceptor.class.getName());
        addBefore(PolicyBasedWSS4JOutInterceptor.class.getName());
    }

    public static void setBindingsConfiguration(Map<String, Object> map) {

        if (map != null) {
            if (!providerConfigMap.isEmpty()) {
                providerConfigMap.clear();
            }
            providerConfigMap.putAll(map);
        } else {
            providerConfigMap.clear();
        }

        //System.out.println("In UNT interceptor = " + providerConfigMap.get("username"));
    }

    public static void setClientBindingsConfiguration(Map<String, Object> map) {

        if (map != null) {
            if (!clientConfigMap.isEmpty()) {
                clientConfigMap.clear();
            }
            clientConfigMap.putAll(map);
        } else {
            clientConfigMap.clear();
        }

        //System.out.println("In UNT interceptor = " + providerConfigMap.get("username"));
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
            //checkConfigMap(clientConfigMap);
            Set<String> client_config_keys = clientConfigMap.keySet();
            Iterator<String> keyIt = client_config_keys.iterator();
            //check whether user name is specified via request context
            boolean user_id_exists = false;
            if (message.getContextualProperty(WSSecurityConstants.CXF_USER_NAME) != null) {
                user_id_exists = true;
            }

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                if (message.getContextualProperty(key) == null) {
                    //check whether config has password
                    if (WSSecurityConstants.CXF_SIG_PROPS.equals(key)) {
                        Map<String, Object> tempMap = (Map<String, Object>) clientConfigMap.
                                        get(WSSecurityConstants.CXF_SIG_PROPS);
                        if (tempMap != null) {
                            Map<String, Object> sigPropsMap = new HashMap<String, Object>(tempMap);
                            Utils.modifyConfigMap(sigPropsMap);
                            Properties sigProps = new Properties();
                            sigProps.putAll(sigPropsMap);
                            message.setContextualProperty(key, sigProps);
                            message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, Utils.getCrypto(sigProps));
                            SignatureAlgorithms.setAlgorithm(message, (String) tempMap.get(SIGNATURE_METHOD));
                        }
                    } else if (WSSecurityConstants.CXF_ENC_PROPS.equals(key)) {
                        Map<String, Object> tempMap = (Map<String, Object>) clientConfigMap.
                                        get(WSSecurityConstants.CXF_ENC_PROPS);
                        if (tempMap != null) {
                            Map<String, Object> encPropsMap = new HashMap<String, Object>(tempMap);
                            Utils.modifyConfigMap(encPropsMap);
                            Properties encProps = new Properties();
                            encProps.putAll(encPropsMap);
                            message.setContextualProperty(key, encProps);
                            message.setContextualProperty(WSSecurityConstants.CXF_ENC_CRYPTO, Utils.getCrypto(encProps));
                        }
                    } else if (WSSecurityConstants.CXF_USER_PASSWORD.equals(key)) {
                        //if user is specified via request context, 
                        //then don't bother checking for password in the server.xml
                        if (!user_id_exists) {
                            String pwd = Utils.changePasswordType
                                            ((SerializableProtectedString) clientConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD));
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
            }

            Set<String> provider_config_keys = providerConfigMap.keySet();
            Iterator<String> keyIt = provider_config_keys.iterator();

            while (keyIt.hasNext()) {
                String key = keyIt.next();

                //check whether config has password
                if (WSSecurityConstants.CXF_SIG_PROPS.equals(key)) {
                    Map<String, Object> tempMap = (Map<String, Object>) providerConfigMap.
                                    get(WSSecurityConstants.CXF_SIG_PROPS);
                    if (tempMap != null) {
                        Map<String, Object> sigPropsMap = new HashMap<String, Object>(tempMap);
                        Utils.modifyConfigMap(sigPropsMap);
                        Properties sigProps = new Properties();
                        sigProps.putAll(sigPropsMap);
                        message.setContextualProperty(key, sigProps);
                        message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, Utils.getCrypto(sigProps));
                        SignatureAlgorithms.setAlgorithm(message, (String) tempMap.get(SIGNATURE_METHOD));
                    }
                } else if (WSSecurityConstants.CXF_ENC_PROPS.equals(key)) {
                    Map<String, Object> tempMap = (Map<String, Object>) providerConfigMap.
                                    get(WSSecurityConstants.CXF_ENC_PROPS);
                    if (tempMap != null) {
                        Map<String, Object> encPropsMap = new HashMap<String, Object>(tempMap);
                        Utils.modifyConfigMap(encPropsMap);
                        Properties encProps = new Properties();
                        encProps.putAll(encPropsMap);
                        message.setContextualProperty(key, encProps);
                        message.setContextualProperty(WSSecurityConstants.CXF_ENC_CRYPTO, Utils.getCrypto(encProps));
                    }
                } else if (WSSecurityConstants.CXF_USER_PASSWORD.equals(key)) {
                    String pwd = Utils.changePasswordType
                                    ((SerializableProtectedString) providerConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD));
                    message.setContextualProperty(key, pwd);
                } else {
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
