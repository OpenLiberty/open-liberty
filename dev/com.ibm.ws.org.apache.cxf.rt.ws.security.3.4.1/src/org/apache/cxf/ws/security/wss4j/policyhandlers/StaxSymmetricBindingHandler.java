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
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.wss4j.TokenStoreCallbackHandler;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.OutboundSecurityContext;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.stax.securityEvent.AbstractSecuredElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;

/**
 *
 */
public class StaxSymmetricBindingHandler extends AbstractStaxBindingHandler {

    private SymmetricBinding sbinding;
    private SoapMessage message;

    public StaxSymmetricBindingHandler(
        WSSSecurityProperties properties,
        SoapMessage msg,
        SymmetricBinding sbinding,
        OutboundSecurityContext outboundSecurityContext
    ) {
        super(properties, msg, sbinding, outboundSecurityContext);
        this.message = msg;
        this.sbinding = sbinding;
    }

    private AbstractTokenWrapper getSignatureToken() {
        if (sbinding.getProtectionToken() != null) {
            return sbinding.getProtectionToken();
        }
        return sbinding.getSignatureToken();
    }

    private AbstractTokenWrapper getEncryptionToken() {
        if (sbinding.getProtectionToken() != null) {
            return sbinding.getProtectionToken();
        }
        return sbinding.getEncryptionToken();
    }

    public void handleBinding() {
        AssertionInfoMap aim = getMessage().get(AssertionInfoMap.class);
        configureTimestamp(aim);
        assertPolicy(sbinding.getName());

        String asymSignatureAlgorithm =
            (String)getMessage().getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
        if (asymSignatureAlgorithm != null && sbinding.getAlgorithmSuite() != null) {
            sbinding.getAlgorithmSuite().setAsymmetricSignature(asymSignatureAlgorithm);
        }
        String symSignatureAlgorithm =
            (String)getMessage().getContextualProperty(SecurityConstants.SYMMETRIC_SIGNATURE_ALGORITHM);
        if (symSignatureAlgorithm != null && sbinding.getAlgorithmSuite() != null) {
            sbinding.getAlgorithmSuite().setSymmetricSignature(symSignatureAlgorithm);
        }

        // Set up CallbackHandler which wraps the configured Handler
        WSSSecurityProperties properties = getProperties();
        TokenStoreCallbackHandler callbackHandler =
            new TokenStoreCallbackHandler(
                properties.getCallbackHandler(), TokenStoreUtils.getTokenStore(message)
            );
        properties.setCallbackHandler(callbackHandler);

        if (sbinding.getProtectionOrder()
            == AbstractSymmetricAsymmetricBinding.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
            assertPolicy(
                new QName(sbinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_BEFORE_SIGNING));
        } else {
            doSignBeforeEncrypt();
            assertPolicy(
                new QName(sbinding.getName().getNamespaceURI(), SPConstants.SIGN_BEFORE_ENCRYPTING));
        }

        if (!isRequestor()) {
            properties.setEncryptSymmetricEncryptionKey(false);
        }

        configureLayout(aim);
        assertAlgorithmSuite(sbinding.getAlgorithmSuite());
        assertWSSProperties(sbinding.getName().getNamespaceURI());
        assertTrustProperties(sbinding.getName().getNamespaceURI());
        assertPolicy(
            new QName(sbinding.getName().getNamespaceURI(), SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));
        if (sbinding.isProtectTokens()) {
            assertPolicy(
                new QName(sbinding.getName().getNamespaceURI(), SPConstants.PROTECT_TOKENS));
        }
    }

