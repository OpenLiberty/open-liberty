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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.crypto.dsig.Reference;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.wss4j.AttachmentCallbackHandler;
import org.apache.cxf.ws.security.wss4j.StaxSerializer;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecBase;
import org.apache.wss4j.dom.message.WSSecDKEncrypt;
import org.apache.wss4j.dom.message.WSSecDKSign;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.opensaml.saml.common.SAMLVersion;

/**
 *
 */
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public class AsymmetricBindingHandler extends AbstractBindingBuilder {

    private static final Logger LOG = LogUtils.getL7dLogger(AsymmetricBindingHandler.class);

    AsymmetricBinding abinding;

    private WSSecEncryptedKey encrKey;
    private String encryptedKeyId;
    private byte[] encryptedKeyValue;

    public AsymmetricBindingHandler(WSSConfig config,
                                    AsymmetricBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) throws SOAPException {
        super(config, binding, saaj, secHeader, aim, message);
        this.abinding = binding;
        protectionOrder = binding.getProtectionOrder();
    }

    public void handleBinding() {
        WSSecTimestamp timestamp = createTimestamp();
        handleLayout(timestamp);
        assertPolicy(abinding.getName());

        if (abinding.getProtectionOrder()
            == AbstractSymmetricAsymmetricBinding.ProtectionOrder.EncryptBeforeSigning) {
            try {
                doEncryptBeforeSign();
                assertPolicy(
                        new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_BEFORE_SIGNING));
            } catch (TokenStoreException ex) {
                throw new Fault(ex);
            }
        } else {
            doSignBeforeEncrypt();
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.SIGN_BEFORE_ENCRYPTING));
        }
        reshuffleTimestamp();

        assertAlgorithmSuite(abinding.getAlgorithmSuite());
        assertWSSProperties(abinding.getName().getNamespaceURI());
        assertTrustProperties(abinding.getName().getNamespaceURI());
        assertPolicy(
            new QName(abinding.getName().getNamespaceURI(), SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));
    }

    private void doSignBeforeEncrypt() {
        try {
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            assertTokenWrapper(initiatorWrapper);
            boolean attached = false;
            if (initiatorWrapper != null) {
                AbstractToken initiatorToken = initiatorWrapper.getToken();
                if (initiatorToken instanceof IssuedToken) {
                    SecurityToken secToken = getSecurityToken();
                    if (secToken == null) {
                        unassertPolicy(initiatorToken, "Security token is not found or expired");
                        return;
                    } else if (isTokenRequired(initiatorToken.getIncludeTokenType())) {
                        Element el = secToken.getToken();
                        this.addEncryptedKeyElement(cloneElement(el));
                        attached = true;
                    }
                } else if (initiatorToken instanceof SamlToken && isRequestor()) {
                    SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)initiatorToken);
                    if (assertionWrapper != null && isTokenRequired(initiatorToken.getIncludeTokenType())) {
                        Element envelope = saaj.getSOAPPart().getEnvelope();
                        envelope = (Element)DOMUtils.getDomElement(envelope);
                        addSupportingElement(assertionWrapper.toDOM(envelope.getOwnerDocument()));
                        storeAssertionAsSecurityToken(assertionWrapper);
                    }
                } else if (initiatorToken instanceof SamlToken) {
                    String tokenId = getSAMLToken();
                    if (tokenId == null) {
                        unassertPolicy(initiatorToken, "Security token is not found or expired");
                        return;
                    }
                }
                assertToken(initiatorToken);
            }

            // Add timestamp
            List<WSEncryptionPart> sigs = new ArrayList<>();
            if (timestampEl != null) {
                WSEncryptionPart timestampPart =
                    convertToEncryptionPart(timestampEl.getElement());
                sigs.add(timestampPart);
            }
            addSupportingTokens(sigs);

            sigs.addAll(this.getSignedParts(null));

            if (isRequestor() && initiatorWrapper != null) {
                doSignature(initiatorWrapper, sigs, attached);
                doEndorse();
            } else if (!isRequestor()) {
                //confirm sig
                addSignatureConfirmation(sigs);

                AbstractTokenWrapper recipientSignatureToken = abinding.getRecipientSignatureToken();
                if (recipientSignatureToken == null) {
                    recipientSignatureToken = abinding.getRecipientToken();
                }
                if (recipientSignatureToken != null) {
                    assertTokenWrapper(recipientSignatureToken);
                    assertToken(recipientSignatureToken.getToken());
                    doSignature(recipientSignatureToken, sigs, attached);
                }
            }

            List<WSEncryptionPart> enc = getEncryptedParts();

            //Check for signature protection
            if (abinding.isEncryptSignature()) {
                if (mainSigId != null) {
                    WSEncryptionPart sigPart = new WSEncryptionPart(mainSigId, "Element");
                    sigPart.setElement(bottomUpElement);
                    enc.add(sigPart);
                }
                if (sigConfList != null && !sigConfList.isEmpty()) {
                    enc.addAll(sigConfList);
                }
                assertPolicy(
                    new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));
            }

            //Do encryption
            AbstractTokenWrapper encToken;
            if (isRequestor()) {
                enc.addAll(encryptedTokensList);
                encToken = abinding.getRecipientEncryptionToken();
                if (encToken == null) {
                    encToken = abinding.getRecipientToken();
                }
            } else {
                encToken = abinding.getInitiatorEncryptionToken();
                if (encToken == null) {
                    encToken = abinding.getInitiatorToken();
                }
            }

            if (encToken != null) {
                WSSecBase encr = null;
                if (encToken.getToken() != null && !enc.isEmpty()) {
                    if (encToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                        encr = doEncryptionDerived(encToken, enc);
                    } else {
                        String symEncAlgorithm = abinding.getAlgorithmSuite().getAlgorithmSuiteType().getEncryption();
                        KeyGenerator keyGen = KeyUtils.getKeyGenerator(symEncAlgorithm);
                        SecretKey symmetricKey = keyGen.generateKey();
                        encr = doEncryption(encToken, enc, false, symmetricKey);
                    }

                    encr.clean();
                }
                assertTokenWrapper(encToken);
                assertToken(encToken.getToken());
            }
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Sign before encryption failed due to : " + reason);
            LOG.log(Level.FINE, e.getMessage(), e);
            throw new Fault(e);
        }
    }

    private AbstractTokenWrapper getEncryptBeforeSignWrapper() {
        AbstractTokenWrapper wrapper;
        if (isRequestor()) {
            wrapper = abinding.getRecipientEncryptionToken();
            if (wrapper == null) {
                wrapper = abinding.getRecipientToken();
            }
        } else {
            wrapper = abinding.getInitiatorEncryptionToken();
            if (wrapper == null) {
                wrapper = abinding.getInitiatorToken();
            }
        }
        assertTokenWrapper(wrapper);

        return wrapper;
    }

    private void doEncryptBeforeSign() throws TokenStoreException {
        AbstractTokenWrapper wrapper = getEncryptBeforeSignWrapper();
        AbstractToken encryptionToken = null;
        if (wrapper != null) {
            encryptionToken = wrapper.getToken();
            assertToken(encryptionToken);
        }

        AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
        if (initiatorWrapper == null) {
            initiatorWrapper = abinding.getInitiatorToken();
        }
        assertTokenWrapper(initiatorWrapper);
        boolean attached = false;

        if (initiatorWrapper != null) {
            AbstractToken initiatorToken = initiatorWrapper.getToken();
            if (initiatorToken instanceof IssuedToken) {
                SecurityToken secToken = getSecurityToken();
                if (secToken == null) {
                    unassertPolicy(initiatorToken, "Security token is not found or expired");
                    return;
                } else if (isTokenRequired(initiatorToken.getIncludeTokenType())) {
                    Element el = secToken.getToken();
                    this.addEncryptedKeyElement(cloneElement(el));
                    attached = true;
                }
            } else if (initiatorToken instanceof SamlToken && isRequestor()) {
                try {
                    SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)initiatorToken);
                    if (assertionWrapper != null && isTokenRequired(initiatorToken.getIncludeTokenType())) {
                        Element envelope = saaj.getSOAPPart().getEnvelope();
                        envelope = (Element)DOMUtils.getDomElement(envelope);
                        addSupportingElement(assertionWrapper.toDOM(envelope.getOwnerDocument()));
                        storeAssertionAsSecurityToken(assertionWrapper);
                    }
                } catch (Exception e) {
                    String reason = e.getMessage();
                    LOG.log(Level.WARNING, "Encrypt before sign failed due to : " + reason);
                    LOG.log(Level.FINE, e.getMessage(), e);
                    throw new Fault(e);
                }
            } else if (initiatorToken instanceof SamlToken) {
                String tokenId = getSAMLToken();
                if (tokenId == null) {
                    unassertPolicy(initiatorToken, "Security token is not found or expired");
                    return;
                }
            }
        }

        List<WSEncryptionPart> sigParts = new ArrayList<>();
        if (timestampEl != null) {
            WSEncryptionPart timestampPart =
                convertToEncryptionPart(timestampEl.getElement());
            sigParts.add(timestampPart);
        }

        try {
            addSupportingTokens(sigParts);
        } catch (WSSecurityException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            unassertPolicy(encryptionToken, ex);
        }

        List<WSEncryptionPart> encrParts = null;
        try {
            encrParts = getEncryptedParts();
            //Signed parts are determined before encryption because encrypted signed headers
            //will not be included otherwise
            sigParts.addAll(this.getSignedParts(null));
        } catch (SOAPException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            throw new Fault(ex);
        }

        WSSecBase encrBase = null;
        SecretKey symmetricKey = null;
        if (encryptionToken != null && !encrParts.isEmpty()) {
            if (encryptionToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                encrBase = doEncryptionDerived(wrapper, encrParts);
            } else {
                String symEncAlgorithm = abinding.getAlgorithmSuite().getAlgorithmSuiteType().getEncryption();
                try {
                    KeyGenerator keyGen = KeyUtils.getKeyGenerator(symEncAlgorithm);
                    symmetricKey = keyGen.generateKey();
                    encrBase = doEncryption(wrapper, encrParts, true, symmetricKey);
                } catch (WSSecurityException ex) {
                    LOG.log(Level.FINE, ex.getMessage(), ex);
                    throw new Fault(ex);
                }
            }
        }

        if (!isRequestor()) {
            addSignatureConfirmation(sigParts);
        }

        try {
            if (!sigParts.isEmpty()) {
                if (initiatorWrapper != null && isRequestor()) {
                    doSignature(initiatorWrapper, sigParts, attached);
                } else if (!isRequestor()) {
                    AbstractTokenWrapper recipientSignatureToken =
                        abinding.getRecipientSignatureToken();
                    if (recipientSignatureToken == null) {
                        recipientSignatureToken = abinding.getRecipientToken();
                    }
                    if (recipientSignatureToken != null) {
                        assertTokenWrapper(recipientSignatureToken);
                        assertToken(recipientSignatureToken.getToken());
                        doSignature(recipientSignatureToken, sigParts, attached);
                    }
                }
            }
        } catch (WSSecurityException | SOAPException | TokenStoreException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            throw new Fault(ex);
        }

        if (isRequestor()) {
            doEndorse();
        }

        if (encrBase != null) {
            encryptTokensInSecurityHeader(encryptionToken, encrBase, symmetricKey);
            encrBase.clean();
        }
    }


    private void encryptTokensInSecurityHeader(AbstractToken encryptionToken,
                                               WSSecBase encrBase,
                                               SecretKey symmetricKey) {
        List<WSEncryptionPart> secondEncrParts = new ArrayList<>();

        // Check for signature protection
        if (abinding.isEncryptSignature()) {
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));

            // Now encrypt the signature using the above token
            if (mainSigId != null) {
                WSEncryptionPart sigPart = new WSEncryptionPart(mainSigId, "Element");
                sigPart.setElement(bottomUpElement);
                secondEncrParts.add(sigPart);
            }

            if (sigConfList != null && !sigConfList.isEmpty()) {
                secondEncrParts.addAll(sigConfList);
            }
        }

        // Add any SupportingTokens that need to be encrypted
        if (isRequestor()) {
            secondEncrParts.addAll(encryptedTokensList);
        }

        if (secondEncrParts.isEmpty()) {
            return;
        }

        // Perform encryption
        if (encryptionToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys
            && encrBase instanceof WSSecDKEncrypt) {
            try {
                Element secondRefList =
                    ((WSSecDKEncrypt)encrBase).encryptForExternalRef(null, secondEncrParts);
                if (secondRefList != null) {
                    ((WSSecDKEncrypt)encrBase).addExternalRefElement(secondRefList);
                }

            } catch (WSSecurityException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                throw new Fault(ex);
            }
        } else if (encrBase instanceof WSSecEncrypt) {
            try {
                // Encrypt, get hold of the ref list and add it
                Element secondRefList = saaj.getSOAPPart()
                    .createElementNS(WSS4JConstants.ENC_NS,
                                     WSS4JConstants.ENC_PREFIX + ":ReferenceList");
                if (lastEncryptedKeyElement != null) {
                    insertAfter(secondRefList, lastEncryptedKeyElement);
                } else {
                    this.insertBeforeBottomUp(secondRefList);
                }
                ((WSSecEncrypt)encrBase).encryptForRef(secondRefList, secondEncrParts, symmetricKey);

            } catch (WSSecurityException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                throw new Fault(ex);
            }
        }
    }

    private WSSecBase doEncryption(AbstractTokenWrapper recToken,
                                    List<WSEncryptionPart> encrParts,
                                    boolean externalRef,
                                    SecretKey symmetricKey) {
        AbstractToken encrToken = recToken.getToken();
        assertPolicy(recToken);
        assertPolicy(encrToken);
        try {
            WSSecEncrypt encr = new WSSecEncrypt(secHeader);
            encr.setEncryptionSerializer(new StaxSerializer());
            encr.setIdAllocator(wssConfig.getIdAllocator());
            encr.setCallbackLookup(callbackLookup);
            encr.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
            encr.setStoreBytesInAttachment(storeBytesInAttachment);
            encr.setExpandXopInclude(isExpandXopInclude());
            encr.setWsDocInfo(wsDocInfo);

            Crypto crypto = getEncryptionCrypto();

            SecurityToken securityToken = null;
            try {
                securityToken = getSecurityToken();
                if (!isRequestor() && securityToken != null
                    && recToken.getToken() instanceof SamlToken) {
                    String tokenType = securityToken.getTokenType();
                    if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                        || WSS4JConstants.SAML_NS.equals(tokenType)) {
                        encr.setCustomEKTokenValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
                        encr.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
                        encr.setCustomEKTokenId(securityToken.getId());
                    } else if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                        || WSS4JConstants.SAML2_NS.equals(tokenType)) {
                        encr.setCustomEKTokenValueType(WSS4JConstants.WSS_SAML2_KI_VALUE_TYPE);
                        encr.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
                        encr.setCustomEKTokenId(securityToken.getId());
                    } else {
                        setKeyIdentifierType(encr, encrToken);
                    }
                } else {
                    setKeyIdentifierType(encr, encrToken);
                }
            } catch (TokenStoreException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                throw new Fault(ex);
            }
            //
            // Using a stored cert is only suitable for the Issued Token case, where
            // we're extracting the cert from a SAML Assertion on the provider side
            //
            if (!isRequestor() && securityToken != null
                && securityToken.getX509Certificate() != null) {
                encr.setUseThisCert(securityToken.getX509Certificate());
            } else if (!isRequestor() && securityToken != null
                && securityToken.getKey() instanceof PublicKey) {
                encr.setUseThisPublicKey((PublicKey)securityToken.getKey());
                encr.setKeyIdentifierType(WSConstants.KEY_VALUE);
            } else {
                setEncryptionUser(encr, encrToken, false, crypto);
            }
            if (!encr.isCertSet() && encr.getUseThisPublicKey() == null && crypto == null) {
                unassertPolicy(recToken, "Missing security configuration. "
                    + "Make sure jaxws:client element is configured "
                    + "with a " + SecurityConstants.ENCRYPT_PROPERTIES + " value.");
            }
            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();
            AlgorithmSuiteType algType = algorithmSuite.getAlgorithmSuiteType();
            encr.setSymmetricEncAlgorithm(algType.getEncryption());
            encr.setKeyEncAlgo(algType.getAsymmetricKeyWrap());
            encr.setMGFAlgorithm(algType.getMGFAlgo());
            encr.setDigestAlgorithm(algType.getEncryptionDigest());
            encr.prepare(crypto, symmetricKey);

            Element encryptedKeyElement = encr.getEncryptedKeyElement();
            List<Element> attachments = encr.getAttachmentEncryptedDataElements();
            //Encrypt, get hold of the ref list and add it
            if (externalRef) {
                Element refList = encr.encryptForRef(null, encrParts, symmetricKey);
                if (refList != null) {
                    insertBeforeBottomUp(refList);
                }
                if (attachments != null) {
                    for (Element attachment : attachments) {
                        this.insertBeforeBottomUp(attachment);
                    }
                }
                if (refList != null || (attachments != null && !attachments.isEmpty())) {
                    this.addEncryptedKeyElement(encryptedKeyElement);
                }
            } else {
                Element refList = encr.encryptForRef(null, encrParts, symmetricKey);
                if (refList != null || (attachments != null && !attachments.isEmpty())) {
                    this.addEncryptedKeyElement(encryptedKeyElement);
                }

                // Add internal refs
                if (refList != null) {
                    encryptedKeyElement.appendChild(refList);
                }
                if (attachments != null) {
                    for (Element attachment : attachments) {
                        this.addEncryptedKeyElement(attachment);
                    }
                }
            }

            // Put BST before EncryptedKey element
            if (encr.getBSTTokenId() != null) {
                encr.prependBSTElementToHeader();
            }

            return encr;
        } catch (InvalidCanonicalizerException | WSSecurityException e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            unassertPolicy(recToken, e);
        }
        return null;
    }

    private WSSecBase doEncryptionDerived(AbstractTokenWrapper recToken,
                                     List<WSEncryptionPart> encrParts) {
        AbstractToken encrToken = recToken.getToken();
        assertPolicy(recToken);
        assertPolicy(encrToken);
        try {
            WSSecDKEncrypt dkEncr = new WSSecDKEncrypt(secHeader);
            dkEncr.setEncryptionSerializer(new StaxSerializer());
            dkEncr.setIdAllocator(wssConfig.getIdAllocator());
            dkEncr.setCallbackLookup(callbackLookup);
            dkEncr.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
            dkEncr.setStoreBytesInAttachment(storeBytesInAttachment);
            dkEncr.setExpandXopInclude(isExpandXopInclude());
            dkEncr.setWsDocInfo(wsDocInfo);
            if (recToken.getToken().getVersion() == SPConstants.SPVersion.SP11) {
                dkEncr.setWscVersion(ConversationConstants.VERSION_05_02);
            }

            if (encrKey == null) {
                setupEncryptedKey(encrToken);
            }

            dkEncr.setTokenIdentifier(this.encryptedKeyId);
            dkEncr.getParts().addAll(encrParts);
            dkEncr.setCustomValueType(WSS4JConstants.SOAPMESSAGE_NS11 + "#"
                + WSS4JConstants.ENC_KEY_VALUE_TYPE);

            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();
            AlgorithmSuiteType algType = algorithmSuite.getAlgorithmSuiteType();
            dkEncr.setSymmetricEncAlgorithm(algType.getEncryption());
            dkEncr.setDerivedKeyLength(algType.getEncryptionDerivedKeyLength() / 8);
            dkEncr.prepare(this.encryptedKeyValue);

            addDerivedKeyElement(dkEncr.getdktElement());
            Element refList = dkEncr.encryptForExternalRef(null, encrParts);
            if (refList != null) {
                insertBeforeBottomUp(refList);
            }
            return dkEncr;
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            unassertPolicy(recToken, e);
        }

        return null;
    }

    private void assertUnusedTokens(AbstractTokenWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        Collection<AssertionInfo> ais = aim.getAssertionInfo(wrapper.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == wrapper) {
                    ai.setAsserted(true);
                }
            }
        }
        ais = aim.getAssertionInfo(wrapper.getToken().getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == wrapper.getToken()) {
                    ai.setAsserted(true);
                }
            }
        }
    }

    private void doSignature(AbstractTokenWrapper wrapper, List<WSEncryptionPart> sigParts, boolean attached)
            throws WSSecurityException, SOAPException, TokenStoreException {

        if (!isRequestor()) {
            assertUnusedTokens(abinding.getInitiatorToken());
            assertUnusedTokens(abinding.getInitiatorEncryptionToken());
            assertUnusedTokens(abinding.getInitiatorSignatureToken());
        } else {
            assertUnusedTokens(abinding.getRecipientToken());
            assertUnusedTokens(abinding.getRecipientEncryptionToken());
            assertUnusedTokens(abinding.getRecipientSignatureToken());
        }

        AbstractToken sigToken = wrapper.getToken();
        if (sigParts.isEmpty()) {
            // Add the BST to the security header if required
            if (!attached && isTokenRequired(sigToken.getIncludeTokenType())) {
                WSSecSignature sig = getSignatureBuilder(sigToken, attached, false);
                sig.appendBSTElementToHeader();
                sig.clean();
            }
            return;
        }
        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            // Set up the encrypted key to use
            setupEncryptedKey(sigToken);

            WSSecDKSign dkSign = new WSSecDKSign(secHeader);
            dkSign.setIdAllocator(wssConfig.getIdAllocator());
            dkSign.setCallbackLookup(callbackLookup);
            dkSign.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
            dkSign.setStoreBytesInAttachment(storeBytesInAttachment);
            dkSign.setExpandXopInclude(isExpandXopInclude());
            dkSign.setWsDocInfo(wsDocInfo);
            if (wrapper.getToken().getVersion() == SPConstants.SPVersion.SP11) {
                dkSign.setWscVersion(ConversationConstants.VERSION_05_02);
            }

            dkSign.setTokenIdentifier(this.encryptedKeyId);

            // Set the algo info
            dkSign.setSignatureAlgorithm(abinding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
            dkSign.setSigCanonicalization(abinding.getAlgorithmSuite().getC14n().getValue());
            AlgorithmSuiteType algType = abinding.getAlgorithmSuite().getAlgorithmSuiteType();
            dkSign.setDigestAlgorithm(algType.getDigest());
            dkSign.setDerivedKeyLength(algType.getSignatureDerivedKeyLength() / 8);
            dkSign.setCustomValueType(WSS4JConstants.SOAPMESSAGE_NS11 + "#"
                    + WSS4JConstants.ENC_KEY_VALUE_TYPE);

            boolean includePrefixes =
                MessageUtils.getContextualBoolean(
                    message, SecurityConstants.ADD_INCLUSIVE_PREFIXES, true
                );
            dkSign.setAddInclusivePrefixes(includePrefixes);

            try {
                dkSign.prepare(this.encryptedKeyValue);

                if (abinding.isProtectTokens()) {
                    assertPolicy(
                        new QName(abinding.getName().getNamespaceURI(), SPConstants.PROTECT_TOKENS));
                    if (bstElement != null) {
                        WSEncryptionPart bstPart =
                            new WSEncryptionPart(bstElement.getAttributeNS(WSS4JConstants.WSU_NS, "Id"));
                        bstPart.setElement(bstElement);
                        sigParts.add(bstPart);
                    } else {
                        WSEncryptionPart ekPart =
                            new WSEncryptionPart(encrKey.getId());
                        ekPart.setElement(encrKey.getEncryptedKeyElement());
                        sigParts.add(ekPart);
                    }
                }

                dkSign.getParts().addAll(sigParts);

                List<Reference> referenceList = dkSign.addReferencesToSign(sigParts);
                if (!referenceList.isEmpty()) {
                    // Add elements to header
                    addDerivedKeyElement(dkSign.getdktElement());

                    //Do signature
                    if (bottomUpElement == null) {
                        dkSign.computeSignature(referenceList, false, null);
                    } else {
                        dkSign.computeSignature(referenceList, true, bottomUpElement);
                    }
                    bottomUpElement = dkSign.getSignatureElement();
                    addSig(dkSign.getSignatureValue());

                    mainSigId = dkSign.getSignatureId();
                }
                dkSign.clean();
            } catch (Exception ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                throw new Fault(ex);
            }
        } else {
            WSSecSignature sig = getSignatureBuilder(sigToken, attached, false);

            if (abinding.isProtectTokens()) {
                assertPolicy(
                    new QName(abinding.getName().getNamespaceURI(), SPConstants.PROTECT_TOKENS));
                if (sig.getCustomTokenId() != null
                    && (sigToken instanceof SamlToken || sigToken instanceof IssuedToken)) {
                    WSEncryptionPart samlPart =
                        new WSEncryptionPart(sig.getCustomTokenId());
                    sigParts.add(samlPart);
                } else if (sig.getBSTTokenId() != null) {
                    // This action must occur before sig.prependBSTElementToHeader
                    WSEncryptionPart bstPart =
                        new WSEncryptionPart(sig.getBSTTokenId());
                    bstPart.setElement(sig.getBinarySecurityTokenElement());
                    sigParts.add(bstPart);
                    sig.prependBSTElementToHeader();
                }
            }

            List<Reference> referenceList = sig.addReferencesToSign(sigParts);
            if (!referenceList.isEmpty()) {
                //Do signature
                if (bottomUpElement == null) {
                    sig.computeSignature(referenceList, false, null);
                } else {
                    sig.computeSignature(referenceList, true, bottomUpElement);
                }
                bottomUpElement = sig.getSignatureElement();

                if (!abinding.isProtectTokens()) {
                    Element bstElement = sig.getBinarySecurityTokenElement();
                    if (bstElement != null) {
                        secHeader.getSecurityHeaderElement().insertBefore(bstElement, bottomUpElement);
                    }
                }

                addSig(sig.getSignatureValue());

                mainSigId = sig.getId();
            }

            sig.clean();
        }
    }

    private void setupEncryptedKey(AbstractToken token) throws WSSecurityException {
        if (!isRequestor() && token.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            //If we already have them, simply return
            if (encryptedKeyId != null && encryptedKeyValue != null) {
                return;
            }

            //Use the secret from the incoming EncryptedKey element
            List<WSHandlerResult> results =
                CastUtils.cast(
                    (List<?>)message.getExchange().getInMessage().get(WSHandlerConstants.RECV_RESULTS));
            if (results != null) {
                WSSecurityEngineResult encryptedKeyResult = getEncryptedKeyResult();
                if (encryptedKeyResult != null) {
                    encryptedKeyId = (String)encryptedKeyResult.get(WSSecurityEngineResult.TAG_ID);
                    encryptedKeyValue = (byte[])encryptedKeyResult.get(WSSecurityEngineResult.TAG_SECRET);
                }

                //In the case where we don't have the EncryptedKey in the
                //request, for the control to have reached this state,
                //the scenario MUST be a case where this is the response
                //message by a listener created for an async client
                //Therefore we will create a new EncryptedKey
                if (encryptedKeyId == null && encryptedKeyValue == null) {
                    createEncryptedKey(token);
                }
            } else {
                unassertPolicy(token, "No security results found");
            }
        } else {
            createEncryptedKey(token);
        }
    }

    private void createEncryptedKey(AbstractToken token)
        throws WSSecurityException {
        //Set up the encrypted key to use
        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        KeyGenerator keyGen = KeyUtils.getKeyGenerator(algType.getEncryption());
        SecretKey symmetricKey = keyGen.generateKey();

        encrKey = this.getEncryptedKeyBuilder(token, symmetricKey);
        Element bstElem = encrKey.getBinarySecurityTokenElement();
        if (bstElem != null) {
            // If a BST is available then use it
            encrKey.prependBSTElementToHeader();
        }

        // Add the EncryptedKey
        this.addEncryptedKeyElement(encrKey.getEncryptedKeyElement());
        encryptedKeyValue = symmetricKey.getEncoded();
        encryptedKeyId = encrKey.getId();
    }

    private String getSAMLToken() {

        List<WSHandlerResult> results = CastUtils.cast((List<?>)message.getExchange().getInMessage()
            .get(WSHandlerConstants.RECV_RESULTS));

        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();

            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                if (actInt.intValue() == WSConstants.ST_SIGNED
                    || actInt.intValue() == WSConstants.ST_UNSIGNED) {
                    Instant created = Instant.now();
                    Instant expires = created.plusSeconds(WSS4JUtils.getSecurityTokenLifetime(message) / 1000L);

                    String id = (String)wser.get(WSSecurityEngineResult.TAG_ID);
                    SecurityToken tempTok = new SecurityToken(id, created, expires);
                    tempTok.setSecret((byte[])wser.get(WSSecurityEngineResult.TAG_SECRET));
                    tempTok.setX509Certificate(
                        (X509Certificate)wser.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE), null
                    );

                    SamlAssertionWrapper samlAssertion =
                        (SamlAssertionWrapper)wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                    if (samlAssertion.getSamlVersion() == SAMLVersion.VERSION_20) {
                        tempTok.setTokenType(WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
                    } else {
                        tempTok.setTokenType(WSS4JConstants.WSS_SAML_TOKEN_TYPE);
                    }

                    message.put(SecurityConstants.TOKEN, tempTok);

                    return id;
                }
            }
        }
        return null;
    }
}
