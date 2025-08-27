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
package org.apache.wss4j.stax.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSInboundSecurityContext;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityEvent.HttpsTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.impl.InboundSecurityContextImpl;
import org.apache.xml.security.stax.securityEvent.AlgorithmSuiteSecurityEvent;
import org.apache.xml.security.stax.securityEvent.ContentEncryptedElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.EncryptedElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SignedElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;

/**
 * Concrete security context implementation
 */
public class InboundWSSecurityContextImpl extends InboundSecurityContextImpl implements WSInboundSecurityContext {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(InboundWSSecurityContextImpl.class);

    private final Deque<SecurityEvent> securityEventQueue = new ArrayDeque<>();
    private boolean operationSecurityEventOccured = false;
    private boolean messageEncryptionTokenOccured = false;
    private boolean allowRSA15KeyTransportAlgorithm = false;
    private boolean disableBSPEnforcement;
    private boolean soap12;

    private List<BSPRule> ignoredBSPRules = Collections.emptyList();

    @Override
    public synchronized void registerSecurityEvent(SecurityEvent securityEvent) throws XMLSecurityException {

        if (WSSecurityEventConstants.AlgorithmSuite.equals(securityEvent.getSecurityEventType())) {
            //do not cache AlgorithmSuite securityEvents and forward them directly to allow
            //the user to check them before they are used internally.
            forwardSecurityEvent(securityEvent);
            return;
        }

        if (operationSecurityEventOccured) {
            if (!this.messageEncryptionTokenOccured
                    && securityEvent instanceof TokenSecurityEvent) {
                @SuppressWarnings("unchecked")
                TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent =
                        (TokenSecurityEvent<? extends InboundSecurityToken>) securityEvent;

                if (tokenSecurityEvent.getSecurityToken().getTokenUsages().contains(WSSecurityTokenConstants.TokenUsage_Encryption)) {
                    InboundSecurityToken securityToken = WSSUtils.getRootToken(tokenSecurityEvent.getSecurityToken());

                    TokenSecurityEvent<? extends InboundSecurityToken> newTokenSecurityEvent =
                            WSSUtils.createTokenSecurityEvent(securityToken, tokenSecurityEvent.getCorrelationID());
                    setTokenUsage(newTokenSecurityEvent, WSSecurityTokenConstants.TOKENUSAGE_MAIN_ENCRYPTION);
                    securityEvent = newTokenSecurityEvent;
                    this.messageEncryptionTokenOccured = true;
                }
            }

            forwardSecurityEvent(securityEvent);
            return;
        }

        if (WSSecurityEventConstants.OPERATION.equals(securityEvent.getSecurityEventType())) {
            operationSecurityEventOccured = true;

            identifySecurityTokenDependenciesAndUsage(securityEventQueue);

            Iterator<SecurityEvent> securityEventIterator = securityEventQueue.descendingIterator();
            while (securityEventIterator.hasNext()) {
                SecurityEvent prevSecurityEvent = securityEventIterator.next();
                forwardSecurityEvent(prevSecurityEvent);
            }
            //forward operation security event
            forwardSecurityEvent(securityEvent);

            securityEventQueue.clear();
            return;
        }

        securityEventQueue.push(securityEvent);
    }

