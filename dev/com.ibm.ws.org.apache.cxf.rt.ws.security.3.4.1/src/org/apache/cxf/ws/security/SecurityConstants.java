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

package org.apache.cxf.ws.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration tags used to configure the WS-SecurityPolicy layer. Some of them are also
 * used by the non WS-SecurityPolicy approach in the WSS4J(Out|In)Interceptors.
 */
public final class SecurityConstants extends org.apache.cxf.rt.security.SecurityConstants {

    //
    // User properties
    //

    /**
     * The actor or role name of the wsse:Security header. If this parameter
     * is omitted, the actor name is not set.
     */
    public static final String ACTOR = "ws-security.actor";

    //
    // Boolean WS-Security configuration tags, e.g. the value should be "true" or "false".
    //

    /**
     * Whether to validate the password of a received UsernameToken or not. The default is true.
     */
    public static final String VALIDATE_TOKEN = "ws-security.validate.token";

    // WebLogic and WCF always encrypt UsernameTokens whenever possible
    //See:  http://e-docs.bea.com/wls/docs103/webserv_intro/interop.html
    //Be default, we will encrypt as well for interop reasons.  However, this
    //setting can be set to false to turn that off.
    /**
     * Whether to always encrypt UsernameTokens that are defined as a SupportingToken. The default
     * is true. This should not be set to false in a production environment, as it exposes the
     * password (or the digest of the password) on the wire.
     */
    public static final String ALWAYS_ENCRYPT_UT = "ws-security.username-token.always.encrypted";

    /**
     * Whether to ensure compliance with the Basic Security Profile (BSP) 1.1 or not. The
     * default value is "true".
     */
    public static final String IS_BSP_COMPLIANT = "ws-security.is-bsp-compliant";

    /**
     * Whether to cache UsernameToken nonces. The default value is "true" for message recipients, and
     * "false" for message initiators. Set it to true to cache for both cases. Set this to "false" to
     * not cache UsernameToken nonces. Note that caching only applies when either a UsernameToken
     * WS-SecurityPolicy is in effect, or else that a UsernameToken action has been configured
     * for the non-security-policy case.
     */
    public static final String ENABLE_NONCE_CACHE = "ws-security.enable.nonce.cache";

    /**
     * Whether to cache Timestamp Created Strings (these are only cached in conjunction with a message
     * Signature).The default value is "true" for message recipients, and "false" for message initiators.
     * Set it to true to cache for both cases. Set this to "false" to not cache Timestamp Created Strings.
     * Note that caching only applies when either a "IncludeTimestamp" policy is in effect, or
     * else that a Timestamp action has been configured for the non-security-policy case.
     */
    public static final String ENABLE_TIMESTAMP_CACHE = "ws-security.enable.timestamp.cache";

    /**
     * Whether to enable streaming WS-Security. If set to false (the default), the old DOM
     * implementation is used. If set to true, the new streaming (StAX) implementation is used.
     */
    public static final String ENABLE_STREAMING_SECURITY =
        "ws-security.enable.streaming";

    /**
     * Whether to return the security error message to the client, and not the default error message.
     * The "real" security errors should not be returned to the client in a deployment scenario,
     * as they may leak information about the deployment, or otherwise provide a "oracle" for attacks.
     * The default is false.
     */
    public static final String RETURN_SECURITY_ERROR = "ws-security.return.security.error";

    /**
     * Set this to "false" in order to remove the SOAP mustUnderstand header from security headers generated based on
     * a WS-SecurityPolicy.
     *
     * The default value is "true" which included the SOAP mustUnderstand header.
     */
    public static final String MUST_UNDERSTAND = "ws-security.must-understand";

    /**
     * Whether to cache SAML2 Token Identifiers, if the token contains a "OneTimeUse" Condition.
     * The default value is "true" for message recipients, and "false" for message initiators.
     * Set it to true to cache for both cases. Set this to "false" to not cache SAML2 Token Identifiers.
     * Note that caching only applies when either a "SamlToken" policy is in effect, or
     * else that a SAML action has been configured for the non-security-policy case.
     */
    public static final String ENABLE_SAML_ONE_TIME_USE_CACHE = "ws-security.enable.saml.cache";

