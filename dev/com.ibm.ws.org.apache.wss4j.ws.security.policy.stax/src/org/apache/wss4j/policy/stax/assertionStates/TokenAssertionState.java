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
package org.apache.wss4j.policy.stax.assertionStates;

import org.apache.wss4j.policy.AssertionState;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.common.WSSPolicyException;
import org.apache.wss4j.policy.model.*;
import org.apache.wss4j.policy.stax.Assertable;
import org.apache.wss4j.policy.stax.DummyPolicyAsserter;
import org.apache.wss4j.policy.stax.PolicyAsserter;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityToken;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * WSP1.3, 5 Token Assertions
 */
public abstract class TokenAssertionState extends AssertionState implements Assertable {

    //todo WSP1.3, 5.2.1 Token Issuer: <sp:Issuer>wsa:EndpointReferenceType</sp:Issuer>
    //todo? WSP1.3 5.2.3 Required Claims
    //todo derived keys?

    private boolean initiator;
    private PolicyAsserter policyAsserter;

    public TokenAssertionState(AbstractSecurityAssertion assertion, boolean asserted, boolean initiator) {
        this(assertion, asserted, null, initiator);
    }

    public TokenAssertionState(AbstractSecurityAssertion assertion, boolean asserted,
                               PolicyAsserter policyAsserter, boolean initiator) {
        super(assertion, asserted);
        this.initiator = initiator;

        this.policyAsserter = policyAsserter;
        if (this.policyAsserter == null) {
            this.policyAsserter = new DummyPolicyAsserter();
        }

        if (asserted) {
            AbstractToken token = (AbstractToken)getAssertion();
            getPolicyAsserter().assertPolicy(token);
            if (token.getDerivedKeys() != null) {
                AbstractToken.DerivedKeys derivedKeys = token.getDerivedKeys();
                String namespace = token.getName().getNamespaceURI();
                getPolicyAsserter().assertPolicy(new QName(namespace, derivedKeys.name()));
            }
        }
    }