    @Override
    protected void forwardSecurityEvent(SecurityEvent securityEvent) throws XMLSecurityException {

        if (!allowRSA15KeyTransportAlgorithm && SecurityEventConstants.AlgorithmSuite.equals(securityEvent.getSecurityEventType())) {
            AlgorithmSuiteSecurityEvent algorithmSuiteSecurityEvent = (AlgorithmSuiteSecurityEvent)securityEvent;
            Boolean allowRSA15 = get(WSSConstants.PROP_ALLOW_RSA15_KEYTRANSPORT_ALGORITHM);
            if ((allowRSA15 == null || !allowRSA15)
                && WSSConstants.NS_XENC_RSA15.equals(algorithmSuiteSecurityEvent.getAlgorithmURI())) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK,
                                              WSSConstants.PROP_ALLOW_RSA15_KEYTRANSPORT_ALGORITHM);
            }
        }

        try {
            super.forwardSecurityEvent(securityEvent);
        } catch (WSSecurityException e) {
            throw e;
        } catch (XMLSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, e);
        }
    }

    private void identifySecurityTokenDependenciesAndUsage(
            Deque<SecurityEvent> securityEventDeque) throws XMLSecurityException {

        MessageTokens messageTokens = new MessageTokens();
        HttpsTokenSecurityEvent httpsTokenSecurityEvent = null;

        List<TokenSecurityEvent<? extends InboundSecurityToken>> tokenSecurityEvents = new ArrayList<>();
        Iterator<SecurityEvent> securityEventIterator = securityEventDeque.iterator();
        while (securityEventIterator.hasNext()) {
            SecurityEvent securityEvent = securityEventIterator.next();
            if (securityEvent instanceof TokenSecurityEvent) {
                @SuppressWarnings("unchecked")
                TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent =
                        (TokenSecurityEvent<? extends InboundSecurityToken>)securityEvent;

                if (WSSecurityEventConstants.HTTPS_TOKEN.equals(securityEvent.getSecurityEventType())) {
                    HttpsTokenSecurityEvent actHttpsTokenSecurityEvent = (HttpsTokenSecurityEvent) tokenSecurityEvent;
                    actHttpsTokenSecurityEvent.getSecurityToken().getTokenUsages().clear();
                    actHttpsTokenSecurityEvent.getSecurityToken().addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
                    messageTokens.messageSignatureTokens =
                        addTokenSecurityEvent(actHttpsTokenSecurityEvent, messageTokens.messageSignatureTokens);
                    HttpsTokenSecurityEvent clonedHttpsTokenSecurityEvent = new HttpsTokenSecurityEvent();
                    clonedHttpsTokenSecurityEvent.setAuthenticationType(actHttpsTokenSecurityEvent.getAuthenticationType());
                    clonedHttpsTokenSecurityEvent.setIssuerName(actHttpsTokenSecurityEvent.getIssuerName());
                    clonedHttpsTokenSecurityEvent.setSecurityToken(actHttpsTokenSecurityEvent.getSecurityToken());
                    clonedHttpsTokenSecurityEvent.getSecurityToken().addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_ENCRYPTION);
                    messageTokens.messageEncryptionTokens =
                        addTokenSecurityEvent(actHttpsTokenSecurityEvent, messageTokens.messageEncryptionTokens);
                    httpsTokenSecurityEvent = clonedHttpsTokenSecurityEvent;
                    continue;
                }
                tokenSecurityEvents.add(tokenSecurityEvent);
            }
        }

        //search the root tokens and create new TokenSecurityEvents if not already there...
        for (int i = 0; i < tokenSecurityEvents.size(); i++) {
            TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent = tokenSecurityEvents.get(i);
            InboundSecurityToken securityToken = WSSUtils.getRootToken(tokenSecurityEvent.getSecurityToken());

            if (!containsSecurityToken(messageTokens.supportingTokens, securityToken)) {
                TokenSecurityEvent<? extends InboundSecurityToken> newTokenSecurityEvent =
                        WSSUtils.createTokenSecurityEvent(securityToken, tokenSecurityEvent.getCorrelationID());
                messageTokens.supportingTokens = addTokenSecurityEvent(newTokenSecurityEvent, messageTokens.supportingTokens);
                securityEventDeque.offer(newTokenSecurityEvent);
            }
            //remove old TokenSecurityEvent so that only root tokens are in the queue
            securityEventDeque.remove(tokenSecurityEvent);
        }

        parseSupportingTokens(messageTokens, httpsTokenSecurityEvent, securityEventDeque);

        if (messageTokens.messageSignatureTokens.isEmpty()) {
            InboundSecurityToken messageSignatureToken = getSupportingTokenSigningToken(messageTokens, securityEventDeque);

            TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent =
                    getTokenSecurityEvent(messageSignatureToken, tokenSecurityEvents);
            if (tokenSecurityEvent != null) {
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.supportingTokens);
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedSupportingTokens);
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.endorsingSupportingTokens);
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEndorsingSupportingTokens);
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEncryptedSupportingTokens);
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.encryptedSupportingTokens);
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.endorsingEncryptedSupportingTokens);
                removeTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEndorsingEncryptedSupportingTokens);
                messageTokens.messageSignatureTokens = addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageSignatureTokens);
            }
        }

        if (messageTokens.messageSignatureTokens.isEmpty()) {
            for (Iterator<TokenSecurityEvent<? extends InboundSecurityToken>> iterator =
                messageTokens.supportingTokens.iterator(); iterator.hasNext();) {
                TokenSecurityEvent<? extends InboundSecurityToken> supportingToken = iterator.next();
                if (supportingToken.getSecurityToken().getTokenUsages().contains(WSSecurityTokenConstants.TokenUsage_Signature)) {
                    iterator.remove();
                    messageTokens.messageSignatureTokens = addTokenSecurityEvent(supportingToken, messageTokens.messageSignatureTokens);
                    break;
                }
            }
        }

        if (messageTokens.messageEncryptionTokens.isEmpty()) {
            for (Iterator<TokenSecurityEvent<? extends InboundSecurityToken>> iterator =
                messageTokens.supportingTokens.iterator(); iterator.hasNext();) {
                TokenSecurityEvent<? extends InboundSecurityToken> supportingToken = iterator.next();
                if (supportingToken.getSecurityToken().getTokenUsages().contains(WSSecurityTokenConstants.TokenUsage_Encryption)) {
                    iterator.remove();
                    messageTokens.messageEncryptionTokens = addTokenSecurityEvent(supportingToken, messageTokens.messageEncryptionTokens);
                    break;
                }
            }
        }

        if (!messageTokens.messageEncryptionTokens.isEmpty()) {
            this.messageEncryptionTokenOccured = true;
        }

        setTokenUsage(messageTokens.messageSignatureTokens, WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
        setTokenUsage(messageTokens.messageEncryptionTokens, WSSecurityTokenConstants.TOKENUSAGE_MAIN_ENCRYPTION);
        setTokenUsage(messageTokens.supportingTokens, WSSecurityTokenConstants.TOKENUSAGE_SUPPORTING_TOKENS);
        setTokenUsage(messageTokens.signedSupportingTokens, WSSecurityTokenConstants.TOKENUSAGE_SIGNED_SUPPORTING_TOKENS);
        setTokenUsage(messageTokens.endorsingSupportingTokens,
                      WSSecurityTokenConstants.TOKENUSAGE_ENDORSING_SUPPORTING_TOKENS);
        setTokenUsage(messageTokens.signedEndorsingSupportingTokens,
                      WSSecurityTokenConstants.TOKENUSAGE_SIGNED_ENDORSING_SUPPORTING_TOKENS);
        setTokenUsage(messageTokens.signedEncryptedSupportingTokens,
                      WSSecurityTokenConstants.TOKENUSAGE_SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        setTokenUsage(messageTokens.encryptedSupportingTokens,
                      WSSecurityTokenConstants.TOKENUSAGE_ENCRYPTED_SUPPORTING_TOKENS);
        setTokenUsage(messageTokens.endorsingEncryptedSupportingTokens,
                      WSSecurityTokenConstants.TOKENUSAGE_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        setTokenUsage(messageTokens.signedEndorsingEncryptedSupportingTokens,
                      WSSecurityTokenConstants.TOKENUSAGE_SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
    }

    private void parseSupportingTokens(MessageTokens messageTokens, HttpsTokenSecurityEvent httpsTokenSecurityEvent,
                                       Deque<SecurityEvent> securityEventDeque) throws XMLSecurityException {
        Iterator<TokenSecurityEvent<? extends InboundSecurityToken>> supportingTokensIterator = messageTokens.supportingTokens.iterator();
        while (supportingTokensIterator.hasNext()) {
            TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent = supportingTokensIterator.next();
            List<InboundSecurityToken> signingSecurityTokens =
                isSignedToken(tokenSecurityEvent, securityEventDeque, httpsTokenSecurityEvent);

            List<QName> securityHeader =
                soap12 ? WSSConstants.SOAP_12_WSSE_SECURITY_HEADER_PATH : WSSConstants.SOAP_11_WSSE_SECURITY_HEADER_PATH;
            List<QName> signatureElementPath = new ArrayList<>(4);
            signatureElementPath.addAll(securityHeader);
            signatureElementPath.add(WSSConstants.TAG_dsig_Signature);
            boolean signsSignature = signsElement(tokenSecurityEvent, signatureElementPath, securityEventDeque);
            boolean encryptsSignature = encryptsElement(tokenSecurityEvent, signatureElementPath, securityEventDeque);

            List<QName> signatureConfirmationElementPath = new ArrayList<>(4);
            signatureConfirmationElementPath.addAll(securityHeader);
            signatureConfirmationElementPath.add(WSSConstants.TAG_WSSE11_SIG_CONF);
            boolean signsSignatureConfirmation =
                signsElement(tokenSecurityEvent, signatureConfirmationElementPath, securityEventDeque);
            boolean encryptsSignatureConfirmation =
                encryptsElement(tokenSecurityEvent, signatureConfirmationElementPath, securityEventDeque);

            List<QName> timestampElementPath = new ArrayList<>(4);
            timestampElementPath.addAll(securityHeader);
            timestampElementPath.add(WSSConstants.TAG_WSU_TIMESTAMP);
            boolean signsTimestamp = signsElement(tokenSecurityEvent, timestampElementPath, securityEventDeque);

            List<QName> usernameTokenElementPath = new ArrayList<>(4);
            usernameTokenElementPath.addAll(securityHeader);
            usernameTokenElementPath.add(WSSConstants.TAG_WSSE_USERNAME_TOKEN);
            boolean encryptsUsernameToken = encryptsElement(tokenSecurityEvent, usernameTokenElementPath, securityEventDeque);

            boolean transportSecurityActive = Boolean.TRUE.equals(get(WSSConstants.TRANSPORT_SECURITY_ACTIVE));

            List<InboundSecurityToken> encryptingSecurityTokens =
                isEncryptedToken(tokenSecurityEvent, securityEventDeque, httpsTokenSecurityEvent);

            boolean signatureUsage =
                tokenSecurityEvent.getSecurityToken().getTokenUsages().contains(WSSecurityTokenConstants.TokenUsage_Signature);
            boolean encryptionUsage =
                tokenSecurityEvent.getSecurityToken().getTokenUsages().contains(WSSecurityTokenConstants.TokenUsage_Encryption);

            if (!transportSecurityActive && signsSignatureConfirmation && signsTimestamp && !signsSignature) {
                supportingTokensIterator.remove();
                messageTokens.messageSignatureTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageSignatureTokens);
                if (encryptionUsage) {
                    messageTokens.messageEncryptionTokens =
                        addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageEncryptionTokens);
                }
            } else if (!transportSecurityActive && signsSignatureConfirmation && !signsSignature) {
                supportingTokensIterator.remove();
                messageTokens.messageSignatureTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageSignatureTokens);
                if (encryptionUsage) {
                    messageTokens.messageEncryptionTokens =
                        addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageEncryptionTokens);
                }
            } else if (!transportSecurityActive && signsTimestamp && !signsSignature) {
                supportingTokensIterator.remove();
                messageTokens.messageSignatureTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageSignatureTokens);
                if (encryptionUsage) {
                    messageTokens.messageEncryptionTokens =
                        addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageEncryptionTokens);
                }
            } else if (!transportSecurityActive
                && (encryptsSignature || encryptsSignatureConfirmation || encryptsUsernameToken)) {
                supportingTokensIterator.remove();
                messageTokens.messageEncryptionTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.messageEncryptionTokens);
            } else if (signsSignature && !signingSecurityTokens.isEmpty() && !encryptingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.signedEndorsingEncryptedSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEndorsingEncryptedSupportingTokens);
            } else if (transportSecurityActive && signsTimestamp && !signingSecurityTokens.isEmpty()
                && !encryptingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.signedEndorsingEncryptedSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEndorsingEncryptedSupportingTokens);
            } else if (signsSignature && signingSecurityTokens.isEmpty() && !encryptingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.endorsingEncryptedSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.endorsingEncryptedSupportingTokens);
            } else if (signsSignature && !signingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.signedEndorsingSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEndorsingSupportingTokens);
            } else if (signatureUsage && !signingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.signedEndorsingSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEndorsingSupportingTokens);
            } else if (signsSignature) {
                supportingTokensIterator.remove();
                messageTokens.endorsingSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.endorsingSupportingTokens);
            } else if (!signingSecurityTokens.isEmpty() && !encryptingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.signedEncryptedSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedEncryptedSupportingTokens);
            } else if (!signingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.signedSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.signedSupportingTokens);
            } else if (!encryptingSecurityTokens.isEmpty()) {
                supportingTokensIterator.remove();
                messageTokens.encryptedSupportingTokens =
                    addTokenSecurityEvent(tokenSecurityEvent, messageTokens.encryptedSupportingTokens);
            }
        }
    }

    private void removeTokenSecurityEvent(TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent,
                                          List<TokenSecurityEvent<? extends InboundSecurityToken>> tokenSecurityEventList) {
        for (int i = 0; i < tokenSecurityEventList.size(); i++) {
            TokenSecurityEvent<? extends InboundSecurityToken> securityEvent = tokenSecurityEventList.get(i);
            if (securityEvent.getSecurityToken().getId().equals(tokenSecurityEvent.getSecurityToken().getId())) {
                tokenSecurityEventList.remove(securityEvent);
                return;
            }
        }
    }

    private List<TokenSecurityEvent<? extends InboundSecurityToken>> addTokenSecurityEvent(
            TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent,
            List<TokenSecurityEvent<? extends InboundSecurityToken>> tokenSecurityEventList) {
        if (tokenSecurityEventList == Collections.<TokenSecurityEvent<? extends InboundSecurityToken>>emptyList()) {
            tokenSecurityEventList = new ArrayList<>();
        }
        tokenSecurityEventList.add(tokenSecurityEvent);
        return tokenSecurityEventList;
    }

    private boolean containsSecurityToken(List<TokenSecurityEvent<? extends InboundSecurityToken>> supportingTokens,
                                          SecurityToken securityToken) {
        if (securityToken != null) {
            for (int i = 0; i < supportingTokens.size(); i++) {
                TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent = supportingTokens.get(i);
                if (tokenSecurityEvent.getSecurityToken().getId().equals(securityToken.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private TokenSecurityEvent<? extends InboundSecurityToken> getTokenSecurityEvent(
            InboundSecurityToken securityToken,
            List<TokenSecurityEvent<? extends InboundSecurityToken>> tokenSecurityEvents) throws XMLSecurityException {
        if (securityToken != null) {
            for (int i = 0; i < tokenSecurityEvents.size(); i++) {
                TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent = tokenSecurityEvents.get(i);
                if (tokenSecurityEvent.getSecurityToken().getId().equals(securityToken.getId())) {
                    return tokenSecurityEvent;
                }
            }
        }
        return null;
    }

    private InboundSecurityToken getSupportingTokenSigningToken(
            MessageTokens messageTokens,
            Deque<SecurityEvent> securityEventDeque
    ) throws XMLSecurityException {

        //todo we have to check if the signingTokens also cover the other supporting tokens!
        for (int i = 0; i < messageTokens.signedSupportingTokens.size(); i++) {
            TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent = messageTokens.signedSupportingTokens.get(i);
            List<? extends InboundSecurityToken> signingSecurityTokens = getSigningToken(tokenSecurityEvent, securityEventDeque);
            if (signingSecurityTokens.size() == 1) {
                return signingSecurityTokens.get(0);
            }
        }
        for (int i = 0; i < messageTokens.signedEndorsingSupportingTokens.size(); i++) {
            TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent = messageTokens.signedEndorsingSupportingTokens.get(i);
            List<InboundSecurityToken> signingSecurityTokens = getSigningToken(tokenSecurityEvent, securityEventDeque);
            if (signingSecurityTokens.size() == 1) {
                return signingSecurityTokens.get(0);
            }
        }
        for (int i = 0; i < messageTokens.signedEncryptedSupportingTokens.size(); i++) {
            TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent = messageTokens.signedEncryptedSupportingTokens.get(i);
            List<InboundSecurityToken> signingSecurityTokens = getSigningToken(tokenSecurityEvent, securityEventDeque);
            if (signingSecurityTokens.size() == 1) {
                return signingSecurityTokens.get(0);
            }
        }
        for (int i = 0; i < messageTokens.signedEndorsingEncryptedSupportingTokens.size(); i++) {
            TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent = messageTokens.signedEndorsingEncryptedSupportingTokens.get(i);
            List<InboundSecurityToken> signingSecurityTokens = getSigningToken(tokenSecurityEvent, securityEventDeque);
            if (signingSecurityTokens.size() == 1) {
                return signingSecurityTokens.get(0);
            }
        }
        return null;
    }

    private List<InboundSecurityToken> getSigningToken(TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent,
                                                       Deque<SecurityEvent> securityEventDeque) throws XMLSecurityException {
        List<InboundSecurityToken> signingSecurityTokens = new ArrayList<>();

        for (Iterator<SecurityEvent> iterator = securityEventDeque.iterator(); iterator.hasNext();) {
            SecurityEvent securityEvent = iterator.next();
            if (WSSecurityEventConstants.SignedElement.equals(securityEvent.getSecurityEventType())) {
                SignedElementSecurityEvent signedElementSecurityEvent = (SignedElementSecurityEvent) securityEvent;
                if (signedElementSecurityEvent.isSigned()
                        && WSSUtils.pathMatches(
                        signedElementSecurityEvent.getElementPath(),
                        ((InboundSecurityToken)tokenSecurityEvent.getSecurityToken()).getElementPath(), false)
                        ) {
                    signingSecurityTokens.add((InboundSecurityToken)signedElementSecurityEvent.getSecurityToken());
                }
            }
        }
        return signingSecurityTokens;
    }

    private void setTokenUsage(List<TokenSecurityEvent<? extends InboundSecurityToken>> tokenSecurityEvents,
                               WSSecurityTokenConstants.TokenUsage tokenUsage) throws XMLSecurityException {
        for (int i = 0; i < tokenSecurityEvents.size(); i++) {
            TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent = tokenSecurityEvents.get(i);
            setTokenUsage(tokenSecurityEvent, tokenUsage);
        }
    }

    private void setTokenUsage(TokenSecurityEvent<? extends InboundSecurityToken> tokenSecurityEvent,
                               WSSecurityTokenConstants.TokenUsage tokenUsage) throws XMLSecurityException {
        tokenSecurityEvent.getSecurityToken().getTokenUsages().remove(WSSecurityTokenConstants.TOKENUSAGE_SUPPORTING_TOKENS);
        tokenSecurityEvent.getSecurityToken().getTokenUsages().remove(WSSecurityTokenConstants.TokenUsage_Signature);
        tokenSecurityEvent.getSecurityToken().getTokenUsages().remove(WSSecurityTokenConstants.TokenUsage_Encryption);
        tokenSecurityEvent.getSecurityToken().addTokenUsage(tokenUsage);
    }

    private List<InboundSecurityToken> isSignedToken(TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent,
                                              Deque<SecurityEvent> securityEventDeque,
                                              HttpsTokenSecurityEvent httpsTokenSecurityEvent) throws XMLSecurityException {
        List<InboundSecurityToken> securityTokenList = new ArrayList<>();
        if (httpsTokenSecurityEvent != null) {
            securityTokenList.add(httpsTokenSecurityEvent.getSecurityToken());
            return securityTokenList;
        }
        for (Iterator<SecurityEvent> iterator = securityEventDeque.iterator(); iterator.hasNext();) {
            SecurityEvent securityEvent = iterator.next();
            if (WSSecurityEventConstants.SignedElement.equals(securityEvent.getSecurityEventType())) {
                SignedElementSecurityEvent signedElementSecurityEvent = (SignedElementSecurityEvent) securityEvent;
                if (signedElementSecurityEvent.isSigned()
                        && tokenSecurityEvent.getSecurityToken() != null
                        && signedElementSecurityEvent.getXmlSecEvent() != null
                        && signedElementSecurityEvent.getXmlSecEvent()
                            == ((InboundSecurityToken)tokenSecurityEvent.getSecurityToken()).getXMLSecEvent()
                        && !securityTokenList.contains(signedElementSecurityEvent.getSecurityToken())) {
                    securityTokenList.add((InboundSecurityToken)signedElementSecurityEvent.getSecurityToken());
                }
            }
        }
        return securityTokenList;
    }

    private List<InboundSecurityToken> isEncryptedToken(TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent,
                                                 Deque<SecurityEvent> securityEventDeque,
                                                 HttpsTokenSecurityEvent httpsTokenSecurityEvent) throws XMLSecurityException {

        List<InboundSecurityToken> securityTokenList = new ArrayList<>();
        if (httpsTokenSecurityEvent != null) {
            securityTokenList.add(httpsTokenSecurityEvent.getSecurityToken());
            return securityTokenList;
        }
        for (Iterator<SecurityEvent> iterator = securityEventDeque.iterator(); iterator.hasNext();) {
            SecurityEvent securityEvent = iterator.next();
            if (WSSecurityEventConstants.EncryptedElement.equals(securityEvent.getSecurityEventType())) {
                EncryptedElementSecurityEvent encryptedElementSecurityEvent = (EncryptedElementSecurityEvent) securityEvent;
                if (encryptedElementSecurityEvent.isEncrypted()
                        && tokenSecurityEvent.getSecurityToken() != null
                        && encryptedElementSecurityEvent.getXmlSecEvent() != null
                        && encryptedElementSecurityEvent.getXmlSecEvent()
                            == ((InboundSecurityToken)tokenSecurityEvent.getSecurityToken()).getXMLSecEvent()
                        && !securityTokenList.contains(encryptedElementSecurityEvent.getSecurityToken())) {
                    securityTokenList.add((InboundSecurityToken)encryptedElementSecurityEvent.getSecurityToken());
                }
            }
        }
        return securityTokenList;
    }

    private boolean signsElement(TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent, List<QName> elementPath,
                                 Deque<SecurityEvent> securityEventDeque) throws XMLSecurityException {
        for (Iterator<SecurityEvent> iterator = securityEventDeque.iterator(); iterator.hasNext();) {
            SecurityEvent securityEvent = iterator.next();
            if (WSSecurityEventConstants.SignedElement.equals(securityEvent.getSecurityEventType())) {
                SignedElementSecurityEvent signedElementSecurityEvent = (SignedElementSecurityEvent) securityEvent;
                if (signedElementSecurityEvent.isSigned()
                        && matchesTokenOrWrappedTokenId(tokenSecurityEvent.getSecurityToken(),
                        signedElementSecurityEvent.getSecurityToken().getId(),
                        SecurityTokenConstants.TokenUsage_Signature)
                        && WSSUtils.pathMatches(elementPath, signedElementSecurityEvent.getElementPath(), false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesTokenOrWrappedTokenId(
            SecurityToken securityToken, String id,
            SecurityTokenConstants.TokenUsage tokenUsage) throws XMLSecurityException {
        if (securityToken.getId().equals(id) && securityToken.getTokenUsages().contains(tokenUsage)) {
            return true;
        }
        List<? extends SecurityToken> wrappedTokens = securityToken.getWrappedTokens();
        for (int i = 0; i < wrappedTokens.size(); i++) {
            boolean match = matchesTokenOrWrappedTokenId(wrappedTokens.get(i), id, tokenUsage);
            if (match) {
                return match;
            }
        }
        return false;
    }

    private boolean encryptsElement(TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent, List<QName> elementPath,
                                    Deque<SecurityEvent> securityEventDeque) throws XMLSecurityException {
        for (Iterator<SecurityEvent> iterator = securityEventDeque.iterator(); iterator.hasNext();) {
            SecurityEvent securityEvent = iterator.next();
            if (WSSecurityEventConstants.EncryptedElement.equals(securityEvent.getSecurityEventType())) {
                EncryptedElementSecurityEvent encryptedElementSecurityEvent = (EncryptedElementSecurityEvent) securityEvent;
                if (encryptedElementSecurityEvent.isEncrypted()
                        && encryptedElementSecurityEvent.getSecurityToken().getId().equals(tokenSecurityEvent.getSecurityToken().getId())
                        && WSSUtils.pathMatches(elementPath, encryptedElementSecurityEvent.getElementPath(), false)) {
                    return true;
                }
            } else if (WSSecurityEventConstants.ContentEncrypted.equals(securityEvent.getSecurityEventType())) {
                ContentEncryptedElementSecurityEvent contentEncryptedElementSecurityEvent =
                    (ContentEncryptedElementSecurityEvent) securityEvent;
                String tokenId = tokenSecurityEvent.getSecurityToken().getId();
                if (contentEncryptedElementSecurityEvent.isEncrypted()
                        && contentEncryptedElementSecurityEvent.getSecurityToken().getId().equals(tokenId)
                        && contentEncryptedElementSecurityEvent.getXmlSecEvent()
                            == ((InboundSecurityToken)tokenSecurityEvent.getSecurityToken()).getXMLSecEvent()
                        && WSSUtils.pathMatches(elementPath, contentEncryptedElementSecurityEvent.getElementPath(), false)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void handleBSPRule(BSPRule bspRule) throws WSSecurityException {
        if (disableBSPEnforcement) {
            return;
        }
        if (!ignoredBSPRules.contains(bspRule)) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY,
                    "empty",
                    new Object[] {"BSP:" + bspRule.name() + ": " + bspRule.getMsg()});
        } else {
            LOG.warn("BSP:" + bspRule.name() + ": " + bspRule.getMsg());
        }
    }

    @Override
    public void ignoredBSPRules(List<BSPRule> bspRules) {
        ignoredBSPRules = new ArrayList<>(bspRules);
    }

    public boolean isDisableBSPEnforcement() {
        return disableBSPEnforcement;
    }

    public void setDisableBSPEnforcement(boolean disableBSPEnforcement) {
        this.disableBSPEnforcement = disableBSPEnforcement;
    }

    public boolean isAllowRSA15KeyTransportAlgorithm() {
        return allowRSA15KeyTransportAlgorithm;
    }

    public void setAllowRSA15KeyTransportAlgorithm(boolean allowRSA15KeyTransportAlgorithm) {
        this.allowRSA15KeyTransportAlgorithm = allowRSA15KeyTransportAlgorithm;
    }

    public boolean isSoap12() {
        return soap12;
    }

    public void setSoap12(boolean soap12) {
        this.soap12 = soap12;
    }

    private static final class MessageTokens {
        List<TokenSecurityEvent<? extends InboundSecurityToken>> messageSignatureTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> messageEncryptionTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> supportingTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> signedSupportingTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> endorsingSupportingTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> signedEndorsingSupportingTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> signedEncryptedSupportingTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> encryptedSupportingTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> endorsingEncryptedSupportingTokens = Collections.emptyList();
        List<TokenSecurityEvent<? extends InboundSecurityToken>> signedEndorsingEncryptedSupportingTokens = Collections.emptyList();
    }
}
