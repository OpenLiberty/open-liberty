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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.xml.crypto.dsig.Reference;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.IssuedToken;
import org.apache.cxf.ws.security.policy.model.KerberosToken;
import org.apache.cxf.ws.security.policy.model.KeyValueToken;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.SecurityContextToken;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedElements;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.TokenWrapper;
import org.apache.cxf.ws.security.policy.model.TransportBinding;
import org.apache.cxf.ws.security.policy.model.TransportToken;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.message.WSSecDKSign;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.saml.ext.AssertionWrapper;

/**
 * 
 */
public class TransportBindingHandler extends AbstractBindingBuilder {
    TransportBinding tbinding;
    
    public TransportBindingHandler(WSSConfig config,
                                   TransportBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) {
        super(config, binding, saaj, secHeader, aim, message);
        this.tbinding = binding;
    }
    
    private void addSignedSupportingTokens(SupportingToken sgndSuppTokens) 
        throws Exception {
        for (Token token : sgndSuppTokens.getTokens()) {
            if (token instanceof UsernameToken) {
                WSSecUsernameToken utBuilder = addUsernameToken((UsernameToken)token);
                if (utBuilder != null) {
                    utBuilder.prepare(saaj.getSOAPPart());
                    utBuilder.appendToHeader(secHeader);
                }
            } else if (token instanceof IssuedToken || token instanceof KerberosToken) {
                SecurityToken secTok = getSecurityToken();
                
                if (includeToken(token.getInclusion())) {
                    //Add the token
                    addEncryptedKeyElement(cloneElement(secTok.getToken()));
                }
            } else if (token instanceof SamlToken) {
                AssertionWrapper assertionWrapper = addSamlToken((SamlToken)token);
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
    
    private static void addSig(List<byte[]> signatureValues, byte[] val) {
        if (val != null) {
            signatureValues.add(val);
        }
    }
    
    public void handleBinding() {
        WSSecTimestamp timestamp = createTimestamp();
        handleLayout(timestamp);
        
        try {
            if (this.isRequestor()) {
                TransportToken transportTokenWrapper = tbinding.getTransportToken();
                if (transportTokenWrapper != null) {
                    Token transportToken = transportTokenWrapper.getToken();
                    if (transportToken instanceof IssuedToken) {
                        SecurityToken secToken = getSecurityToken();
                        if (secToken == null) {
                            policyNotAsserted(transportToken, "No transport token id");
                            return;
                        } else {
                            policyAsserted(transportToken);
                        }
                        if (includeToken(transportToken.getInclusion())) {
                            Element el = secToken.getToken();
                            addEncryptedKeyElement(cloneElement(el));
                        } 
                    }
                    
                    handleNonEndorsingSupportingTokens();
                    handleEndorsingSupportingTokens();
                }
            } else {
                addSignatureConfirmation(null);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            throw new Fault(e);
        }
    }
    
    /**
     * Handle the non-endorsing supporting tokens
     */
    private void handleNonEndorsingSupportingTokens() throws Exception {
        Collection<AssertionInfo> ais;
        
        ais = aim.get(SP12Constants.SIGNED_SUPPORTING_TOKENS);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                SupportingToken sgndSuppTokens = (SupportingToken)ai.getAssertion();
                if (sgndSuppTokens != null) {
                    addSignedSupportingTokens(sgndSuppTokens);
                }
                ai.setAsserted(true);
            }
        }
        
        ais = aim.get(SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                SupportingToken sgndSuppTokens = (SupportingToken)ai.getAssertion();
                if (sgndSuppTokens != null) {
                    addSignedSupportingTokens(sgndSuppTokens);
                }
                ai.setAsserted(true);
            }
        }
        
        ais = aim.get(SP12Constants.ENCRYPTED_SUPPORTING_TOKENS);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                SupportingToken encrSuppTokens = (SupportingToken)ai.getAssertion();
                if (encrSuppTokens != null) {
                    addSignedSupportingTokens(encrSuppTokens);
                }
                ai.setAsserted(true);
            }
        }
        