    /**
     * Whether to store bytes (CipherData or BinarySecurityToken) in an attachment. The default is
     * true if MTOM is enabled. Set it to false to BASE-64 encode the bytes and "inlined" them in
     * the message instead. Setting this to true is more efficient, as it means that the BASE-64
     * encoding step can be skipped. This only applies to the DOM WS-Security stack.
     */
    public static final String STORE_BYTES_IN_ATTACHMENT = "ws-security.store.bytes.in.attachment";

    /**
     * This configuration flag allows the user to decide whether the default Attachment-Complete
     * transform or the Attachment-Content-Only transform should be used when an Attachment is encrypted
     * via a WS-SecurityPolicy expression. The default is "false", meaning that the "complete"
     * transformation is used.
     */
    public static final String USE_ATTACHMENT_ENCRYPTION_CONTENT_ONLY_TRANSFORM =
        "ws-security.swa.encryption.attachment.transform.content";

    /**
     * Whether to use the STR (Security Token Reference) Transform when (externally) signing a SAML Token.
     * The default is true. Some frameworks cannot handle processing the SecurityTokenReference is created,
     * hence set this configuration option to "false" in this case.
     */
    public static final String USE_STR_TRANSFORM = "ws-security.use.str.transform";

    /**
     * Whether to add an InclusiveNamespaces PrefixList as a CanonicalizationMethod child when generating
     * Signatures using WSConstants.C14N_EXCL_OMIT_COMMENTS. Default is "true".
     */
    public static final String ADD_INCLUSIVE_PREFIXES = "ws-security.add.inclusive.prefixes";

    /**
     * Whether to disable the enforcement of the WS-SecurityPolicy 'RequireClientCertificate' policy.
     * Default is "false". Some servers may not do client certificate verification at the start of the SSL
     * handshake, and therefore the client certs may not be available to the WS-Security layer for policy
     * verification at that time.
     */
    public static final String DISABLE_REQ_CLIENT_CERT_CHECK = "ws-security.disable.require.client.cert.check";

    /**
     * Whether to search for and expand xop:Include Elements for encryption and signature (on the outbound
     * side) or for signature verification (on the inbound side). This ensures that the actual bytes are signed,
     * and not just the reference. The default is "true" if MTOM is enabled, false otherwise.
     */
    public static final String EXPAND_XOP_INCLUDE = "ws-security.expand.xop.include";

    //
    // Non-boolean WS-Security Configuration parameters
    //

    /**
     * The time in seconds to append to the Creation value of an incoming Timestamp to determine
     * whether to accept the Timestamp as valid or not. The default value is 300 seconds (5 minutes).
     */
    public static final String TIMESTAMP_TTL = "ws-security.timestamp.timeToLive";

    /**
     * The time in seconds in the future within which the Created time of an incoming
     * Timestamp is valid. The default value is "60", to avoid problems where clocks are
     * slightly askew. To reject all future-created Timestamps, set this value to "0".
     */
    public static final String TIMESTAMP_FUTURE_TTL = "ws-security.timestamp.futureTimeToLive";

    /**
     * The time in seconds to append to the Creation value of an incoming UsernameToken to determine
     * whether to accept the UsernameToken as valid or not. The default value is 300 seconds (5 minutes).
     */
    public static final String USERNAMETOKEN_TTL = "ws-security.usernametoken.timeToLive";

    /**
     * The time in seconds in the future within which the Created time of an incoming
     * UsernameToken is valid. The default value is "60", to avoid problems where clocks are
     * slightly askew. To reject all future-created UsernameTokens, set this value to "0".
     */
    public static final String USERNAMETOKEN_FUTURE_TTL = "ws-security.usernametoken.futureTimeToLive";

    /**
     * The SpnegoClientAction implementation to use for SPNEGO. This allows the user to plug in
     * a different implementation to obtain a service ticket.
     */
    public static final String SPNEGO_CLIENT_ACTION = "ws-security.spnego.client.action";

    /**
     * This holds a reference to a ReplayCache instance used to cache UsernameToken nonces. The
     * default instance that is used is the EHCacheReplayCache.
     */
    public static final String NONCE_CACHE_INSTANCE =
            "ws-security.nonce.cache.instance";

