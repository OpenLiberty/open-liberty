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

package org.apache.cxf.rt.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains some configuration tags that can be used to configure various security properties. These
 * tags are shared between the SOAP stack (WS-SecurityPolicy configuration), as well as the REST stack (JAX-RS
 * XML Security).
 *
 * The configuration tags largely relate to properties for signing, encryption as well as SAML tokens. Most of
 * the signing/encryption tags refer to Apache WSS4J "Crypto" objects, which are used by both stacks to control
 * how certificates/keys are retrieved, etc.
 *
 * More specific configuration tags for WS-SecurityPolicy are configured in the SecurityConstants
 * class in the cxf-rt-ws-security module, which extends this class.
 */
public class SecurityConstants {

    //
    // User properties
    //

    /**
     * The user's name. It is used as follows:
     * a) As the name in the UsernameToken for WS-Security.
     * b) As the alias name in the keystore to get the user's cert and private key for signature
     *    if {@link SIGNATURE_USERNAME} is not set.
     * c) As the alias name in the keystore to get the user's public key for encryption if
     *    {@link ENCRYPT_USERNAME} is not set.
     */
    public static final String USERNAME = "security.username";

    /**
     * The user's password when a {@link CALLBACK_HANDLER} is not defined. This is only used for the password
     * in a WS-Security UsernameToken.
     */
    public static final String PASSWORD = "security.password";

    /**
     * The user's name for signature. It is used as the alias name in the keystore to get the user's cert
     * and private key for signature. If this is not defined, then {@link USERNAME} is used instead. If
     * that is also not specified, it uses the the default alias set in the properties file referenced by
     * {@link SIGNATURE_PROPERTIES}. If that's also not set, and the keystore only contains a single key,
     * that key will be used.
     */
    public static final String SIGNATURE_USERNAME = "security.signature.username";

    /**
     * The user's password for signature when a {@link CALLBACK_HANDLER} is not defined.
     */
    public static final String SIGNATURE_PASSWORD = "security.signature.password";

    /**
     * The user's name for encryption. It is used as the alias name in the keystore to get the user's public
     * key for encryption. If this is not defined, then {@link USERNAME} is used instead. If
     * that is also not specified, it uses the the default alias set in the properties file referenced by
     * {@link ENCRYPT_PROPERTIES}. If that's also not set, and the keystore only contains a single key,
     * that key will be used.
     *
     * For the WS-Security web service provider, the "useReqSigCert" keyword can be used to accept (encrypt to)
     * any client whose public key is in the service's truststore (defined in {@link ENCRYPT_PROPERTIES}).
     */
    public static final String ENCRYPT_USERNAME = "security.encryption.username";

    //
    // Callback class and Crypto properties
    //

    /**
     * The CallbackHandler implementation class used to obtain passwords, for both outbound and inbound
     * requests. The value of this tag must be either:
     * a) The class name of a {@link javax.security.auth.callback.CallbackHandler} instance, which must
     * be accessible via the classpath.
     * b) A {@link javax.security.auth.callback.CallbackHandler} instance.
     */
    public static final String CALLBACK_HANDLER = "security.callback-handler";

    /**
     * The SAML CallbackHandler implementation class used to construct SAML Assertions. The value of this
     * tag must be either:
     * a) The class name of a {@link javax.security.auth.callback.CallbackHandler} instance, which must
     * be accessible via the classpath.
     * b) A {@link javax.security.auth.callback.CallbackHandler} instance.
     */
    public static final String SAML_CALLBACK_HANDLER = "security.saml-callback-handler";

    /**
     * The Crypto property configuration to use for signature, if {@link SIGNATURE_CRYPTO} is not set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String SIGNATURE_PROPERTIES = "security.signature.properties";

    /**
     * The Crypto property configuration to use for encryption, if {@link ENCRYPT_CRYPTO} is not set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String ENCRYPT_PROPERTIES = "security.encryption.properties";

    /**
     * A Crypto object to be used for signature. If this is not defined then the
     * {@link SIGNATURE_PROPERTIES} is used instead.
     */
    public static final String SIGNATURE_CRYPTO = "security.signature.crypto";