        ais = aim.get(SP12Constants.SUPPORTING_TOKENS);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                SupportingToken suppTokens = (SupportingToken)ai.getAssertion();
                if (suppTokens != null && suppTokens.getTokens() != null 
                    && suppTokens.getTokens().size() > 0) {
                    handleSupportingTokens(suppTokens, false);
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
        List<byte[]> signatureValues = new ArrayList<byte[]>();
        
        ais = aim.get(SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        if (ais != null) {
            SupportingToken sgndSuppTokens = null;
            for (AssertionInfo ai : ais) {
                sgndSuppTokens = (SupportingToken)ai.getAssertion();
                ai.setAsserted(true);
            }
            if (sgndSuppTokens != null) {
                for (Token token : sgndSuppTokens.getTokens()) {
                    handleEndorsingToken(token, sgndSuppTokens, signatureValues);
                }
            }
        }
        
        ais = aim.get(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
        if (ais != null) {
            SupportingToken endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingToken)ai.getAssertion();
                ai.setAsserted(true);
            } 
            
            if (endSuppTokens != null) {
                for (Token token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens, signatureValues);
                }
            }
        }
        ais = aim.get(SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (ais != null) {
            SupportingToken endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingToken)ai.getAssertion();
                ai.setAsserted(true);
            } 
            
