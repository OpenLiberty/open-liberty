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

package org.apache.wss4j.dom.engine;

import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.dom.message.token.SignatureConfirmation;
import org.apache.wss4j.dom.message.token.Timestamp;
import org.apache.wss4j.dom.message.token.UsernameToken;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.List;


public class WSSecurityEngineResult extends java.util.HashMap<String, Object> {

    //
    // Tokens
    //

    /**
     *
     */
    private static final long serialVersionUID = 8877354445092724300L;

    /**
     * Tag denoting the SAML Assertion found, if applicable.
     *
     * The value under this tag is of type SamlAssertionWrapper.
     */
    public static final String TAG_SAML_ASSERTION = "saml-assertion";

    /**
     * Tag denoting the timestamp found, if applicable.
     *
     * The value under this tag is of type
     * org.apache.wss4j.dom.message.token.Timestamp.
     */
    public static final String TAG_TIMESTAMP = "timestamp";

    /**
     * Tag denoting references to the DOM elements that have been
     * cryptographically protected.
     *
     * The value under this tag is of type SecurityContextToken.
     */
    public static final String TAG_SECURITY_CONTEXT_TOKEN = "security-context-token";

    /**
     * Tag denoting a UsernameToken object
     */
    public static final String TAG_USERNAME_TOKEN = "username-token";

    /**
     * Tag denoting a DerivedKeyToken object
     */
    public static final String TAG_DERIVED_KEY_TOKEN = "derived-key-token";

    /**
     * Tag denoting the signature confirmation of a signed element,
     * if applicable.
     *
     * The value under this tag is of type
     * org.apache.wss4j.dom.message.token.SignatureConfirmation.
     */
    public static final String TAG_SIGNATURE_CONFIRMATION = "signature-confirmation";

    /**
     * Tag denoting the BinarySecurityToken found, if applicable.
     *
     * The value under this tag is of type BinarySecurity.
     */
    public static final String TAG_BINARY_SECURITY_TOKEN = "binary-security-token";

    /**
     * Tag denoting a Transformed Token. For certain tokens, the Validator may return
     * an SamlAssertionWrapper instance which corresponds to a transformed version of the
     * initial token. For example, a Username Token credential might be validated
     * by an STS and transformed into a SAML Assertion. This tag then holds the
     * transformed SamlAssertionWrapper instance, as a component of the Result corresponding
     * to the Username Token.
     *
     * The value under this tag is of type SamlAssertionWrapper.
     */
    public static final String TAG_TRANSFORMED_TOKEN = "transformed-token";

    /**
     * Tag denoting that the TAG_*_TOKEN result has been validated by a Validator
     * implementation. Some of the processors do not have a default validator implementation,
     * and so this is not set. Note that this is set for the NoOpValidator if it is
     * configured.
     *
     * The value under this tag is a Boolean instance.
     */
    public static final String TAG_VALIDATED_TOKEN = "validated-token";

    /**
     * Tag denoting the DOM Element of the processed token (if a token has been processed).
     *
     * The value under this tag is of type org.w3c.dom.Element
     */
    public static final String TAG_TOKEN_ELEMENT = "token-element";

    //
    // Keys and certs
    //

    /**
     * Tag denoting the X.509 certificate found, if applicable.
     *
     * The value under this tag is of type java.security.cert.X509Certificate.
     */
    public static final String TAG_X509_CERTIFICATE = "x509-certificate";

    /**
     * Tag denoting the signature value of a signed element, if applicable.
     *
     * The value under this tag is of type byte[].
     */
    public static final String TAG_SIGNATURE_VALUE = "signature-value";

    /**
     * Tag denoting the X.509 certificate chain found, if applicable.
     *
     * The value under this tag is of type java.security.cert.X509Certificate[].
     */
    public static final String TAG_X509_CERTIFICATES = "x509-certificates";

    /**
     * Tag denoting how the X.509 certificate (chain) was referenced, if applicable.
     *
     * The value under this tag is of type STRParser.REFERENCE_TYPE.
     */
    public static final String TAG_X509_REFERENCE_TYPE = "x509-reference-type";

    /**
     * Tag denoting the encrypted key bytes
     *
     * The value under this tag is a byte array
     */
    public static final String TAG_ENCRYPTED_EPHEMERAL_KEY = "encrypted-ephemeral-key-bytes";

    /**
     * Tag denoting a byte[] secret associated with this token
     */
    public static final String TAG_SECRET = "secret";

    /**
     * Tag denoting a PublicKey associated with this token
     */
    public static final String TAG_PUBLIC_KEY = "public-key";

    //
    // General tags
    //

    /**
     * Tag denoting the cryptographic operation performed
     *
     * The value under this tag is of type java.lang.Integer
     */
    public static final String TAG_ACTION = "action";

    /**
     * Tag denoting the security principal found, if applicable.
     *
     * The value under this tag is of type java.security.Principal.
     */
    public static final String TAG_PRINCIPAL = "principal";

    /**
     * Tag denoting the security subject found, if applicable.
     *
     * The value under this tag is of type javax.security.auth.Subject.
     */
    public static final String TAG_SUBJECT = "subject";

    /**
     * Tag denoting references to a List of Data ref URIs.
     *
     * The value under this tag is of type List.
     */
    public static final String TAG_DATA_REF_URIS = "data-ref-uris";