    /**
     * This holds a reference to a ReplayCache instance used to cache Timestamp Created Strings. The
     * default instance that is used is the EHCacheReplayCache.
     */
    public static final String TIMESTAMP_CACHE_INSTANCE =
        "ws-security.timestamp.cache.instance";

    /**
     * This holds a reference to a ReplayCache instance used to cache SAML2 Token Identifiers, when
     * the token has a "OneTimeUse" Condition. The default instance that is used is the EHCacheReplayCache.
     */
    public static final String SAML_ONE_TIME_USE_CACHE_INSTANCE = "ws-security.saml.cache.instance";

    /**
     * Set this property to point to a configuration file for the underlying caching implementation for the
     * TokenStore. The default configuration file that is used is cxf-ehcache.xml in this module.
     */
    public static final String CACHE_CONFIG_FILE =
        "ws-security.cache.config.file";

    /**
     * The TokenStore instance to use to cache security tokens. By default this uses the
     * EHCacheTokenStore if EhCache is available. Otherwise it uses the MemoryTokenStore.
     */
    public static final String TOKEN_STORE_CACHE_INSTANCE =
        "org.apache.cxf.ws.security.tokenstore.TokenStore";

    /**
     * The Cache Identifier to use with the TokenStore. CXF uses the following key to retrieve a
     * token store: "org.apache.cxf.ws.security.tokenstore.TokenStore-<identifier>". This key can be
     * used to configure service-specific cache configuration. If the identifier does not match, then it
     * falls back to a cache configuration with key "org.apache.cxf.ws.security.tokenstore.TokenStore".
     *
     * The default "<identifier>" is the QName of the service in question. However to pick up a
     * custom cache configuration (for example, if you want to specify a TokenStore per-client proxy),
     * it can be configured with this identifier instead.
     */
    public static final String CACHE_IDENTIFIER = "ws-security.cache.identifier";

    /**
     * The Subject Role Classifier to use. If one of the WSS4J Validators returns a JAAS Subject
     * from Validation, then the WSS4JInInterceptor will attempt to create a SecurityContext
     * based on this Subject. If this value is not specified, then it tries to get roles using
     * the DefaultSecurityContext in cxf-rt-core. Otherwise it uses this value in combination
     * with the SUBJECT_ROLE_CLASSIFIER_TYPE to get the roles from the Subject.
     */
    public static final String SUBJECT_ROLE_CLASSIFIER = "ws-security.role.classifier";

    /**
     * The Subject Role Classifier Type to use. If one of the WSS4J Validators returns a JAAS Subject
     * from Validation, then the WSS4JInInterceptor will attempt to create a SecurityContext
     * based on this Subject. Currently accepted values are "prefix" or "classname". Must be
     * used in conjunction with the SUBJECT_ROLE_CLASSIFIER. The default value is "prefix".
     */
    public static final String SUBJECT_ROLE_CLASSIFIER_TYPE = "ws-security.role.classifier.type";

    /**
     * This configuration tag allows the user to override the default Asymmetric Signature
     * algorithm (RSA-SHA1) for use in WS-SecurityPolicy, as the WS-SecurityPolicy specification
     * does not allow the use of other algorithms at present.
     */
    public static final String ASYMMETRIC_SIGNATURE_ALGORITHM =
        "ws-security.asymmetric.signature.algorithm";

    /**
     * This configuration tag allows the user to override the default Symmetric Signature
     * algorithm (HMAC-SHA1) for use in WS-SecurityPolicy, as the WS-SecurityPolicy specification
     * does not allow the use of other algorithms at present.
     */
    public static final String SYMMETRIC_SIGNATURE_ALGORITHM =
        "ws-security.symmetric.signature.algorithm";

    /**
     * This holds a reference to a PasswordEncryptor instance, which is used to encrypt or
     * decrypt passwords in the Merlin Crypto implementation (or any custom Crypto implementations).
     *
     * By default, WSS4J uses the JasyptPasswordEncryptor, which must be instantiated with a
     * password to use to decrypt keystore passwords in the Merlin Crypto properties file.
     * This password is obtained via the CallbackHandler defined via PW_CALLBACK_CLASS
     * or PW_CALLBACK_REF.
     *
     * The encrypted passwords must be stored in the format "ENC(encoded encrypted password)".
     */
    public static final String PASSWORD_ENCRYPTOR_INSTANCE =
        "ws-security.password.encryptor.instance";

