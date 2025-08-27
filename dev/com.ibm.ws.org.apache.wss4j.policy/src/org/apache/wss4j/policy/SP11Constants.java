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
package org.apache.wss4j.policy;

import javax.xml.namespace.QName;

public class SP11Constants extends SPConstants {

    private static SP11Constants sp11Constants;

    protected SP11Constants() {
    }

    public static synchronized SP11Constants getInstance() {
        if (sp11Constants == null) {
            sp11Constants = new SP11Constants();
        }
        return sp11Constants;
    }

    public static final String SP_NS = "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy";

    public static final String SP_PREFIX = "sp";

    public static final QName INCLUDE_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.ATTR_INCLUDE_TOKEN, SP11Constants.SP_PREFIX);

    public static final String INCLUDE_NEVER =
            SP11Constants.SP_NS + SPConstants.INCLUDE_TOKEN_NEVER_SUFFIX;

    public static final String INCLUDE_ONCE =
            SP11Constants.SP_NS + SPConstants.INCLUDE_TOKEN_ONCE_SUFFIX;

    public static final String INCLUDE_ALWAYS_TO_RECIPIENT =
            SP11Constants.SP_NS + SPConstants.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT_SUFFIX;

    public static final String INCLUDE_ALWAYS_TO_INITIATOR =
            SP11Constants.SP_NS + SPConstants.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR_SUFFIX;

    public static final String INCLUDE_ALWAYS =
            SP11Constants.SP_NS + SPConstants.INCLUDE_TOKEN_ALWAYS_SUFFIX;

    public static final QName ATTR_XPATH_VERSION = new QName(
            SP11Constants.SP_NS, SPConstants.XPATH_VERSION, SP11Constants.SP_PREFIX);

    public static final QName TRANSPORT_BINDING = new QName(
            SP11Constants.SP_NS, SPConstants.TRANSPORT_BINDING, SP11Constants.SP_PREFIX);

    public static final QName ALGORITHM_SUITE = new QName(
            SP11Constants.SP_NS, SPConstants.ALGORITHM_SUITE, SP11Constants.SP_PREFIX);

    public static final QName LAYOUT = new QName(
            SP11Constants.SP_NS, SPConstants.LAYOUT, SP11Constants.SP_PREFIX);

    public static final QName STRICT = new QName(
            SP11Constants.SP_NS, SPConstants.LAYOUT_STRICT, SP11Constants.SP_PREFIX);

    public static final QName LAX = new QName(
            SP11Constants.SP_NS, SPConstants.LAYOUT_LAX, SP11Constants.SP_PREFIX);

    public static final QName LAXTSFIRST = new QName(
            SP11Constants.SP_NS, SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST, SP11Constants.SP_PREFIX);

    public static final QName LAXTSLAST = new QName(
            SP11Constants.SP_NS, SPConstants.LAYOUT_LAX_TIMESTAMP_LAST, SP11Constants.SP_PREFIX);

    public static final QName INCLUDE_TIMESTAMP = new QName(
            SP11Constants.SP_NS, SPConstants.INCLUDE_TIMESTAMP, SP11Constants.SP_PREFIX);

    public static final QName TRANSPORT_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.TRANSPORT_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName HTTPS_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.HTTPS_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName KERBEROS_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.KERBEROS_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName SPNEGO_CONTEXT_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.SPNEGO_CONTEXT_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName SECURITY_CONTEXT_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.SECURITY_CONTEXT_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName SECURE_CONVERSATION_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.SECURE_CONVERSATION_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName SAML_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.SAML_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName REL_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.REL_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName KEY_VALUE_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.KEY_VALUE_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName SIGNATURE_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.SIGNATURE_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName SIGNED_PARTS = new QName(
            SP11Constants.SP_NS, SPConstants.SIGNED_PARTS, SP11Constants.SP_PREFIX);

    public static final QName ENCRYPTED_PARTS = new QName(
            SP11Constants.SP_NS, SPConstants.ENCRYPTED_PARTS, SP11Constants.SP_PREFIX);

    public static final QName SIGNED_ELEMENTS = new QName(
            SP11Constants.SP_NS, SPConstants.SIGNED_ELEMENTS, SP11Constants.SP_PREFIX);

    public static final QName XPATH_EXPR = new QName(
            SP11Constants.SP_NS, SPConstants.XPATH_EXPR, SP11Constants.SP_PREFIX);

    public static final QName ENCRYPTED_ELEMENTS = new QName(
            SP11Constants.SP_NS, SPConstants.ENCRYPTED_ELEMENTS, SP11Constants.SP_PREFIX);

    public static final QName CONTENT_ENCRYPTED_ELEMENTS = new QName(
            SP11Constants.SP_NS, SPConstants.CONTENT_ENCRYPTED_ELEMENTS, SP11Constants.SP_PREFIX);

    public static final QName REQUIRED_ELEMENTS = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRED_ELEMENTS, SP11Constants.SP_PREFIX);

    public static final QName REQUIRED_PARTS = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRED_PARTS, SP11Constants.SP_PREFIX);

    public static final QName USERNAME_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.USERNAME_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName WSS_USERNAME_TOKEN10 = new QName(
            SP11Constants.SP_NS, SPConstants.USERNAME_TOKEN10, SP11Constants.SP_PREFIX);

    public static final QName WSS_USERNAME_TOKEN11 = new QName(
            SP11Constants.SP_NS, SPConstants.USERNAME_TOKEN11, SP11Constants.SP_PREFIX);

    public static final QName ENCRYPTION_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.ENCRYPTION_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName X509_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.X509_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_V1_TOKEN_10 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_V1_TOKEN10, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_V3_TOKEN_10 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_V3_TOKEN10, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_PKCS7_TOKEN_10 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_PKCS7_TOKEN10, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_PKI_PATH_V1_TOKEN_10 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_V1_TOKEN_11 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_V1_TOKEN11, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_V3_TOKEN_11 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_V3_TOKEN11, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_PKCS7_TOKEN_11 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_PKCS7_TOKEN11, SP11Constants.SP_PREFIX);

    public static final QName WSS_X509_PKI_PATH_V1_TOKEN_11 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11, SP11Constants.SP_PREFIX);

    public static final QName ISSUED_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.ISSUED_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName SUPPORTING_TOKENS = new QName(
            SP11Constants.SP_NS, SPConstants.SUPPORTING_TOKENS, SP11Constants.SP_PREFIX);

    public static final QName SIGNED_SUPPORTING_TOKENS = new QName(
            SP11Constants.SP_NS, SPConstants.SIGNED_SUPPORTING_TOKENS, SP11Constants.SP_PREFIX);

    public static final QName ENDORSING_SUPPORTING_TOKENS = new QName(
            SP11Constants.SP_NS, SPConstants.ENDORSING_SUPPORTING_TOKENS, SP11Constants.SP_PREFIX);

    public static final QName SIGNED_ENDORSING_SUPPORTING_TOKENS = new QName(
            SP11Constants.SP_NS, SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS, SP11Constants.SP_PREFIX);

    public static final QName PROTECTION_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.PROTECTION_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName ASYMMETRIC_BINDING = new QName(
            SP11Constants.SP_NS, SPConstants.ASYMMETRIC_BINDING, SP11Constants.SP_PREFIX);

    public static final QName SYMMETRIC_BINDING = new QName(
            SP11Constants.SP_NS, SPConstants.SYMMETRIC_BINDING, SP11Constants.SP_PREFIX);

    public static final QName INITIATOR_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.INITIATOR_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName INITIATOR_SIGNATURE_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.INITIATOR_SIGNATURE_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName INITIATOR_ENCRYPTION_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.INITIATOR_ENCRYPTION_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName RECIPIENT_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.RECIPIENT_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName RECIPIENT_SIGNATURE_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.RECIPIENT_SIGNATURE_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName RECIPIENT_ENCRYPTION_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.RECIPIENT_ENCRYPTION_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName ENCRYPT_SIGNATURE = new QName(
            SP11Constants.SP_NS, SPConstants.ENCRYPT_SIGNATURE, SP11Constants.SP_PREFIX);

    public static final QName PROTECT_TOKENS = new QName(
            SP11Constants.SP_NS, SPConstants.PROTECT_TOKENS, SP11Constants.SP_PREFIX);

    public static final QName ENCRYPT_BEFORE_SIGNING = new QName(
            SP11Constants.SP_NS, SPConstants.ENCRYPT_BEFORE_SIGNING, SP11Constants.SP_PREFIX);

    public static final QName SIGN_BEFORE_ENCRYPTING = new QName(
            SP11Constants.SP_NS, SPConstants.SIGN_BEFORE_ENCRYPTING, SP11Constants.SP_PREFIX);

    public static final QName ONLY_SIGN_ENTIRE_HEADERS_AND_BODY = new QName(
            SP11Constants.SP_NS, SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_KEY_IDENTIFIER_REFERENCE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_ISSUER_SERIAL_REFERENCE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_ISSUER_SERIAL_REFERENCE, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_EMBEDDED_TOKEN_REFERENCE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_EMBEDDED_TOKEN_REFERENCE, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_THUMBPRINT_REFERENCE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_THUMBPRINT_REFERENCE, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_REF_KEY_IDENTIFIER = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_REF_ISSUER_SERIAL = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_REF_EXTERNAL_URI = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_REF_EMBEDDED_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_REF_THUMBPRINT = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_REF_THUMBPRINT, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_REF_ENCRYPTED_KEY = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_REF_ENCRYPTED_KEY, SP11Constants.SP_PREFIX);

    public static final QName WSS10 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS10, SP11Constants.SP_PREFIX);

    public static final QName WSS11 = new QName(
            SP11Constants.SP_NS, SPConstants.WSS11, SP11Constants.SP_PREFIX);

    public static final QName TRUST_10 = new QName(
            SP11Constants.SP_NS, SPConstants.TRUST_10, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_SIGNATURE_CONFIRMATION = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_SIGNATURE_CONFIRMATION, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_CLIENT_CHALLENGE = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_CLIENT_CHALLENGE, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_SERVER_CHALLENGE = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_SERVER_CHALLENGE, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_CLIENT_ENTROPY = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_CLIENT_ENTROPY, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_SERVER_ENTROPY = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_SERVER_ENTROPY, SP11Constants.SP_PREFIX);

    public static final QName MUST_SUPPORT_ISSUED_TOKENS = new QName(
            SP11Constants.SP_NS, SPConstants.MUST_SUPPORT_ISSUED_TOKENS, SP11Constants.SP_PREFIX);

    public static final QName ISSUER = new QName(
            SP11Constants.SP_NS, SPConstants.ISSUER, SP11Constants.SP_PREFIX);

    public static final QName ISSUER_NAME = new QName(
            SP11Constants.SP_NS, SPConstants.ISSUER_NAME, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_DERIVED_KEYS = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_DERIVED_KEYS, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_EXTERNAL_URI_REFERNCE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_EXTERNAL_URI_REFERENCE, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_EXTERNAL_REFERNCE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_EXTERNAL_REFERENCE, SP11Constants.SP_PREFIX);

    public static final QName REQUIRE_INTERNAL_REFERENCE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUIRE_INTERNAL_REFERENCE, SP11Constants.SP_PREFIX);

    public static final QName REQUEST_SECURITY_TOKEN_TEMPLATE = new QName(
            SP11Constants.SP_NS, SPConstants.REQUEST_SECURITY_TOKEN_TEMPLATE, SP11Constants.SP_PREFIX);

    public static final QName SC10_SECURITY_CONTEXT_TOKEN = new QName(
            SP11Constants.SP_NS, SPConstants.SC10_SECURITY_CONTEXT_TOKEN, SP11Constants.SP_PREFIX);

    public static final QName BOOTSTRAP_POLICY = new QName(
            SP11Constants.SP_NS, SPConstants.BOOTSTRAP_POLICY, SP11Constants.SP_PREFIX);

    public static final QName XPATH = new QName(
            SP11Constants.SP_NS, SPConstants.XPATH_EXPR, SP11Constants.SP_PREFIX);

    public static final QName HEADER = new QName(
            SP11Constants.SP_NS, "Header", SP11Constants.SP_PREFIX);

    public static final QName BODY = new QName(
            SP11Constants.SP_NS, "Body", SP11Constants.SP_PREFIX);


    @Override
    public IncludeTokenType getInclusionFromAttributeValue(String value) throws IllegalArgumentException {
        if (value == null || value.length() == 0) {
            return IncludeTokenType.INCLUDE_TOKEN_ALWAYS;
        } else if (INCLUDE_ALWAYS.equals(value)) {
            return IncludeTokenType.INCLUDE_TOKEN_ALWAYS;
        } else if (INCLUDE_ALWAYS_TO_RECIPIENT.equals(value)) {
            return IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT;
        } else if (INCLUDE_ALWAYS_TO_INITIATOR.equals(value)) {
            return IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR;
        } else if (INCLUDE_NEVER.equals(value)) {
            return IncludeTokenType.INCLUDE_TOKEN_NEVER;
        } else if (INCLUDE_ONCE.equals(value)) {
            return IncludeTokenType.INCLUDE_TOKEN_ONCE;
        }
        throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
    }

    @Override
    public String getAttributeValueFromInclusion(IncludeTokenType value) throws IllegalArgumentException {
        switch (value) {
            case INCLUDE_TOKEN_ALWAYS:
                return SP11Constants.INCLUDE_ALWAYS;
            case INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT:
                return SP11Constants.INCLUDE_ALWAYS_TO_RECIPIENT;
            case INCLUDE_TOKEN_ALWAYS_TO_INITIATOR:
                return SP11Constants.INCLUDE_ALWAYS_TO_INITIATOR;
            case INCLUDE_TOKEN_NEVER:
                return SP11Constants.INCLUDE_NEVER;
            case INCLUDE_TOKEN_ONCE:
                return SP11Constants.INCLUDE_ONCE;
            default:
                throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
        }
    }

    @Override
    public QName getIncludeToken() {
        return INCLUDE_TOKEN;
    }

    @Override
    public QName getIssuer() {
        return ISSUER;
    }

    @Override
    public QName getIssuerName() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getClaims() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getX509Token() {
        return X509_TOKEN;
    }

    @Override
    public QName getRequireIssuerSerialReference() {
        return REQUIRE_ISSUER_SERIAL_REFERENCE;
    }

    @Override
    public QName getRequireEmbeddedTokenReference() {
        return REQUIRE_EMBEDDED_TOKEN_REFERENCE;
    }

    @Override
    public QName getRequireThumbprintReference() {
        return REQUIRE_THUMBPRINT_REFERENCE;
    }

    @Override
    public QName getHttpsToken() {
        return HTTPS_TOKEN;
    }

    @Override
    public QName getUsernameToken() {
        return USERNAME_TOKEN;
    }

    @Override
    public QName getCreated() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getNonce() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getIssuedToken() {
        return ISSUED_TOKEN;
    }

    @Override
    public QName getRequireExternalReference() {
        return REQUIRE_EXTERNAL_REFERNCE;
    }

    @Override
    public QName getRequireInternalReference() {
        return REQUIRE_INTERNAL_REFERENCE;
    }

    @Override
    public QName getRequestSecurityTokenTemplate() {
        return REQUEST_SECURITY_TOKEN_TEMPLATE;
    }

    @Override
    public QName getKerberosToken() {
        return KERBEROS_TOKEN;
    }

    @Override
    public QName getSpnegoContextToken() {
        return SPNEGO_CONTEXT_TOKEN;
    }

    @Override
    public QName getSecurityContextToken() {
        return SECURITY_CONTEXT_TOKEN;
    }

    @Override
    public QName getRequireExternalUriReference() {
        return REQUIRE_EXTERNAL_URI_REFERNCE;
    }

    @Override
    public QName getSc13SecurityContextToken() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getSc10SecurityContextToken() {
        return SC10_SECURITY_CONTEXT_TOKEN;
    }

    @Override
    public QName getSecureConversationToken() {
        return SECURE_CONVERSATION_TOKEN;
    }

    @Override
    public QName getMustNotSendCancel() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getMustNotSendAmend() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getMustNotSendRenew() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getBootstrapPolicy() {
        return BOOTSTRAP_POLICY;
    }

    @Override
    public QName getSamlToken() {
        return SAML_TOKEN;
    }

    @Override
    public QName getRelToken() {
        return REL_TOKEN;
    }

    @Override
    public QName getRequireKeyIdentifierReference() {
        return REQUIRE_KEY_IDENTIFIER_REFERENCE;
    }

    @Override
    public QName getKeyValueToken() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getRsaKeyValue() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getSignedParts() {
        return SIGNED_PARTS;
    }

    @Override
    public QName getSignedElements() {
        return SIGNED_ELEMENTS;
    }

    @Override
    public QName getXPathExpression() {
        return XPATH_EXPR;
    }

    @Override
    public QName getXPath2Expression() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getEncryptedParts() {
        return ENCRYPTED_PARTS;
    }

    @Override
    public QName getEncryptedElements() {
        return ENCRYPTED_ELEMENTS;
    }

    @Override
    public QName getContentEncryptedElements() {
        return CONTENT_ENCRYPTED_ELEMENTS;
    }

    @Override
    public QName getRequiredElements() {
        return REQUIRED_ELEMENTS;
    }

    @Override
    public QName getRequiredParts() {
        return REQUIRED_PARTS;
    }

    @Override
    public QName getAlgorithmSuite() {
        return ALGORITHM_SUITE;
    }

    @Override
    public QName getLayout() {
        return LAYOUT;
    }

    @Override
    public QName getBody() {
        return BODY;
    }

    @Override
    public QName getAttachments() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getContentSignatureTransform() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getAttachmentCompleteSignatureTransform() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getHeader() {
        return HEADER;
    }

    @Override
    public QName getEncryptSignature() {
        return ENCRYPT_SIGNATURE;
    }

    @Override
    public QName getProtectTokens() {
        return PROTECT_TOKENS;
    }

    @Override
    public QName getOnlySignEntireHeadersAndBody() {
        return ONLY_SIGN_ENTIRE_HEADERS_AND_BODY;
    }

    @Override
    public QName getTransportBinding() {
        return TRANSPORT_BINDING;
    }

    @Override
    public QName getSymmetricBinding() {
        return SYMMETRIC_BINDING;
    }

    @Override
    public QName getAsymmetricBinding() {
        return ASYMMETRIC_BINDING;
    }

    @Override
    public QName getEncryptionToken() {
        return ENCRYPTION_TOKEN;
    }

    @Override
    public QName getSignatureToken() {
        return SIGNATURE_TOKEN;
    }

    @Override
    public QName getProtectionToken() {
        return PROTECTION_TOKEN;
    }

    @Override
    public QName getTransportToken() {
        return TRANSPORT_TOKEN;
    }

    @Override
    public QName getInitiatorToken() {
        return INITIATOR_TOKEN;
    }

    @Override
    public QName getInitiatorSignatureToken() {
        return INITIATOR_SIGNATURE_TOKEN;
    }

    @Override
    public QName getInitiatorEncryptionToken() {
        return INITIATOR_ENCRYPTION_TOKEN;
    }

    @Override
    public QName getRecipientToken() {
        return RECIPIENT_TOKEN;
    }

    @Override
    public QName getRecipientSignatureToken() {
        return RECIPIENT_SIGNATURE_TOKEN;
    }

    @Override
    public QName getRecipientEncryptionToken() {
        return RECIPIENT_ENCRYPTION_TOKEN;
    }

    @Override
    public QName getTrust10() {
        return TRUST_10;
    }

    @Override
    public QName getTrust13() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getScopePolicy15() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getMustSupportClientChallenge() {
        return MUST_SUPPORT_CLIENT_CHALLENGE;
    }

    @Override
    public QName getMustSupportServerChallenge() {
        return MUST_SUPPORT_SERVER_CHALLENGE;
    }

    @Override
    public QName getRequireClientEntropy() {
        return REQUIRE_CLIENT_ENTROPY;
    }

    @Override
    public QName getRequireServerEntropy() {
        return REQUIRE_SERVER_ENTROPY;
    }

    @Override
    public QName getMustSupportIssuedTokens() {
        return MUST_SUPPORT_ISSUED_TOKENS;
    }

    @Override
    public QName getRequireRequestSecurityTokenCollection() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getRequireAppliesTo() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getMustSupportInteractiveChallenge() {
        return EMPTY_QNAME;
    }

    @Override
    public QName getWss10() {
        return WSS10;
    }

    @Override
    public QName getMustSupportRefKeyIdentifier() {
        return MUST_SUPPORT_REF_KEY_IDENTIFIER;
    }

    @Override
    public QName getMustSupportRefIssuerSerial() {
        return MUST_SUPPORT_REF_ISSUER_SERIAL;
    }

    @Override
    public QName getMustSupportRefExternalUri() {
        return MUST_SUPPORT_REF_EXTERNAL_URI;
    }

    @Override
    public QName getMustSupportRefEmbeddedToken() {
        return MUST_SUPPORT_REF_EMBEDDED_TOKEN;
    }

    @Override
    public QName getWss11() {
        return WSS11;
    }

    @Override
    public QName getMustSupportRefThumbprint() {
        return MUST_SUPPORT_REF_THUMBPRINT;
    }

    @Override
    public QName getMustSupportRefEncryptedKey() {
        return MUST_SUPPORT_REF_ENCRYPTED_KEY;
    }

    @Override
    public QName getRequireSignatureConfirmation() {
        return REQUIRE_SIGNATURE_CONFIRMATION;
    }
}