            if (endSuppTokens != null) {
                for (Token token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens, signatureValues);
                }
            }
        }
        ais = aim.get(SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (ais != null) {
            SupportingToken endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingToken)ai.getAssertion();
                ai.setAsserted(true);
            } 
            
            if (endSuppTokens != null) {
                for (Token token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens, signatureValues);
                }
            }
        }
    }
    
    private void handleEndorsingToken(
        Token token, SupportingToken wrapper, List<byte[]> signatureValues
    ) throws Exception {
        if (token instanceof IssuedToken
            || token instanceof SecureConversationToken
            || token instanceof SecurityContextToken
            || token instanceof KerberosToken) {
            addSig(
                signatureValues, 
                doIssuedTokenSignature(token, wrapper)
            );
        } else if (token instanceof X509Token
            || token instanceof KeyValueToken) {
            addSig(
                signatureValues, 
                doX509TokenSignature(token, wrapper)
            );
        } else if (token instanceof SamlToken) {
            AssertionWrapper assertionWrapper = addSamlToken((SamlToken)token);

            Element envelope = saaj.getSOAPPart().getEnvelope();
            envelope = (Element)DOMUtils.getDomElement(envelope);
            assertionWrapper.toDOM(envelope.getOwnerDocument());
            storeAssertionAsSecurityToken(assertionWrapper);
            addSig(
                signatureValues, 
                doIssuedTokenSignature(token, wrapper)
            );
        } else if (token instanceof UsernameToken) {
            // Create a UsernameToken object for derived keys and store the security token
            WSSecUsernameToken usernameToken = addDKUsernameToken((UsernameToken)token, true);
            String id = usernameToken.getId();
            byte[] secret = usernameToken.getDerivedKey();

            Date created = new Date();
            Date expires = new Date();
            expires.setTime(created.getTime() + 300000);
            SecurityToken tempTok = 
                new SecurityToken(id, usernameToken.getUsernameTokenElement(), created, expires);
            tempTok.setSecret(secret);
            getTokenStore().add(tempTok);
            message.setContextualProperty(SecurityConstants.TOKEN_ID, tempTok.getId());
            
            addSig(
                signatureValues, 
                doIssuedTokenSignature(token, wrapper)
            );
        }
    }
    

    private byte[] doX509TokenSignature(Token token, SupportingToken wrapper) 
        throws Exception {
        
        Document doc = saaj.getSOAPPart();
        
        List<WSEncryptionPart> sigParts = 
            signPartsAndElements(wrapper.getSignedParts(), wrapper.getSignedElements());
        
        if (token.isDerivedKeys()) {
            WSSecEncryptedKey encrKey = getEncryptedKeyBuilder(wrapper, token);
            
            Element bstElem = encrKey.getBinarySecurityTokenElement();
            if (bstElem != null) {
                addTopDownElement(bstElem);
            }
            encrKey.appendToHeader(secHeader);
            
            WSSecDKSign dkSig = new WSSecDKSign(wssConfig);
            
            dkSig.setSigCanonicalization(binding.getAlgorithmSuite().getInclusiveC14n());
            dkSig.setSignatureAlgorithm(binding.getAlgorithmSuite().getSymmetricSignature());
            dkSig.setDerivedKeyLength(binding.getAlgorithmSuite().getSignatureDerivedKeyLength() / 8);
            
            dkSig.setExternalKey(encrKey.getEphemeralKey(), encrKey.getId());
            
            dkSig.prepare(doc, secHeader);
            
            dkSig.setParts(sigParts);
            List<Reference> referenceList = dkSig.addReferencesToSign(sigParts, secHeader);
            
            //Do signature
            dkSig.appendDKElementToHeader(secHeader);
            dkSig.computeSignature(referenceList, false, null);
            
            return dkSig.getSignatureValue();
        } else {
            WSSecSignature sig = getSignatureBuilder(wrapper, token, false);
            if (sig != null) {
                sig.prependBSTElementToHeader(secHeader);
            
                List<Reference> referenceList = sig.addReferencesToSign(sigParts, secHeader);
                
                if (bottomUpElement == null) {
                    sig.computeSignature(referenceList, false, null);
                } else {
                    sig.computeSignature(referenceList, true, bottomUpElement);
                }
                bottomUpElement = sig.getSignatureElement();
                mainSigId = sig.getId();
            
                return sig.getSignatureValue();
            } else {
                return new byte[0];
            }
        }
    }

    private byte[] doIssuedTokenSignature(
        Token token, SupportingToken wrapper
    ) throws Exception {
        boolean tokenIncluded = false;
        // Get the issued token
        SecurityToken secTok = getSecurityToken();
        if (secTok == null) {
            LOG.fine("The retrieved SecurityToken was null");
            throw new WSSecurityException("The retrieved SecurityToken was null");
        }
        
        if (includeToken(token.getInclusion())) {
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
        
        if (token.isDerivedKeys()) {
            return doDerivedKeySignature(tokenIncluded, secTok, token, sigParts);
        } else {
            return doSignature(tokenIncluded, secTok, token, wrapper, sigParts);
        }
    }
    
    private byte[] doDerivedKeySignature(
        boolean tokenIncluded,
        SecurityToken secTok,
        Token token,
        List<WSEncryptionPart> sigParts
    ) throws Exception {
        //Do Signature with derived keys
        WSSecDKSign dkSign = new WSSecDKSign(wssConfig);
        AlgorithmSuite algorithmSuite = tbinding.getAlgorithmSuite();

        //Setting the AttachedReference or the UnattachedReference according to the flag
        Element ref;
        if (tokenIncluded) {
            ref = secTok.getAttachedReference();
        } else {
            ref = secTok.getUnattachedReference();
        }

        if (ref != null) {
            dkSign.setExternalKey(secTok.getSecret(), cloneElement(ref));
        } else {
            dkSign.setExternalKey(secTok.getSecret(), secTok.getId());
        }
        
        if (token instanceof UsernameToken) {
            dkSign.setCustomValueType(WSConstants.WSS_USERNAME_TOKEN_VALUE_TYPE);
        } 

        // Set the algo info
        dkSign.setSignatureAlgorithm(algorithmSuite.getSymmetricSignature());
        dkSign.setDerivedKeyLength(algorithmSuite.getSignatureDerivedKeyLength() / 8);
        if (token.getSPConstants() == SP12Constants.INSTANCE) {
            dkSign.setWscVersion(ConversationConstants.VERSION_05_12);
        }
        Document doc = saaj.getSOAPPart();
        dkSign.prepare(doc, secHeader);

        addDerivedKeyElement(dkSign.getdktElement());

        dkSign.setParts(sigParts);
        List<Reference> referenceList = dkSign.addReferencesToSign(sigParts, secHeader);

        //Do signature
        dkSign.computeSignature(referenceList, false, null);

        return dkSign.getSignatureValue();
    }
    
    private byte[] doSignature(
        boolean tokenIncluded,
        SecurityToken secTok,
        Token token,
        TokenWrapper wrapper,
        List<WSEncryptionPart> sigParts
    ) throws Exception {
        WSSecSignature sig = new WSSecSignature(wssConfig);
        
        //Setting the AttachedReference or the UnattachedReference according to the flag
        Element ref;
        if (tokenIncluded) {
            ref = secTok.getAttachedReference();
        } else {
            ref = secTok.getUnattachedReference();
        }
        
        if (ref != null) {
            SecurityTokenReference secRef = 
                new SecurityTokenReference(cloneElement(ref), false);
            sig.setSecurityTokenReference(secRef);
            sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
        } else if (token instanceof UsernameToken) {
            sig.setCustomTokenId(secTok.getId());
            sig.setCustomTokenValueType(WSConstants.WSS_USERNAME_TOKEN_VALUE_TYPE);
            int type = tokenIncluded ? WSConstants.CUSTOM_SYMM_SIGNING 
                    : WSConstants.CUSTOM_SYMM_SIGNING_DIRECT;
            sig.setKeyIdentifierType(type);
        } else if (secTok.getTokenType() == null) {
            sig.setCustomTokenValueType(WSConstants.WSS_SAML_KI_VALUE_TYPE);
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
            if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                || WSConstants.SAML_NS.equals(tokenType)) {
                sig.setCustomTokenValueType(WSConstants.WSS_SAML_KI_VALUE_TYPE);
                sig.setKeyIdentifierType(WSConstants.CUSTOM_KEY_IDENTIFIER);
            } else if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                || WSConstants.SAML2_NS.equals(tokenType)) {
                sig.setCustomTokenValueType(WSConstants.WSS_SAML2_KI_VALUE_TYPE);
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
                crypto = getSignatureCrypto(wrapper);
            }
            String uname = crypto.getX509Identifier(secTok.getX509Certificate());
            if (uname == null) {
                String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
                uname = (String)message.getContextualProperty(userNameKey);
            }
            String password = getPassword(uname, token, WSPasswordCallback.SIGNATURE);
            if (password == null) {
                password = "";
            }
            sig.setUserInfo(uname, password);
            sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getAsymmetricSignature());
        } else {
            crypto = getSignatureCrypto(wrapper);
            sig.setSecretKey(secTok.getSecret());
            sig.setSignatureAlgorithm(binding.getAlgorithmSuite().getSymmetricSignature());
        }
        sig.setSigCanonicalization(binding.getAlgorithmSuite().getInclusiveC14n());

        Document doc = saaj.getSOAPPart();
        sig.prepare(doc, crypto, secHeader);

        sig.setParts(sigParts);
        List<Reference> referenceList = sig.addReferencesToSign(sigParts, secHeader);

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
        SignedEncryptedParts signedParts,
        SignedEncryptedElements signedElements
    ) throws SOAPException {
        
        List<WSEncryptionPart> result = new ArrayList<WSEncryptionPart>();
        List<Element> found = new ArrayList<Element>();
        
        // Add timestamp
        if (timestampEl != null) {
            WSEncryptionPart timestampPart = 
                    new WSEncryptionPart("Timestamp", WSConstants.WSU_NS, "Element");
            String id = addWsuIdToElement(timestampEl.getElement());
            timestampPart.setId(id);
            timestampPart.setElement(timestampEl.getElement());
            
            found.add(timestampPart.getElement());
            result.add(timestampPart);
        }

        // Add SignedParts
        if (signedParts != null) {
            List<WSEncryptionPart> parts = new ArrayList<WSEncryptionPart>();
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
            try {
                result.addAll(
                    this.getElements(
                        "Element", signedElements.getXPathExpressions(), 
                        signedElements.getDeclaredNamespaces(), found
                    )
                );
            } catch (XPathExpressionException e) {
                LOG.log(Level.FINE, e.getMessage(), e);
                // REVISIT
            }
        }

        return result;
    }


}
