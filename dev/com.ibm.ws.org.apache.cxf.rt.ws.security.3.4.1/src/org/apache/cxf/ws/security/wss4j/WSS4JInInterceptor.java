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
import java.security.Provider;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.ThreadLocalSecurityProvider;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.validate.NoOpValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;

/**
 * Performs WS-Security inbound actions.
 */
public class WSS4JInInterceptor extends AbstractWSS4JInterceptor {

    /**
     * This configuration tag specifies the default attribute name where the roles are present
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME_DEFAULT =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    public static final String PROCESSOR_MAP = "wss4j.processor.map";
    public static final String VALIDATOR_MAP = "wss4j.validator.map";

    public static final String SECURITY_PROCESSED = WSS4JInInterceptor.class.getName() + ".DONE";

    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JInInterceptor.class);
    private boolean ignoreActions;

    /**
     *
     */
    private WSSConfig defaultConfig;


    public WSS4JInInterceptor() {
        super();

        setPhase(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJInInterceptor.class.getName());
        getAfter().add("org.apache.cxf.ws.addressing.soap.MAPCodec");
    }
    public WSS4JInInterceptor(boolean ignore) {
        this();
        ignoreActions = ignore;
    }

    public WSS4JInInterceptor(Map<String, Object> properties) {
        this();
        setProperties(properties);
        WSSConfig config = WSSConfig.getNewInstance();

        // Set any custom WSS4J Processor instances that are configured
        final Map<QName, Object> processorMap = CastUtils.cast(
            (Map<?, ?>)properties.get(PROCESSOR_MAP));
        if (processorMap != null) {
            for (Map.Entry<QName, Object> entry : processorMap.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Class<?>) {
                    config.setProcessor(entry.getKey(), (Class<?>)val);
                } else if (val instanceof Processor) {
                    config.setProcessor(entry.getKey(), (Processor)val);
                } else if (val == null) {
                    config.setProcessor(entry.getKey(), (Class<?>)null);
                }
            }
        }

