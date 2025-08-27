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

package org.apache.cxf.ws.security.wss4j;

import java.net.URL;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;
import org.apache.wss4j.dom.saml.DOMSAMLUtil;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SamlToken.SamlTokenType;
import org.opensaml.saml.common.SAMLVersion;

/**
 * An interceptor to create and add a SAML token to the security header of an outbound
 * request, and to process a SAML Token on an inbound request.
 */
public class SamlTokenInterceptor extends AbstractTokenInterceptor {

    public SamlTokenInterceptor() {
        super();
    }

    protected void processToken(SoapMessage message) {
        Header h = findSecurityHeader(message, false);
        if (h == null) {
            return;
        }
        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if ("Assertion".equals(child.getLocalName())
                && (WSS4JConstants.SAML_NS.equals(child.getNamespaceURI())
                    || WSS4JConstants.SAML2_NS.equals(child.getNamespaceURI()))) {
                try {
                    List<WSSecurityEngineResult> samlResults = processToken(child, message);
                    if (samlResults != null) {
                        List<WSHandlerResult> results = CastUtils.cast((List<?>)message
                                .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new ArrayList<>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }

                        boolean signed = false;
                        for (WSSecurityEngineResult result : samlResults) {
                            SamlAssertionWrapper wrapper =
                                (SamlAssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                            if (wrapper.isSigned()) {
                                signed = true;
                                break;
                            }
                        }
                        assertTokens(message, SPConstants.SAML_TOKEN, signed);

                        Integer key = WSConstants.ST_UNSIGNED;
                        if (signed) {
                            key = WSConstants.ST_SIGNED;
                        }
                        WSHandlerResult rResult =
                            new WSHandlerResult(null, samlResults,
                                                Collections.singletonMap(key, samlResults));
                        results.add(0, rResult);

                        // Check version against policy
                        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
                        for (AssertionInfo ai
                            : PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SAML_TOKEN)) {
                            SamlToken samlToken = (SamlToken)ai.getAssertion();
                            for (WSSecurityEngineResult result : samlResults) {
                                SamlAssertionWrapper assertionWrapper =
                                    (SamlAssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);

                                if (!checkVersion(aim, samlToken, assertionWrapper)) {
                                    ai.setNotAsserted("Wrong SAML Version");
                                }

                                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                                Certificate[] tlsCerts = null;
                                if (tlsInfo != null) {
                                    tlsCerts = tlsInfo.getPeerCertificates();
                                }
                                if (!DOMSAMLUtil.checkHolderOfKey(assertionWrapper, null, tlsCerts)) {
                                    ai.setNotAsserted("Assertion fails holder-of-key requirements");
                                    continue;
                                }
                                if (!DOMSAMLUtil.checkSenderVouches(assertionWrapper, tlsCerts, null, null)) {
                                    ai.setNotAsserted("Assertion fails sender-vouches requirements");
                                    continue;
                                }
                            }
                        }

                        if (signed) {
                            Principal principal =
                                (Principal)samlResults.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);

                            SecurityContext sc = message.get(SecurityContext.class);
                            if (sc == null || sc.getUserPrincipal() == null) {
                                message.put(SecurityContext.class, new DefaultSecurityContext(principal, null));
                            }
                        }
                    }
                } catch (WSSecurityException ex) {
                    throw WSS4JUtils.createSoapFault(message, message.getVersion(), ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    private List<WSSecurityEngineResult> processToken(Element tokenElement, final SoapMessage message)
        throws WSSecurityException {

        RequestData data = new CXFRequestData();
        Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message);
        try {
            data.setCallbackHandler(SecurityUtils.getCallbackHandler(o));
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
        data.setMsgContext(message);
        data.setWssConfig(WSSConfig.getNewInstance());

        data.setSigVerCrypto(getCrypto(SecurityConstants.SIGNATURE_CRYPTO,
                                     SecurityConstants.SIGNATURE_PROPERTIES, message));

        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        data.setWsDocInfo(wsDocInfo);

        SAMLTokenProcessor p = new SAMLTokenProcessor();
        return p.handleToken(tokenElement, data);
    }

    protected AbstractToken assertTokens(SoapMessage message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        PolicyUtils.assertPolicy(aim, "WssSamlV11Token10");
        PolicyUtils.assertPolicy(aim, "WssSamlV11Token11");
        PolicyUtils.assertPolicy(aim, "WssSamlV20Token11");
        return assertTokens(message, SPConstants.SAML_TOKEN, true);
    }

    protected void addToken(SoapMessage message) {
        WSSConfig.init();
        SamlToken tok = (SamlToken)assertTokens(message);

        Header h = findSecurityHeader(message, true);
        try {
            SamlAssertionWrapper wrapper = addSamlToken(tok, message);
            if (wrapper == null) {
                AssertionInfoMap aim = message.get(AssertionInfoMap.class);
                Collection<AssertionInfo> ais =
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SAML_TOKEN);
                for (AssertionInfo ai : ais) {
                    if (ai.isAsserted()) {
                        ai.setAsserted(false);
                    }
                }
                return;
            }
            Element el = (Element)h.getObject();
            el = (Element)DOMUtils.getDomElement(el);
            el.appendChild(wrapper.toDOM(el.getOwnerDocument()));
        } catch (WSSecurityException ex) {
            policyNotAsserted(tok, ex.getMessage(), message);
        }
    }


    private SamlAssertionWrapper addSamlToken(
        SamlToken token, SoapMessage message
    ) throws WSSecurityException {
        //
        // Get the SAML CallbackHandler
        //
        Object o =
            SecurityUtils.getSecurityPropertyValue(SecurityConstants.SAML_CALLBACK_HANDLER, message);

        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        if (handler == null) {
            return null;
        }

        AssertionInfoMap aim = message.get(AssertionInfoMap.class);

        SAMLCallback samlCallback = new SAMLCallback();
        SamlTokenType tokenType = token.getSamlTokenType();
        if (tokenType == SamlTokenType.WssSamlV11Token10 || tokenType == SamlTokenType.WssSamlV11Token11) {
            samlCallback.setSamlVersion(Version.SAML_11);
            PolicyUtils.assertPolicy(aim, "WssSamlV11Token10");
            PolicyUtils.assertPolicy(aim, "WssSamlV11Token11");

        } else if (tokenType == SamlTokenType.WssSamlV20Token11) {
            samlCallback.setSamlVersion(Version.SAML_20);
            PolicyUtils.assertPolicy(aim, "WssSamlV20Token11");
        }
        SAMLUtil.doSAMLCallback(handler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        if (samlCallback.isSignAssertion()) {
            String issuerName = samlCallback.getIssuerKeyName();
            if (issuerName == null) {
                String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
                issuerName = (String)SecurityUtils.getSecurityPropertyValue(userNameKey, message);
            }
            String password = samlCallback.getIssuerKeyPassword();
            if (password == null) {
                password =
                    (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.PASSWORD, message);
                if (StringUtils.isEmpty(password)) {
                    password =
                        (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD, message);
                }
                if (StringUtils.isEmpty(password)) {
                    password =
                        getPassword(issuerName, token, WSPasswordCallback.SIGNATURE, message);
                }
            }
            Crypto crypto = samlCallback.getIssuerCrypto();
            if (crypto == null) {
                crypto =
                    getCrypto(SecurityConstants.SIGNATURE_CRYPTO,
                              SecurityConstants.SIGNATURE_PROPERTIES, message);
            }

            assertion.signAssertion(
                    issuerName,
                    password,
                    crypto,
                    samlCallback.isSendKeyValue(),
                    samlCallback.getCanonicalizationAlgorithm(),
                    samlCallback.getSignatureAlgorithm()
            );
        }

        return assertion;
    }

    private Crypto getCrypto(
        String cryptoKey,
        String propKey,
        SoapMessage message
    ) throws WSSecurityException {
        Crypto crypto = (Crypto)SecurityUtils.getSecurityPropertyValue(cryptoKey, message);
        if (crypto != null) {
            return crypto;
        }

        Object o = SecurityUtils.getSecurityPropertyValue(propKey, message);
        if (o == null) {
            return null;
        }

        URL propsURL = SecurityUtils.loadResource(message, o);
        Properties properties = WSS4JUtils.getProps(o, propsURL);

        if (properties != null) {
            PasswordEncryptor passwordEncryptor = WSS4JUtils.getPasswordEncryptor(message);
            crypto = CryptoFactory.getInstance(properties, this.getClass().getClassLoader(), passwordEncryptor);
        }
        return crypto;
    }

    /**
     * Check the policy version against the received assertion
     */
    private boolean checkVersion(
        AssertionInfoMap aim,
        SamlToken samlToken,
        SamlAssertionWrapper assertionWrapper
    ) {
        SamlTokenType tokenType = samlToken.getSamlTokenType();
        if ((tokenType == SamlTokenType.WssSamlV11Token10
            || tokenType == SamlTokenType.WssSamlV11Token11)
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_11) {
            return false;
        } else if (tokenType == SamlTokenType.WssSamlV20Token11
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_20) {
            return false;
        }
        PolicyUtils.assertPolicy(aim, new QName(samlToken.getVersion().getNamespace(), tokenType.name()));
        return true;
    }

}