    private void doEncryptBeforeSign() {
        try {
            AbstractTokenWrapper encryptionWrapper = getEncryptionToken();
            assertTokenWrapper(encryptionWrapper);
            AbstractToken encryptionToken = encryptionWrapper.getToken();

            String tokenId = null;
            SecurityToken tok = null;
            if (encryptionToken instanceof KerberosToken) {
                tok = getSecurityToken();
                if (MessageUtils.isRequestor(message)) {
                    addKerberosToken((KerberosToken)encryptionToken, false, true, true);
                }
            } else if (encryptionToken instanceof IssuedToken) {
                tok = getSecurityToken();
                addIssuedToken((IssuedToken)encryptionToken, tok, false, true);

                if (tok == null && !isRequestor()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken securityToken =
                        findInboundSecurityToken(WSSecurityEventConstants.SAML_TOKEN);
                    tokenId = WSS4JUtils.parseAndStoreStreamingSecurityToken(securityToken, message);
                }
            } else if (encryptionToken instanceof SecureConversationToken
                || encryptionToken instanceof SecurityContextToken
                || encryptionToken instanceof SpnegoContextToken) {
                tok = getSecurityToken();
                if (tok != null && isRequestor()) {
                    WSSSecurityProperties properties = getProperties();
                    WSSConstants.Action actionToPerform = WSSConstants.CUSTOM_TOKEN;
                    properties.addAction(actionToPerform);
                } else if (tok == null && !isRequestor()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken securityToken =
                        findInboundSecurityToken(WSSecurityEventConstants.SECURITY_CONTEXT_TOKEN);
                    tokenId = WSS4JUtils.parseAndStoreStreamingSecurityToken(securityToken, message);
                }
            } else if (encryptionToken instanceof X509Token) {
                if (isRequestor()) {
                    tokenId = setupEncryptedKey(encryptionWrapper, encryptionToken);
                } else {
                    org.apache.xml.security.stax.securityToken.SecurityToken securityToken =
                        findEncryptedKeyToken();
                    tokenId = WSS4JUtils.parseAndStoreStreamingSecurityToken(securityToken, message);
                }
            } else if (encryptionToken instanceof UsernameToken) {
                unassertPolicy(sbinding, "UsernameTokens not supported with Symmetric binding");
                return;
            }
            assertToken(encryptionToken);
            if (tok == null) {
                tokenId = XMLUtils.getIDFromReference(tokenId);

                // Get hold of the token from the token storage
                tok = TokenStoreUtils.getTokenStore(message).getToken(tokenId);
            }

            // Store key
            if (!(MessageUtils.isRequestor(message) && encryptionToken instanceof KerberosToken)) {
                storeSecurityToken(encryptionToken, tok);
            }

            List<SecurePart> encrParts = null;
            List<SecurePart> sigParts = null;
            try {
                encrParts = getEncryptedParts();
                //Signed parts are determined before encryption because encrypted signed headers
                //will not be included otherwise
                sigParts = getSignedParts();
            } catch (SOAPException ex) {
                throw new Fault(ex);
            }

            addSupportingTokens();

            if (encryptionToken != null && !encrParts.isEmpty()) {
                if (isRequestor()) {
                    encrParts.addAll(encryptedTokensList);
                }

                //Check for signature protection
                if (sbinding.isEncryptSignature()) {
                    SecurePart part =
                        new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                    encrParts.add(part);
                    if (signatureConfirmationAdded) {
                        part = new SecurePart(WSSConstants.TAG_WSSE11_SIG_CONF, Modifier.Element);
                        encrParts.add(part);
                    }
                    assertPolicy(
                        new QName(sbinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));
                }

                doEncryption(encryptionWrapper, encrParts, true);
            }

            if (timestampAdded) {
                SecurePart part =
                    new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                sigParts.add(part);
            }
            sigParts.addAll(this.getSignedParts());

            if (!sigParts.isEmpty()) {
                AbstractTokenWrapper sigAbstractTokenWrapper = getSignatureToken();
                if (sigAbstractTokenWrapper != null) {
                    AbstractToken sigToken = sigAbstractTokenWrapper.getToken();
                    if (isRequestor()) {
                        doSignature(sigAbstractTokenWrapper, sigToken, tok, sigParts);
                    } else {
                        addSignatureConfirmation(sigParts);
                        doSignature(sigAbstractTokenWrapper, sigToken, tok, sigParts);
                    }
                }
            }

            removeSignatureIfSignedSAML();
            enforceEncryptBeforeSigningWithSignedSAML();
            prependSignatureToSC();
            putCustomTokenAfterSignature();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new Fault(ex);
        }
    }

