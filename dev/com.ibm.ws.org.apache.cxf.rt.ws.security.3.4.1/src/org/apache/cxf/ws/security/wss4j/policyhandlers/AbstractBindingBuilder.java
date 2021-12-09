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

package org.apache.cxf.ws.security.wss4j.policyhandlers;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.Reference;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.wss4j.AttachmentCallbackHandler;
import org.apache.cxf.ws.security.wss4j.CXFCallbackLookup;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.neethi.Assertion;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.callback.CallbackLookup;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecBase;
import org.apache.wss4j.dom.message.WSSecDKSign;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecSignatureConfirmation;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding.ProtectionOrder;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.Attachments;
import org.apache.wss4j.policy.model.ContentEncryptedElements;
import org.apache.wss4j.policy.model.EncryptedElements;
import org.apache.wss4j.policy.model.EncryptedParts;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KeyValueToken;
import org.apache.wss4j.policy.model.Layout.LayoutType;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SamlToken.SamlTokenType;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SignedElements;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.Wss10;
import org.apache.wss4j.policy.model.Wss11;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.policy.model.X509Token.TokenType;

/**
 *
 */
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public abstract class AbstractBindingBuilder extends AbstractCommonBindingHandler {
    public static final String CRYPTO_CACHE = "ws-security.crypto.cache";
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractBindingBuilder.class);

    protected AbstractSymmetricAsymmetricBinding.ProtectionOrder protectionOrder =
        AbstractSymmetricAsymmetricBinding.ProtectionOrder.SignBeforeEncrypting;

    protected final WSSConfig wssConfig;
    protected SOAPMessage saaj;
    protected WSSecHeader secHeader;
    protected AssertionInfoMap aim;
    protected AbstractBinding binding;
    protected WSSecTimestamp timestampEl;
    protected String mainSigId;
    protected List<WSEncryptionPart> sigConfList;

    protected Set<WSEncryptionPart> encryptedTokensList = new HashSet<>();

    protected Set<Integer> signatures = new HashSet<>();

    protected Element bottomUpElement;
    protected Element topDownElement;
    protected Element bstElement;
    protected Element lastEncryptedKeyElement;

    protected final CallbackLookup callbackLookup;
    protected boolean storeBytesInAttachment;
    protected WSDocInfo wsDocInfo;
    private boolean expandXopInclude;

    private Element lastSupportingTokenElement;
    private Element lastDerivedKeyElement;

    private List<AbstractSecurityAssertion> suppTokenParts = new ArrayList<>();
    private List<SupportingToken> endSuppTokList;
    private List<SupportingToken> sgndEndSuppTokList;

    public AbstractBindingBuilder(
                           WSSConfig config,
                           AbstractBinding binding,
                           SOAPMessage saaj,
                           WSSecHeader secHeader,
                           AssertionInfoMap aim,
                           SoapMessage message) throws SOAPException {
        super(message);
        this.wssConfig = config;
        this.binding = binding;
        this.aim = aim;
        this.secHeader = secHeader;
        this.saaj = saaj;
        message.getExchange().put(WSHandlerConstants.SEND_SIGV, signatures);

        boolean storeBytes =
            MessageUtils.getContextualBoolean(
                message, SecurityConstants.STORE_BYTES_IN_ATTACHMENT, true
            );
        boolean mtomEnabled = AttachmentUtil.isMtomEnabled(message);
        if (storeBytes && mtomEnabled) {
            storeBytesInAttachment = true;
            if (binding instanceof AbstractSymmetricAsymmetricBinding
                && (ProtectionOrder.EncryptBeforeSigning
                    == ((AbstractSymmetricAsymmetricBinding)binding).getProtectionOrder()
                    || ((AbstractSymmetricAsymmetricBinding)binding).isProtectTokens())) {
                LOG.fine("Disabling SecurityConstants.STORE_BYTES_IN_ATTACHMENT due to "
                    + "EncryptBeforeSigning or ProtectTokens policy.");
                storeBytesInAttachment = false;
            }
        }
        expandXopInclude = MessageUtils.getContextualBoolean(
            message, SecurityConstants.EXPAND_XOP_INCLUDE, mtomEnabled);

        wsDocInfo = new WSDocInfo(secHeader.getSecurityHeaderElement().getOwnerDocument());

        Element soapBody = SAAJUtils.getBody(saaj);
        if (soapBody != null) {
            callbackLookup = new CXFCallbackLookup(soapBody.getOwnerDocument(), soapBody);
        } else {
            callbackLookup = null;
        }
    }

    protected void insertAfter(Element child, Element sib) {
        child = (Element)DOMUtils.getDomElement(child);
        if (sib.getNextSibling() == null) {
            secHeader.getSecurityHeaderElement().appendChild(child);
        } else {
            secHeader.getSecurityHeaderElement().insertBefore(child, sib.getNextSibling());
        }
    }

    protected void addDerivedKeyElement(Element el) {
        if (lastDerivedKeyElement != null) {
            insertAfter(el, lastDerivedKeyElement);
        } else if (lastEncryptedKeyElement != null) {
            insertAfter(el, lastEncryptedKeyElement);
        } else if (topDownElement != null) {
            insertAfter(el, topDownElement);
        } else if (secHeader.getSecurityHeaderElement().getFirstChild() != null) {
            secHeader.getSecurityHeaderElement().insertBefore(
                el, secHeader.getSecurityHeaderElement().getFirstChild()
            );
        } else {
            secHeader.getSecurityHeaderElement().appendChild(el);
        }
        lastEncryptedKeyElement = el;
    }

    protected void addEncryptedKeyElement(Element el) {
        if (lastEncryptedKeyElement != null) {
            insertAfter(el, lastEncryptedKeyElement);
        } else if (lastDerivedKeyElement != null) {
            secHeader.getSecurityHeaderElement().insertBefore(el, lastDerivedKeyElement);
        } else if (topDownElement != null) {
            insertAfter(el, topDownElement);
        } else if (secHeader.getSecurityHeaderElement().getFirstChild() != null) {
            secHeader.getSecurityHeaderElement().insertBefore(
                el, secHeader.getSecurityHeaderElement().getFirstChild()
            );
        } else {
            secHeader.getSecurityHeaderElement().appendChild(el);
        }
        lastEncryptedKeyElement = el;
    }

    protected void addSupportingElement(Element el) {
        el = (Element)DOMUtils.getDomElement(el);
        if (lastSupportingTokenElement != null) {
            insertAfter(el, lastSupportingTokenElement);
        } else if (lastDerivedKeyElement != null) {
            insertAfter(el, lastDerivedKeyElement);
        } else if (lastEncryptedKeyElement != null) {
            insertAfter(el, lastEncryptedKeyElement);
        } else if (topDownElement != null) {
            insertAfter(el, topDownElement);
        } else if (bottomUpElement != null) {
            secHeader.getSecurityHeaderElement().insertBefore(el, bottomUpElement);
        } else {
            secHeader.getSecurityHeaderElement().appendChild(el);
        }
        lastSupportingTokenElement = el;
    }

    protected void insertBeforeBottomUp(Element el) {
        if (bottomUpElement == null) {
            secHeader.getSecurityHeaderElement().appendChild(el);
        } else {
            secHeader.getSecurityHeaderElement().insertBefore(el, bottomUpElement);
        }
        bottomUpElement = el;
    }

    protected void addTopDownElement(Element el) {
        if (topDownElement == null) {
            if (secHeader.getSecurityHeaderElement().getFirstChild() == null) {
                secHeader.getSecurityHeaderElement().appendChild(el);
            } else {
                secHeader.getSecurityHeaderElement().insertBefore(
                    el, secHeader.getSecurityHeaderElement().getFirstChild()
                );
            }
        } else {
            insertAfter(el, topDownElement);
        }
        topDownElement = el;
    }

    protected final Map<Object, Crypto> getCryptoCache() {
        EndpointInfo info = message.getExchange().getEndpoint().getEndpointInfo();
        synchronized (info) {
            Map<Object, Crypto> o =
                CastUtils.cast((Map<?, ?>)message.getContextualProperty(CRYPTO_CACHE));
            if (o == null) {
                o = CastUtils.cast((Map<?, ?>)info.getProperty(CRYPTO_CACHE));
            }
            if (o == null) {
                o = new ConcurrentHashMap<>();
                info.setProperty(CRYPTO_CACHE, o);
            }
            return o;
        }
    }

    protected final TokenStore getTokenStore() throws TokenStoreException {
        return TokenStoreUtils.getTokenStore(message);
    }

    protected WSSecTimestamp createTimestamp() {
        if (binding.isIncludeTimestamp()) {
            Object o = message.getContextualProperty(SecurityConstants.TIMESTAMP_TTL);
            int ttl = 300;  //default is 300 seconds
            if (o instanceof Number) {
                ttl = ((Number)o).intValue();
            } else if (o instanceof String) {
                ttl = Integer.parseInt((String)o);
            }
            if (ttl <= 0) {
                ttl = 300;
            }
            timestampEl = new WSSecTimestamp(secHeader);
            timestampEl.setIdAllocator(wssConfig.getIdAllocator());
            timestampEl.setWsTimeSource(wssConfig.getCurrentTime());
            timestampEl.setTimeToLive(ttl);
            timestampEl.prepare();

            String namespace = binding.getName().getNamespaceURI();
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.INCLUDE_TIMESTAMP));
        }
        return timestampEl;
    }

    protected WSSecTimestamp handleLayout(WSSecTimestamp timestamp) {
        if (binding.getLayout() != null) {
            AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.LAYOUT);

            if (binding.getLayout().getLayoutType() == LayoutType.LaxTsLast) {
                if (timestamp == null) {
                    ai.setNotAsserted(SPConstants.LAYOUT_LAX_TIMESTAMP_LAST + " requires a timestamp");
                } else {
                    ai.setAsserted(true);
                    assertPolicy(
                        new QName(binding.getLayout().getName().getNamespaceURI(),
                                  SPConstants.LAYOUT_LAX_TIMESTAMP_LAST));
                    Element el = timestamp.getElement();
                    secHeader.getSecurityHeaderElement().appendChild(el);
                    if (bottomUpElement == null) {
                        bottomUpElement = el;
                    }
                }
            } else if (binding.getLayout().getLayoutType() == LayoutType.LaxTsFirst) {
                if (timestamp == null) {
                    ai.setNotAsserted(SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST + " requires a timestamp");
                } else {
                    addTopDownElement(timestampEl.getElement());
                    ai.setAsserted(true);
                    assertPolicy(
                         new QName(binding.getLayout().getName().getNamespaceURI(),
                                   SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST));
                }
            } else if (timestampEl != null) {
                if (ai != null) {
                    ai.setAsserted(true);
                }
                addTopDownElement(timestampEl.getElement());
            } else if (ai != null) {
                ai.setAsserted(true);
            }

            assertPolicy(
                new QName(binding.getLayout().getName().getNamespaceURI(), SPConstants.LAYOUT_LAX));
            assertPolicy(
                new QName(binding.getLayout().getName().getNamespaceURI(), SPConstants.LAYOUT_STRICT));
        } else if (timestampEl != null) {
            addTopDownElement(timestampEl.getElement());
        }
        return timestamp;
    }

    protected void reshuffleTimestamp() {
        // Make sure that the Timestamp is in first place, if that is what the policy requires
        if (binding.getLayout() != null && timestampEl != null) {
            if (binding.getLayout().getLayoutType() == LayoutType.LaxTsFirst
                && secHeader.getSecurityHeaderElement().getFirstChild() != timestampEl.getElement()) {
                Node firstChild = secHeader.getSecurityHeaderElement().getFirstChild();
                while (firstChild != null && firstChild.getNodeType() != Node.ELEMENT_NODE) {
                    firstChild = firstChild.getNextSibling();
                }
                if (firstChild != null && firstChild != timestampEl.getElement()) {
                    secHeader.getSecurityHeaderElement().insertBefore(timestampEl.getElement(), firstChild);
                }
            } else if (binding.getLayout().getLayoutType() == LayoutType.LaxTsLast
                && secHeader.getSecurityHeaderElement().getLastChild() != timestampEl.getElement()) {
                secHeader.getSecurityHeaderElement().appendChild(timestampEl.getElement());
            }
        }
    }

    private List<SupportingToken> handleSupportingTokens(
        Collection<AssertionInfo> tokensInfos,
        boolean endorse
    ) throws WSSecurityException {
        List<SupportingToken> ret = new ArrayList<>();
        if (tokensInfos != null) {
            for (AssertionInfo assertionInfo : tokensInfos) {
                if (assertionInfo.getAssertion() instanceof SupportingTokens) {
                    assertionInfo.setAsserted(true);
                    try {
                        handleSupportingTokens((SupportingTokens)assertionInfo.getAssertion(), endorse, ret);
                    } catch (SOAPException | TokenStoreException ex) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
                    }
                }
            }
        }
        return ret;
    }

    protected List<SupportingToken> handleSupportingTokens(
        SupportingTokens suppTokens,
        boolean endorse,
        List<SupportingToken> ret
    ) throws WSSecurityException, SOAPException, TokenStoreException {
        if (suppTokens == null) {
            return ret;
        }
        for (AbstractToken token : suppTokens.getTokens()) {
            assertToken(token);
            if (!isTokenRequired(token.getIncludeTokenType())) {
                // Check for any SignedParts so as *not* to sign them
                getSignedParts(suppTokens);
                continue;
            }
            if (token instanceof UsernameToken) {
                handleUsernameTokenSupportingToken(
                    (UsernameToken)token, endorse, suppTokens.isEncryptedToken(), ret
                );
            } else if (token instanceof IssuedToken
                    || token instanceof SecureConversationToken
                    || token instanceof SecurityContextToken
                    || token instanceof KerberosToken
                    || token instanceof SpnegoContextToken) {
                //ws-trust/ws-sc stuff.......
                SecurityToken secToken = getSecurityToken();
                if (secToken == null) {
                    unassertPolicy(token, "Could not find IssuedToken");
                }
                Element clone = cloneElement(secToken.getToken());
                secToken.setToken(clone);
                addSupportingElement(clone);

                String id = XMLUtils.getIDFromReference(secToken.getId());

                if (suppTokens.isEncryptedToken()) {
                    WSEncryptionPart part = new WSEncryptionPart(id, "Element");
                    part.setElement(clone);
                    encryptedTokensList.add(part);
                }

                if (secToken.getX509Certificate() == null) {
                    ret.add(
                        new SupportingToken(token, new WSSecurityTokenHolder(secToken, secHeader),
                                            getSignedParts(suppTokens))
                    );
                } else {
                    ret.add(signSupportingToken(secToken, id, token, suppTokens));
                }

            } else if (token instanceof X509Token) {
                //We have to use a cert. Prepare X509 signature
                WSSecSignature sig = getSignatureBuilder(token, false, endorse);
                assertPolicy(suppTokens);
                Element bstElem = sig.getBinarySecurityTokenElement();
                if (bstElem != null) {
                    if (lastEncryptedKeyElement != null) {
                        if (lastEncryptedKeyElement.getNextSibling() != null) {
                            secHeader.getSecurityHeaderElement().insertBefore(bstElem,
                                lastEncryptedKeyElement.getNextSibling());
                        } else {
                            secHeader.getSecurityHeaderElement().appendChild(bstElem);
                        }
                    } else {
                        sig.prependBSTElementToHeader();
                    }
                    if (suppTokens.isEncryptedToken()) {
                        WSEncryptionPart part = new WSEncryptionPart(sig.getBSTTokenId(), "Element");
                        part.setElement(bstElem);
                        encryptedTokensList.add(part);
                    }
                }
                ret.add(new SupportingToken(token, sig, getSignedParts(suppTokens)));
            } else if (token instanceof KeyValueToken) {
                WSSecSignature sig = getSignatureBuilder(token, false, endorse);
                assertPolicy(suppTokens);
                if (suppTokens.isEncryptedToken()) {
                    WSEncryptionPart part = new WSEncryptionPart(sig.getBSTTokenId(), "Element");
                    encryptedTokensList.add(part);
                }
                ret.add(new SupportingToken(token, sig, getSignedParts(suppTokens)));
            } else if (token instanceof SamlToken) {
                SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)token);
                if (assertionWrapper != null) {
                    Element envelope = saaj.getSOAPPart().getEnvelope();
                    envelope = (Element)DOMUtils.getDomElement(envelope);
                    Element assertionElement = assertionWrapper.toDOM(envelope.getOwnerDocument());
                    addSupportingElement(assertionElement);
                    ret.add(new SupportingToken(token, assertionWrapper, getSignedParts(suppTokens)));
                    if (suppTokens.isEncryptedToken()) {
                        WSEncryptionPart part = new WSEncryptionPart(assertionWrapper.getId(), "Element");
                        part.setElement(assertionElement);
                        encryptedTokensList.add(part);
                    }
                }
            }
        }

        return ret;
    }

    private SupportingToken signSupportingToken(SecurityToken secToken, String id,
                                                AbstractToken token, SupportingTokens suppTokens)
        throws SOAPException {
        WSSecSignature sig = new WSSecSignature(secHeader);
        sig.setIdAllocator(wssConfig.getIdAllocator());
        sig.setCallbackLookup(callbackLookup);
        sig.setX509Certificate(secToken.getX509Certificate());
        sig.setCustomTokenId(id);
        sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
        sig.setWsDocInfo(wsDocInfo);
        sig.setExpandXopInclude(isExpandXopInclude());
        sig.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
        sig.setStoreBytesInAttachment(storeBytesInAttachment);

        String tokenType = secToken.getTokenType();
        if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML_NS.equals(tokenType)) {
            sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
        } else if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML2_NS.equals(tokenType)) {
            sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE);
        } else if (tokenType != null) {
            sig.setCustomTokenValueType(tokenType);
        } else {
            sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
        }
        sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAlgorithmSuiteType().getAsymmetricSignature());
        sig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());

        Crypto crypto = secToken.getCrypto();
        String uname = null;
        try {
            uname = crypto.getX509Identifier(secToken.getX509Certificate());
        } catch (WSSecurityException e1) {
            LOG.log(Level.FINE, e1.getMessage(), e1);
            throw new Fault(e1);
        }

        String password =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD, message);
        if (StringUtils.isEmpty(password)) {
            password = getPassword(uname, token, WSPasswordCallback.SIGNATURE);
        }
        sig.setUserInfo(uname, password);
        try {
            sig.prepare(secToken.getCrypto());
        } catch (WSSecurityException e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            throw new Fault(e);
        }

        return new SupportingToken(token, sig, getSignedParts(suppTokens));
    }

    protected void handleUsernameTokenSupportingToken(
        UsernameToken token, boolean endorse, boolean encryptedToken, List<SupportingToken> ret
    ) throws WSSecurityException {
        if (endorse && isTokenRequired(token.getIncludeTokenType())) {
            byte[] salt = UsernameTokenUtil.generateSalt(true);
            WSSecUsernameToken utBuilder = addDKUsernameToken(token, salt, true);
            if (utBuilder != null) {
                utBuilder.prepare(salt);
                addSupportingElement(utBuilder.getUsernameTokenElement());
                ret.add(new SupportingToken(token, utBuilder, null, salt));
                if (encryptedToken) {
                    WSEncryptionPart part = new WSEncryptionPart(utBuilder.getId(), "Element");
                    part.setElement(utBuilder.getUsernameTokenElement());
                    encryptedTokensList.add(part);
                }
            }
        } else if (!endorse) {
            WSSecUsernameToken utBuilder = addUsernameToken(token);
            if (utBuilder != null) {
                utBuilder.prepare();
                addSupportingElement(utBuilder.getUsernameTokenElement());
                ret.add(new SupportingToken(token, utBuilder, null));
                //WebLogic and WCF always encrypt these
                //See:  http://e-docs.bea.com/wls/docs103/webserv_intro/interop.html
                //encryptedTokensIdList.add(utBuilder.getId());
                if (encryptedToken
                    || MessageUtils.getContextualBoolean(message,
                                                         SecurityConstants.ALWAYS_ENCRYPT_UT,
                                                         true)) {
                    WSEncryptionPart part = new WSEncryptionPart(utBuilder.getId(), "Element");
                    part.setElement(utBuilder.getUsernameTokenElement());
                    encryptedTokensList.add(part);
                }
            }
        }
    }

    protected Element cloneElement(Element el) {
        Document doc = secHeader.getSecurityHeaderElement().getOwnerDocument();
        if (!doc.equals(el.getOwnerDocument())) {

            XMLStreamReader reader = StaxUtils.createXMLStreamReader(el);
            DocumentFragment fragment = doc.createDocumentFragment();
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter(fragment);
            try {
                StaxUtils.copy(reader, writer);
                return (Element)fragment.getFirstChild();
            } catch (XMLStreamException ex) {
                LOG.log(Level.FINE, "Error cloning security element", ex);
            }
        }
        return el;
    }

    protected void addSignatureParts(List<SupportingToken> tokenList, List<WSEncryptionPart> sigParts) {

        boolean useSTRTransform =
            MessageUtils.getContextualBoolean(
                message, SecurityConstants.USE_STR_TRANSFORM, true
            );

        for (SupportingToken supportingToken : tokenList) {

            Object tempTok = supportingToken.getTokenImplementation();
            WSEncryptionPart part = null;

            if (tempTok instanceof WSSecSignature) {
                WSSecSignature tempSig = (WSSecSignature) tempTok;
                SecurityTokenReference secRef = tempSig.getSecurityTokenReference();

                if (WSS4JConstants.WSS_SAML_KI_VALUE_TYPE.equals(secRef.getKeyIdentifierValueType())
                    || WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE.equals(secRef.getKeyIdentifierValueType())) {

                    Element secRefElement = cloneElement(secRef.getElement());
                    addSupportingElement(secRefElement);

                    part = new WSEncryptionPart("STRTransform", null, "Element");
                    part.setId(tempSig.getSecurityTokenReferenceURI());
                    part.setElement(secRefElement);
                } else {
                    if (tempSig.getBSTTokenId() != null) {
                        part = new WSEncryptionPart(tempSig.getBSTTokenId());
                        part.setElement(tempSig.getBinarySecurityTokenElement());
                    }
                }
            } else if (tempTok instanceof WSSecUsernameToken) {
                WSSecUsernameToken unt = (WSSecUsernameToken)tempTok;
                part = new WSEncryptionPart(unt.getId());
                part.setElement(unt.getUsernameTokenElement());
            } else if (tempTok instanceof BinarySecurity) {
                BinarySecurity bst = (BinarySecurity)tempTok;
                part = new WSEncryptionPart(bst.getID());
                part.setElement(bst.getElement());
            } else if (tempTok instanceof SamlAssertionWrapper) {
                SamlAssertionWrapper assertionWrapper = (SamlAssertionWrapper)tempTok;

                Document doc = assertionWrapper.getElement().getOwnerDocument();
                boolean saml1 = assertionWrapper.getSaml1() != null;
                if (useSTRTransform) {
                    // TODO We only support using a KeyIdentifier for the moment
                    SecurityTokenReference secRef =
                        createSTRForSamlAssertion(doc, assertionWrapper.getId(), saml1, false);
                    Element clone = cloneElement(secRef.getElement());
                    addSupportingElement(clone);
                    part = new WSEncryptionPart("STRTransform", null, "Element");
                    part.setId(secRef.getID());
                    part.setElement(clone);
                } else {
                    part = new WSEncryptionPart(assertionWrapper.getId());
                    part.setElement(assertionWrapper.getElement());
                }
            } else if (tempTok instanceof WSSecurityTokenHolder) {
                SecurityToken token = ((WSSecurityTokenHolder)tempTok).getToken();
                String tokenType = token.getTokenType();
                if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                    || WSS4JConstants.SAML_NS.equals(tokenType)
                    || WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                    || WSS4JConstants.SAML2_NS.equals(tokenType)) {
                    Document doc = token.getToken().getOwnerDocument();
                    boolean saml1 = WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                        || WSS4JConstants.SAML_NS.equals(tokenType);
                    String id = token.getId();
                    if (id == null || "".equals(id)) {
                        if (saml1) {
                            id = token.getToken().getAttributeNS(null, "AssertionID");
                        } else {
                            id = token.getToken().getAttributeNS(null, "ID");
                        }
                    }
                    if (useSTRTransform) {
                        SecurityTokenReference secRef =
                            createSTRForSamlAssertion(doc, id, saml1, false);
                        Element clone = cloneElement(secRef.getElement());
                        addSupportingElement(clone);
                        part = new WSEncryptionPart("STRTransform", null, "Element");
                        part.setId(secRef.getID());
                        part.setElement(clone);
                    } else {
                        part = new WSEncryptionPart(id);
                        part.setElement(token.getToken());
                    }
                } else {
                    String id = XMLUtils.getIDFromReference(token.getId());

                    part = new WSEncryptionPart(id);
                    part.setElement(token.getToken());
                }
            } else {
                unassertPolicy(supportingToken.getToken(),
                                  "UnsupportedTokenInSupportingToken: " + tempTok);
            }
            if (part != null) {
                sigParts.add(part);
            }
        }
    }

    /**
     * Create a SecurityTokenReference to point to a SAML Assertion
     * @param doc The owner Document instance
     * @param id The Assertion ID
     * @param saml1 Whether the Assertion is a SAML1 or SAML2 Assertion
     * @param useDirectReferenceToAssertion whether to refer directly to the assertion or not
     * @return a SecurityTokenReference to a SAML Assertion
     */
    private SecurityTokenReference createSTRForSamlAssertion(
        Document doc,
        String id,
        boolean saml1,
        boolean useDirectReferenceToAssertion
    ) {
        SecurityTokenReference secRefSaml = new SecurityTokenReference(doc);
        String secRefID = wssConfig.getIdAllocator().createSecureId("STR-", secRefSaml);
        secRefSaml.setID(secRefID);

        if (useDirectReferenceToAssertion) {
            org.apache.wss4j.common.token.Reference ref =
                new org.apache.wss4j.common.token.Reference(doc);
            ref.setURI("#" + id);
            if (saml1) {
                ref.setValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
                secRefSaml.addTokenType(WSS4JConstants.WSS_SAML_TOKEN_TYPE);
            } else {
                secRefSaml.addTokenType(WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
            }
            secRefSaml.setReference(ref);
        } else {
            Element keyId = doc.createElementNS(WSS4JConstants.WSSE_NS, "wsse:KeyIdentifier");
            String valueType = null;
            if (saml1) {
                valueType = WSS4JConstants.WSS_SAML_KI_VALUE_TYPE;
                secRefSaml.addTokenType(WSS4JConstants.WSS_SAML_TOKEN_TYPE);
            } else {
                valueType = WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE;
                secRefSaml.addTokenType(WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
            }
            keyId.setAttributeNS(
                null, "ValueType", valueType
            );
            keyId.appendChild(doc.createTextNode(id));
            Element elem = secRefSaml.getElement();
            elem.appendChild(keyId);
        }
        return secRefSaml;
    }

    protected WSSecUsernameToken addUsernameToken(UsernameToken token) {
        assertToken(token);
        if (!isTokenRequired(token.getIncludeTokenType())) {
            return null;
        }

        String userName = (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.USERNAME, message);
        if (!StringUtils.isEmpty(userName)) {
            WSSecUsernameToken utBuilder = new WSSecUsernameToken(secHeader);
            utBuilder.setIdAllocator(wssConfig.getIdAllocator());
            utBuilder.setWsTimeSource(wssConfig.getCurrentTime());

            // If NoPassword property is set we don't need to set the password
            if (token.getPasswordType() == UsernameToken.PasswordType.NoPassword) {
                utBuilder.setUserInfo(userName, null);
                utBuilder.setPasswordType(null);
            } else {
                String password =
                    (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.PASSWORD, message);
                if (StringUtils.isEmpty(password)) {
                    password = getPassword(userName, token, WSPasswordCallback.USERNAME_TOKEN);
                }

                if (password != null) {
                    // If the password is available then build the token
                    if (token.getPasswordType() == UsernameToken.PasswordType.HashPassword) {
                        utBuilder.setPasswordType(WSS4JConstants.PASSWORD_DIGEST);
                    } else {
                        utBuilder.setPasswordType(WSS4JConstants.PASSWORD_TEXT);
                    }
                    utBuilder.setUserInfo(userName, password);
                } else {
                    unassertPolicy(token, "No password available");
                    return null;
                }
            }

            if (token.isCreated() && token.getPasswordType() != UsernameToken.PasswordType.HashPassword) {
                utBuilder.addCreated();
            }
            if (token.isNonce() && token.getPasswordType() != UsernameToken.PasswordType.HashPassword) {
                utBuilder.addNonce();
            }

            return utBuilder;
        }
        unassertPolicy(token, "No username available");
        return null;
    }

    protected WSSecUsernameToken addDKUsernameToken(UsernameToken token, byte[] salt, boolean useMac) {
        assertToken(token);
        if (!isTokenRequired(token.getIncludeTokenType())) {
            return null;
        }

        String userName = (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.USERNAME, message);
        if (!StringUtils.isEmpty(userName)) {
            WSSecUsernameToken utBuilder = new WSSecUsernameToken(secHeader);
            utBuilder.setIdAllocator(wssConfig.getIdAllocator());
            utBuilder.setWsTimeSource(wssConfig.getCurrentTime());

            String password =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.PASSWORD, message);
            if (StringUtils.isEmpty(password)) {
                password = getPassword(userName, token, WSPasswordCallback.USERNAME_TOKEN);
            }

            if (!StringUtils.isEmpty(password)) {
                // If the password is available then build the token
                utBuilder.setUserInfo(userName, password);
                utBuilder.addDerivedKey(useMac,  1000);
                utBuilder.prepare(salt);
            } else {
                unassertPolicy(token, "No password available");
                return null;
            }

            return utBuilder;
        }
        unassertPolicy(token, "No username available");
        return null;
    }

    protected SamlAssertionWrapper addSamlToken(SamlToken token) throws WSSecurityException, TokenStoreException {
        assertToken(token);
        if (!isTokenRequired(token.getIncludeTokenType())) {
            return null;
        }

        //
        // Get the SAML CallbackHandler
        //
        Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.SAML_CALLBACK_HANDLER, message);

        if (o == null) {
            SecurityToken securityToken = getSecurityToken();
            if (securityToken != null) {
                Element tokenElement = securityToken.getToken();
                String namespace = tokenElement.getNamespaceURI();
                String localname = tokenElement.getLocalName();
                SamlTokenType tokenType = token.getSamlTokenType();
                if ((tokenType == SamlTokenType.WssSamlV11Token10 || tokenType == SamlTokenType.WssSamlV11Token11)
                    && WSS4JConstants.SAML_NS.equals(namespace) && "Assertion".equals(localname)) {
                    return new SamlAssertionWrapper(tokenElement);
                } else if (tokenType == SamlTokenType.WssSamlV20Token11
                    && WSS4JConstants.SAML2_NS.equals(namespace) && "Assertion".equals(localname)) {
                    return new SamlAssertionWrapper(tokenElement);
                }
            }
        }

        SAMLCallback samlCallback = new SAMLCallback();
        SamlTokenType tokenType = token.getSamlTokenType();
        if (tokenType == SamlTokenType.WssSamlV11Token10 || tokenType == SamlTokenType.WssSamlV11Token11) {
            samlCallback.setSamlVersion(Version.SAML_11);
        } else if (tokenType == SamlTokenType.WssSamlV20Token11) {
            samlCallback.setSamlVersion(Version.SAML_20);
        }
        try {
            CallbackHandler handler = SecurityUtils.getCallbackHandler(o);
            if (handler == null) {
                unassertPolicy(token, "No SAML CallbackHandler available");
                return null;
            }
            SAMLUtil.doSAMLCallback(handler, samlCallback);
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
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
                    (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD, message);
                if (StringUtils.isEmpty(password)) {
                    password = getPassword(issuerName, token, WSPasswordCallback.SIGNATURE);
                }
            }
            Crypto crypto = samlCallback.getIssuerCrypto();
            if (crypto == null) {
                crypto = getSignatureCrypto();
            }

            assertion.signAssertion(
                    issuerName,
                    password,
                    crypto,
                    samlCallback.isSendKeyValue(),
                    samlCallback.getCanonicalizationAlgorithm(),
                    samlCallback.getSignatureAlgorithm(),
                    samlCallback.getSignatureDigestAlgorithm()
            );
        }

        return assertion;
    }

    /**
     * Store a SAML Assertion as a SecurityToken
     */
    protected void storeAssertionAsSecurityToken(SamlAssertionWrapper assertion) throws TokenStoreException {
        String id = findIDFromSamlToken(assertion.getElement());
        if (id == null) {
            return;
        }
        SecurityToken secToken = new SecurityToken(id);
        if (assertion.getSaml2() != null) {
            secToken.setTokenType(WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
        } else {
            secToken.setTokenType(WSS4JConstants.WSS_SAML_TOKEN_TYPE);
        }
        secToken.setToken(assertion.getElement());
        getTokenStore().add(secToken);
        message.put(SecurityConstants.TOKEN_ID, secToken.getId());
    }

    protected String findIDFromSamlToken(Element samlToken) {
        String id = null;
        if (samlToken != null) {
            QName elName = DOMUtils.getElementQName(samlToken);
            if (elName.equals(new QName(WSS4JConstants.SAML_NS, "Assertion"))
                && samlToken.hasAttributeNS(null, "AssertionID")) {
                id = samlToken.getAttributeNS(null, "AssertionID");
            } else if (elName.equals(new QName(WSS4JConstants.SAML2_NS, "Assertion"))
                && samlToken.hasAttributeNS(null, "ID")) {
                id = samlToken.getAttributeNS(null, "ID");
            }
            if (id == null) {
                id = samlToken.getAttributeNS(WSS4JConstants.WSU_NS, "Id");
            }
        }
        return id;
    }

    protected String getPassword(String userName, Assertion info, int usage) {
        //Then try to get the password from the given callback handler
        Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, message);
        CallbackHandler handler = null;
        try {
            handler = SecurityUtils.getCallbackHandler(o);
            if (handler == null) {
                // Don't unassert for signature as we might get the password from the crypto properties
                if (usage == WSPasswordCallback.SIGNATURE) {
                    LOG.info("No CallbackHandler available to retrieve a password. We will now try the crypto "
                             + "properties file for a private password");
                } else {
                    unassertPolicy(info, "No callback handler and no password available");
                }
                return null;
            }
        } catch (Exception ex) {
            // Don't unassert for signature as we might get the password from the crypto properties
            if (usage == WSPasswordCallback.SIGNATURE) {
                LOG.info("No CallbackHandler available to retrieve a password. We will now try the crypto "
                         + "properties file for a private password");
            } else {
                unassertPolicy(info, "No callback handler and no password available");
            }
            return null;
        }

        WSPasswordCallback[] cb = {new WSPasswordCallback(userName, usage)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            unassertPolicy(info, e);
        }

        //get the password
        return cb[0].getPassword();
    }

    /**
     * Generates a wsu:Id attribute for the provided {@code Element} and returns the attribute value
     * or finds and returns the value of the attribute if it already exists.
     *
     * @param element the {@code Element} to check/create the attribute on
     *
     * @return the generated or discovered wsu:Id attribute value
     */
    public String addWsuIdToElement(Element element) {
        String id;

        //first try to get the Id attr
        Attr idAttr = element.getAttributeNodeNS(null, "Id");
        if (idAttr == null) {
            //then try the wsu:Id value
            idAttr = element.getAttributeNodeNS(PolicyConstants.WSU_NAMESPACE_URI, "Id");
        }

        if (idAttr != null) {
            id = idAttr.getValue();
        } else {
            //Add an id
            id = wssConfig.getIdAllocator().createId("_", element);
            String pfx = null;
            try {
                pfx = element.lookupPrefix(PolicyConstants.WSU_NAMESPACE_URI);
            } catch (Throwable t) {
                pfx = DOMUtils.getPrefixRecursive(element, PolicyConstants.WSU_NAMESPACE_URI);
            }
            boolean found = !StringUtils.isEmpty(pfx);
            int cnt = 0;
            while (StringUtils.isEmpty(pfx)) {
                pfx = "wsu" + (cnt == 0 ? "" : cnt);

                String ns;
                try {
                    ns = element.lookupNamespaceURI(pfx);
                } catch (Throwable t) {
                    ns = DOMUtils.getNamespace(element, pfx);
                }

                if (!StringUtils.isEmpty(ns)) {
                    pfx = null;
                    cnt++;
                }
            }
            if (!found) {
                idAttr = element.getOwnerDocument().createAttributeNS(WSDLConstants.NS_XMLNS, "xmlns:" + pfx);
                idAttr.setValue(PolicyConstants.WSU_NAMESPACE_URI);
                element.setAttributeNodeNS(idAttr);
            }
            idAttr = element.getOwnerDocument().createAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                                               pfx + ":Id");
            idAttr.setValue(id);
            element.setAttributeNodeNS(idAttr);
        }

        return id;
    }

    public List<WSEncryptionPart> getEncryptedParts()
        throws SOAPException {

        EncryptedParts parts = null;
        EncryptedElements elements = null;
        ContentEncryptedElements celements = null;

        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(SPConstants.ENCRYPTED_PARTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                parts = (EncryptedParts)ai.getAssertion();
                ai.setAsserted(true);
            }
        }

        ais = getAllAssertionsByLocalname(SPConstants.ENCRYPTED_ELEMENTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                elements = (EncryptedElements)ai.getAssertion();
                ai.setAsserted(true);
            }
        }

        ais = getAllAssertionsByLocalname(SPConstants.CONTENT_ENCRYPTED_ELEMENTS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                celements = (ContentEncryptedElements)ai.getAssertion();
                ai.setAsserted(true);
            }
        }

        if (parts == null && elements == null && celements == null) {
            return new ArrayList<>();
        }

        List<WSEncryptionPart> securedParts = new ArrayList<>();
        boolean isBody = false;
        if (parts != null) {
            isBody = parts.isBody();
            for (Header head : parts.getHeaders()) {
                WSEncryptionPart wep = new WSEncryptionPart(head.getName(),
                                                            head.getNamespace(),
                                                            "Header");
                securedParts.add(wep);
            }

            Attachments attachments = parts.getAttachments();
            if (attachments != null) {
                String encModifier = "Element";
                if (MessageUtils.getContextualBoolean(
                    message, SecurityConstants.USE_ATTACHMENT_ENCRYPTION_CONTENT_ONLY_TRANSFORM, false)) {
                    encModifier = "Content";
                }
                WSEncryptionPart wep = new WSEncryptionPart("cid:Attachments", encModifier);
                securedParts.add(wep);
            }
        }

        // REVISIT consider catching exceptions and unassert failed assertions or
        // to process and assert them one at a time.  Additionally, a found list
        // should be applied to all operations that involve adding anything to
        // the encrypted list to prevent duplication / errors in encryption.
        return getPartsAndElements(false,
                                   isBody,
                                   securedParts,
                                   elements == null ? null : elements.getXPaths(),
                                   celements == null ? null : celements.getXPaths());
    }

    public List<WSEncryptionPart> getSignedParts(SupportingTokens supportingToken)
        throws SOAPException {

        boolean isSignBody = false;

        SignedParts parts = null;
        SignedElements elements = null;

        if (supportingToken != null && supportingToken.isEndorsing()) {
            parts = supportingToken.getSignedParts();
            elements = supportingToken.getSignedElements();
            // Store them so that the main Signature doesn't sign them
            if (parts != null) {
                suppTokenParts.add(parts);
                this.assertPolicy(parts.getName());
            }
            if (elements != null) {
                suppTokenParts.add(elements);
                this.assertPolicy(elements.getName());
            }
        } else {
            Collection<AssertionInfo> ais = getAllAssertionsByLocalname(SPConstants.SIGNED_PARTS);
            if (!ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    SignedParts signedParts = (SignedParts)ai.getAssertion();
                    ai.setAsserted(true);
                    if (!suppTokenParts.contains(signedParts)) {
                        parts = signedParts;
                    }
                }
            }

            ais = getAllAssertionsByLocalname(SPConstants.SIGNED_ELEMENTS);
            if (!ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    SignedElements signedElements = (SignedElements)ai.getAssertion();
                    ai.setAsserted(true);
                    if (!suppTokenParts.contains(signedElements)) {
                        elements = signedElements;
                    }
                }
            }
        }

        if (parts == null && elements == null) {
            return new ArrayList<>();
        }

        List<WSEncryptionPart> signedParts = new ArrayList<>();
        if (parts != null) {
            isSignBody = parts.isBody();
            for (Header head : parts.getHeaders()) {
                WSEncryptionPart wep = new WSEncryptionPart(head.getName(),
                                                            head.getNamespace(),
                                                            "Header");
                signedParts.add(wep);
            }
            Attachments attachments = parts.getAttachments();
            if (attachments != null) {
                String modifier = "Element";
                if (attachments.isContentSignatureTransform()) {
                    modifier = "Content";
                }
                WSEncryptionPart wep = new WSEncryptionPart("cid:Attachments", modifier);
                signedParts.add(wep);
            }
        }

        return getPartsAndElements(true,
                                   isSignBody,
                                   signedParts,
                                   elements == null ? null : elements.getXPaths(),
                                   null);
    }

    /**
     * Identifies the portions of the message to be signed/encrypted.
     *
     * @param sign
     *            whether the matches are to be signed or encrypted
     * @param includeBody
     *            if the body should be included in the signature/encryption
     * @param parts
     *            any {@code WSEncryptionPart}s to match for signature or
     *            encryption as specified by WS-SP signed parts or encrypted
     *            parts. Parts without a name match all elements with the
     *            provided namespace.
     * @param xpaths
     *            any XPath expressions to sign/encrypt matches
     * @param contentXpaths
     *            any XPath expressions to content encrypt
     * @return a configured list of {@code WSEncryptionPart}s suitable for
     *         processing by WSS4J
     * @throws SOAPException
     *             if there is an error extracting SOAP content from the SAAJ
     *             model
     */
    public List<WSEncryptionPart> getPartsAndElements(boolean sign,
                                                    boolean includeBody,
                                                    List<WSEncryptionPart> parts,
                                                    List<org.apache.wss4j.policy.model.XPath> xpaths,
                                                    List<org.apache.wss4j.policy.model.XPath> contentXpaths)
        throws SOAPException {

        List<WSEncryptionPart> result = new ArrayList<>();

        List<Element> found = new ArrayList<>();

        // Handle sign/enc parts
        result.addAll(this.getParts(sign, includeBody, parts, found));

        // Handle sign/enc elements
        result.addAll(this.getElements("Header", xpaths, found, sign));

        if (!sign) {
            // Handle content encrypted elements
            result.addAll(this.getElements("Content", contentXpaths, found, sign));
        }

        return result;
    }

    /**
     * Identifies the portions of the message to be signed/encrypted.
     *
     * @param sign
     *            whether the matches are to be signed or encrypted
     * @param includeBody
     *            if the body should be included in the signature/encryption
     * @param parts
     *            any {@code WSEncryptionPart}s to match for signature or
     *            encryption as specified by WS-SP signed parts or encrypted
     *            parts. Parts without a name match all elements with the
     *            provided namespace.
     * @param found
     *            a list of elements that have previously been tagged for
     *            signing/encryption. Populated with additional matches found by
     *            this method and used to prevent including the same element
     *            twice under the same operation.
     * @return a configured list of {@code WSEncryptionPart}s suitable for
     *         processing by WSS4J
     * @throws SOAPException
     *             if there is an error extracting SOAP content from the SAAJ
     *             model
     */
    protected List<WSEncryptionPart> getParts(boolean sign,
            boolean includeBody, List<WSEncryptionPart> parts,
            List<Element> found) throws SOAPException {

        List<WSEncryptionPart> result = new ArrayList<>();
        Element soapBody = SAAJUtils.getBody(this.saaj);
        soapBody = (Element)DOMUtils.getDomElement(soapBody);

        if (includeBody && !found.contains(soapBody)) {
            found.add(soapBody);
            final String id = this.addWsuIdToElement(soapBody);
            if (sign) {
                WSEncryptionPart bodyPart = new WSEncryptionPart(id, "Element");
                bodyPart.setElement(soapBody);
                result.add(bodyPart);
            } else {
                WSEncryptionPart bodyPart = new WSEncryptionPart(id, "Content");
                bodyPart.setElement(soapBody);
                result.add(bodyPart);
            }
        }

        final SOAPHeader header = SAAJUtils.getHeader(saaj);

        // Handle sign/enc parts
        for (WSEncryptionPart part : parts) {
            if (part.getId() != null && part.getId().startsWith("cid:")) {
                // Attachments are handled inside WSS4J via a CallbackHandler
                result.add(part);
                continue;
            }
            final List<Element> elements;

            if (StringUtils.isEmpty(part.getName())) {
                // An entire namespace
                elements =
                    DOMUtils.getChildrenWithNamespace(header, part.getNamespace());
            } else {
                // All elements with a given name and namespace
                elements =
                    DOMUtils.getChildrenWithName(header, part.getNamespace(), part.getName());
            }

            for (Element el : elements) {
                if (!found.contains(el)) {
                    found.add(el);
                    // Generate an ID for the element and use this ID or else
                    // WSS4J will only ever sign/encrypt the first matching
                    // element with the same name and namespace as that in the
                    // WSEncryptionPart
                    final String id = this.addWsuIdToElement(el);
                    WSEncryptionPart elPart =
                        new WSEncryptionPart(id, part.getEncModifier());
                    elPart.setElement(el);
                    result.add(elPart);
                }
            }
        }

        return result;
    }

    /**
     * Identifies the portions of the message to be signed/encrypted.
     *
     * @param encryptionModifier
     *            indicates the scope of the crypto operation over matched
     *            elements. Either "Content" or "Element".
     * @param xpaths
     *            any XPath expressions to sign/encrypt matches
     * @param found
     *            a list of elements that have previously been tagged for
     *            signing/encryption. Populated with additional matches found by
     *            this method and used to prevent including the same element
     *            twice under the same operation.
     * @param forceId
     *         force adding a wsu:Id onto the elements.  Recommended for signatures.
     * @return a configured list of {@code WSEncryptionPart}s suitable for
     *         processing by WSS4J
     * @throws SOAPException
     *             if there is an error extracting SOAP content from the SAAJ
     *             model
     */
    protected List<WSEncryptionPart> getElements(String encryptionModifier,
            List<org.apache.wss4j.policy.model.XPath> xpaths,
            List<Element> found,
            boolean forceId) throws SOAPException {

        List<WSEncryptionPart> result = new ArrayList<>();

        if (xpaths != null && !xpaths.isEmpty()) {
            boolean useSTRTransform =
                MessageUtils.getContextualBoolean(
                    message, SecurityConstants.USE_STR_TRANSFORM, true
                );

            XPathFactory factory = XPathFactory.newInstance();
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            } catch (javax.xml.xpath.XPathFactoryConfigurationException ex) {
                // ignore
            }

            for (org.apache.wss4j.policy.model.XPath xPath : xpaths) {
                XPath xpath = factory.newXPath();
                if (xPath.getPrefixNamespaceMap() != null) {
                    xpath.setNamespaceContext(new MapNamespaceContext(xPath.getPrefixNamespaceMap()));
                }

                NodeList list = null;

                try {
                    Element envelope = saaj.getSOAPPart().getEnvelope();
                    envelope = (Element)DOMUtils.getDomElement(envelope);
                    list = (NodeList)xpath.evaluate(xPath.getXPath(), envelope, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    LOG.log(Level.WARNING, "Failure in evaluating an XPath expression", e);
                }

                if (list != null) {
                    for (int x = 0; x < list.getLength(); x++) {
                        Element el = (Element)list.item(x);

                        if (!found.contains(el)) {
                            found.add(el);
                            WSEncryptionPart part = null;
                            boolean saml1 = WSS4JConstants.SAML_NS.equals(el.getNamespaceURI())
                                && "Assertion".equals(el.getLocalName());
                            boolean saml2 = WSS4JConstants.SAML2_NS.equals(el.getNamespaceURI())
                                && "Assertion".equals(el.getLocalName());

                            if (useSTRTransform && (saml1 || saml2)) {
                                String id = saml2 ? el.getAttributeNS(null, "ID")
                                    : el.getAttributeNS(null, "AssertionID");
                                SecurityTokenReference secRef =
                                    createSTRForSamlAssertion(el.getOwnerDocument(), id, saml1, false);
                                Element clone = cloneElement(secRef.getElement());
                                addSupportingElement(clone);
                                part = new WSEncryptionPart("STRTransform", null, "Element");
                                part.setId(secRef.getID());
                                part.setElement(clone);
                            } else {
                                String id = setIdOnElement(el, forceId);
                                part = new WSEncryptionPart(id, encryptionModifier);
                                part.setElement(el);
                            }
                            part.setXpath(xPath.getXPath());

                            result.add(part);
                        }
                    }
                }
            }
        }

        return result;
    }

    private String setIdOnElement(Element element, boolean forceId) {
        if (forceId) {
            return this.addWsuIdToElement(element);
        }

        //not forcing an ID on this.  Use one if there is one
        //there already, but don't force one
        Attr idAttr = element.getAttributeNodeNS(null, "Id");
        if (idAttr == null) {
            //then try the wsu:Id value
            idAttr = element.getAttributeNodeNS(PolicyConstants.WSU_NAMESPACE_URI, "Id");
        }
        if (idAttr != null) {
            return idAttr.getValue();
        }

        return null;
    }

    protected WSSecEncryptedKey getEncryptedKeyBuilder(AbstractToken token,
                                                       SecretKey symmetricKey) throws WSSecurityException {
        WSSecEncryptedKey encrKey = new WSSecEncryptedKey(secHeader);
        encrKey.setIdAllocator(wssConfig.getIdAllocator());
        encrKey.setCallbackLookup(callbackLookup);
        encrKey.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
        encrKey.setStoreBytesInAttachment(storeBytesInAttachment);
        Crypto crypto = getEncryptionCrypto();
        message.getExchange().put(SecurityConstants.ENCRYPT_CRYPTO, crypto);
        setKeyIdentifierType(encrKey, token);

        boolean alsoIncludeToken = false;
        // Find out do we also need to include the token as per the Inclusion requirement
        if (token instanceof X509Token
            && token.getIncludeTokenType() != IncludeTokenType.INCLUDE_TOKEN_NEVER
            && encrKey.getKeyIdentifierType() != WSConstants.BST_DIRECT_REFERENCE) {
            alsoIncludeToken = true;
        }

        String encrUser = setEncryptionUser(encrKey, token, false, crypto);

        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        encrKey.setKeyEncAlgo(algType.getAsymmetricKeyWrap());
        encrKey.setMGFAlgorithm(algType.getMGFAlgo());

        encrKey.prepare(crypto, symmetricKey);

        if (alsoIncludeToken) {
            X509Certificate encCert = getEncryptCert(crypto, encrUser);
            BinarySecurity bstToken = new X509Security(saaj.getSOAPPart());
            ((X509Security)bstToken).setX509Certificate(encCert);
            bstToken.addWSUNamespace();
            bstToken.setID(wssConfig.getIdAllocator().createSecureId("X509-", encCert));
            WSSecurityUtil.prependChildElement(
                secHeader.getSecurityHeaderElement(), bstToken.getElement()
            );
            bstElement = bstToken.getElement();
        }

        return encrKey;
    }

    private X509Certificate getEncryptCert(Crypto crypto, String encrUser) throws WSSecurityException {
        // Check for prepared encryption certificate
        X509Certificate encrCert =
            (X509Certificate)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CERT, message);
        if (encrCert != null) {
            return encrCert;
        }
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(encrUser);
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        if (certs != null && certs.length > 0) {
            return certs[0];
        }

        return null;
    }

    public Crypto getSignatureCrypto() throws WSSecurityException {
        return getCrypto(SecurityConstants.SIGNATURE_CRYPTO, SecurityConstants.SIGNATURE_PROPERTIES);
    }

    public Crypto getEncryptionCrypto() throws WSSecurityException {
        Crypto crypto =
            getCrypto(SecurityConstants.ENCRYPT_CRYPTO, SecurityConstants.ENCRYPT_PROPERTIES);
        boolean enableRevocation = false;
        String enableRevStr =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENABLE_REVOCATION, message);
        if (enableRevStr != null) {
            enableRevocation = Boolean.parseBoolean(enableRevStr);
        }
        if (enableRevocation && crypto != null) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            String encrUser =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_USERNAME, message);
            if (encrUser == null) {
                try {
                    encrUser = crypto.getDefaultX509Identifier();
                } catch (WSSecurityException e1) {
                    throw new Fault(e1);
                }
            }
            cryptoType.setAlias(encrUser);
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            if (certs != null && certs.length > 0) {
                crypto.verifyTrust(certs, enableRevocation, null, null);
            }
        }
        if (crypto != null) {
            this.message.getExchange().put(SecurityConstants.ENCRYPT_CRYPTO, crypto);
        }
        return crypto;

    }

    protected Crypto getCrypto(
        String cryptoKey,
        String propKey
    ) throws WSSecurityException {
        Crypto crypto = (Crypto)SecurityUtils.getSecurityPropertyValue(cryptoKey, message);
        if (crypto != null) {
            return crypto;
        }

        Object o = SecurityUtils.getSecurityPropertyValue(propKey, message);
        if (o == null) {
            return null;
        }

        crypto = getCryptoCache().get(o);
        if (crypto != null) {
            return crypto;
        }

        URL propsURL = SecurityUtils.loadResource(message, o);
        Properties properties = WSS4JUtils.getProps(o, propsURL);

        if (properties != null) {
            crypto = CryptoFactory.getInstance(properties,
                                               Loader.getClassLoader(CryptoFactory.class),
                                               WSS4JUtils.getPasswordEncryptor(message));
            getCryptoCache().put(o, crypto);
        }
        return crypto;
    }

    public void setKeyIdentifierType(WSSecBase secBase, AbstractToken token) {
        boolean tokenTypeSet = false;

        if (token instanceof X509Token) {
            X509Token x509Token = (X509Token)token;
            if (x509Token.isRequireIssuerSerialReference()) {
                secBase.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
                tokenTypeSet = true;
            } else if (x509Token.isRequireKeyIdentifierReference()) {
                secBase.setKeyIdentifierType(WSConstants.SKI_KEY_IDENTIFIER);
                tokenTypeSet = true;
            } else if (x509Token.isRequireThumbprintReference()) {
                secBase.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
                tokenTypeSet = true;
            }
        } else if (token instanceof KeyValueToken) {
            secBase.setKeyIdentifierType(WSConstants.KEY_VALUE);
            tokenTypeSet = true;
        }

        assertToken(token);

        if (!tokenTypeSet) {
            boolean requestor = isRequestor();
            if (token.getIncludeTokenType() == IncludeTokenType.INCLUDE_TOKEN_NEVER
                || token instanceof X509Token
                && ((token.getIncludeTokenType() == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT
                    && !requestor)
                || (token.getIncludeTokenType() == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR
                    && requestor))) {
                Wss10 wss = getWss10();
                assertPolicy(wss);
                if (wss == null || wss.isMustSupportRefKeyIdentifier()) {
                    secBase.setKeyIdentifierType(WSConstants.SKI_KEY_IDENTIFIER);
                } else if (wss.isMustSupportRefIssuerSerial()) {
                    secBase.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
                } else if (wss instanceof Wss11 && ((Wss11) wss).isMustSupportRefThumbprint()) {
                    secBase.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
                } else {
                    secBase.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
                }
            } else {
                secBase.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
            }
        }
    }

    public String setEncryptionUser(WSSecEncryptedKey encrKeyBuilder, AbstractToken token,
                                  boolean sign, Crypto crypto) {
        // Check for prepared certificate property
        X509Certificate encrCert =
            (X509Certificate)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_CERT, message);
        if (encrCert != null) {
            encrKeyBuilder.setUseThisCert(encrCert);
            return null;
        }

        String key = sign ? SecurityConstants.SIGNATURE_USERNAME : SecurityConstants.ENCRYPT_USERNAME;
        String encrUser = (String)SecurityUtils.getSecurityPropertyValue(key, message);

        if (crypto != null && (encrUser == null || "".equals(encrUser))) {
            try {
                encrUser = crypto.getDefaultX509Identifier();
            } catch (WSSecurityException e1) {
                throw new Fault(e1);
            }
        }
        if (encrUser == null || "".equals(encrUser)) {
            unassertPolicy(token, "A " + (sign ? "signature" : "encryption") + " username needs to be declared.");
        }
        if (ConfigurationConstants.USE_REQ_SIG_CERT.equals(encrUser)) {
            List<WSHandlerResult> results =
                CastUtils.cast((List<?>)
                    message.getExchange().getInMessage().get(WSHandlerConstants.RECV_RESULTS));
            if (results != null) {
                encrKeyBuilder.setUseThisCert(WSS4JUtils.getReqSigCert(results));

                //TODO This is a hack, this should not come under USE_REQ_SIG_CERT
                if (encrKeyBuilder.isCertSet()) {
                    encrKeyBuilder.setUserInfo(getUsername(results));
                }
            } else {
                unassertPolicy(token, "No security results in incoming message");
            }
        } else {
            encrKeyBuilder.setUserInfo(encrUser);
        }

        return encrUser;
    }

    /**
     * Scan through <code>WSHandlerResult<code> list for a Username token and return
     * the username if a Username Token found
     * @param results
     * @return
     */
    public static String getUsername(List<WSHandlerResult> results) {
        /*
         * Scan the results for a matching actor. Use results only if the
         * receiving Actor and the sending Actor match.
         */
        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();
            /*
             * Scan the results for a username token. Use the username
             * of this token to set the alias for the encryption user
             */
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                if (actInt.intValue() == WSConstants.UT) {
                    UsernameTokenPrincipal principal
                        = (UsernameTokenPrincipal)wser.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                    return principal.getName();
                }
            }
        }

        return null;
    }

    protected WSSecurityEngineResult getEncryptedKeyResult() {

        List<WSHandlerResult> results = CastUtils.cast((List<?>)message.getExchange().getInMessage()
            .get(WSHandlerConstants.RECV_RESULTS));

        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> encryptedResults = rResult.getActionResults().get(WSConstants.ENCR);
            if (encryptedResults != null) {
                for (WSSecurityEngineResult wser : encryptedResults) {
                    String encryptedKeyID = (String)wser.get(WSSecurityEngineResult.TAG_ID);
                    if (encryptedKeyID != null && encryptedKeyID.length() != 0) {
                        return wser;
                    }
                }
            }
        }
        return null;
    }

    private void checkForX509PkiPath(WSSecSignature sig, AbstractToken token) {
        if (token instanceof X509Token) {
            X509Token x509Token = (X509Token) token;
            TokenType tokenType = x509Token.getTokenType();
            if (tokenType == TokenType.WssX509PkiPathV1Token10
                || tokenType == TokenType.WssX509PkiPathV1Token11) {
                sig.setUseSingleCertificate(false);
            }
        }
    }

    protected WSSecSignature getSignatureBuilder(
        AbstractToken token, boolean attached, boolean endorse
    ) throws WSSecurityException, TokenStoreException {
        WSSecSignature sig = new WSSecSignature(secHeader);
        sig.setIdAllocator(wssConfig.getIdAllocator());
        sig.setCallbackLookup(callbackLookup);
        sig.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
        sig.setStoreBytesInAttachment(storeBytesInAttachment);
        sig.setExpandXopInclude(isExpandXopInclude());
        sig.setWsDocInfo(wsDocInfo);

        checkForX509PkiPath(sig, token);
        if (token instanceof IssuedToken || token instanceof SamlToken) {
            assertToken(token);
            SecurityToken securityToken = getSecurityToken();
            String tokenType = securityToken.getTokenType();

            Element ref;
            if (attached) {
                ref = securityToken.getAttachedReference();
            } else {
                ref = securityToken.getUnattachedReference();
            }

            if (ref != null) {
                SecurityTokenReference secRef =
                    new SecurityTokenReference(cloneElement(ref), new BSPEnforcer());
                sig.setSecurityTokenReference(secRef);
                sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
            } else {
                int type = attached ? WSConstants.CUSTOM_SYMM_SIGNING
                    : WSConstants.CUSTOM_SYMM_SIGNING_DIRECT;
                if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                    || WSS4JConstants.SAML_NS.equals(tokenType)) {
                    sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
                    sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
                } else if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                    || WSS4JConstants.SAML2_NS.equals(tokenType)) {
                    sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE);
                    sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
                } else {
                    sig.setCustomTokenValueType(tokenType);
                    sig.setKeyIdentifierType(type);
                }
            }

            String sigTokId;
            if (attached) {
                sigTokId = securityToken.getWsuId();
                if (sigTokId == null) {
                    sigTokId = securityToken.getId();
                }
                if (sigTokId.startsWith("#")) {
                    sigTokId = sigTokId.substring(1);
                }
            } else {
                sigTokId = securityToken.getId();
            }

            sig.setCustomTokenId(sigTokId);
        } else {
            setKeyIdentifierType(sig, token);
            // Find out do we also need to include the token as per the Inclusion requirement
            if (token instanceof X509Token
                && token.getIncludeTokenType() != IncludeTokenType.INCLUDE_TOKEN_NEVER
                && (sig.getKeyIdentifierType() != WSConstants.BST_DIRECT_REFERENCE
                    && sig.getKeyIdentifierType() != WSConstants.KEY_VALUE)) {
                sig.setIncludeSignatureToken(true);
            }
        }

        boolean encryptCrypto = false;
        String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
        String type = "signature";
        if (binding instanceof SymmetricBinding && !endorse) {
            encryptCrypto = ((SymmetricBinding)binding).getProtectionToken() != null;
            userNameKey = SecurityConstants.ENCRYPT_USERNAME;
        }

        Crypto crypto = encryptCrypto ? getEncryptionCrypto() : getSignatureCrypto();

        if (endorse && crypto == null && binding instanceof SymmetricBinding) {
            type = "encryption";
            userNameKey = SecurityConstants.ENCRYPT_USERNAME;
            crypto = getEncryptionCrypto();
        }

        if (!encryptCrypto) {
            message.getExchange().put(SecurityConstants.SIGNATURE_CRYPTO, crypto);
        }
        String user = (String)SecurityUtils.getSecurityPropertyValue(userNameKey, message);
        if (StringUtils.isEmpty(user)) {
            if (crypto != null) {
                try {
                    user = crypto.getDefaultX509Identifier();
                    if (StringUtils.isEmpty(user)) {
                        unassertPolicy(token, "No configured " + type + " username detected");
                        return null;
                    }
                } catch (WSSecurityException e1) {
                    LOG.log(Level.FINE, e1.getMessage(), e1);
                    throw new Fault(e1);
                }
            } else {
                unassertPolicy(token, "Security configuration could not be detected. "
                    + "Potential cause: Make sure jaxws:client element with name "
                    + "attribute value matching endpoint port is defined as well as a "
                    + SecurityConstants.SIGNATURE_PROPERTIES + " element within it.");
                return null;
            }
        }

        String password =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD, message);
        if (StringUtils.isEmpty(password)) {
            password = getPassword(user, token, WSPasswordCallback.SIGNATURE);
        }
        sig.setUserInfo(user, password);
        sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAlgorithmSuiteType().getAsymmetricSignature());
        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        sig.setDigestAlgo(algType.getDigest());
        sig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());

        boolean includePrefixes =
            MessageUtils.getContextualBoolean(
                message, SecurityConstants.ADD_INCLUSIVE_PREFIXES, true
            );
        sig.setAddInclusivePrefixes(includePrefixes);

        try {
            sig.prepare(crypto);
        } catch (WSSecurityException e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            unassertPolicy(token, e);
        }

        return sig;
    }

    protected void doEndorsedSignatures(List<SupportingToken> tokenList,
                                        boolean isTokenProtection,
                                        boolean isSigProtect) {

        for (SupportingToken supportingToken : tokenList) {
            Object tempTok = supportingToken.getTokenImplementation();

            List<WSEncryptionPart> sigParts = new ArrayList<>();
            WSEncryptionPart sigPart = new WSEncryptionPart(mainSigId);
            sigPart.setElement(bottomUpElement);
            sigParts.add(sigPart);

            if (supportingToken.getSignedParts() != null) {
                for (WSEncryptionPart signedPart : supportingToken.getSignedParts()) {
                    sigParts.add(signedPart);
                }
            }

            if (tempTok instanceof WSSecSignature) {
                WSSecSignature sig = (WSSecSignature)tempTok;
                if (isTokenProtection && sig.getBSTTokenId() != null) {
                    WSEncryptionPart bstPart =
                        new WSEncryptionPart(sig.getBSTTokenId());
                    bstPart.setElement(sig.getBinarySecurityTokenElement());
                    sigParts.add(bstPart);
                }
                try {
                    List<Reference> referenceList = sig.addReferencesToSign(sigParts);
                    sig.computeSignature(referenceList, false, null);

                    addSig(sig.getSignatureValue());
                    if (isSigProtect) {
                        WSEncryptionPart part = new WSEncryptionPart(sig.getId(), "Element");
                        encryptedTokensList.add(part);
                    }
                } catch (WSSecurityException e) {
                    unassertPolicy(supportingToken.getToken(), e);
                }

            } else if (tempTok instanceof WSSecurityTokenHolder) {
                SecurityToken token = ((WSSecurityTokenHolder)tempTok).getToken();
                if (isTokenProtection) {
                    sigParts.add(new WSEncryptionPart(token.getId()));
                }

                try {
                    if (supportingToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                        doSymmSignatureDerived(supportingToken.getToken(), token, sigParts,
                                               isTokenProtection, isSigProtect);
                    } else {
                        doSymmSignature(supportingToken.getToken(), token, sigParts, isSigProtect);
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, e.getMessage(), e);
                }
            } else if (tempTok instanceof WSSecUsernameToken) {
                WSSecUsernameToken utBuilder = (WSSecUsernameToken)tempTok;
                String id = utBuilder.getId();

                Instant created = Instant.now();
                Instant expires = created.plusSeconds(WSS4JUtils.getSecurityTokenLifetime(message) / 1000L);
                SecurityToken secToken = new SecurityToken(id,
                                                           utBuilder.getUsernameTokenElement(),
                                                           created,
                                                           expires);

                if (isTokenProtection) {
                    sigParts.add(new WSEncryptionPart(secToken.getId()));
                }

                try {
                    byte[] secret = utBuilder.getDerivedKey(supportingToken.getSalt());
                    secToken.setSecret(secret);
                    Arrays.fill(supportingToken.getSalt(), (byte)0);

                    if (supportingToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                        doSymmSignatureDerived(supportingToken.getToken(), secToken, sigParts,
                                               isTokenProtection, isSigProtect);
                    } else {
                        doSymmSignature(supportingToken.getToken(), secToken, sigParts, isSigProtect);
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, e.getMessage(), e);
                }

            }
        }
    }

    private void doSymmSignatureDerived(AbstractToken policyToken, SecurityToken tok,
                                 List<WSEncryptionPart> sigParts, boolean isTokenProtection,
                                 boolean isSigProtect)
        throws WSSecurityException {

        Document doc = saaj.getSOAPPart();
        WSSecDKSign dkSign = new WSSecDKSign(secHeader);
        dkSign.setIdAllocator(wssConfig.getIdAllocator());
        dkSign.setCallbackLookup(callbackLookup);
        dkSign.setStoreBytesInAttachment(storeBytesInAttachment);
        dkSign.setExpandXopInclude(isExpandXopInclude());

        //Check whether it is security policy 1.2 and use the secure conversation accordingly
        if (policyToken.getVersion() == SPConstants.SPVersion.SP11) {
            dkSign.setWscVersion(ConversationConstants.VERSION_05_02);
        }

        //Check for whether the token is attached in the message or not
        boolean attached = false;
        if (isTokenRequired(policyToken.getIncludeTokenType())) {
            attached = true;
        }

        // Setting the AttachedReference or the UnattachedReference according to the flag
        Element ref;
        if (attached) {
            ref = tok.getAttachedReference();
        } else {
            ref = tok.getUnattachedReference();
        }

        if (ref != null) {
            ref = cloneElement(ref);
            dkSign.setStrElem(ref);
        } else if (!isRequestor() && policyToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            // If the Encrypted key used to create the derived key is not
            // attached use key identifier as defined in WSS1.1 section
            // 7.7 Encrypted Key reference
            SecurityTokenReference tokenRef
                = new SecurityTokenReference(doc);
            if (tok.getSHA1() != null) {
                tokenRef.setKeyIdentifierEncKeySHA1(tok.getSHA1());
                tokenRef.addTokenType(WSS4JConstants.WSS_ENC_KEY_VALUE_TYPE);
            }
            dkSign.setStrElem(tokenRef.getElement());

        } else {
            dkSign.setTokenIdentifier(tok.getId());
        }

        //Set the algo info
        dkSign.setSignatureAlgorithm(binding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
        dkSign.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());
        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        dkSign.setDerivedKeyLength(algType.getSignatureDerivedKeyLength() / 8);
        if (tok.getSHA1() != null) {
            //Set the value type of the reference
            dkSign.setCustomValueType(WSS4JConstants.SOAPMESSAGE_NS11 + "#"
                + WSS4JConstants.ENC_KEY_VALUE_TYPE);
        } else if (policyToken instanceof UsernameToken) {
            dkSign.setCustomValueType(WSS4JConstants.WSS_USERNAME_TOKEN_VALUE_TYPE);
        }

        dkSign.prepare(tok.getSecret());

        if (isTokenProtection) {
            String sigTokId = XMLUtils.getIDFromReference(tok.getId());
            sigParts.add(new WSEncryptionPart(sigTokId));
        }

        dkSign.getParts().addAll(sigParts);

        List<Reference> referenceList = dkSign.addReferencesToSign(sigParts);

        //Add elements to header
        addSupportingElement(dkSign.getdktElement());

        //Do signature
        dkSign.computeSignature(referenceList, false, null);

        if (isSigProtect) {
            WSEncryptionPart part = new WSEncryptionPart(dkSign.getSignatureId(), "Element");
            encryptedTokensList.add(part);
        }

        addSig(dkSign.getSignatureValue());
        dkSign.clean();
    }

    private void doSymmSignature(AbstractToken policyToken, SecurityToken tok,
                                         List<WSEncryptionPart> sigParts,
                                         boolean isSigProtect)
        throws WSSecurityException {

        WSSecSignature sig = new WSSecSignature(secHeader);
        sig.setIdAllocator(wssConfig.getIdAllocator());
        sig.setCallbackLookup(callbackLookup);
        sig.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
        sig.setStoreBytesInAttachment(storeBytesInAttachment);
        sig.setExpandXopInclude(isExpandXopInclude());
        sig.setWsDocInfo(wsDocInfo);

        // If a EncryptedKeyToken is used, set the correct value type to
        // be used in the wsse:Reference in ds:KeyInfo
        if (policyToken instanceof X509Token) {
            if (isRequestor()) {
                // TODO Add support for SAML2 here
                sig.setCustomTokenValueType(
                    WSS4JConstants.SOAPMESSAGE_NS11 + "#" + WSS4JConstants.ENC_KEY_VALUE_TYPE
                );
                sig.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING);
            } else {
                //the tok has to be an EncryptedKey token
                sig.setEncrKeySha1value(tok.getSHA1());
                sig.setKeyIdentifierType(WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER);
            }

        } else {
            String tokenType = tok.getTokenType();
            if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                || WSS4JConstants.SAML_NS.equals(tokenType)) {
                sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
            } else if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                || WSS4JConstants.SAML2_NS.equals(tokenType)) {
                sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE);
            } else if (tokenType != null) {
                sig.setCustomTokenValueType(tokenType);
            } else if (policyToken instanceof UsernameToken) {
                sig.setCustomTokenValueType(WSS4JConstants.WSS_USERNAME_TOKEN_VALUE_TYPE);
            } else {
                sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
            }
            sig.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING);
        }

        String sigTokId = tok.getWsuId();
        if (sigTokId == null) {
            sigTokId = tok.getId();
        }

        sigTokId = XMLUtils.getIDFromReference(sigTokId);
        sig.setCustomTokenId(sigTokId);
        sig.setSecretKey(tok.getSecret());
        sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        sig.setDigestAlgo(algType.getDigest());
        sig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());
        sig.prepare(getSignatureCrypto());

        sig.getParts().addAll(sigParts);
        List<Reference> referenceList = sig.addReferencesToSign(sigParts);

        //Do signature
        sig.computeSignature(referenceList, false, null);

        if (isSigProtect) {
            WSEncryptionPart part = new WSEncryptionPart(sig.getId(), "Element");
            encryptedTokensList.add(part);
        }

        addSig(sig.getSignatureValue());
    }

    protected void addSupportingTokens(List<WSEncryptionPart> sigs) throws WSSecurityException {
        Collection<AssertionInfo> sgndSuppTokens =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_SUPPORTING_TOKENS);
        List<SupportingToken> sigSuppTokList = this.handleSupportingTokens(sgndSuppTokens, false);

        Collection<AssertionInfo> endSuppTokens =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENDORSING_SUPPORTING_TOKENS);
        endSuppTokList = this.handleSupportingTokens(endSuppTokens, true);

        Collection<AssertionInfo> sgndEndSuppTokens =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        sgndEndSuppTokList = this.handleSupportingTokens(sgndEndSuppTokens, true);

        Collection<AssertionInfo> sgndEncryptedSuppTokens =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        List<SupportingToken> sgndEncSuppTokList
            = this.handleSupportingTokens(sgndEncryptedSuppTokens, false);

        Collection<AssertionInfo> endorsingEncryptedSuppTokens =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        endSuppTokList.addAll(this.handleSupportingTokens(endorsingEncryptedSuppTokens, true));

        Collection<AssertionInfo> sgndEndEncSuppTokens =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        sgndEndSuppTokList.addAll(this.handleSupportingTokens(sgndEndEncSuppTokens, true));

        Collection<AssertionInfo> supportingToks =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SUPPORTING_TOKENS);
        this.handleSupportingTokens(supportingToks, false);

        Collection<AssertionInfo> encryptedSupportingToks =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_SUPPORTING_TOKENS);
        this.handleSupportingTokens(encryptedSupportingToks, false);

        //Setup signature parts
        addSignatureParts(sigSuppTokList, sigs);
        addSignatureParts(sgndEncSuppTokList, sigs);
        addSignatureParts(sgndEndSuppTokList, sigs);
    }

    protected void doEndorse() {
        boolean tokenProtect = false;
        boolean sigProtect = false;
        if (binding instanceof AsymmetricBinding) {
            tokenProtect = ((AsymmetricBinding)binding).isProtectTokens();
            sigProtect = ((AsymmetricBinding)binding).isEncryptSignature();
        } else if (binding instanceof SymmetricBinding) {
            tokenProtect = ((SymmetricBinding)binding).isProtectTokens();
            sigProtect = ((SymmetricBinding)binding).isEncryptSignature();
        }
        // Do endorsed signatures
        doEndorsedSignatures(endSuppTokList, tokenProtect, sigProtect);

        // Do signed endorsing signatures
        doEndorsedSignatures(sgndEndSuppTokList, tokenProtect, sigProtect);
    }

    protected void addSignatureConfirmation(List<WSEncryptionPart> sigParts) {
        Wss10 wss10 = getWss10();

        if (!(wss10 instanceof Wss11)
            || !((Wss11)wss10).isRequireSignatureConfirmation()) {
            //If we don't require sig confirmation simply go back :-)
            return;
        }

        List<WSHandlerResult> results =
            CastUtils.cast((List<?>)
                message.getExchange().getInMessage().get(WSHandlerConstants.RECV_RESULTS));
        /*
         * loop over all results gathered by all handlers in the chain. For each
         * handler result get the various actions. After that loop we have all
         * signature results in the signatureActions list
         */
        List<WSSecurityEngineResult> signatureActions = new ArrayList<>();
        for (WSHandlerResult wshResult : results) {
            if (wshResult.getActionResults().containsKey(WSConstants.SIGN)) {
                signatureActions.addAll(wshResult.getActionResults().get(WSConstants.SIGN));
            }
            if (wshResult.getActionResults().containsKey(WSConstants.UT_SIGN)) {
                signatureActions.addAll(wshResult.getActionResults().get(WSConstants.UT_SIGN));
            }
        }

        sigConfList = new ArrayList<>();
        // prepare a SignatureConfirmation token
        WSSecSignatureConfirmation wsc = new WSSecSignatureConfirmation(secHeader);
        wsc.setIdAllocator(wssConfig.getIdAllocator());
        if (!signatureActions.isEmpty()) {
            for (WSSecurityEngineResult wsr : signatureActions) {
                byte[] sigVal = (byte[]) wsr.get(WSSecurityEngineResult.TAG_SIGNATURE_VALUE);
                wsc.setSignatureValue(sigVal);
                wsc.prepare();
                addSupportingElement(wsc.getSignatureConfirmationElement());
                if (sigParts != null) {
                    WSEncryptionPart part = new WSEncryptionPart(wsc.getId(), "Element");
                    part.setElement(wsc.getSignatureConfirmationElement());
                    sigParts.add(part);
                    sigConfList.add(part);
                }
            }
        } else {
            //No Sig value
            wsc.prepare();
            addSupportingElement(wsc.getSignatureConfirmationElement());
            if (sigParts != null) {
                WSEncryptionPart part = new WSEncryptionPart(wsc.getId(), "Element");
                part.setElement(wsc.getSignatureConfirmationElement());
                sigParts.add(part);
                sigConfList.add(part);
            }
        }

        assertPolicy(
            new QName(wss10.getName().getNamespaceURI(), SPConstants.REQUIRE_SIGNATURE_CONFIRMATION));
    }

    /**
     * Processes the parts to be signed and reconfigures those parts that have
     * already been encrypted.
     *
     * @param encryptedParts
     *            the parts that have been encrypted
     * @param signedParts
     *            the parts that are to be signed
     *
     * @throws IllegalArgumentException
     *             if an element in {@code signedParts} contains a {@code
     *             WSEncryptionPart} with a {@code null} {@code id} value
     *             and the {@code WSEncryptionPart} {@code name} value is not
     *             "Token"
     */
    public void handleEncryptedSignedHeaders(List<WSEncryptionPart> encryptedParts,
            List<WSEncryptionPart> signedParts) {

        final List<WSEncryptionPart> signedEncryptedParts = new ArrayList<>();

        for (WSEncryptionPart encryptedPart : encryptedParts) {
            final Iterator<WSEncryptionPart> signedPartsIt = signedParts.iterator();
            while (signedPartsIt.hasNext()) {
                WSEncryptionPart signedPart = signedPartsIt.next();
                // Everything has to be ID based except for the case of a part
                // indicating "Token" as the element name.  This name is a flag
                // for WSS4J to sign the initiator token used in the signature.
                // Since the encryption happened before the signature creation,
                // this element can't possibly be encrypted so we can safely ignore
                // if it were ever to be set before this method is called.
                if (signedPart.getId() == null && !"Token".equals(signedPart.getName())) {
                    throw new IllegalArgumentException(
                            "WSEncryptionPart must be ID based but no id was found.");
                } else if ("Header".equals(encryptedPart.getEncModifier())
                        && signedPart.getId().equals(encryptedPart.getId())) {
                    // We are to sign something that has already been encrypted.
                    // We need to preserve the original aspects of signedPart but
                    // change the ID to the encrypted ID.

                    signedPartsIt.remove();
                    WSEncryptionPart part = new WSEncryptionPart(
                            encryptedPart.getEncId(),
                            encryptedPart.getEncModifier());
                    part.setElement(encryptedPart.getElement());
                    signedEncryptedParts.add(part);
                }
            }
        }

        signedParts.addAll(signedEncryptedParts);
    }

    /**
     * Convert a DOM Element into a WSEncryptionPart, adding a (wsu:)Id if there is not
     * one already.
     * @param element The DOM Element to convert
     * @return The WSEncryptionPart representing the DOM Element argument
     */
    public WSEncryptionPart convertToEncryptionPart(Element element) {
        String id = addWsuIdToElement(element);
        WSEncryptionPart part = new WSEncryptionPart(id);
        part.setElement(element);
        return part;
    }

    static class SupportingToken {
        private final AbstractToken token;
        private final Object tokenImplementation;
        private final List<WSEncryptionPart> signedParts;
        private final byte[] salt;

        SupportingToken(AbstractToken token, Object tokenImplementation,
                        List<WSEncryptionPart> signedParts) {
            this(token, tokenImplementation, signedParts, null);
        }

        SupportingToken(AbstractToken token, Object tokenImplementation,
                               List<WSEncryptionPart> signedParts, byte[] salt) {
            this.token = token;
            this.tokenImplementation = tokenImplementation;
            this.signedParts = signedParts;
            this.salt = salt;
        }

        public AbstractToken getToken() {
            return token;
        }

        public Object getTokenImplementation() {
            return tokenImplementation;
        }

        public List<WSEncryptionPart> getSignedParts() {
            return signedParts;
        }

        public byte[] getSalt() {
            return salt;
        }

    }

    protected void addSig(byte[] val) {
        if (val != null && val.length > 0) {
            signatures.add(Arrays.hashCode(val));
        }
    }

    public boolean isExpandXopInclude() {
        return expandXopInclude;
    }
}
