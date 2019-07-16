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

package com.ibm.ws.wssecurity.cxf.interceptor;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.cxf.ws.security.wss4j.SamlTokenInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.processor.SAMLTokenProcessor;
import org.apache.ws.security.validate.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;

/**
 * An interceptor to create and add a SAML token to the security header of an outbound
 * request, and to process a SAML Token on an inbound request.
 */
public class WSSecuritySamlTokenInterceptor extends SamlTokenInterceptor {
    private static final TraceComponent tc = Tr.register(WSSecuritySamlTokenInterceptor.class,
                                                         WSSecurityConstants.TR_GROUP,
                                                         WSSecurityConstants.TR_RESOURCE_BUNDLE);

    public static final String WSSEC = "ws-security";
    public static final String CXF_SIG_PROPS = WSSEC + ".signature.properties";
    public static final String CXF_ENC_PROPS = WSSEC + ".encryption.properties";

    /**
     * @param p
     */
    public WSSecuritySamlTokenInterceptor() {
        super();
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        boolean isReq = MessageUtils.isRequestor(message);
        boolean isOut = MessageUtils.isOutbound(message);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " isReq:" + isReq + "isOut:" + isOut);
        };
        if (isReq != isOut) {
            super.handleMessage(message);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SamlTokenInterceptor handled(1)");
            };
            return;
        }
        if (isReq) {
            super.handleMessage(message);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SamlTokenInterceptor handled(1)");
            };
            return;
        } else {
            if (message.containsKey(WSS4JInInterceptor.SECURITY_PROCESSED)) {
                //The full policy interceptors handled this
                return;
            }
            processSamlToken(message);
        }
        return;
    }

    @Trivial
    private void processSamlToken(SoapMessage message) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "processSamlToken(1)");
        };
        Header h = findSecurityHeader(message, false);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "processSamlToken(2):" + h);
        };
        if (h == null) {
            return;
        }
        Element el = (Element) h.getObject();
        el = (Element) DOMUtils.getDomElement(el);
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if ("Assertion".equals(child.getLocalName())) {
                try {
                    List<WSSecurityEngineResult> samlResults = processToken(child, message);
                    if (samlResults != null) {
                        List<WSHandlerResult> results = CastUtils.cast((List<?>) message
                                        .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new ArrayList<WSHandlerResult>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }
                        WSHandlerResult rResult = new WSHandlerResult(null, samlResults);
                        results.add(0, rResult);

                        assertSamlTokens(message);

                        Principal principal =
                                        (Principal) samlResults.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
                        message.put(WSS4JInInterceptor.PRINCIPAL_RESULT, principal);

                        SecurityContext sc = message.get(SecurityContext.class);
                        if (sc == null || sc.getUserPrincipal() == null) {
                            message.put(SecurityContext.class, new DefaultSecurityContext(principal, null));
                        }

                    }
                } catch (WSSecurityException ex) {
                    throw new Fault(ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    @SuppressWarnings("unchecked")
    @Trivial
    private List<WSSecurityEngineResult> processToken(Element tokenElement, final SoapMessage message)
                    throws WSSecurityException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "processToken(1):" + tokenElement);
        };
        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        RequestData data = new RequestData() {
            @Override
            public CallbackHandler getCallbackHandler() {
                return getCallback(message);
            }

            @Override
            public Validator getValidator(QName qName) throws WSSecurityException {
                String key = null;
                if (WSSecurityEngine.SAML_TOKEN.equals(qName)) {
                    key = SecurityConstants.SAML1_TOKEN_VALIDATOR;
                } else if (WSSecurityEngine.SAML2_TOKEN.equals(qName)) {
                    key = SecurityConstants.SAML2_TOKEN_VALIDATOR;
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "found key?:" + (key != null));
                };
                if (key != null) {
                    Object o = message.getContextualProperty(key);
                    try {
                        if (o instanceof Validator) {
                            return (Validator) o;
                        } else if (o instanceof Class) {
                            return (Validator) ((Class<?>) o).newInstance();
                        } else if (o instanceof String) {
                            return (Validator) ClassLoaderUtils.loadClass(o.toString(),
                                                                          WSSecuritySamlTokenInterceptor.class)
                                            .newInstance();
                        }
                    } catch (RuntimeException t) {
                        throw t;
                    } catch (Throwable t) {
                        throw new WSSecurityException(t.getMessage(), t);
                    }
                }
                return super.getValidator(qName);
            }
        };
        data.setWssConfig(WSSConfig.getNewInstance());
        // IBM Specific settings.
        SAMLTokenProcessor p = new SAMLTokenProcessor();
        // Get the cryptor properties and set them into requestData
        Object o = message.getContextualProperty(CXF_SIG_PROPS);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "found sig object:" + (o != null));
        };
        if (o != null) {
            Map<String, Object> sigPropsMap = (Map<String, Object>) o;
            Properties sigProps = new Properties();
            sigProps.putAll(sigPropsMap);
            // Do not print out the trace unless necessary. It will show the key-password
            //System.out.println("sigProps:" + sigProps);
            //org.apache.ws.security.crypto.provider=org.apache.ws.security.components.crypto.Merlin
            Crypto sigCrypto = CryptoFactory.getInstance(sigProps);
            //System.out.println("sig/encCrypto:" + sigCrypto);
            data.setEncCrypto(sigCrypto);
        }
        // Get the enc cryptor properties and set them into requestData
        Object oe = message.getContextualProperty(CXF_ENC_PROPS);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "found enc object:" + (oe != null));
        };
        if (oe != null) {
            Map<String, Object> encPropsMap = (Map<String, Object>) oe;
            Properties encProps = new Properties();
            encProps.putAll(encPropsMap);
            // Do not print out the trace unless necessary. It will show the key-password
            //org.apache.ws.security.crypto.provider=org.apache.ws.security.components.crypto.Merlin
            //System.out.println("enc/sigProps:" + encProps);
            Crypto encCrypto = CryptoFactory.getInstance(encProps);
            //System.out.println("sig/encCrypto:" + encCrypto);
            data.setSigCrypto(encCrypto);
        }

        List<WSSecurityEngineResult> results =
                        p.handleToken(tokenElement, data, wsDocInfo);
        return results;
    }

    @Trivial
    private SamlToken assertSamlTokens(SoapMessage message) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "assertSamlToken(1)");
        };
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.SAML_TOKEN);
        SamlToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (SamlToken) ai.getAssertion();
            ai.setAsserted(true);
        }
        ais = aim.getAssertionInfo(SP12Constants.SUPPORTING_TOKENS);
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }
        ais = aim.getAssertionInfo(SP12Constants.SIGNED_SUPPORTING_TOKENS);
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "assertSamlToken(2)" + (tok != null));
        };
        return tok;
    }

    @Trivial
    private Header findSecurityHeader(SoapMessage message, boolean create) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "findSecurityHeader(1) create" + create);
        };
        for (Header h : message.getHeaders()) {
            QName n = h.getName();
            if (n.getLocalPart().equals("Security")
                && (n.getNamespaceURI().equals(WSConstants.WSSE_NS)
                || n.getNamespaceURI().equals(WSConstants.WSSE11_NS))) {
                return h;
            }
        }
        if (!create) {
            return null;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "findSecurityHeader(2)");
        };
        Document doc = DOMUtils.createDocument();
        Element el = doc.createElementNS(WSConstants.WSSE_NS, "wsse:Security");
        el.setAttributeNS(WSConstants.XMLNS_NS, "xmlns:wsse", WSConstants.WSSE_NS);
        SoapHeader sh = new SoapHeader(new QName(WSConstants.WSSE_NS, "Security"), el);
        sh.setMustUnderstand(true);
        message.getHeaders().add(sh);
        return sh;
    }

    private CallbackHandler getCallback(SoapMessage message) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);

        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler) o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler) ClassLoaderUtils
                                .loadClass((String) o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getCallBack():" + handler);
        };
        return handler;
    }

}