        // Set any custom WSS4J Validator instances that are configured
        Map<QName, Object> validatorMap = CastUtils.cast(
            (Map<?, ?>)properties.get(VALIDATOR_MAP));
        if (validatorMap == null) {
            validatorMap = CastUtils.cast((Map<?, ?>)properties.get(ConfigurationConstants.VALIDATOR_MAP));
        }
        if (validatorMap != null) {
            for (Map.Entry<QName, Object> entry : validatorMap.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Class<?>) {
                    config.setValidator(entry.getKey(), (Class<?>)val);
                } else if (val instanceof Validator) {
                    config.setValidator(entry.getKey(), (Validator)val);
                }
            }
        }

        defaultConfig = config;
    }

    /**
     * Setting this value to true means that WSS4J does not compare the "actions" that were processed against
     * the list of actions that were configured. It also means that CXF/WSS4J does not throw an error if no actions
     * were specified. Setting this to true could be a potential security risk, as there is then no guarantee that
     * the message contains the desired security token.
     */
    public void setIgnoreActions(boolean i) {
        ignoreActions = i;
    }

    private SOAPMessage getSOAPMessage(SoapMessage msg) {
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        return msg.getContent(SOAPMessage.class);
    }

    @Override
    public Object getProperty(Object msgContext, String key) {
        // use the superclass first
        Object result = super.getProperty(msgContext, key);

        // handle the special case of the SEND_SIGV
        if (result == null
            && WSHandlerConstants.SEND_SIGV.equals(key)
            && this.isRequestor((SoapMessage)msgContext)) {
            result = ((SoapMessage)msgContext).getExchange().getOutMessage().get(key);
        }
        return result;
    }
    public final boolean isGET(SoapMessage message) {
        String method = (String)message.get(org.apache.cxf.message.Message.HTTP_REQUEST_METHOD);
        return "GET".equals(method) && message.getContent(XMLStreamReader.class) == null;
    }

    public void handleMessage(SoapMessage msg) throws Fault {
        if (msg.containsKey(SECURITY_PROCESSED) || isGET(msg) || msg.getExchange() == null) {
            return;
        }

        Object provider = msg.getExchange().get(Provider.class);
        final boolean useCustomProvider = provider != null && ThreadLocalSecurityProvider.isInstalled();
        try {
            if (useCustomProvider) {
                ThreadLocalSecurityProvider.setProvider((Provider)provider);
            }
            handleMessageInternal(msg);
        } finally {
            if (useCustomProvider) {
                ThreadLocalSecurityProvider.unsetProvider();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleMessageInternal(SoapMessage msg) throws Fault {
        boolean utWithCallbacks =
            MessageUtils.getContextualBoolean(msg, SecurityConstants.VALIDATE_TOKEN, true);
        translateProperties(msg);

        RequestData reqData = new CXFRequestData();

        WSSConfig config = (WSSConfig)msg.getContextualProperty(WSSConfig.class.getName());
        WSSecurityEngine engine;
        if (config != null) {
            engine = new WSSecurityEngine();
            engine.setWssConfig(config);
        } else {
            engine = getSecurityEngine(utWithCallbacks);
            if (engine == null) {
                engine = new WSSecurityEngine();
            }
            config = engine.getWssConfig();
        }
        reqData.setWssConfig(config);
        //Liberty code change start, for debug only
        boolean doDebug = LOG.isLoggable(Level.FINE);
        
        if (doDebug) {
            LOG.fine("WSS4JInInterceptor: saml audience restriction validation = " + SecurityUtils.getSecurityPropertyBoolean(SecurityConstants.AUDIENCE_RESTRICTION_VALIDATION,                                                                                                                msg, true));
        }
        //Liberty code change end
        // Add Audience Restrictions for SAML
        reqData.setAudienceRestrictions(SAMLUtils.getAudienceRestrictions(msg, true));

        SOAPMessage doc = getSOAPMessage(msg);

        

        SoapVersion version = msg.getVersion();
        try {
            reqData.setEncryptionSerializer(new StaxSerializer());
        } catch (InvalidCanonicalizerException e) {
            throw new SoapFault(new Message("SECURITY_FAILED", LOG), e, version.getReceiver());
        }

        if (doDebug) {
            LOG.fine("WSS4JInInterceptor: enter handleMessage()");
        }

        /*
         * The overall try, just to have a finally at the end to perform some
         * housekeeping.
         */
        try {
            reqData.setMsgContext(msg);
            reqData.setAttachmentCallbackHandler(new AttachmentCallbackHandler(msg));

            setAlgorithmSuites(msg, reqData);

            reqData.setCallbackHandler(getCallback(reqData, utWithCallbacks));

            computeAction(msg, reqData);
            String action = getAction(msg, version);
            List<Integer> actions = WSSecurityUtil.decodeAction(action);

            String actor = (String)getOption(ConfigurationConstants.ACTOR);
            if (actor == null) {
                actor = (String)msg.getContextualProperty(SecurityConstants.ACTOR);
            }
            reqData.setActor(actor);

            // Configure replay caching
            configureReplayCaches(reqData, actions, msg);

            TLSSessionInfo tlsInfo = msg.get(TLSSessionInfo.class);
            if (tlsInfo != null) {
                Certificate[] tlsCerts = tlsInfo.getPeerCertificates();
                reqData.setTlsCerts(tlsCerts);
            }

            /*
             * Get and check the Signature specific parameters first because
             * they may be used for encryption too.
             */
            doReceiverAction(actions, reqData);

            // Only search for and expand (Signed) XOP Elements if MTOM is enabled (and not
            // explicitly specified by the user)
            if (getString(ConfigurationConstants.EXPAND_XOP_INCLUDE_FOR_SIGNATURE, msg) == null
                && getString(ConfigurationConstants.EXPAND_XOP_INCLUDE, msg) == null) {
                reqData.setExpandXopInclude(AttachmentUtil.isMtomEnabled(msg));
            }

            /*get chance to check msg context enableRevocation setting
             *when use policy based ws-security where the WSHandler configuration
             *isn't available
             */
            boolean enableRevocation = reqData.isRevocationEnabled()
                || PropertyUtils.isTrue(SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENABLE_REVOCATION,
                                       msg));
            reqData.setEnableRevocation(enableRevocation);

            Element soapBody = SAAJUtils.getBody(doc);
            if (soapBody != null) {
                engine.setCallbackLookup(new CXFCallbackLookup(soapBody.getOwnerDocument(), soapBody));
            }

            Element elem =
                WSSecurityUtil.getSecurityHeader(doc.getSOAPHeader(), actor, version.getVersion() != 1.1);
            elem = (Element)DOMUtils.getDomElement(elem);
            Node originalNode = null;
            if (elem != null) {
                originalNode = elem.cloneNode(true);
            }
            WSHandlerResult wsResult = engine.processSecurityHeader(elem, reqData);
            importNewDomToSAAJ(doc, elem, originalNode, wsResult);
            Element header = SAAJUtils.getHeader(doc);
            Element body = SAAJUtils.getBody(doc);
            header = (Element)DOMUtils.getDomElement(header);
            body = (Element)DOMUtils.getDomElement(body);
            if (!(wsResult.getResults() == null || wsResult.getResults().isEmpty())) {
                // security header found
                if (reqData.isEnableSignatureConfirmation()) {
                    checkSignatureConfirmation(reqData, wsResult);
                }

                checkActions(msg, wsResult.getResults(), actions);

                doResults(
                    msg, actor,
                    header,
                    body,
                    wsResult, utWithCallbacks
                );
            } else { // no security header found
                if (doc.getSOAPPart().getEnvelope().getBody().hasFault() && isRequestor(msg)) {
                    LOG.warning("The request is a SOAP Fault, but it is not secured");
                    // We allow lax action matching here for backwards compatibility
                    // with manually configured WSS4JInInterceptors that previously
                    // allowed faults to pass through even if their actions aren't
                    // a strict match against those configured.  In the WS-SP case,
                    // we will want to still call doResults as it handles asserting
                    // certain assertions that do not require a WS-S header such as
                    // a sp:TransportBinding assertion.  In the case of WS-SP,
                    // the unasserted assertions will provide confirmation that
                    // security was not sufficient.
                    // checkActions(msg, reqData, wsResult, actions);
                    doResults(msg, actor,
                              header,
                              body,
                              wsResult, utWithCallbacks);
                } else {
                    checkActions(msg, wsResult.getResults(), actions);
                    doResults(msg, actor,
                              header,
                              body,
                              wsResult, utWithCallbacks);
                }
            }
            if (SAAJUtils.getBody(doc) != null) {
                advanceBody(msg, body);
            }
            SAAJInInterceptor.replaceHeaders(doc, msg);

            if (doDebug) {
                LOG.fine("WSS4JInInterceptor: exit handleMessage()");
            }
            msg.put(SECURITY_PROCESSED, Boolean.TRUE);

        } catch (WSSecurityException e) {
            throw WSS4JUtils.createSoapFault(msg, version, e);
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("STAX_EX", LOG), e, version.getSender());
        } catch (SOAPException e) {
            throw new SoapFault(new Message("SAAJ_EX", LOG), e, version.getSender());
        } finally {
            reqData = null;
        }
    }
    private void importNewDomToSAAJ(SOAPMessage doc, Element elem,
                                    Node originalNode, WSHandlerResult wsResult) throws SOAPException {
        if (DOMUtils.isJava9SAAJ()
            && originalNode != null && !originalNode.isEqualNode(elem)) {
            //ensure the new decrypted dom element could be imported into the SAAJ
            Node node = null;
            Document document = null;
            Element body = SAAJUtils.getBody(doc);
            if (body != null) {
                document = body.getOwnerDocument();
            }
            if (elem != null && elem.getOwnerDocument() != null
                && elem.getOwnerDocument().getDocumentElement() != null) {
                node = elem.getOwnerDocument().
                    getDocumentElement().getFirstChild().getNextSibling().getFirstChild();
            }
            if (document != null && node != null) {
                Node newNode = null;
                try {
                    newNode = DOMUtils.getDomElement(document.importNode(node, true));
                    elem.getOwnerDocument().getDocumentElement().getFirstChild().
                        getNextSibling().replaceChild(newNode, node);
                    List<WSSecurityEngineResult> encryptResults = wsResult.getActionResults().get(WSConstants.ENCR);
                    if (encryptResults != null) {
                        for (WSSecurityEngineResult result : wsResult.getActionResults().get(WSConstants.ENCR)) {
                            List<WSDataRef> dataRefs = CastUtils.cast((List<?>)result
                                                                      .get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                            for (WSDataRef dataRef : dataRefs) {
                                if (dataRef.getProtectedElement() == node) {
                                    dataRef.setProtectedElement((Element)newNode);
                                }
                            }
                        }
                    }

                    List<WSSecurityEngineResult> signedResults = new ArrayList<>();
                    if (wsResult.getActionResults().containsKey(WSConstants.SIGN)) {
                        signedResults.addAll(wsResult.getActionResults().get(WSConstants.SIGN));
                    }
                    if (wsResult.getActionResults().containsKey(WSConstants.UT_SIGN)) {
                        signedResults.addAll(wsResult.getActionResults().get(WSConstants.UT_SIGN));
                    }
                    if (wsResult.getActionResults().containsKey(WSConstants.ST_SIGNED)) {
                        signedResults.addAll(wsResult.getActionResults().get(WSConstants.ST_SIGNED));
                    }
                    for (WSSecurityEngineResult result : signedResults) {
                        List<WSDataRef> dataRefs = CastUtils.cast((List<?>)result
                                                                  .get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                        for (WSDataRef dataRef :dataRefs) {
                            if (dataRef.getProtectedElement() == node) {
                                dataRef.setProtectedElement((Element)newNode);
                            }
                        }
                    }
                } catch (Exception ex) {
                    //just to the best try
                    LOG.log(Level.FINE, "Something wrong during importNewDomToSAAJ", ex);
                }

            }

        }
    }

    protected void checkActions(
        SoapMessage msg,
        List<WSSecurityEngineResult> wsResult,
        List<Integer> actions
    ) throws WSSecurityException {
        if (ignoreActions) {
            // Not applicable for the WS-SecurityPolicy case
            return;
        }

        // now check the security actions: do they match, in any order?
        if (!checkReceiverResultsAnyOrder(wsResult, actions)) {
            LOG.warning("Security processing failed (actions mismatch)");
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }

        // Now check to see if SIGNATURE_PARTS are specified
        String signatureParts =
            (String)getProperty(msg, ConfigurationConstants.SIGNATURE_PARTS);
        if (signatureParts != null) {
            String warning = "To enforce that particular elements were signed you must either "
                + "use WS-SecurityPolicy, or else use the CryptoCoverageChecker or "
                + "DefaultCryptoCoverageChecker";
            LOG.warning(warning);
        }

    }

    /**
     * Do whatever is necessary to determine the action for the incoming message and
     * do whatever other setup work is necessary.
     *
     * @param msg
     * @param reqData
     */
    protected void computeAction(SoapMessage msg, RequestData reqData) throws WSSecurityException {
        //
        // Try to get Crypto Provider from message context properties.
        // It gives a possibility to use external Crypto Provider
        //
        Crypto encCrypto =
            (Crypto)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CRYPTO, msg);
        if (encCrypto != null) {
            reqData.setDecCrypto(encCrypto);
        }
        Crypto sigCrypto =
            (Crypto)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_CRYPTO, msg);
        if (sigCrypto != null) {
            reqData.setSigVerCrypto(sigCrypto);
        }
    }

    protected void configureReplayCaches(RequestData reqData, List<Integer> actions, SoapMessage msg)
            throws WSSecurityException {
        if (isNonceCacheRequired(actions, msg)) {
            ReplayCache nonceCache =
                getReplayCache(
                    msg, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE
                );
            reqData.setNonceReplayCache(nonceCache);
        }

        if (isTimestampCacheRequired(actions, msg)) {
            ReplayCache timestampCache =
                getReplayCache(
                    msg, SecurityConstants.ENABLE_TIMESTAMP_CACHE, SecurityConstants.TIMESTAMP_CACHE_INSTANCE
                );
            reqData.setTimestampReplayCache(timestampCache);
        }

        if (isSamlCacheRequired(actions, msg)) {
            ReplayCache samlCache =
                getReplayCache(
                    msg, SecurityConstants.ENABLE_SAML_ONE_TIME_USE_CACHE,
                    SecurityConstants.SAML_ONE_TIME_USE_CACHE_INSTANCE
                );
            reqData.setSamlOneTimeUseReplayCache(samlCache);
        }
    }

    /**
     * Is a Nonce Cache required, i.e. are we expecting a UsernameToken
     */
    protected boolean isNonceCacheRequired(List<Integer> actions, SoapMessage msg) {
        return actions.contains(WSConstants.UT) || actions.contains(WSConstants.UT_NOPASSWORD);
    }

    /**
     * Is a Timestamp cache required, i.e. are we expecting a Timestamp
     */
    protected boolean isTimestampCacheRequired(List<Integer> actions, SoapMessage msg) {
        return actions.contains(WSConstants.TS);
    }

    /**
     * Is a SAML Cache required, i.e. are we expecting a SAML Token
     */
    protected boolean isSamlCacheRequired(List<Integer> actions, SoapMessage msg) {
        return actions.contains(WSConstants.ST_UNSIGNED) || actions.contains(WSConstants.ST_SIGNED);
    }

    /**
     * Set a WSS4J AlgorithmSuite object on the RequestData context, to restrict the
     * algorithms that are allowed for encryption, signature, etc.
     */
    protected void setAlgorithmSuites(SoapMessage message, RequestData data) throws WSSecurityException {
        super.decodeAlgorithmSuite(data);
    }

    protected void doResults(
        SoapMessage msg,
        String actor,
        Element soapHeader,
        Element soapBody,
        WSHandlerResult wsResult,
        boolean utWithCallbacks
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        /*
         * All ok up to this point. Now construct and setup the security result
         * structure. The service may fetch this and check it.
         */
        List<WSHandlerResult> results = CastUtils.cast((List<?>)msg.get(WSHandlerConstants.RECV_RESULTS));
        if (results == null) {
            results = new LinkedList<>();
            msg.put(WSHandlerConstants.RECV_RESULTS, results);
        }
        results.add(0, wsResult);

        WSS4JSecurityContextCreator contextCreator =
            (WSS4JSecurityContextCreator)SecurityUtils.getSecurityPropertyValue(
                SecurityConstants.SECURITY_CONTEXT_CREATOR, msg);
        if (contextCreator != null) {
            contextCreator.createSecurityContext(msg, wsResult);
        } else {
            new DefaultWSS4JSecurityContextCreator().createSecurityContext(msg, wsResult);
        }
    }

    protected void advanceBody(
        SoapMessage msg, Node body
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new DOMSource(body));
        // advance just past body
        int evt = reader.next();

        if (reader.hasNext() && evt != XMLStreamConstants.END_ELEMENT) {
            reader.next();
        }

        msg.setContent(XMLStreamReader.class, reader);
    }

    private String getAction(SoapMessage msg, SoapVersion version) {
        String action = (String)getOption(ConfigurationConstants.ACTION);
        if (action == null) {
            action = (String)msg.get(ConfigurationConstants.ACTION);
        }
        if (action == null && !ignoreActions) {
            LOG.warning("No security action was defined!");
            throw new SoapFault("No security action was defined!", version.getReceiver());
        }
        return action;
    }

    protected CallbackHandler getCallback(RequestData reqData, boolean utWithCallbacks)
        throws WSSecurityException {
        if (!utWithCallbacks) {
            CallbackHandler pwdCallback = null;
            try {
                pwdCallback = getCallback(reqData);
            } catch (Exception ex) {
                // ignore
            }
            return new DelegatingCallbackHandler(pwdCallback);
        }
        try {
            return getCallback(reqData);
        } catch (TokenStoreException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
    }

    protected CallbackHandler getCallback(RequestData reqData) throws WSSecurityException, TokenStoreException {
        Object o =
            SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER,
                                                   (SoapMessage)reqData.getMsgContext());
        CallbackHandler cbHandler = null;
        try {
            cbHandler = SecurityUtils.getCallbackHandler(o);
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
        if (cbHandler == null) {
            try {
                cbHandler = getPasswordCallbackHandler(reqData);
            } catch (WSSecurityException sec) {
                Endpoint ep = ((SoapMessage)reqData.getMsgContext()).getExchange().getEndpoint();
                if (ep != null && ep.getEndpointInfo() != null) {
                    TokenStore store =
                        TokenStoreUtils.getTokenStore((SoapMessage)reqData.getMsgContext());
                    return new TokenStoreCallbackHandler(null, store);
                }
                throw sec;
            }
        }

        // Defer to SecurityConstants.SIGNATURE_PASSWORD for decryption if no callback handler is defined
        if (cbHandler == null) {
            String signatureUser =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_USERNAME,
                                                               (SoapMessage)reqData.getMsgContext());
            String password =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD,
                                                       (SoapMessage)reqData.getMsgContext());
            if (!(StringUtils.isEmpty(signatureUser) || StringUtils.isEmpty(password))) {
                cbHandler = new CallbackHandler() {

                    @Override
                    public void handle(Callback[] callbacks)
                        throws IOException, UnsupportedCallbackException {
                        for (Callback c : callbacks) {
                            WSPasswordCallback pwCallback = (WSPasswordCallback)c;
                            if (WSPasswordCallback.DECRYPT == pwCallback.getUsage()
                                && signatureUser.equals(pwCallback.getIdentifier())) {
                                pwCallback.setPassword(password);
                            }
                        }
                    }
                };
            }

        }

        Endpoint ep = ((SoapMessage)reqData.getMsgContext()).getExchange().getEndpoint();
        if (ep != null && ep.getEndpointInfo() != null) {
            TokenStore store = TokenStoreUtils.getTokenStore((SoapMessage)reqData.getMsgContext());
            return new TokenStoreCallbackHandler(cbHandler, store);
        }
        return cbHandler;
    }



    /**
     * @return      the WSSecurityEngine in use by this interceptor.
     */
    protected WSSecurityEngine getSecurityEngine(boolean utWithCallbacks) {
        if (!utWithCallbacks) {
            WSSConfig config = WSSConfig.getNewInstance();
            config.setValidator(WSConstants.USERNAME_TOKEN, new NoOpValidator());
            WSSecurityEngine ret = new WSSecurityEngine();
            ret.setWssConfig(config);
            return ret;
        } else if (defaultConfig != null) {
            WSSecurityEngine engine = new WSSecurityEngine();
            engine.setWssConfig(defaultConfig);
            return engine;
        }

        return null;
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
        SoapMessage message, String booleanKey, String instanceKey
    ) throws WSSecurityException {
        return WSS4JUtils.getReplayCache(message, booleanKey, instanceKey);
    }

}