    @Override
    public boolean assertEvent(SecurityEvent securityEvent) throws WSSPolicyException, XMLSecurityException {

        if (isAsserted()) {
            //just return true when this token assertion is already fulfilled.
            return true;
        }

        @SuppressWarnings("unchecked")
        TokenSecurityEvent<SecurityToken> tokenSecurityEvent = (TokenSecurityEvent<SecurityToken>) securityEvent;
        AbstractToken abstractToken = (AbstractToken) getAssertion();
        final AbstractSecurityAssertion parentAssertion = abstractToken.getParentAssertion();

        int ignoreToken = 0;
        final List<WSSecurityTokenConstants.TokenUsage> tokenUsages = tokenSecurityEvent.getSecurityToken().getTokenUsages();
        Iterator<WSSecurityTokenConstants.TokenUsage> tokenUsageIterator = tokenUsages.iterator();
        loop:
        while (tokenUsageIterator.hasNext()) {
            WSSecurityTokenConstants.TokenUsage tokenUsage = tokenUsageIterator.next();
            if (WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE.equals(tokenUsage)) {
                if (initiator && !(parentAssertion instanceof RecipientToken)
                        && !(parentAssertion instanceof RecipientSignatureToken)
                        && !(parentAssertion instanceof SignatureToken)
                        && !(parentAssertion instanceof ProtectionToken)
                        && !(parentAssertion instanceof TransportToken)) {
                    ignoreToken++;
                    continue loop;
                } else if (!initiator && !(parentAssertion instanceof InitiatorToken)
                        && !(parentAssertion instanceof InitiatorSignatureToken)
                        && !(parentAssertion instanceof SignatureToken)
                        && !(parentAssertion instanceof ProtectionToken)
                        && !(parentAssertion instanceof TransportToken)) {
                    ignoreToken++;
                    continue loop;
                }
            } else if (WSSecurityTokenConstants.TokenUsage_Signature.equals(tokenUsage)) {
                throw new WSSPolicyException("Illegal token usage!");
            } else if (WSSecurityTokenConstants.TOKENUSAGE_MAIN_ENCRYPTION.equals(tokenUsage)) {
                if (initiator && !(parentAssertion instanceof InitiatorToken)
                        && !(parentAssertion instanceof InitiatorEncryptionToken)
                        && !(parentAssertion instanceof EncryptionToken)
                        && !(parentAssertion instanceof ProtectionToken)
                        && !(parentAssertion instanceof TransportToken)) {
                    ignoreToken++;
                    continue loop;
                } else if (!initiator && !(parentAssertion instanceof RecipientToken)
                        && !(parentAssertion instanceof RecipientEncryptionToken)
                        && !(parentAssertion instanceof EncryptionToken)
                        && !(parentAssertion instanceof ProtectionToken)
                        && !(parentAssertion instanceof TransportToken)) {
                    ignoreToken++;
                    continue loop;
                }
            } else if (WSSecurityTokenConstants.TokenUsage_Encryption.equals(tokenUsage)) {
                throw new WSSPolicyException("Illegal token usage!");
            } else if (WSSecurityTokenConstants.TOKENUSAGE_SUPPORTING_TOKENS.equals(tokenUsage)
                || WSSecurityTokenConstants.TOKENUSAGE_SIGNED_SUPPORTING_TOKENS.equals(tokenUsage)
                || WSSecurityTokenConstants.TOKENUSAGE_ENDORSING_SUPPORTING_TOKENS.equals(tokenUsage)
                || WSSecurityTokenConstants.TOKENUSAGE_SIGNED_ENDORSING_SUPPORTING_TOKENS.equals(tokenUsage)
                || WSSecurityTokenConstants.TOKENUSAGE_SIGNED_ENCRYPTED_SUPPORTING_TOKENS.equals(tokenUsage)
                || WSSecurityTokenConstants.TOKENUSAGE_ENCRYPTED_SUPPORTING_TOKENS.equals(tokenUsage)
                || WSSecurityTokenConstants.TOKENUSAGE_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS.equals(tokenUsage)
                || WSSecurityTokenConstants.TOKENUSAGE_SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS.equals(tokenUsage)
            ) {

                if (parentAssertion instanceof TransportToken) {
                    continue loop;
                }

                if (!(parentAssertion instanceof SupportingTokens)) {
                    ignoreToken++;
                    continue loop;
                }

                // Allow *SupportingToken policies which are processed as Signed|Encrypted*SupportingTokens
                final SupportingTokens supportingTokens = (SupportingTokens) parentAssertion;
                final String tokenUsageName = tokenUsage.getName();
                final String supportingTokensName = supportingTokens.getName().getLocalPart();
                if (!tokenUsageName.equals(supportingTokensName)) {
                    if (supportingTokensName.contains("Endorsing") && !tokenUsageName.contains("Endorsing")) {
                        ignoreToken++;
                        continue loop;
                    }
                    if (supportingTokensName.startsWith("Signed") && !tokenUsageName.startsWith("Signed")) {
                        ignoreToken++;
                        continue loop;
                    }
                    if (supportingTokensName.contains("Encrypted") && !tokenUsageName.contains("Encrypted")) {
                        ignoreToken++;
                        continue loop;
                    }
                }
            }
        }
        if (ignoreToken >= tokenUsages.size()) {
            //token is not for us, so return true to prevent false alarm
            return true;
        }

        boolean asserted = true;

        //WSP1.3, 5.1 Token Inclusion
        //todo do we need a global token cache to fullfill ".../IncludeToken/Once" ?
        SPConstants.IncludeTokenType includeTokenType = abstractToken.getIncludeTokenType();
        boolean isIncludedInMessage =
            ((InboundSecurityToken)tokenSecurityEvent.getSecurityToken()).isIncludedInMessage();
        switch (includeTokenType) {
            case INCLUDE_TOKEN_NEVER:
                if (isIncludedInMessage) {
                    setErrorMessage("Token must not be included");
                    asserted = false;
                }
                break;
            case INCLUDE_TOKEN_ONCE:
                break;
            case INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT:
                if (initiator && isIncludedInMessage) {
                    setErrorMessage("Token must not be included");
                    asserted = false;
                } else if (!initiator && !isIncludedInMessage) {
                    setErrorMessage("Token must be included");
                    asserted = false;
                }
                break;
            case INCLUDE_TOKEN_ALWAYS_TO_INITIATOR:
                if (initiator && !isIncludedInMessage) {
                    setErrorMessage("Token must be included");
                    asserted = false;
                } else if (!initiator && isIncludedInMessage) {
                    setErrorMessage("Token must not be included");
                    asserted = false;
                }
                break;
            case INCLUDE_TOKEN_ALWAYS:
                if (!isIncludedInMessage) {
                    setErrorMessage("Token must be included");
                    asserted = false;
                }
                break;
        }

        //WSP1.3, 5.3 Token Properties
        boolean hasDerivedKeys = false;
        hasDerivedKeys = hasDerivedKeys(tokenSecurityEvent.getSecurityToken());
        String namespace = getAssertion().getName().getNamespaceURI();
        if (abstractToken.getDerivedKeys() != null) {
            AbstractToken.DerivedKeys derivedKeys = abstractToken.getDerivedKeys();
            switch (derivedKeys) {
                case RequireDerivedKeys:
                case RequireExplicitDerivedKeys:
                case RequireImpliedDerivedKeys:
                    if (!hasDerivedKeys) {
                        setErrorMessage("Derived key must be used");
                        getPolicyAsserter().unassertPolicy(new QName(namespace, derivedKeys.name()),
                                                         "Derived key must be used");
                        asserted = false;
                    } else {
                        getPolicyAsserter().assertPolicy(new QName(namespace, derivedKeys.name()));
                    }
                    break;
            }
        } else {
            if (hasDerivedKeys) {
                setErrorMessage("Derived key must not be used");
                asserted = false;
            }
        }

        asserted &= assertToken(tokenSecurityEvent, abstractToken);
        if (asserted) {
            setAsserted(true);
        }
        if (!asserted && (tokenUsages.contains(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE)
                || tokenUsages.contains(WSSecurityTokenConstants.TOKENUSAGE_MAIN_ENCRYPTION))) {
            //return false if not asserted for the main signature and encryption tokens
            return false;
        } else {
            //always return true for supporting tokens.
            return true;
        }
    }

    public abstract boolean assertToken(TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent,
                                        AbstractToken abstractToken)
        throws WSSPolicyException, XMLSecurityException;

    protected boolean hasDerivedKeys(SecurityToken securityToken) throws XMLSecurityException {
        if (securityToken == null) {
            return false;
        } else if (WSSecurityTokenConstants.DerivedKeyToken.equals(securityToken.getTokenType())) {
            return true;
        }

        if (securityToken.getWrappedTokens().isEmpty()) {
            return false;
        }

        //all wrapped tokens must be derived!:
        boolean hasDerivedKeys = true;
        for (int i = 0; i < securityToken.getWrappedTokens().size(); i++) {
            SecurityToken wrappedSecurityToken = securityToken.getWrappedTokens().get(i);
            hasDerivedKeys &= hasDerivedKeys(wrappedSecurityToken);
        }
        return hasDerivedKeys;
    }

    protected PolicyAsserter getPolicyAsserter() {
        return policyAsserter;
    }

    protected boolean isInitiator() {
        return initiator;
    }
}