    /**
     * A Crypto object to be used for encryption. If this is not defined then the
     * {@link ENCRYPT_PROPERTIES} is used instead.
     */
    public static final String ENCRYPT_CRYPTO = "security.encryption.crypto";

    /**
     * A message property for prepared X509 certificate to be used for encryption.
     * If this is not defined, then the certificate will be either loaded from the
     * keystore {@link ENCRYPT_PROPERTIES} or extracted from request (when WS-Security is used and
     * if {@link ENCRYPT_USERNAME} has value "useReqSigCert").
     */
    public static final String ENCRYPT_CERT = "security.encryption.certificate";

    //
    // Boolean Security configuration tags, e.g. the value should be "true" or "false".
    //

    /**
     * Whether to enable Certificate Revocation List (CRL) checking or not when verifying trust
     * in a certificate. The default value is "false".
     */
    public static final String ENABLE_REVOCATION = "security.enableRevocation";

    /**
     * Whether to allow unsigned saml assertions as SecurityContext Principals. The default is false.
     * Note that "unsigned" refers to an internal signature. Even if the token is signed by an
     * external signature (as per the "sender-vouches" requirement), this boolean must still be
     * configured if you want to use the token to set up the security context.
     */
    public static final String ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL =
            "security.enable.unsigned-saml-assertion.principal";

    /**
     * Whether to allow UsernameTokens with no password to be used as SecurityContext Principals.
     * The default is false.
     */
    public static final String ENABLE_UT_NOPASSWORD_PRINCIPAL =
            "security.enable.ut-no-password.principal";

    /**
     * Whether to validate the SubjectConfirmation requirements of a received SAML Token
     * (sender-vouches or holder-of-key). The default is true.
     */
    public static final String VALIDATE_SAML_SUBJECT_CONFIRMATION =
        "security.validate.saml.subject.conf";

    /**
     * Set this to "false" if security context must not be created from JAAS Subject.
     *
     * The default value is "true".
     */
    public static final String SC_FROM_JAAS_SUBJECT = "security.sc.jaas-subject";

    /**
     * Enable SAML AudienceRestriction validation. If this is set to "true", then IF the
     * SAML Token contains Audience Restriction URIs, one of them must match one of the values of the
     * AUDIENCE_RESTRICTIONS property. The default is "true" for SOAP services, "false" for REST services.
     */
    public static final String AUDIENCE_RESTRICTION_VALIDATION = "security.validate.audience-restriction";

    //
    // Non-boolean WS-Security Configuration parameters
    //

    /**
     * The attribute URI of the SAML AttributeStatement where the role information is stored.
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME = "security.saml-role-attributename";

    /**
     * A String of regular expressions (separated by the value specified for CERT_CONSTRAINTS_SEPARATOR)
     * which will be applied to the subject DN of the certificate used for signature validation, after trust
     * verification of the certificate chain associated with the certificate.
     */
    public static final String SUBJECT_CERT_CONSTRAINTS = "security.subject.cert.constraints";

    /**
     * The separator that is used to parse certificate constraints configured in the SUBJECT_CERT_CONSTRAINTS
     * tag. By default it is a comma - ",".
     */
    public static final String CERT_CONSTRAINTS_SEPARATOR = "security.cert.constraints.separator";

    //
    // STS Client Configuration tags
    //

    /**
     * A reference to the STSClient class used to communicate with the STS.
     */
    public static final String STS_CLIENT = "security.sts.client";

    /**
     * The "AppliesTo" address to send to the STS. The default is the endpoint address of the
     * service provider.
     */
    public static final String STS_APPLIES_TO = "security.sts.applies-to";