    /**
     * A delegated credential to use for WS-Security. Currently only a Kerberos GSSCredential
     * Object is supported. This is used to retrieve a service ticket instead of using the
     * client credentials.
     */
    public static final String DELEGATED_CREDENTIAL = "ws-security.delegated.credential";

    /**
     * A WSS4JSecurityContextCreator implementation that is used to create a CXF SecurityContext
     * from the set of WSS4J processing results. The default implementation is the
     * DefaultWSS4JSecurityContextCreator. This configuration tag allows the user to plug in
     * a custom way of setting up the CXF SecurityContext.
     */
    public static final String SECURITY_CONTEXT_CREATOR = "ws-security.security.context.creator";

    /**
     * The security token lifetime value (in milliseconds). The default is "300000" (5 minutes).
     */
    public static final String SECURITY_TOKEN_LIFETIME = "ws-security.security.token.lifetime";

    //
    // Validator implementations for validating received security tokens
    //

    /**
     * The WSS4J Validator instance to use to validate UsernameTokens. The default value is the
     * UsernameTokenValidator.
     */
    public static final String USERNAME_TOKEN_VALIDATOR = "ws-security.ut.validator";

    /**
     * The WSS4J Validator instance to use to validate SAML 1.1 Tokens. The default value is the
     * SamlAssertionValidator.
     */
    public static final String SAML1_TOKEN_VALIDATOR = "ws-security.saml1.validator";

    /**
     * The WSS4J Validator instance to use to validate SAML 2.0 Tokens. The default value is the
     * SamlAssertionValidator.
     */
    public static final String SAML2_TOKEN_VALIDATOR = "ws-security.saml2.validator";

    /**
     * The WSS4J Validator instance to use to validate Timestamps. The default value is the
     * TimestampValidator.
     */
    public static final String TIMESTAMP_TOKEN_VALIDATOR = "ws-security.timestamp.validator";

    /**
     * The WSS4J Validator instance to use to validate trust in credentials used in
     * Signature verification. The default value is the SignatureTrustValidator.
     */
    public static final String SIGNATURE_TOKEN_VALIDATOR = "ws-security.signature.validator";

    /**
     * The WSS4J Validator instance to use to validate BinarySecurityTokens. The default value
     * is the NoOpValidator.
     */
    public static final String BST_TOKEN_VALIDATOR = "ws-security.bst.validator";

    /**
     * The WSS4J Validator instance to use to validate SecurityContextTokens. The default value is
     * the NoOpValidator.
     */
    public static final String SCT_TOKEN_VALIDATOR = "ws-security.sct.validator";

    /**
     * This refers to a Map of QName, SecurityPolicyValidator, which retrieves a SecurityPolicyValidator
     * implementation to validate a particular security policy, based on the QName of the policy. Any
     * SecurityPolicyValidator implementation defined in this map will override the default value
     * used internally for the corresponding QName.
     */
    public static final String POLICY_VALIDATOR_MAP = "ws-security.policy.validator.map";

    //
    // Kerberos Configuration tags
    //

    /**
     * Whether to request credential delegation or not in the KerberosClient. If this is set to "true",
     * then it tries to get a kerberos service ticket that can be used for delegation. The default
     * is "false".
     */
    public static final String KERBEROS_REQUEST_CREDENTIAL_DELEGATION =
        "ws-security.kerberos.request.credential.delegation";

    /**
     * Whether to use credential delegation or not in the KerberosClient. If this is set to "true",
     * then it tries to get a GSSCredential Object from the Message Context using the
     * DELEGATED_CREDENTIAL configuration tag below, and then use this to obtain a service ticket.
     * The default is "false".
     */
    public static final String KERBEROS_USE_CREDENTIAL_DELEGATION =
        "ws-security.kerberos.use.credential.delegation";

    /**
     * Whether the Kerberos username is in servicename form or not. The default is "false".
     */
    public static final String KERBEROS_IS_USERNAME_IN_SERVICENAME_FORM =
        "ws-security.kerberos.is.username.in.servicename.form";