    private void doSignBeforeEncrypt() {
        AbstractTokenWrapper sigAbstractTokenWrapper = getSignatureToken();
        assertTokenWrapper(sigAbstractTokenWrapper);
        AbstractToken sigToken = sigAbstractTokenWrapper.getToken();
        String sigTokId = null;

        try {
            SecurityToken sigTok = null;
            if (sigToken != null) {
                if (sigToken instanceof KerberosToken) {
                    sigTok = getSecurityToken();
                    if (isRequestor()) {
                        addKerberosToken((KerberosToken)sigToken, false, true, true);
                    }
                } else if (sigToken instanceof IssuedToken) {
                    sigTok = getSecurityToken();
                    addIssuedToken((IssuedToken)sigToken, sigTok, false, true);

                    if (sigTok == null && !isRequestor()) {
                        org.apache.xml.security.stax.securityToken.SecurityToken securityToken =
                            findInboundSecurityToken(WSSecurityEventConstants.SAML_TOKEN);
                        sigTokId = WSS4JUtils.parseAndStoreStreamingSecurityToken(securityToken, message);
                    }
                } else if (sigToken instanceof SecureConversationToken
                    || sigToken instanceof SecurityContextToken
                    || sigToken instanceof SpnegoContextToken) {
                    sigTok = getSecurityToken();
                    if (sigTok != null && isRequestor()) {
                        WSSSecurityProperties properties = getProperties();
                        WSSConstants.Action actionToPerform = WSSConstants.CUSTOM_TOKEN;
                        properties.addAction(actionToPerform);
                    } else if (sigTok == null && !isRequestor()) {
                        org.apache.xml.security.stax.securityToken.SecurityToken securityToken =
                            findInboundSecurityToken(WSSecurityEventConstants.SECURITY_CONTEXT_TOKEN);
                        sigTokId = WSS4JUtils.parseAndStoreStreamingSecurityToken(securityToken, message);
                    }
                } else if (sigToken instanceof X509Token) {
                    if (isRequestor()) {
                        sigTokId = setupEncryptedKey(sigAbstractTokenWrapper, sigToken);
                    } else {
                        org.apache.xml.security.stax.securityToken.SecurityToken securityToken =
                            findEncryptedKeyToken();
                        sigTokId = WSS4JUtils.parseAndStoreStreamingSecurityToken(securityToken, message);
                    }
                } else if (sigToken instanceof UsernameToken) {
                    unassertPolicy(sbinding, "UsernameTokens not supported with Symmetric binding");
                    return;
                }
                assertToken(sigToken);
            } else {
                unassertPolicy(sbinding, "No signature token");
                return;
            }

            if (sigTok == null && StringUtils.isEmpty(sigTokId)) {
                unassertPolicy(sigAbstractTokenWrapper, "No signature token id");
                return;
            }
            if (sigTok == null) {
                sigTok = TokenStoreUtils.getTokenStore(message).getToken(sigTokId);
            }

            // Store key
            if (!(MessageUtils.isRequestor(message) && sigToken instanceof KerberosToken)) {
                storeSecurityToken(sigToken, sigTok);
            }

            // Add timestamp
            List<SecurePart> sigs = new ArrayList<>();
            if (timestampAdded) {
                SecurePart part =
                    new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                sigs.add(part);
            }
            sigs.addAll(this.getSignedParts());

            if (!isRequestor()) {
                addSignatureConfirmation(sigs);
            }

            if (!sigs.isEmpty()) {
                doSignature(sigAbstractTokenWrapper, sigToken, sigTok, sigs);
            }

            addSupportingTokens();
            removeSignatureIfSignedSAML();
            prependSignatureToSC();

            //Encryption
            List<SecurePart> enc = getEncryptedParts();

            //Check for signature protection
            if (sbinding.isEncryptSignature()) {
                SecurePart part =
                    new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                enc.add(part);
                if (signatureConfirmationAdded) {
                    part = new SecurePart(WSSConstants.TAG_WSSE11_SIG_CONF, Modifier.Element);
                    enc.add(part);
                }
                assertPolicy(
                    new QName(sbinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));
            }

            //Do encryption
            if (isRequestor()) {
                enc.addAll(encryptedTokensList);
            }
            AbstractTokenWrapper encrAbstractTokenWrapper = getEncryptionToken();
            doEncryption(encrAbstractTokenWrapper, enc, false);

            putCustomTokenAfterSignature();
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    private void doEncryption(AbstractTokenWrapper recToken,
                              List<SecurePart> encrParts,
                              boolean externalRef) throws SOAPException {
        //Do encryption
        if (recToken != null && recToken.getToken() != null) {
            AbstractToken encrToken = recToken.getToken();
            AlgorithmSuite algorithmSuite = sbinding.getAlgorithmSuite();

            // Action
            WSSSecurityProperties properties = getProperties();
            WSSConstants.Action actionToPerform = WSSConstants.ENCRYPT;
            if (recToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                actionToPerform = WSSConstants.ENCRYPT_WITH_DERIVED_KEY;
                if (MessageUtils.isRequestor(message) && recToken.getToken() instanceof X509Token) {
                    properties.setDerivedKeyTokenReference(
                        WSSConstants.DerivedKeyTokenReference.EncryptedKey);
                } else {
                    properties.setDerivedKeyTokenReference(
                        WSSConstants.DerivedKeyTokenReference.DirectReference);
                }
                AlgorithmSuiteType algSuiteType = sbinding.getAlgorithmSuite().getAlgorithmSuiteType();
                properties.setDerivedEncryptionKeyLength(
                           algSuiteType.getEncryptionDerivedKeyLength() / 8);
            }

            if (recToken.getVersion() == SPConstants.SPVersion.SP12) {
                properties.setUse200512Namespace(true);
            }

            properties.getEncryptionSecureParts().addAll(encrParts);
            properties.addAction(actionToPerform);

            if (isRequestor()) {
                properties.setEncryptionKeyIdentifier(getKeyIdentifierType(encrToken));
                properties.setDerivedKeyKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
            } else if (recToken.getToken() instanceof KerberosToken && !isRequestor()) {
                properties.setEncryptionKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_KERBEROS_SHA1_IDENTIFIER);
                properties.setDerivedKeyKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_KERBEROS_SHA1_IDENTIFIER);
                if (recToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                    properties.setEncryptionKeyIdentifier(
                        WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
                }
            } else if ((recToken.getToken() instanceof IssuedToken
                || recToken.getToken() instanceof SecureConversationToken
                || recToken.getToken() instanceof SpnegoContextToken) && !isRequestor()) {
                properties.setEncryptionKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
            } else {
                properties.setEncryptionKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER);
                if (recToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                    properties.setDerivedKeyKeyIdentifier(
                        WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER);
                    properties.setEncryptionKeyIdentifier(
                        WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
                    properties.setEncryptSymmetricEncryptionKey(false);
                }
            }

            // Find out do we also need to include the token as per the Inclusion requirement
            WSSecurityTokenConstants.KeyIdentifier keyIdentifier = properties.getEncryptionKeyIdentifier();
            if (encrToken instanceof X509Token
                && isTokenRequired(encrToken.getIncludeTokenType())
                && (WSSecurityTokenConstants.KeyIdentifier_IssuerSerial.equals(keyIdentifier)
                    || WSSecurityTokenConstants.KEYIDENTIFIER_THUMBPRINT_IDENTIFIER.equals(keyIdentifier)
                    || WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE.equals(
                        keyIdentifier))) {
                properties.setIncludeEncryptionToken(true);
            } else {
                properties.setIncludeEncryptionToken(false);
            }

            properties.setEncryptionKeyTransportAlgorithm(
                       algorithmSuite.getAlgorithmSuiteType().getAsymmetricKeyWrap());
            properties.setEncryptionSymAlgorithm(
                       algorithmSuite.getAlgorithmSuiteType().getEncryption());
            properties.setEncryptionKeyTransportDigestAlgorithm(
                       algorithmSuite.getAlgorithmSuiteType().getEncryptionDigest());
            properties.setEncryptionKeyTransportMGFAlgorithm(
                       algorithmSuite.getAlgorithmSuiteType().getMGFAlgo());

            String encUser =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.ENCRYPT_USERNAME, message);
            if (encUser == null) {
                encUser = (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.USERNAME, message);
            }
            if (encUser != null && properties.getEncryptionUser() == null) {
                properties.setEncryptionUser(encUser);
            }
            if (ConfigurationConstants.USE_REQ_SIG_CERT.equals(encUser)) {
                properties.setUseReqSigCertForEncryption(true);
            }

            if (encrToken instanceof KerberosToken || encrToken instanceof IssuedToken
                || encrToken instanceof SpnegoContextToken || encrToken instanceof SecurityContextToken
                || encrToken instanceof SecureConversationToken) {
                properties.setEncryptSymmetricEncryptionKey(false);
            }
        }
    }