    /**
     * Whether to write out an X509Certificate structure in UseKey/KeyInfo, or whether to write
     * out a KeyValue structure. The default value is "false".
     */
    public static final String STS_TOKEN_USE_CERT_FOR_KEYINFO = "security.sts.token.usecert";

    /**
     * Whether to cancel a token when using SecureConversation after successful invocation. The
     * default is "false".
     */
    public static final String STS_TOKEN_DO_CANCEL = "security.sts.token.do.cancel";

    /**
     * Whether to fall back to calling "issue" after failing to renew an expired token. Some
     * STSs do not support the renew binding, and so we should just issue a new token after expiry.
     * The default is true.
     */
    public static final String STS_ISSUE_AFTER_FAILED_RENEW = "security.issue.after.failed.renew";

    /**
     * Set this to "false" to not cache a SecurityToken per proxy object in the
     * IssuedTokenInterceptorProvider. This should be done if a token is being retrieved
     * from an STS in an intermediary. The default value is "true".
     */
    public static final String CACHE_ISSUED_TOKEN_IN_ENDPOINT =
        "security.cache.issued.token.in.endpoint";

    /**
     * Whether to avoid STS client trying send WS-MetadataExchange call using
     * STS EPR WSA address when the endpoint contract contains no WS-MetadataExchange info.
     * The default value is "false".
     */
    public static final String DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS =
        "security.sts.disable-wsmex-call-using-epr-address";

    /**
     * Whether to prefer to use WS-MEX over a STSClient's location/wsdlLocation properties
     * when making an STS RequestSecurityToken call. This can be set to true for the scenario
     * of making a WS-MEX call to an initial STS, and using the returned token to make another
     * call to an STS (which is configured using the STSClient configuration). Default is
     * "false".
     */
    public static final String PREFER_WSMEX_OVER_STS_CLIENT_CONFIG =
        "security.sts.prefer-wsmex";

    /**
     * Switch STS client to send Soap 1.2 messages
     */
    public static final String STS_CLIENT_SOAP12_BINDING =
        "security.sts.client-soap12-binding";

    /**
     *
     * A Crypto object to be used for the STS. If this is not defined then the
     * {@link STS_TOKEN_PROPERTIES} is used instead.
     *
     * WCF's trust server sometimes will encrypt the token in the response IN ADDITION TO
     * the full security on the message. These properties control the way the STS client
     * will decrypt the EncryptedData elements in the response.
     *
     * These are also used by the STSClient to send/process any RSA/DSAKeyValue tokens
     * used if the KeyType is "PublicKey"
     */
    public static final String STS_TOKEN_CRYPTO = "security.sts.token.crypto";

    /**
     * The Crypto property configuration to use for the STS, if {@link STS_TOKEN_CRYPTO} is not
     * set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String STS_TOKEN_PROPERTIES = "security.sts.token.properties";

    /**
     * The alias name in the keystore to get the user's public key to send to the STS for the
     * PublicKey KeyType case.
     */
    public static final String STS_TOKEN_USERNAME = "security.sts.token.username";

    /**
     * The token to be sent to the STS in an "ActAs" field. It can be either:
     * a) A String (which must be an XML statement like "<wst:OnBehalfOf xmlns:wst=...>...</wst:OnBehalfOf>")
     * b) A DOM Element
     * c) A CallbackHandler object to use to obtain the token
     *
     * In the case of a CallbackHandler, it must be able to handle a
     * org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object, which contains a
     * reference to the current Message. The CallbackHandler implementation is required to set
     * the token Element to be sent in the request on the Callback.
     *
     * Some examples that can be reused are:
     * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
     * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
     */
    public static final String STS_TOKEN_ACT_AS = "security.sts.token.act-as";