    /**
     * The JAAS Context name to use for Kerberos.
     */
    public static final String KERBEROS_JAAS_CONTEXT_NAME = "ws-security.kerberos.jaas.context";

    /**
     * The Kerberos Service Provider Name (spn) to use.
     */
    public static final String KERBEROS_SPN = "ws-security.kerberos.spn";

    /**
     * A reference to the KerberosClient class used to obtain a service ticket.
     */
    public static final String KERBEROS_CLIENT = "ws-security.kerberos.client";

    /**
     * Prefix of all constants meant to change customAlgSuite.
     */
    public static final String CUSTOM_ALG_SUITE_PREFIX = "ws-security.custom.alg.suite.";

    /**
     * Default value is: http://www.w3.org/2001/04/xmlenc#sha256
     */
    public static final String CUSTOM_ALG_SUITE_DIGEST_ALGORITHM = "ws-security.custom.alg.suite.digest.algorithm";

    /**
     * Default value is: http://www.w3.org/2009/xmlenc11#aes256-gcm
     */
    public static final String CUSTOM_ALG_SUITE_ENCRYPTION_ALGORITHM =
        "ws-security.custom.alg.suite.encryption.algorithm";

    /**
     * Default value is: http://www.w3.org/2001/04/xmlenc#kw-aes256
     */
    public static final String CUSTOM_ALG_SUITE_SYMMETRIC_KEY_ENCRYPTION_ALGORITHM =
        "ws-security.custom.alg.suite.symmetric.key.encryption.algorithm";

    /**
     * Default value is: http://www.w3.org/2001/04/xmlenc#rsa-1_5
     */
    public static final String CUSTOM_ALG_SUITE_ASYMMETRIC_KEY_ENCRYPTION_ALGORITHM =
            "ws-security.custom.alg.suite.asymmetric.key.encryption.algorithm";

    /**
     * hDefault value is: ttp://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1
     */
    public static final String CUSTOM_ALG_SUITE_ENCRYPTION_KEY_DERIVATION =
        "ws-security.custom.alg.suite.encryption.key.derivation";

    /**
     * Default value is: http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1
     */
    public static final String CUSTOM_ALG_SUITE_SIGNATURE_KEY_DERIVATION =
        "ws-security.custom.alg.suite.signature.key.derivation";

    /**
     * Default value is: http://www.w3.org/2000/09/xmldsig#hmac-sha1"
     */
    public static final String CUSTOM_ALG_SUITE_SYMMETRIC_SIGNATURE =
        "ws-security.custom.alg.suite.symmetric.signature";

    /**
     * Default value is: http://www.w3.org/2000/09/xmldsig#rsa-sha1",
     */
    public static final String CUSTOM_ALG_SUITE_ASYMMETRIC_SIGNATURE =
        "ws-security.custom.alg.suite.asymmetric.signature";

    /**
     * Default value is: 256
     */
    public static final String CUSTOM_ALG_SUITE_ENCRYPTION_DERIVED_KEY_LENGTH =
        "ws-security.custom.alg.suite.encryption.derived.key.length";

    /**
     * Default value is: 192
     */
    public static final String CUSTOM_ALG_SUITE_SIGNATURE_DERIVED_KEY_LENGTH =
        "ws-security.custom.alg.suite.signature.derived.key.length";

    /**
     * Default value is: 256
     */
    public static final String CUSTOM_ALG_SUITE_MINIMUM_SYMMETRIC_KEY_LENGTH =
        "ws-security.custom.alg.suite.minimum.symmetric.key.length";

    /**
     * Default value is: 256
     */
    public static final String CUSTOM_ALG_SUITE_MAXIMUM_SYMMETRIC_KEY_LENGTH =
        "ws-security.custom.alg.suite.maximum.symmetric.key.length";

    /**
     * Default value is: 1024
     */
    public static final String CUSTOM_ALG_SUITE_MINIMUM_ASYMMETRIC_KEY_LENGTH =
        "ws-security.custom.alg.suite.minimum.asymmetric.key.length";

    /**
     * Default value is: 4096
     */
    public static final String CUSTOM_ALG_SUITE_MAXIMUM_ASYMMETRIC_KEY_LENGTH =
        "ws-security.custom.alg.suite.maximum.asymmetric.key.length";

    //
    // Internal tags
    //