    /**
     * Tag denoting the encrypted key transport algorithm.
     *
     * The value under this tag is of type String.
     */
    public static final String TAG_ENCRYPTED_KEY_TRANSPORT_METHOD = "encrypted-key-transport-method";

    /**
     * Tag denoting the algorithm that was used to sign the message
     *
     * The value under this tag is of type String.
     */
    public static final String TAG_SIGNATURE_METHOD = "signature-method";

    /**
     * Tag denoting the algorithm that was used to do canonicalization
     *
     * The value under this tag is of type String.
     */
    public static final String TAG_CANONICALIZATION_METHOD = "canonicalization-method";

    /**
     * Tag denoting a delegation credential found, if applicable.
     *
     * For Kerberos (if delegation is enabled), the value under this tag is of type GSSCredential
     */
    public static final String TAG_DELEGATION_CREDENTIAL = "delegation-credential";

    /**
     * The (wsu) Id of the token corresponding to this result.
     */
    public static final String TAG_ID = "id";

    public WSSecurityEngineResult(int act) {
        put(TAG_ACTION, act);
    }

    public WSSecurityEngineResult(
        int act,
        SamlAssertionWrapper ass
    ) {
        put(TAG_ACTION, act);
        put(TAG_SAML_ASSERTION, ass);
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
        put(TAG_TOKEN_ELEMENT, ass.getElement());
    }

    public WSSecurityEngineResult(
        int act,
        Principal princ,
        X509Certificate[] certs,
        byte[] sv
    ) {
        put(TAG_ACTION, act);
        put(TAG_PRINCIPAL, princ);
        put(TAG_X509_CERTIFICATES, certs);
        put(TAG_SIGNATURE_VALUE, sv);
        if (certs != null && certs.length > 0) {
            put(TAG_X509_CERTIFICATE, certs[0]);
        }
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
    }

    public
    WSSecurityEngineResult(
        int act,
        Principal princ,
        X509Certificate[] certs,
        List<WSDataRef> dataRefs,
        byte[] sv
    ) {
        this(act, princ, certs, sv);
        put(TAG_DATA_REF_URIS, dataRefs);
    }

    public WSSecurityEngineResult(
        int act,
        byte[] decryptedKey,
        byte[] encryptedKeyBytes,
        List<WSDataRef> dataRefUris
    ) {
        put(TAG_ACTION, act);
        put(TAG_SECRET, decryptedKey);
        put(TAG_ENCRYPTED_EPHEMERAL_KEY, encryptedKeyBytes);
        put(TAG_DATA_REF_URIS, dataRefUris);
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
    }

    public WSSecurityEngineResult(
        int act,
        byte[] decryptedKey,
        byte[] encryptedKeyBytes,
        List<WSDataRef> dataRefUris,
        X509Certificate[] certs
    ) {
        put(TAG_ACTION, act);
        put(TAG_SECRET, decryptedKey);
        put(TAG_ENCRYPTED_EPHEMERAL_KEY, encryptedKeyBytes);
        put(TAG_DATA_REF_URIS, dataRefUris);
        put(TAG_X509_CERTIFICATES, certs);
        if (certs != null && certs.length > 0) {
            put(TAG_X509_CERTIFICATE, certs[0]);
        }
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
    }

    public WSSecurityEngineResult(int act, List<WSDataRef> dataRefUris) {
        put(TAG_ACTION, act);
        put(TAG_DATA_REF_URIS, dataRefUris);
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
    }

    public WSSecurityEngineResult(int act, Timestamp tstamp) {
        put(TAG_ACTION, act);
        put(TAG_TIMESTAMP, tstamp);
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
        put(TAG_TOKEN_ELEMENT, tstamp.getElement());
    }

    public WSSecurityEngineResult(int act, SecurityContextToken sct) {
        put(TAG_ACTION, act);
        put(TAG_SECURITY_CONTEXT_TOKEN, sct);
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
        put(TAG_TOKEN_ELEMENT, sct.getElement());
    }

    public WSSecurityEngineResult(int act, SignatureConfirmation sc) {
        put(TAG_ACTION, act);
        put(TAG_SIGNATURE_CONFIRMATION, sc);
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
        put(TAG_TOKEN_ELEMENT, sc.getElement());
    }

    public WSSecurityEngineResult(int act, UsernameToken usernameToken) {
        this(act, usernameToken, null);
    }

    public WSSecurityEngineResult(int act, UsernameToken usernameToken, Principal principal) {
        put(TAG_ACTION, act);
        put(TAG_USERNAME_TOKEN, usernameToken);
        put(TAG_PRINCIPAL, principal);
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
        put(TAG_TOKEN_ELEMENT, usernameToken.getElement());
    }

    public WSSecurityEngineResult(int act, BinarySecurity token, X509Certificate[] certs) {
        put(TAG_ACTION, act);
        put(TAG_BINARY_SECURITY_TOKEN, token);
        put(TAG_X509_CERTIFICATES, certs);
        if (certs != null && certs.length > 0) {
            put(TAG_X509_CERTIFICATE, certs[0]);
        }
        put(TAG_VALIDATED_TOKEN, Boolean.FALSE);
        put(TAG_TOKEN_ELEMENT, token.getElement());
    }


}