    /**
     * The token to be sent to the STS in an "OnBehalfOf" field. It can be either:
     * a) A String (which must be an XML statement like "<wst:OnBehalfOf xmlns:wst=...>...</wst:OnBehalfOf>")
     * b) A DOM Element
     * c) A CallbackHandler object to use to obtain the token
     *
     * In the case of a CallbackHandler, it must be able to handle a
     * org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object, which contains a
     * reference to the current Message. The CallbackHandler implementation is required to set
     * the token Element to be sent in the request on the Callback.
     *
     * Some examples that can be reused are:
     * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
     * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
     */
    public static final String STS_TOKEN_ON_BEHALF_OF = "security.sts.token.on-behalf-of";

    /**
     * This is the value in seconds within which a token is considered to be expired by the
     * client. When a cached token (from a STS) is retrieved by the client, it is considered
     * to be expired if it will expire in a time less than the value specified by this tag.
     * This prevents token expiry when the message is en route / being processed by the
     * service. When the token is found to be expired then it will be renewed via the STS.
     *
     * The default value is 10 (seconds). Specify 0 to avoid this check.
     */
    public static final String STS_TOKEN_IMMINENT_EXPIRY_VALUE =
        "security.sts.token.imminent-expiry-value";

    /**
     * An implementation of the STSTokenCacher interface, if you want to plug in custom caching behaviour for
     * STS clients. The default value is the DefaultSTSTokenCacher.
     */
    public static final String STS_TOKEN_CACHER_IMPL =
        "security.sts.token.cacher.impl";

    /**
     * Check that we are not invoking on the STS using its own IssuedToken policy - in which case we
     * will end up with a recursive loop. This check might be a problem in the unlikely scenario that the
     * remote endpoint has the same service / port QName as the STS, so this configuration flag allows to
     * disable this check for that scenario. The default is "true".
     */
    public static final String STS_CHECK_FOR_RECURSIVE_CALL =
        "security.sts.check.for.recursive.call";

    /**
     * This property contains a comma separated String corresponding to a list of audience restriction URIs.
     * The default value for this property contains the request URL and the Service QName. If the
     * AUDIENCE_RESTRICTION_VALIDATION property is "true", and if a received SAML Token contains audience
     * restriction URIs, then one of them must match one of the values specified in this property.
     */
    public static final String AUDIENCE_RESTRICTIONS = "security.audience-restrictions";

    public static final Set<String> COMMON_PROPERTIES;

    static {
        Set<String> s = new HashSet<>(Arrays.asList(new String[] {
            USERNAME, PASSWORD, SIGNATURE_USERNAME, ENCRYPT_USERNAME,
            CALLBACK_HANDLER, SAML_CALLBACK_HANDLER, SIGNATURE_PROPERTIES,
            SIGNATURE_CRYPTO, ENCRYPT_PROPERTIES, ENCRYPT_CRYPTO, ENCRYPT_CERT,
            ENABLE_REVOCATION, SUBJECT_CERT_CONSTRAINTS, ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL,
            ENABLE_UT_NOPASSWORD_PRINCIPAL,
            AUDIENCE_RESTRICTION_VALIDATION, SAML_ROLE_ATTRIBUTENAME,
            ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL, SC_FROM_JAAS_SUBJECT,
            STS_TOKEN_USE_CERT_FOR_KEYINFO, STS_TOKEN_DO_CANCEL, CACHE_ISSUED_TOKEN_IN_ENDPOINT,
            DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS, STS_TOKEN_CRYPTO,
            STS_TOKEN_PROPERTIES, STS_TOKEN_USERNAME, STS_TOKEN_ACT_AS, STS_TOKEN_ON_BEHALF_OF,
            STS_CLIENT, STS_APPLIES_TO, CACHE_ISSUED_TOKEN_IN_ENDPOINT, PREFER_WSMEX_OVER_STS_CLIENT_CONFIG,
            STS_TOKEN_IMMINENT_EXPIRY_VALUE, STS_TOKEN_CACHER_IMPL, AUDIENCE_RESTRICTIONS,
            STS_CHECK_FOR_RECURSIVE_CALL
        }));
        COMMON_PROPERTIES = Collections.unmodifiableSet(s);
    }

    protected SecurityConstants() {
        // complete
    }
}
