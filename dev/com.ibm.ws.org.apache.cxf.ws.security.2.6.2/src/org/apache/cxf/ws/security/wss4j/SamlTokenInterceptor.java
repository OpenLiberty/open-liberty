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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.cache.ReplayCacheFactory;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.cache.ReplayCache;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.processor.SAMLTokenProcessor;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.SAMLParms;
import org.apache.ws.security.validate.Validator;

import org.opensaml.common.SAMLVersion;

/**
 * An interceptor to create and add a SAML token to the security header of an outbound
 * request, and to process a SAML Token on an inbound request.
 */
public class SamlTokenInterceptor extends AbstractSoapInterceptor {
    
    public static final String WSSEC = "ws-security";
    public static final String CXF_SIG_PROPS = WSSEC + ".signature.properties";
    public static final String CXF_ENC_PROPS = WSSEC + ".encryption.properties";
    public static final String SAML_ONE_TIME_USE_CACHE_INSTANCE = "ws-security.saml.cache.instance";
    public static final String ENABLE_SAML_ONE_TIME_USE_CACHE = "ws-security.enable.saml.cache";
    
    private static final Logger LOG = LogUtils.getL7dLogger(SamlTokenInterceptor.class);
    private static final Set<QName> HEADERS = new HashSet<QName>();

    static {
        HEADERS.add(new QName(WSConstants.WSSE_NS, "Security"));
        HEADERS.add(new QName(WSConstants.WSSE11_NS, "Security"));
    }

