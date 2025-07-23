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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.wss4j.TokenStoreCallbackHandler;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.stax.ext.OutboundSecurityContext;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;

/**
 *
 */
public class StaxAsymmetricBindingHandler extends AbstractStaxBindingHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxAsymmetricBindingHandler.class);

    private AsymmetricBinding abinding;
    private SoapMessage message;

    public StaxAsymmetricBindingHandler(
        WSSSecurityProperties properties,
        SoapMessage msg,
        AsymmetricBinding abinding,
        OutboundSecurityContext outboundSecurityContext
    ) {
        super(properties, msg, abinding, outboundSecurityContext);
        this.message = msg;
        this.abinding = abinding;
    }

    public void handleBinding() {
        AssertionInfoMap aim = getMessage().get(AssertionInfoMap.class);
        configureTimestamp(aim);
        assertPolicy(abinding.getName());

        String asymSignatureAlgorithm =
            (String)getMessage().getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
        if (asymSignatureAlgorithm != null && abinding.getAlgorithmSuite() != null) {
            abinding.getAlgorithmSuite().setAsymmetricSignature(asymSignatureAlgorithm);
        }
        String symSignatureAlgorithm =
            (String)getMessage().getContextualProperty(SecurityConstants.SYMMETRIC_SIGNATURE_ALGORITHM);
        if (symSignatureAlgorithm != null && abinding.getAlgorithmSuite() != null) {
            abinding.getAlgorithmSuite().setSymmetricSignature(symSignatureAlgorithm);
        }

        if (abinding.getProtectionOrder()
            == AbstractSymmetricAsymmetricBinding.ProtectionOrder.EncryptBeforeSigning) {
            doEncryptBeforeSign();
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_BEFORE_SIGNING));
        } else {
            doSignBeforeEncrypt();
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.SIGN_BEFORE_ENCRYPTING));
        }

        configureLayout(aim);
        assertAlgorithmSuite(abinding.getAlgorithmSuite());
        assertWSSProperties(abinding.getName().getNamespaceURI());
        assertTrustProperties(abinding.getName().getNamespaceURI());
        assertPolicy(
            new QName(abinding.getName().getNamespaceURI(), SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));
        if (abinding.isProtectTokens()) {
            assertPolicy(
                new QName(abinding.getName().getNamespaceURI(), SPConstants.PROTECT_TOKENS));
        }
    }

    private void doSignBeforeEncrypt() {
        try {
            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }
            if (initiatorWrapper != null) {
                assertTokenWrapper(initiatorWrapper);
                AbstractToken initiatorToken = initiatorWrapper.getToken();
                if (initiatorToken instanceof IssuedToken) {
                    SecurityToken sigTok = getSecurityToken();
                    addIssuedToken((IssuedToken)initiatorToken, sigTok, false, true);

                    if (sigTok != null) {
                        storeSecurityToken(initiatorToken, sigTok);
                        outboundSecurityContext.remove(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION);
                    }

                    // Set up CallbackHandler which wraps the configured Handler
                    WSSSecurityProperties properties = getProperties();
                    TokenStoreCallbackHandler callbackHandler =
                        new TokenStoreCallbackHandler(
                            properties.getCallbackHandler(), TokenStoreUtils.getTokenStore(message)
                        );
                    properties.setCallbackHandler(callbackHandler);
                } else if (initiatorToken instanceof SamlToken) {
                    addSamlToken((SamlToken)initiatorToken, false, true);
                }
                assertToken(initiatorToken);
            }

            // Add timestamp
            List<SecurePart> sigs = new ArrayList<>();
            if (timestampAdded) {
                SecurePart part =
                    new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                sigs.add(part);
            }
            sigs.addAll(this.getSignedParts());

            if (isRequestor() && initiatorWrapper != null) {
                doSignature(initiatorWrapper, sigs);
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
                }
                if (recipientSignatureToken != null && !sigs.isEmpty()) {
                    doSignature(recipientSignatureToken, sigs);
                }
            }

            addSupportingTokens();
            removeSignatureIfSignedSAML();
            prependSignatureToSC();

            List<SecurePart> enc = getEncryptedParts();

            //Check for signature protection
            if (abinding.isEncryptSignature()) {
                SecurePart part =
                    new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                enc.add(part);
                if (signatureConfirmationAdded) {
                    SecurePart securePart =
                        new SecurePart(WSSConstants.TAG_WSSE11_SIG_CONF, Modifier.Element);
                    enc.add(securePart);
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
                assertTokenWrapper(encToken);
                assertToken(encToken.getToken());
            }
            doEncryption(encToken, enc, false);

            putCustomTokenAfterSignature();
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Sign before encryption failed due to : " + reason);
            throw new Fault(e);
        }
    }

    private void doEncryptBeforeSign() {
        try {
            AbstractTokenWrapper wrapper;
            AbstractToken encryptionToken = null;
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
            if (wrapper != null) {
                encryptionToken = wrapper.getToken();
                assertToken(encryptionToken);
            }

            AbstractTokenWrapper initiatorWrapper = abinding.getInitiatorSignatureToken();
            if (initiatorWrapper == null) {
                initiatorWrapper = abinding.getInitiatorToken();
            }

            if (initiatorWrapper != null) {
                assertTokenWrapper(initiatorWrapper);
                AbstractToken initiatorToken = initiatorWrapper.getToken();
                if (initiatorToken instanceof IssuedToken) {
                    SecurityToken sigTok = getSecurityToken();
                    addIssuedToken((IssuedToken)initiatorToken, sigTok, false, true);

                    if (sigTok != null) {
                        storeSecurityToken(initiatorToken, sigTok);
                        outboundSecurityContext.remove(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION);
                    }

                    // Set up CallbackHandler which wraps the configured Handler
                    WSSSecurityProperties properties = getProperties();
                    TokenStoreCallbackHandler callbackHandler =
                        new TokenStoreCallbackHandler(
                            properties.getCallbackHandler(), TokenStoreUtils.getTokenStore(message)
                        );
                    properties.setCallbackHandler(callbackHandler);
                } else if (initiatorToken instanceof SamlToken) {
                    addSamlToken((SamlToken)initiatorToken, false, true);
                }
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
                } else {
                    addSignatureConfirmation(sigParts);
                }

                //Check for signature protection
                if (abinding.isEncryptSignature()) {
                    SecurePart part =
                        new SecurePart(new QName(WSSConstants.NS_DSIG, "Signature"), Modifier.Element);
                    encrParts.add(part);
                    if (signatureConfirmationAdded) {
                        SecurePart securePart =
                            new SecurePart(WSSConstants.TAG_WSSE11_SIG_CONF, Modifier.Element);
                        encrParts.add(securePart);
                    }
                    assertPolicy(
                        new QName(abinding.getName().getNamespaceURI(), SPConstants.ENCRYPT_SIGNATURE));
                }

                doEncryption(wrapper, encrParts, true);
            }

            if (timestampAdded) {
                SecurePart part =
                    new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
                sigParts.add(part);
            }

            if (!sigParts.isEmpty()) {
                if (initiatorWrapper != null && isRequestor()) {
                    doSignature(initiatorWrapper, sigParts);
                } else if (!isRequestor()) {
                    AbstractTokenWrapper recipientSignatureToken = abinding.getRecipientSignatureToken();
                    if (recipientSignatureToken == null) {
                        recipientSignatureToken = abinding.getRecipientToken();
                    }
                    if (recipientSignatureToken != null) {
                        assertTokenWrapper(recipientSignatureToken);
                        assertToken(recipientSignatureToken.getToken());
                        doSignature(recipientSignatureToken, sigParts);
                    }
                }
            }

            removeSignatureIfSignedSAML();
            enforceEncryptBeforeSigningWithSignedSAML();
            prependSignatureToSC();
            putCustomTokenAfterSignature();
        } catch (Exception e) {
            String reason = e.getMessage();
            LOG.log(Level.WARNING, "Encrypt before signing failed due to : " + reason);
            throw new Fault(e);
        }
    }

    private void doEncryption(AbstractTokenWrapper recToken,
                                    List<SecurePart> encrParts,
                                    boolean externalRef) throws SOAPException {
        //Do encryption
        if (recToken != null && recToken.getToken() != null && !encrParts.isEmpty()) {
            AbstractToken encrToken = recToken.getToken();
            AlgorithmSuite algorithmSuite = abinding.getAlgorithmSuite();

            // Action
            WSSSecurityProperties properties = getProperties();
            WSSConstants.Action actionToPerform = WSSConstants.ENCRYPT;
            if (recToken.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
                actionToPerform = WSSConstants.ENCRYPT_WITH_DERIVED_KEY;
            }
            properties.addAction(actionToPerform);

            properties.getEncryptionSecureParts().addAll(encrParts);
            properties.setEncryptionKeyIdentifier(getKeyIdentifierType(encrToken));

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

            //
            // Using a stored cert is only suitable for the Issued Token case, where
            // we're extracting the cert from a SAML Assertion on the provider side
            //
            if (!isRequestor() && recToken.getToken() instanceof IssuedToken) {
                properties.setUseReqSigCertForEncryption(true);
            }
        }
    }

    private void doSignature(AbstractTokenWrapper wrapper, List<SecurePart> sigParts)
        throws WSSecurityException, SOAPException {

        // Action
        WSSSecurityProperties properties = getProperties();
        WSSConstants.Action actionToPerform = WSSConstants.SIGNATURE;
        if (wrapper.getToken().getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            actionToPerform = WSSConstants.SIGNATURE_WITH_DERIVED_KEY;
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
        configureSignature(sigToken, false);

        if (abinding.isProtectTokens() && (sigToken instanceof X509Token)
            && sigToken.getIncludeTokenType() != IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            SecurePart securePart =
                new SecurePart(new QName(WSSConstants.NS_WSSE10, "BinarySecurityToken"), Modifier.Element);
            properties.addSignaturePart(securePart);
        } else if (sigToken instanceof IssuedToken || sigToken instanceof SecurityContextToken
            || sigToken instanceof SecureConversationToken || sigToken instanceof SpnegoContextToken
            || sigToken instanceof SamlToken) {
            properties.setIncludeSignatureToken(false);
        }

        if (sigToken.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            properties.setSignatureAlgorithm(
                   abinding.getAlgorithmSuite().getSymmetricSignature());
        }
    }

}
