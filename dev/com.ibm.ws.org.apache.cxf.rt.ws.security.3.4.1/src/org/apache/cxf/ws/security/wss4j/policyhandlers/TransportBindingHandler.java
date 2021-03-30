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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.crypto.dsig.Reference;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.AttachmentCallbackHandler;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecDKSign;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KeyValueToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SignedElements;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.TransportBinding;
import org.apache.wss4j.policy.model.TransportToken;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.X509Token;

/**
 *
 */
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public class TransportBindingHandler extends AbstractBindingBuilder {
    TransportBinding tbinding;

    public TransportBindingHandler(WSSConfig config,
                                   TransportBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) throws SOAPException {
        super(config, binding, saaj, secHeader, aim, message);
        this.tbinding = binding;
    }

    private void addSignedSupportingTokens(SupportingTokens sgndSuppTokens)
        throws Exception {
        for (AbstractToken token : sgndSuppTokens.getTokens()) {
            assertToken(token);
            if (token != null && !isTokenRequired(token.getIncludeTokenType())) {
                continue;
            }

            if (token instanceof UsernameToken) {
                WSSecUsernameToken utBuilder = addUsernameToken((UsernameToken)token);
                if (utBuilder != null) {
                    utBuilder.prepare();
                    utBuilder.appendToHeader();
                }
            } else if (token instanceof IssuedToken || token instanceof KerberosToken
                || token instanceof SpnegoContextToken) {
                SecurityToken secTok = getSecurityToken();

                if (isTokenRequired(token.getIncludeTokenType())) {
                    //Add the token
                    addEncryptedKeyElement(cloneElement(secTok.getToken()));
                }
            } else if (token instanceof SamlToken) {
                SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)token);
                if (assertionWrapper != null) {
                    Element envelope = saaj.getSOAPPart().getEnvelope();
                    envelope = (Element)DOMUtils.getDomElement(envelope);
                    addSupportingElement(assertionWrapper.toDOM(envelope.getOwnerDocument()));
                }
            } else {
                //REVISIT - not supported for signed.  Exception?
            }
        }

    }

    public void handleBinding() {
        WSSecTimestamp timestamp = createTimestamp();
        handleLayout(timestamp);

        try {
            if (this.isRequestor()) {
                TransportToken transportTokenWrapper = tbinding.getTransportToken();
                if (transportTokenWrapper != null) {
                    AbstractToken transportToken = transportTokenWrapper.getToken();
                    if (transportToken instanceof IssuedToken) {
                        SecurityToken secToken = getSecurityToken();
                        if (secToken == null) {
                            unassertPolicy(transportToken, "No transport token id");
                            return;
                        }
                        assertPolicy(transportToken);
                        if (isTokenRequired(transportToken.getIncludeTokenType())) {
                            Element el = secToken.getToken();
                            addEncryptedKeyElement(cloneElement(el));
                        }
                    }
                    assertToken(transportToken);
                    assertTokenWrapper(transportTokenWrapper);
                }

                handleNonEndorsingSupportingTokens();
                if (transportTokenWrapper != null) {
                    handleEndorsingSupportingTokens();
                }
            } else {
                handleNonEndorsingSupportingTokens();
                if (tbinding != null && tbinding.getTransportToken() != null) {
                    assertTokenWrapper(tbinding.getTransportToken());
                    assertToken(tbinding.getTransportToken().getToken());
                    handleEndorsingSupportingTokens();
                }
                addSignatureConfirmation(null);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            throw new Fault(e);
        }

        if (tbinding != null) {
            assertPolicy(tbinding.getName());
            assertAlgorithmSuite(tbinding.getAlgorithmSuite());
            assertWSSProperties(tbinding.getName().getNamespaceURI());
            assertTrustProperties(tbinding.getName().getNamespaceURI());
        }
        assertPolicy(SP12Constants.SIGNED_PARTS);
        assertPolicy(SP11Constants.SIGNED_PARTS);
        assertPolicy(SP12Constants.ENCRYPTED_PARTS);
        assertPolicy(SP11Constants.ENCRYPTED_PARTS);
    }

    /**
     * Handle the non-endorsing supporting tokens
     */
    private void handleNonEndorsingSupportingTokens() throws Exception {
        Collection<AssertionInfo> ais;

        ais = getAllAssertionsByLocalname(SPConstants.SIGNED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens sgndSuppTokens = (SupportingTokens)ai.getAssertion();
                if (sgndSuppTokens != null) {
                    addSignedSupportingTokens(sgndSuppTokens);
                }
                ai.setAsserted(true);
            }
        }

        ais = getAllAssertionsByLocalname(SPConstants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens sgndSuppTokens = (SupportingTokens)ai.getAssertion();
                if (sgndSuppTokens != null) {
                    addSignedSupportingTokens(sgndSuppTokens);
                }
                ai.setAsserted(true);
            }
        }

        ais = getAllAssertionsByLocalname(SPConstants.ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens encrSuppTokens = (SupportingTokens)ai.getAssertion();
                if (encrSuppTokens != null) {
                    addSignedSupportingTokens(encrSuppTokens);
                }
                ai.setAsserted(true);
            }
        }

        ais = getAllAssertionsByLocalname(SPConstants.SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens suppTokens = (SupportingTokens)ai.getAssertion();
                if (suppTokens != null && suppTokens.getTokens() != null
                    && suppTokens.getTokens().size() > 0) {
                    handleSupportingTokens(suppTokens, false, new ArrayList<>());
                }
                ai.setAsserted(true);
            }
        }
    }

    /**
     * Handle the endorsing supporting tokens
     */
    private void handleEndorsingSupportingTokens() throws Exception {
        Collection<AssertionInfo> ais;

        ais = getAllAssertionsByLocalname(SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens sgndSuppTokens = null;
            for (AssertionInfo ai : ais) {
                sgndSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }
            if (sgndSuppTokens != null) {
                for (AbstractToken token : sgndSuppTokens.getTokens()) {
                    handleEndorsingToken(token, sgndSuppTokens);
                }
            }
        }

        ais = getAllAssertionsByLocalname(SPConstants.ENDORSING_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }

            if (endSuppTokens != null) {
                for (AbstractToken token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens);
                }
            }
        }
        ais = getAllAssertionsByLocalname(SPConstants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }

            if (endSuppTokens != null) {
                for (AbstractToken token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens);
                }
            }
        }
        ais = getAllAssertionsByLocalname(SPConstants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }

            if (endSuppTokens != null) {
                for (AbstractToken token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens);
                }
            }
        }
    }

    private void handleEndorsingToken(
        AbstractToken token, SupportingTokens wrapper
    ) throws Exception {
        assertToken(token);
        if (token != null && !isTokenRequired(token.getIncludeTokenType())) {
            return;
        }

        if (token instanceof IssuedToken
            || token instanceof SecureConversationToken
            || token instanceof SecurityContextToken
            || token instanceof KerberosToken
            || token instanceof SpnegoContextToken) {
            addSig(doIssuedTokenSignature(token, wrapper));
        } else if (token instanceof X509Token
            || token instanceof KeyValueToken) {
            addSig(doX509TokenSignature(token, wrapper));
        } else if (token instanceof SamlToken) {
            SamlAssertionWrapper assertionWrapper = addSamlToken((SamlToken)token);
            Element envelope = saaj.getSOAPPart().getEnvelope();
            envelope = (Element)DOMUtils.getDomElement(envelope);
            assertionWrapper.toDOM(envelope.getOwnerDocument());
            storeAssertionAsSecurityToken(assertionWrapper);
            addSig(doIssuedTokenSignature(token, wrapper));
        } else if (token instanceof UsernameToken) {
            // Create a UsernameToken object for derived keys and store the security token
            byte[] salt = UsernameTokenUtil.generateSalt(true);
            WSSecUsernameToken usernameToken = addDKUsernameToken((UsernameToken)token, salt, true);
            String id = usernameToken.getId();
            byte[] secret = usernameToken.getDerivedKey(salt);
            Arrays.fill(salt, (byte)0);

            Instant created = Instant.now();
            Instant expires = created.plusSeconds(WSS4JUtils.getSecurityTokenLifetime(message) / 1000L);
            SecurityToken tempTok = new SecurityToken(id,
                                                      usernameToken.getUsernameTokenElement(),
                                                      created,
                                                      expires);
            tempTok.setSecret(secret);
            getTokenStore().add(tempTok);
            message.put(SecurityConstants.TOKEN_ID, tempTok.getId());

            addSig(doIssuedTokenSignature(token, wrapper));
        }
    }


    private byte[] doX509TokenSignature(AbstractToken token, SupportingTokens wrapper)
        throws Exception {

        List<WSEncryptionPart> sigParts =
            signPartsAndElements(wrapper.getSignedParts(), wrapper.getSignedElements());

        if (token.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
            KeyGenerator keyGen = KeyUtils.getKeyGenerator(algType.getEncryption());
            SecretKey symmetricKey = keyGen.generateKey();

            WSSecEncryptedKey encrKey = getEncryptedKeyBuilder(token, symmetricKey);
            assertPolicy(wrapper);

            Element bstElem = encrKey.getBinarySecurityTokenElement();
            if (bstElem != null) {
                addTopDownElement(bstElem);
            }
            encrKey.appendToHeader();

            WSSecDKSign dkSig = new WSSecDKSign(secHeader);
            dkSig.setIdAllocator(wssConfig.getIdAllocator());
            dkSig.setCallbackLookup(callbackLookup);
            if (token.getVersion() == SPConstants.SPVersion.SP11) {
                dkSig.setWscVersion(ConversationConstants.VERSION_05_02);
            }

            dkSig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());
            dkSig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
            dkSig.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
            dkSig.setStoreBytesInAttachment(storeBytesInAttachment);
            dkSig.setExpandXopInclude(isExpandXopInclude());
            dkSig.setWsDocInfo(wsDocInfo);

            dkSig.setDerivedKeyLength(algType.getSignatureDerivedKeyLength() / 8);

            dkSig.setTokenIdentifier(encrKey.getId());

            dkSig.prepare(symmetricKey.getEncoded());

            dkSig.getParts().addAll(sigParts);
            List<Reference> referenceList = dkSig.addReferencesToSign(sigParts);

            //Do signature
            dkSig.appendDKElementToHeader();
            dkSig.computeSignature(referenceList, false, null);

            dkSig.clean();
            return dkSig.getSignatureValue();
        }
        WSSecSignature sig = getSignatureBuilder(token, false, false);
        assertPolicy(wrapper);
        if (sig != null) {
            sig.prependBSTElementToHeader();

            List<Reference> referenceList = sig.addReferencesToSign(sigParts);

            if (bottomUpElement == null) {
                sig.computeSignature(referenceList, false, null);
            } else {
                sig.computeSignature(referenceList, true, bottomUpElement);
            }
            bottomUpElement = sig.getSignatureElement();
            mainSigId = sig.getId();

            return sig.getSignatureValue();
        }
        return new byte[0];
    }

    private byte[] doIssuedTokenSignature(
        final AbstractToken token, final SupportingTokens wrapper
    ) throws Exception {
        boolean tokenIncluded = false;
        // Get the issued token
        SecurityToken secTok = getSecurityToken();
        if (secTok == null) {
            LOG.fine("The retrieved SecurityToken was null");
            Exception ex = new Exception("The retrieved SecurityToken was null");
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, ex
            );
        }

        if (isTokenRequired(token.getIncludeTokenType())) {
            //Add the token
            Element el = cloneElement(secTok.getToken());
            //if (securityTok != null) {
                //do we need to sign this as well?
                //String id = addWsuIdToElement(el);
                //sigParts.add(new WSEncryptionPart(id));
            //}

            addEncryptedKeyElement(el);
            tokenIncluded = true;
        }

        List<WSEncryptionPart> sigParts =
                signPartsAndElements(wrapper.getSignedParts(), wrapper.getSignedElements());

        if (token.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            return doDerivedKeySignature(tokenIncluded, secTok, token, sigParts);
        }
        return doSignature(tokenIncluded, secTok, token, sigParts);
    }

    private byte[] doDerivedKeySignature(
        boolean tokenIncluded,
        SecurityToken secTok,
        AbstractToken token,
        List<WSEncryptionPart> sigParts
    ) throws Exception {
        //Do Signature with derived keys
        WSSecDKSign dkSign = new WSSecDKSign(secHeader);
        dkSign.setIdAllocator(wssConfig.getIdAllocator());
        dkSign.setCallbackLookup(callbackLookup);
        dkSign.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
        dkSign.setStoreBytesInAttachment(storeBytesInAttachment);
        dkSign.setExpandXopInclude(isExpandXopInclude());
        dkSign.setWsDocInfo(wsDocInfo);

        AlgorithmSuite algorithmSuite = tbinding.getAlgorithmSuite();

        //Setting the AttachedReference or the UnattachedReference according to the flag
        Element ref;
        if (tokenIncluded) {
            ref = secTok.getAttachedReference();
        } else {
            ref = secTok.getUnattachedReference();
        }

        if (ref != null) {
            dkSign.setStrElem(cloneElement(ref));
        } else {
            dkSign.setTokenIdentifier(secTok.getId());
        }

        if (token instanceof UsernameToken) {
            dkSign.setCustomValueType(WSS4JConstants.WSS_USERNAME_TOKEN_VALUE_TYPE);
        }

        // Set the algo info
        dkSign.setSignatureAlgorithm(algorithmSuite.getAlgorithmSuiteType().getSymmetricSignature());
        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        dkSign.setDerivedKeyLength(algType.getSignatureDerivedKeyLength() / 8);
        if (token.getVersion() == SPConstants.SPVersion.SP11) {
            dkSign.setWscVersion(ConversationConstants.VERSION_05_02);
        }
        dkSign.prepare(secTok.getSecret());

        addDerivedKeyElement(dkSign.getdktElement());

        dkSign.getParts().addAll(sigParts);
        List<Reference> referenceList = dkSign.addReferencesToSign(sigParts);

        //Do signature
        dkSign.computeSignature(referenceList, false, null);

        dkSign.clean();
        return dkSign.getSignatureValue();
    }

    private byte[] doSignature(
        boolean tokenIncluded,
        SecurityToken secTok,
        AbstractToken token,
        List<WSEncryptionPart> sigParts
    ) throws Exception {
        WSSecSignature sig = new WSSecSignature(secHeader);
        sig.setIdAllocator(wssConfig.getIdAllocator());
        sig.setCallbackLookup(callbackLookup);
        sig.setAttachmentCallbackHandler(new AttachmentCallbackHandler(message));
        sig.setStoreBytesInAttachment(storeBytesInAttachment);
        sig.setExpandXopInclude(isExpandXopInclude());
        sig.setWsDocInfo(wsDocInfo);

        //Setting the AttachedReference or the UnattachedReference according to the flag
        Element ref;
        if (tokenIncluded) {
            ref = secTok.getAttachedReference();
        } else {
            ref = secTok.getUnattachedReference();
        }

        if (ref != null) {
            SecurityTokenReference secRef =
                new SecurityTokenReference(cloneElement(ref), new BSPEnforcer());
            sig.setSecurityTokenReference(secRef);
            sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
        } else if (token instanceof UsernameToken) {
            sig.setCustomTokenId(secTok.getId());
            sig.setCustomTokenValueType(WSS4JConstants.WSS_USERNAME_TOKEN_VALUE_TYPE);
            int type = tokenIncluded ? WSConstants.CUSTOM_SYMM_SIGNING
                    : WSConstants.CUSTOM_SYMM_SIGNING_DIRECT;
            sig.setKeyIdentifierType(type);
        } else if (secTok.getTokenType() == null) {
            sig.setCustomTokenValueType(WSS4JConstants.WSS_SAML_KI_VALUE_TYPE);
            sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
        } else {
            String id = secTok.getWsuId();
            if (id == null) {
                sig.setCustomTokenId(secTok.getId());
                sig.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING_DIRECT);
            } else {
                sig.setCustomTokenId(secTok.getWsuId());
                sig.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING);
            }
            String tokenType = secTok.getTokenType();
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
            }
        }
        Crypto crypto = null;
        if (secTok.getSecret() == null) {
            sig.setX509Certificate(secTok.getX509Certificate());

            crypto = secTok.getCrypto();
            if (crypto == null) {
                crypto = getSignatureCrypto();
            }
            if (crypto == null) {
                LOG.fine("No signature Crypto properties are available");
                Exception ex = new Exception("No signature Crypto properties are available");
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, ex
                );
            }

            String uname = crypto.getX509Identifier(secTok.getX509Certificate());
            if (uname == null) {
                String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
                uname = (String)SecurityUtils.getSecurityPropertyValue(userNameKey, message);
            }

            String password =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD, message);
            if (StringUtils.isEmpty(password)) {
                password = getPassword(uname, token, WSPasswordCallback.SIGNATURE);
            }

            sig.setUserInfo(uname, password);
            sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAlgorithmSuiteType().getAsymmetricSignature());
        } else {
            crypto = getSignatureCrypto();
            sig.setSecretKey(secTok.getSecret());
            sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
        }
        sig.setSigCanonicalization(binding.getAlgorithmSuite().getC14n().getValue());
        AlgorithmSuiteType algType = binding.getAlgorithmSuite().getAlgorithmSuiteType();
        sig.setDigestAlgo(algType.getDigest());

        sig.prepare(crypto);

        sig.getParts().addAll(sigParts);
        List<Reference> referenceList = sig.addReferencesToSign(sigParts);

        //Do signature
        if (bottomUpElement == null) {
            sig.computeSignature(referenceList, false, null);
        } else {
            sig.computeSignature(referenceList, true, bottomUpElement);
        }
        bottomUpElement = sig.getSignatureElement();
        mainSigId = sig.getId();

        return sig.getSignatureValue();
    }

    /**
     * Identifies the portions of the message to be signed/encrypted.
     */
    private List<WSEncryptionPart> signPartsAndElements(
        SignedParts signedParts,
        SignedElements signedElements
    ) throws SOAPException {

        List<WSEncryptionPart> result = new ArrayList<>();
        List<Element> found = new ArrayList<>();

        // Add timestamp
        if (timestampEl != null) {
            WSEncryptionPart timestampPart =
                    new WSEncryptionPart("Timestamp", WSS4JConstants.WSU_NS, "Element");
            String id = addWsuIdToElement(timestampEl.getElement());
            timestampPart.setId(id);
            timestampPart.setElement(timestampEl.getElement());

            found.add(timestampPart.getElement());
            result.add(timestampPart);
        }

        // Add SignedParts
        if (signedParts != null) {
            List<WSEncryptionPart> parts = new ArrayList<>();
            boolean isSignBody = signedParts.isBody();

            for (Header head : signedParts.getHeaders()) {
                WSEncryptionPart wep =
                    new WSEncryptionPart(head.getName(), head.getNamespace(), "Element");
                parts.add(wep);
            }

            // Handle sign/enc parts
            result.addAll(this.getParts(true, isSignBody, parts, found));
        }

        if (signedElements != null) {
            // Handle SignedElements
            result.addAll(
                this.getElements("Element", signedElements.getXPaths(), found, true)
            );
        }

        return result;
    }


}