    /**
     * @param p
     */
    public SamlTokenInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addAfter(PolicyBasedWSS4JOutInterceptor.class.getName());
        addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
    }
    
    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    public void handleMessage(SoapMessage message) throws Fault {

        boolean isReq = MessageUtils.isRequestor(message);
        boolean isOut = MessageUtils.isOutbound(message);
        
        if (isReq != isOut) {
            //outbound on server side and inbound on client side doesn't need
            //any saml token stuff, assert policies and return
            assertSamlTokens(message);
            return;
        }
        if (isReq) {
            if (message.containsKey(PolicyBasedWSS4JOutInterceptor.SECURITY_PROCESSED)) {
                //The full policy interceptors handled this
                return;
            }
            addSamlToken(message);
        } else {
            if (message.containsKey(WSS4JInInterceptor.SECURITY_PROCESSED)) {
                //The full policy interceptors handled this
                return;
            }
            processSamlToken(message);
        }
    }
    
    private void processSamlToken(SoapMessage message) {
        Header h = findSecurityHeader(message, false);
        if (h == null) {
            return;
        }
        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if ("Assertion".equals(child.getLocalName())) {
                try {
                    List<WSSecurityEngineResult> samlResults = processToken(child, message);
                    if (samlResults != null) {
                        List<WSHandlerResult> results = CastUtils.cast((List<?>)message
                                .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new ArrayList<WSHandlerResult>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }
                        WSHandlerResult rResult = new WSHandlerResult(null, samlResults);
                        results.add(0, rResult);

                        assertSamlTokens(message);
                        
                        Principal principal = 
                            (Principal)samlResults.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
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

    private List<WSSecurityEngineResult> processToken(Element tokenElement, final SoapMessage message)
        throws WSSecurityException {
        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        RequestData data = new RequestData() {
            public CallbackHandler getCallbackHandler() {
                return getCallback(message);
            }
            public Validator getValidator(QName qName) throws WSSecurityException {
                String key = null;
                if (WSSecurityEngine.SAML_TOKEN.equals(qName)) {
                    key = SecurityConstants.SAML1_TOKEN_VALIDATOR;
                } else if (WSSecurityEngine.SAML2_TOKEN.equals(qName)) {
                    key = SecurityConstants.SAML2_TOKEN_VALIDATOR;
                } 
                if (key != null) {
                    Object o = message.getContextualProperty(key);
                    try {
                        if (o instanceof Validator) {
                            return (Validator)o;
                        } else if (o instanceof Class) {
                            return (Validator)((Class<?>)o).newInstance();
                        } else if (o instanceof String) {
                            return (Validator)ClassLoaderUtils.loadClass(o.toString(),
                                                                         SamlTokenInterceptor.class)
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
        ReplayCache samlCache = 
                getReplayCache(
                    message, ENABLE_SAML_ONE_TIME_USE_CACHE, 
                    SAML_ONE_TIME_USE_CACHE_INSTANCE
            );
        data.setSamlOneTimeUseReplayCache(samlCache);
        SAMLTokenProcessor p = new SAMLTokenProcessor();
        // Get the cryptor properties and set them into requestData
        Object o = message.getContextualProperty(CXF_SIG_PROPS);
        // System.out.println("sigProps:" + o);
        if (o != null) {
            // @TJJ was forced to add a try catch for the 
            // Map<String, Object> sigPropsMap = (Map<String, Object>)o;
            // As compiler wont compile because its an unchecked cast
            @SuppressWarnings("unchecked")
            Map<String, Object> sigPropsMap = (Map<String, Object>)o;
            Properties sigProps = new Properties();
            sigProps.putAll(sigPropsMap);
            //System.out.println("sigProps:" + sigProps);
            //org.apache.ws.security.crypto.provider=org.apache.ws.security.components.crypto.Merlin
            sigProps.put("org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin");
            Crypto sigCrypto = CryptoFactory.getInstance(sigProps);
            //System.out.println("sig/encCrypto:" + sigCrypto);
            data.setEncCrypto(sigCrypto);
        }
        // Get the enc cryptor properties and set them into requestData
        Object oe = message.getContextualProperty(CXF_ENC_PROPS);
        //System.out.println("encProps:" + oe);
        if (oe != null) {
            // @TJJ was forced to add a unchecked warning 
            // Map<String, Object> sigPropsMap = (Map<String, Object>)oe;
            // As compiler wont compile because its an unchecked cast
            @SuppressWarnings("unchecked")
            Map<String, Object> encPropsMap = (Map<String, Object>)oe;
            Properties encProps = new Properties();
            encProps.putAll(encPropsMap);
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

    /**
     * Get a ReplayCache instance. It first checks to see whether caching has been explicitly
     * enabled or disabled via the booleanKey argument. If it has been set to false then no
     * replay caching is done (for this booleanKey). If it has not been specified, then caching
     * is enabled only if we are not the initiator of the exchange. If it has been specified, then
     * caching is enabled.
     * 
     * It tries to get an instance of ReplayCache via the instanceKey argument from a
     * contextual property, and failing that the message exchange. If it can't find any, then it
     * defaults to using an EH-Cache instance and stores that on the message exchange.
     */
    protected ReplayCache getReplayCache(
                                         SoapMessage message, String booleanKey, String instanceKey) {
        boolean specified = false;
        Object o = message.getContextualProperty(booleanKey);
        if (o != null) {
            if (!MessageUtils.isTrue(o)) {
                return null;
            }
            specified = true;
        }

        if (!specified && MessageUtils.isRequestor(message)) {
            return null;
        }
        Endpoint ep = message.getExchange().get(Endpoint.class);
        if (ep != null && ep.getEndpointInfo() != null) {
            EndpointInfo info = ep.getEndpointInfo();
            synchronized (info) {
                ReplayCache replayCache =
                                (ReplayCache) message.getContextualProperty(instanceKey);
                if (replayCache == null) {
                    replayCache = (ReplayCache) info.getProperty(instanceKey);
                }
                if (replayCache == null) {
                    ReplayCacheFactory replayCacheFactory = ReplayCacheFactory.newInstance();
                    String cacheKey = instanceKey;
                    if (info.getName() != null) {
                        cacheKey += "-" + info.getName().toString().hashCode();
                    }
                    replayCache = replayCacheFactory.newReplayCache(cacheKey, message);
                    info.setProperty(instanceKey, replayCache);
                }
                return replayCache;
            }
        }
        return null;
    }

    private SamlToken assertSamlTokens(SoapMessage message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.SAML_TOKEN);
        SamlToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (SamlToken)ai.getAssertion();
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
        return tok;
    }


    private void addSamlToken(SoapMessage message) {
        SamlToken tok = assertSamlTokens(message);

        Header h = findSecurityHeader(message, true);
        try {
            AssertionWrapper wrapper = addSamlToken(tok, message);
            if (wrapper == null) {
                AssertionInfoMap aim = message.get(AssertionInfoMap.class);
                Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.SAML_TOKEN);
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

    
    private AssertionWrapper addSamlToken(
        SamlToken token, SoapMessage message
    ) throws WSSecurityException {
        //
        // Get the SAML CallbackHandler
        //
        Object o = message.getContextualProperty(SecurityConstants.SAML_CALLBACK_HANDLER);

        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        if (handler == null) {
            return null;
        }

        SAMLParms samlParms = new SAMLParms();
        samlParms.setCallbackHandler(handler);
        if (token.isUseSamlVersion11Profile10() || token.isUseSamlVersion11Profile11()) {
            samlParms.setSAMLVersion(SAMLVersion.VERSION_11);
        } else if (token.isUseSamlVersion20Profile11()) {
            samlParms.setSAMLVersion(SAMLVersion.VERSION_20);
        }
        AssertionWrapper assertion = new AssertionWrapper(samlParms);

        boolean selfSignAssertion = 
            MessageUtils.getContextualBoolean(
                message, SecurityConstants.SELF_SIGN_SAML_ASSERTION, false
            );
        if (selfSignAssertion) {
            Crypto crypto = 
                getCrypto(
                    token, SecurityConstants.SIGNATURE_CRYPTO,
                    SecurityConstants.SIGNATURE_PROPERTIES, message
                );

            String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
            String user = (String)message.getContextualProperty(userNameKey);
            if (crypto != null && StringUtils.isEmpty(user)) {
                try {
                    user = crypto.getDefaultX509Identifier();
                } catch (WSSecurityException e1) {
                    throw new Fault(e1);
                }
            }
            if (StringUtils.isEmpty(user)) {
                return null;
            }

            String password = (String)message.getContextualProperty(SecurityConstants.PASSWORD);
            if (StringUtils.isEmpty(password)) {
                password = getPassword(user, token, WSPasswordCallback.SIGNATURE, message);
            }
            if (password == null) {
                password = "";
            }

            // TODO configure using a KeyValue here
            assertion.signAssertion(user, password, crypto, false);
        }

        return assertion;
    }

    private Crypto getCrypto(
        SamlToken samlToken, 
        String cryptoKey, 
        String propKey,
        SoapMessage message
    ) throws WSSecurityException {
        Crypto crypto = (Crypto)message.getContextualProperty(cryptoKey);
        if (crypto != null) {
            return crypto;
        }

        Object o = message.getContextualProperty(propKey);
        if (o == null) {
            return null;
        }

        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            URL url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, this.getClass());
                }
                if (url == null) {
                    try {
                        url = new URL((String)o);
                    } catch (Exception ex) {
                        //ignore
                    }
                }
                if (url != null) {
                    InputStream ins = url.openStream();
                    properties = new Properties();
                    properties.load(ins);
                    ins.close();
                } else if (samlToken != null) {
                    policyNotAsserted(samlToken, "Could not find properties file " + o, message);
                }
            } catch (IOException e) {
                if (samlToken != null) {
                    policyNotAsserted(samlToken, e.getMessage(), message);
                }
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            try {
                InputStream ins = ((URL)o).openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                if (samlToken != null) {
                    policyNotAsserted(samlToken, e.getMessage(), message);
                }
            }            
        }

        if (properties != null) {
            crypto = CryptoFactory.getInstance(properties);
        }
        return crypto;
    }

    private Header findSecurityHeader(SoapMessage message, boolean create) {
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
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        return handler;
    }
    
    public String getPassword(String userName, SamlToken info, int type, SoapMessage message) {
        //Then try to get the password from the given callback handler
    
        CallbackHandler handler = getCallback(message);
        if (handler == null) {
            policyNotAsserted(info, "No callback handler and no password available", message);
            return null;
        }
        
        WSPasswordCallback[] cb = {new WSPasswordCallback(userName, type)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            policyNotAsserted(info, e, message);
        }
        
        //get the password
        return cb[0].getPassword();
    }
    
    protected void policyNotAsserted(SamlToken assertion, String reason, SoapMessage message) {
        if (assertion == null) {
            return;
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);

        Collection<AssertionInfo> ais;
        ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason);
                }
            }
        }
        if (!assertion.isOptional()) {
            throw new PolicyException(new Message(reason, LOG));
        }
    }
    
    protected void policyNotAsserted(SamlToken assertion, Exception reason, SoapMessage message) {
        if (assertion == null) {
            return;
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais;
        ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason.getMessage());
                }
            }
        }
        throw new PolicyException(reason);
    }
    
    
}