    public static final String TOKEN = "ws-security.token";
    public static final String TOKEN_ID = "ws-security.token.id";
    public static final String TOKEN_ELEMENT = "ws-security.token.element";

    public static final Set<String> ALL_PROPERTIES;

    static {
        Set<String> s = new HashSet<>(Arrays.asList(new String[] {
            ACTOR, VALIDATE_TOKEN, ALWAYS_ENCRYPT_UT, IS_BSP_COMPLIANT, ENABLE_NONCE_CACHE,
            ENABLE_TIMESTAMP_CACHE, TIMESTAMP_TTL, TIMESTAMP_FUTURE_TTL,
            KERBEROS_CLIENT, SPNEGO_CLIENT_ACTION, KERBEROS_JAAS_CONTEXT_NAME, KERBEROS_SPN,
            NONCE_CACHE_INSTANCE, TIMESTAMP_CACHE_INSTANCE, CACHE_CONFIG_FILE,
            TOKEN_STORE_CACHE_INSTANCE, USERNAME_TOKEN_VALIDATOR, SAML1_TOKEN_VALIDATOR,
            SAML2_TOKEN_VALIDATOR, TIMESTAMP_TOKEN_VALIDATOR, SIGNATURE_TOKEN_VALIDATOR,
            BST_TOKEN_VALIDATOR, SCT_TOKEN_VALIDATOR, TOKEN, TOKEN_ID, SUBJECT_ROLE_CLASSIFIER,
            SUBJECT_ROLE_CLASSIFIER_TYPE, MUST_UNDERSTAND, ASYMMETRIC_SIGNATURE_ALGORITHM,
            PASSWORD_ENCRYPTOR_INSTANCE, ENABLE_SAML_ONE_TIME_USE_CACHE,
            SAML_ONE_TIME_USE_CACHE_INSTANCE, ENABLE_STREAMING_SECURITY, RETURN_SECURITY_ERROR,
            CACHE_IDENTIFIER, DELEGATED_CREDENTIAL, KERBEROS_USE_CREDENTIAL_DELEGATION,
            KERBEROS_IS_USERNAME_IN_SERVICENAME_FORM, KERBEROS_REQUEST_CREDENTIAL_DELEGATION,
            POLICY_VALIDATOR_MAP, STORE_BYTES_IN_ATTACHMENT, USE_ATTACHMENT_ENCRYPTION_CONTENT_ONLY_TRANSFORM,
            SYMMETRIC_SIGNATURE_ALGORITHM, SECURITY_CONTEXT_CREATOR, SECURITY_TOKEN_LIFETIME,
            DISABLE_REQ_CLIENT_CERT_CHECK, EXPAND_XOP_INCLUDE,
            CUSTOM_ALG_SUITE_MAXIMUM_ASYMMETRIC_KEY_LENGTH,
            CUSTOM_ALG_SUITE_MINIMUM_ASYMMETRIC_KEY_LENGTH,
            CUSTOM_ALG_SUITE_MAXIMUM_SYMMETRIC_KEY_LENGTH,
            CUSTOM_ALG_SUITE_MINIMUM_SYMMETRIC_KEY_LENGTH,
            CUSTOM_ALG_SUITE_SIGNATURE_DERIVED_KEY_LENGTH,
            CUSTOM_ALG_SUITE_ENCRYPTION_DERIVED_KEY_LENGTH,
            CUSTOM_ALG_SUITE_SIGNATURE_KEY_DERIVATION,
            CUSTOM_ALG_SUITE_ENCRYPTION_KEY_DERIVATION,
            CUSTOM_ALG_SUITE_ASYMMETRIC_KEY_ENCRYPTION_ALGORITHM,
            CUSTOM_ALG_SUITE_SYMMETRIC_KEY_ENCRYPTION_ALGORITHM,
            CUSTOM_ALG_SUITE_ENCRYPTION_ALGORITHM, CUSTOM_ALG_SUITE_DIGEST_ALGORITHM
        }));
        for (String commonProperty : COMMON_PROPERTIES) {
            s.add(commonProperty);
            s.add("ws-" + commonProperty);
        }
        ALL_PROPERTIES = Collections.unmodifiableSet(s);
    }

    private SecurityConstants() {
        //utility class
    }
}