    private void doSignature(AbstractTokenWrapper wrapper, AbstractToken policyToken,
                             SecurityToken tok, List<SecurePart> sigParts)
        throws WSSecurityException, SOAPException {

        // Action
        WSSSecurityProperties properties = getProperties();
        WSSConstants.Action actionToPerform = WSSConstants.SIGNATURE;
        if (wrapper.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            actionToPerform = WSSConstants.SIGNATURE_WITH_DERIVED_KEY;
            if (MessageUtils.isRequestor(message) && policyToken instanceof X509Token) {
                properties.setDerivedKeyTokenReference(
                    WSSConstants.DerivedKeyTokenReference.EncryptedKey);
            } else {
                properties.setDerivedKeyTokenReference(
                    WSSConstants.DerivedKeyTokenReference.DirectReference);
            }
            AlgorithmSuiteType algSuiteType = sbinding.getAlgorithmSuite().getAlgorithmSuiteType();
            properties.setDerivedSignatureKeyLength(
                       algSuiteType.getSignatureDerivedKeyLength() / 8);
        }

        if (policyToken.getVersion() == SPConstants.SPVersion.SP12) {
            properties.setUse200512Namespace(true);
        }

        List<WSSConstants.Action> actionList = properties.getActions();
        // Add a Signature directly before Kerberos, otherwise just append it
        boolean actionAdded = false;
        for (int i = 0; i < actionList.size(); i++) {
            WSSConstants.Action action = actionList.get(i);
            if (action.equals(WSSConstants.KERBEROS_TOKEN)) {
                actionList.add(i, actionToPerform);
                actionAdded = true;
                break;
            }
        }
        if (!actionAdded) {
            actionList.add(actionToPerform);
        }

        properties.getSignatureSecureParts().addAll(sigParts);

        AbstractToken sigToken = wrapper.getToken();
        if (sbinding.isProtectTokens() && sigToken instanceof X509Token && isRequestor()) {
            SecurePart securePart =
                new SecurePart(new QName(WSSConstants.NS_XMLENC, "EncryptedKey"), Modifier.Element);
            properties.addSignaturePart(securePart);
        }

        configureSignature(sigToken, false);

        if (policyToken instanceof X509Token) {
            properties.setIncludeSignatureToken(false);
            if (isRequestor()) {
                properties.setSignatureKeyIdentifier(
                    WSSecurityTokenConstants.KeyIdentifier_EncryptedKey);
            } else {
                properties.setSignatureKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER);
                if (wrapper.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                    properties.setDerivedKeyKeyIdentifier(
                        WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER);
                    properties.setSignatureKeyIdentifier(
                        WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
                }
            }
        } else if (policyToken instanceof KerberosToken) {
            if (isRequestor()) {
                properties.setDerivedKeyKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
            } else {
                if (wrapper.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                    properties.setSignatureKeyIdentifier(
                        WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
                } else {
                    properties.setSignatureKeyIdentifier(
                        WSSecurityTokenConstants.KEYIDENTIFIER_KERBEROS_SHA1_IDENTIFIER);
                }
                properties.setDerivedKeyKeyIdentifier(
                    WSSecurityTokenConstants.KEYIDENTIFIER_KERBEROS_SHA1_IDENTIFIER);
            }
        } else if (policyToken instanceof IssuedToken || policyToken instanceof SecurityContextToken
            || policyToken instanceof SecureConversationToken || policyToken instanceof SpnegoContextToken) {
            if (!isRequestor()) {
                properties.setIncludeSignatureToken(false);
            } else {
                properties.setIncludeSignatureToken(true);
            }
            properties.setDerivedKeyKeyIdentifier(
                WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
        }

        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            properties.setSignatureAlgorithm(
                   sbinding.getAlgorithmSuite().getSymmetricSignature());
        }
    }

    private String setupEncryptedKey(AbstractTokenWrapper wrapper, AbstractToken sigToken) throws WSSecurityException {

        Instant created = Instant.now();
        Instant expires = created.plusSeconds(WSS4JUtils.getSecurityTokenLifetime(message) / 1000L);
        SecurityToken tempTok =
            new SecurityToken(IDGenerator.generateID(null), created, expires);

        KeyGenerator keyGenerator =
            KeyUtils.getKeyGenerator(sbinding.getAlgorithmSuite().getAlgorithmSuiteType().getEncryption());
        SecretKey symmetricKey = keyGenerator.generateKey();
        tempTok.setKey(symmetricKey);
        tempTok.setSecret(symmetricKey.getEncoded());

        TokenStoreUtils.getTokenStore(message).add(tempTok);

        return tempTok.getId();
    }

    private org.apache.xml.security.stax.securityToken.SecurityToken findEncryptedKeyToken()
        throws XMLSecurityException {
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingEventList =
            (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".in");
        if (incomingEventList != null) {
            for (SecurityEvent incomingEvent : incomingEventList) {
                if (WSSecurityEventConstants.ENCRYPTED_PART == incomingEvent.getSecurityEventType()
                    || WSSecurityEventConstants.EncryptedElement
                        == incomingEvent.getSecurityEventType()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken token =
                        ((AbstractSecuredElementSecurityEvent)incomingEvent).getSecurityToken();
                    if (token != null && token.getKeyWrappingToken() != null
                        && token.getKeyWrappingToken().getSecretKey() != null
                        && token.getKeyWrappingToken().getSha1Identifier() != null) {
                        return token.getKeyWrappingToken();
                    } else if (token != null && token.getSecretKey() != null
                        && token.getSha1Identifier() != null) {
                        return token;
                    }
                }
            }

            // Fall back to a Signature in case there was no encrypted Element in the request
            for (SecurityEvent incomingEvent : incomingEventList) {
                if (WSSecurityEventConstants.SIGNED_PART == incomingEvent.getSecurityEventType()
                    || WSSecurityEventConstants.SignedElement
                        == incomingEvent.getSecurityEventType()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken token =
                        ((AbstractSecuredElementSecurityEvent)incomingEvent).getSecurityToken();
                    if (token != null && token.getKeyWrappingToken() != null
                        && token.getKeyWrappingToken().getSecretKey() != null
                        && token.getKeyWrappingToken().getSha1Identifier() != null) {
                        return token.getKeyWrappingToken();
                    } else if (token != null && token.getSecretKey() != null
                        && token.getSha1Identifier() != null) {
                        return token;
                    }
                }
            }
        }
        return null;
    }

}
