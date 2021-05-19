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

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.neethi.Assertion;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.HttpsToken;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KeyValueToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.Trust10;
import org.apache.wss4j.policy.model.Trust13;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.Wss10;
import org.apache.wss4j.policy.model.Wss11;
import org.apache.wss4j.policy.model.X509Token;

/**
 * Some common functionality to be shared between the two binding handlers (DOM + StAX)
 */
public abstract class AbstractCommonBindingHandler {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractCommonBindingHandler.class);
    protected final SoapMessage message;

    public AbstractCommonBindingHandler(
        SoapMessage msg
    ) {
        this.message = msg;
    }

    protected void unassertPolicy(Assertion assertion, String reason) {
        if (assertion == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Not asserting " + assertion.getName() + ": " + reason);
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.get(assertion.getName());
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

    protected void unassertPolicy(Assertion assertion, Exception reason) {
        if (assertion == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Not asserting " + assertion.getName() + ": " + reason);
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason.getMessage());
                }
            }
        }
        if (!assertion.isOptional()) {
            throw new PolicyException(new Message(reason.getMessage(), LOG), reason);
        }
    }

    protected void assertTokenWrapper(AbstractTokenWrapper tokenWrapper) {
        if (tokenWrapper == null) {
            return;
        }
        assertPolicy(tokenWrapper.getName());
        assertToken(tokenWrapper.getToken());
    }

    protected void assertToken(AbstractToken token) {
        if (token == null) {
            return;
        }
        assertPolicy(token.getName());

        String namespace = token.getName().getNamespaceURI();
        if (token.getDerivedKeys() != null) {
            assertPolicy(new QName(namespace, token.getDerivedKeys().name()));
        }

        if (token instanceof X509Token) {
            X509Token x509Token = (X509Token)token;
            assertX509Token(x509Token);
        } else if (token instanceof HttpsToken) {
            HttpsToken httpsToken = (HttpsToken)token;
            if (httpsToken.getAuthenticationType() != null) {
                assertPolicy(new QName(namespace, httpsToken.getAuthenticationType().name()));
            }
        } else if (token instanceof KeyValueToken) {
            KeyValueToken keyValueToken = (KeyValueToken)token;
            if (keyValueToken.isRsaKeyValue()) {
                assertPolicy(new QName(namespace, SPConstants.RSA_KEY_VALUE));
            }
        } else if (token instanceof UsernameToken) {
            UsernameToken usernameToken = (UsernameToken)token;
            assertUsernameToken(usernameToken);
        } else if (token instanceof SecureConversationToken) {
            SecureConversationToken scToken = (SecureConversationToken)token;
            assertSecureConversationToken(scToken);
        } else if (token instanceof SecurityContextToken) {
            SecurityContextToken scToken = (SecurityContextToken)token;
            assertSecurityContextToken(scToken);
        } else if (token instanceof SpnegoContextToken) {
            SpnegoContextToken scToken = (SpnegoContextToken)token;
            assertSpnegoContextToken(scToken);
        } else if (token instanceof IssuedToken) {
            IssuedToken issuedToken = (IssuedToken)token;
            assertIssuedToken(issuedToken);
        } else if (token instanceof KerberosToken) {
            KerberosToken kerberosToken = (KerberosToken)token;
            assertKerberosToken(kerberosToken);
        } else if (token instanceof SamlToken) {
            SamlToken samlToken = (SamlToken)token;
            assertSamlToken(samlToken);
        }
    }

    private void assertX509Token(X509Token token) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isRequireEmbeddedTokenReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_EMBEDDED_TOKEN_REFERENCE));
        }
        if (token.isRequireIssuerSerialReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_ISSUER_SERIAL_REFERENCE));
        }
        if (token.isRequireKeyIdentifierReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE));
        }
        if (token.isRequireThumbprintReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_THUMBPRINT_REFERENCE));
        }
        if (token.getTokenType() != null) {
            assertPolicy(new QName(namespace, token.getTokenType().name()));
        }
    }

    private void assertUsernameToken(UsernameToken token) {
        String namespace = token.getName().getNamespaceURI();

        if (token.getPasswordType() != null) {
            assertPolicy(new QName(namespace, token.getPasswordType().name()));
        }
        if (token.getUsernameTokenType() != null) {
            assertPolicy(new QName(namespace, token.getUsernameTokenType().name()));
        }
        if (token.isCreated()) {
            assertPolicy(SP13Constants.CREATED);
        }
        if (token.isNonce()) {
            assertPolicy(SP13Constants.NONCE);
        }
    }

    private void assertSecurityContextToken(SecurityContextToken token) {
        String namespace = token.getName().getNamespaceURI();
        if (token.isRequireExternalUriReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_EXTERNAL_URI_REFERENCE));
        }
        if (token.isSc10SecurityContextToken()) {
            assertPolicy(new QName(namespace, SPConstants.SC10_SECURITY_CONTEXT_TOKEN));
        }
        if (token.isSc13SecurityContextToken()) {
            assertPolicy(new QName(namespace, SPConstants.SC13_SECURITY_CONTEXT_TOKEN));
        }
    }

    private void assertSecureConversationToken(SecureConversationToken token) {
        assertSecurityContextToken(token);

        String namespace = token.getName().getNamespaceURI();
        if (token.isMustNotSendAmend()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_NOT_SEND_AMEND));
        }
        if (token.isMustNotSendCancel()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_NOT_SEND_CANCEL));
        }
        if (token.isMustNotSendRenew()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_NOT_SEND_RENEW));
        }
    }

    private void assertSpnegoContextToken(SpnegoContextToken token) {
        String namespace = token.getName().getNamespaceURI();
        if (token.isMustNotSendAmend()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_NOT_SEND_AMEND));
        }
        if (token.isMustNotSendCancel()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_NOT_SEND_CANCEL));
        }
        if (token.isMustNotSendRenew()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_NOT_SEND_RENEW));
        }
    }

    private void assertIssuedToken(IssuedToken token) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isRequireExternalReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_EXTERNAL_REFERENCE));
        }
        if (token.isRequireInternalReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_INTERNAL_REFERENCE));
        }
    }

    private void assertKerberosToken(KerberosToken token) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isRequireKeyIdentifierReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE));
        }
        if (token.getApReqTokenType() != null) {
            assertPolicy(new QName(namespace, token.getApReqTokenType().name()));
        }
    }

    private void assertSamlToken(SamlToken token) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isRequireKeyIdentifierReference()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE));
        }
        if (token.getSamlTokenType() != null) {
            assertPolicy(new QName(namespace, token.getSamlTokenType().name()));
        }
    }

    protected void assertAlgorithmSuite(AlgorithmSuite algorithmSuite) {
        if (algorithmSuite == null) {
            return;
        }

        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> algorithmSuiteAis = aim.get(algorithmSuite.getName());
        for (AssertionInfo ai : algorithmSuiteAis) {
            ai.setAsserted(true);
        }

        AlgorithmSuiteType algorithmSuiteType = algorithmSuite.getAlgorithmSuiteType();
        String namespace = algorithmSuiteType.getNamespace();
        if (namespace != null) {
            Collection<AssertionInfo> algAis =
                aim.get(new QName(namespace, algorithmSuiteType.getName()));
            if (algAis != null) {
                for (AssertionInfo algAi : algAis) {
                    algAi.setAsserted(true);
                }
            }
        }
    }

    protected void assertWSSProperties(String namespace) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> wss10Ais = aim.get(new QName(namespace, SPConstants.WSS10));
        if (wss10Ais != null) {
            for (AssertionInfo ai : wss10Ais) {
                ai.setAsserted(true);
                Wss10 wss10 = (Wss10)ai.getAssertion();
                assertWSS10Properties(wss10);
            }
        }

        Collection<AssertionInfo> wss11Ais = aim.get(new QName(namespace, SPConstants.WSS11));
        if (wss11Ais != null) {
            for (AssertionInfo ai : wss11Ais) {
                ai.setAsserted(true);
                Wss11 wss11 = (Wss11)ai.getAssertion();
                assertWSS10Properties(wss11);

                if (wss11.isMustSupportRefThumbprint()) {
                    assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_REF_THUMBPRINT));
                }
                if (wss11.isMustSupportRefEncryptedKey()) {
                    assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_REF_ENCRYPTED_KEY));
                }
                if (wss11.isRequireSignatureConfirmation()) {
                    assertPolicy(new QName(namespace, SPConstants.REQUIRE_SIGNATURE_CONFIRMATION));
                }
            }
        }
    }

    private void assertWSS10Properties(Wss10 wss10) {
        String namespace = wss10.getName().getNamespaceURI();
        if (wss10.isMustSupportRefEmbeddedToken()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN));
        }
        if (wss10.isMustSupportRefKeyIdentifier()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER));
        }
        if (wss10.isMustSupportRefIssuerSerial()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL));
        }
        if (wss10.isMustSupportRefExternalURI()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI));
        }
    }

    protected void assertTrustProperties(String namespace) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> trust10Ais = aim.get(new QName(namespace, SPConstants.TRUST_10));
        if (trust10Ais != null) {
            for (AssertionInfo ai : trust10Ais) {
                ai.setAsserted(true);
                Trust10 trust10 = (Trust10)ai.getAssertion();
                assertTrust10Properties(trust10);
            }
        }

        Collection<AssertionInfo> trust13Ais = aim.get(new QName(namespace, SPConstants.TRUST_13));
        if (trust13Ais != null) {
            for (AssertionInfo ai : trust13Ais) {
                ai.setAsserted(true);
                Trust13 trust13 = (Trust13)ai.getAssertion();
                assertTrust10Properties(trust13);

                if (trust13.isRequireRequestSecurityTokenCollection()) {
                    assertPolicy(new QName(namespace, SPConstants.REQUIRE_REQUEST_SECURITY_TOKEN_COLLECTION));
                }
                if (trust13.isRequireAppliesTo()) {
                    assertPolicy(new QName(namespace, SPConstants.REQUIRE_APPLIES_TO));
                }
                if (trust13.isScopePolicy15()) {
                    assertPolicy(new QName(namespace, SPConstants.SCOPE_POLICY_15));
                }
                if (trust13.isMustSupportInteractiveChallenge()) {
                    assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_INTERACTIVE_CHALLENGE));
                }
            }
        }
    }

    private void assertTrust10Properties(Trust10 trust10) {
        String namespace = trust10.getName().getNamespaceURI();
        if (trust10.isMustSupportClientChallenge()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_CLIENT_CHALLENGE));
        }
        if (trust10.isMustSupportIssuedTokens()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_ISSUED_TOKENS));
        }
        if (trust10.isMustSupportServerChallenge()) {
            assertPolicy(new QName(namespace, SPConstants.MUST_SUPPORT_SERVER_CHALLENGE));
        }
        if (trust10.isRequireClientEntropy()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_CLIENT_ENTROPY));
        }
        if (trust10.isRequireServerEntropy()) {
            assertPolicy(new QName(namespace, SPConstants.REQUIRE_SERVER_ENTROPY));
        }
    }

    protected Collection<AssertionInfo> getAllAssertionsByLocalname(String localname) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        return PolicyUtils.getAllAssertionsByLocalname(aim, localname);
    }

    protected SoapMessage getMessage() {
        return message;
    }

    protected boolean isRequestor() {
        return MessageUtils.isRequestor(message);
    }

    protected boolean isTokenRequired(IncludeTokenType includeToken) {
        if (includeToken == IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            return false;
        } else if (includeToken == IncludeTokenType.INCLUDE_TOKEN_ALWAYS) {
            return true;
        } else {
            boolean initiator = MessageUtils.isRequestor(message);
            if (initiator && (includeToken == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT
                || includeToken == IncludeTokenType.INCLUDE_TOKEN_ONCE)) {
                return true;
            } else if (!initiator && includeToken == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR) {
                return true;
            }
            return false;
        }
    }

    protected Wss10 getWss10() {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.WSS10);
        if (ai == null) {
            ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.WSS11);
        }

        if (ai != null) {
            return (Wss10)ai.getAssertion();
        }
        return null;
    }

    protected SecurityToken getSecurityToken() throws TokenStoreException {
        SecurityToken st = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
        if (st == null) {
            String id = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
            if (id != null) {
                st = TokenStoreUtils.getTokenStore(message).getToken(id);
            }
        }
        return st;
    }

    protected void assertPolicy(QName name) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        PolicyUtils.assertPolicy(aim, name);
    }

    protected void assertPolicy(Assertion assertion) {
        if (assertion == null) {
            return;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Asserting " + assertion.getName());
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setAsserted(true);
                }
            }
        }
    }

}
