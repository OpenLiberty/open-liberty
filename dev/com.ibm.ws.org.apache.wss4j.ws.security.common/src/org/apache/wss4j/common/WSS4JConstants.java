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
package org.apache.wss4j.common;

/**
 */
public class WSS4JConstants {

    protected WSS4JConstants() {
        // complete
    }

    //
    // Namespaces
    //
    public static final String WSSE_NS =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String WSSE11_NS =
        "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";
    public static final String OLD_WSSE_NS =
        "http://schemas.xmlsoap.org/ws/2002/04/secext";
    public static final String WSU_NS =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    public static final String SOAPMESSAGE_NS =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0";
    public static final String SOAPMESSAGE_NS11 =
        "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1";
    public static final String USERNAMETOKEN_NS =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0";
    public static final String X509TOKEN_NS =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0";
    public static final String SAMLTOKEN_NS =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0";
    public static final String SAMLTOKEN_NS11 =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1";
    public static final String KERBEROS_NS11 =
        "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1";

    public static final String SIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    public static final String ENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String ENC11_NS = "http://www.w3.org/2009/xmlenc11#";
    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    public static final String SAML_NS = "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String SAMLP_NS = "urn:oasis:names:tc:SAML:1.0:protocol";
    public static final String SAML2_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String SAMLP2_NS = "urn:oasis:names:tc:SAML:2.0:protocol";

    public static final String URI_SOAP11_ENV =
        "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String URI_SOAP12_ENV =
        "http://www.w3.org/2003/05/soap-envelope";
    public static final String URI_SOAP11_NEXT_ACTOR =
        "http://schemas.xmlsoap.org/soap/actor/next";
    public static final String URI_SOAP12_NEXT_ROLE =
        "http://www.w3.org/2003/05/soap-envelope/role/next";
    public static final String URI_SOAP12_NONE_ROLE =
        "http://www.w3.org/2003/05/soap-envelope/role/none";
    public static final String URI_SOAP12_ULTIMATE_ROLE =
        "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";

    public static final String C14N_OMIT_COMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
    public static final String C14N_WITH_COMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments";
    public static final String C14N_EXCL_OMIT_COMMENTS =
        "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String C14N_EXCL_WITH_COMMENTS =
        "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    public static final String NS_XMLDSIG_FILTER2 =
        "http://www.w3.org/2002/06/xmldsig-filter2";
    public static final String NS_XMLDSIG_ENVELOPED_SIGNATURE =
        SIG_NS + "enveloped-signature";
    public static final String SWA_ATTACHMENT_CONTENT_SIG_TRANS =
        "http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Content-Signature-Transform";
    public static final String SWA_ATTACHMENT_COMPLETE_SIG_TRANS =
        "http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Complete-Signature-Transform";
    public static final String SWA_ATTACHMENT_CIPHERTEXT_TRANS =
        "http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Ciphertext-Transform";
    public static final String SWA_ATTACHMENT_ENCRYPTED_DATA_TYPE_CONTENT_ONLY =
        "http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Content-Only";
    public static final String SWA_ATTACHMENT_ENCRYPTED_DATA_TYPE_COMPLETE =
        "http://docs.oasis-open.org/wss/oasis-wss-SwAProfile-1.1#Attachment-Complete";
    public static final String XOP_NS = "http://www.w3.org/2004/08/xop/include";

    public static final String KEYTRANSPORT_RSA15 =
        "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
    public static final String KEYTRANSPORT_RSAOAEP =
        "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";
    public static final String KEYTRANSPORT_RSAOAEP_XENC11 =
        "http://www.w3.org/2009/xmlenc11#rsa-oaep";
    public static final String KEYWRAP_AES128 =
            "http://www.w3.org/2001/04/xmlenc#kw-aes128";
    public static final String KEYWRAP_AES192 =
            "http://www.w3.org/2001/04/xmlenc#kw-aes192";
    public static final String KEYWRAP_AES256 =
            "http://www.w3.org/2001/04/xmlenc#kw-aes256";
    public static final String KEYWRAP_TRIPLEDES =
            "http://www.w3.org/2001/04/xmlenc#kw-tripledes";
    public static final String KEYDERIVATION_CONCATKDF =
            "http://www.w3.org/2009/xmlenc11#ConcatKDF";
    public static final String KEYDERIVATION_HKDF =
            "http://www.w3.org/2021/04/xmldsig-more#hkdf";
    public static final String AGREEMENT_METHOD_ECDH_ES =
            "http://www.w3.org/2009/xmlenc11#ECDH-ES";
    public static final String AGREEMENT_METHOD_X25519 =
            "http://www.w3.org/2021/04/xmldsig-more#x25519";
    public static final String AGREEMENT_METHOD_X448 =
            "http://www.w3.org/2021/04/xmldsig-more#x448";
    public static final String TRIPLE_DES =
        "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";
    public static final String AES_128 =
        "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    public static final String AES_256 =
        "http://www.w3.org/2001/04/xmlenc#aes256-cbc";
    public static final String AES_192 =
        "http://www.w3.org/2001/04/xmlenc#aes192-cbc";
    public static final String AES_128_GCM =
        "http://www.w3.org/2009/xmlenc11#aes128-gcm";
    public static final String AES_192_GCM =
        "http://www.w3.org/2009/xmlenc11#aes192-gcm";
    public static final String AES_256_GCM =
        "http://www.w3.org/2009/xmlenc11#aes256-gcm";
    public static final String DSA =
        "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
    public static final String RSA =
        "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    public static final String RSA_SHA1 =
        "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    public static final String RSA_SHA256 =
        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    public static final String RSA_SHA512 =
        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";
    public static final String SHA1 =
        "http://www.w3.org/2000/09/xmldsig#sha1";
    public static final String SHA256 =
        "http://www.w3.org/2001/04/xmlenc#sha256";
    public static final String SHA384 =
        "http://www.w3.org/2001/04/xmldsig-more#sha384";
    public static final String SHA512 =
            "http://www.w3.org/2001/04/xmlenc#sha512";
    public static final String HMAC_SHA1 =
        "http://www.w3.org/2000/09/xmldsig#hmac-sha1";
    public static final String HMAC_SHA256 =
        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha256";
    public static final String HMAC_SHA384 =
        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha384";
    public static final String HMAC_SHA512 =
        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha512";
    public static final String HMAC_MD5 =
        "http://www.w3.org/2001/04/xmldsig-more#hmac-md5";
    public static final String ECDSA_SHA1 =
            "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1";
    public static final String ECDSA_SHA384 =
            "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384";
    public static final String ECDSA_SHA256 =
            "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";
    public static final String ECDSA_SHA512 =
            "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512";
    // see RFC 9231 for these algorithm definitions
    public static final String ED25519 =
            "http://www.w3.org/2021/04/xmldsig-more#eddsa-ed25519";
    public static final String ED448 =
            "http://www.w3.org/2021/04/xmldsig-more#eddsa-ed448";

    public static final String MGF_SHA1 = "http://www.w3.org/2009/xmlenc11#mgf1sha1";
    public static final String MGF_SHA224 = "http://www.w3.org/2009/xmlenc11#mgf1sha224";
    public static final String MGF_SHA256 = "http://www.w3.org/2009/xmlenc11#mgf1sha256";
    public static final String MGF_SHA384 = "http://www.w3.org/2009/xmlenc11#mgf1sha384";
    public static final String MGF_SHA512 = "http://www.w3.org/2009/xmlenc11#mgf1sha512";

    public static final String WST_NS = "http://schemas.xmlsoap.org/ws/2005/02/trust";
    /**
     * WS-Trust 1.3 namespace
     */
    public static final String WST_NS_05_12 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    /**
     * WS-Trust 1.4 namespace
     */
    public static final String WST_NS_08_02 = "http://docs.oasis-open.org/ws-sx/ws-trust/200802";

    public static final String WSC_SCT = "http://schemas.xmlsoap.org/ws/2005/02/sc/sct";

    public static final String WSC_SCT_05_12 =
        "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/sct";

    //
    // Localnames
    //
    public static final String WSSE_LN = "Security";
    public static final String THUMBPRINT = "ThumbprintSHA1";
    public static final String SAML_ASSERTION_ID = "SAMLAssertionID";
    public static final String SAML2_ASSERTION_ID = "SAMLID";
    public static final String ENC_KEY_VALUE_TYPE = "EncryptedKey";
    public static final String ENC_KEY_SHA1_URI = "EncryptedKeySHA1";
    public static final String SIG_LN = "Signature";
    public static final String SIG_INFO_LN = "SignedInfo";
    public static final String ENC_KEY_LN = "EncryptedKey";
    public static final String ENC_DATA_LN = "EncryptedData";
    public static final String REF_LIST_LN = "ReferenceList";
    public static final String REF_LN = "Reference";
    public static final String USERNAME_TOKEN_LN = "UsernameToken";
    public static final String BINARY_TOKEN_LN = "BinarySecurityToken";
    public static final String TIMESTAMP_TOKEN_LN = "Timestamp";
    public static final String USERNAME_LN = "Username";
    public static final String PASSWORD_LN = "Password";
    public static final String PASSWORD_TYPE_ATTR = "Type";
    public static final String NONCE_LN = "Nonce";
    public static final String CREATED_LN = "Created";
    public static final String EXPIRES_LN = "Expires";
    public static final String SIGNATURE_CONFIRMATION_LN = "SignatureConfirmation";
    public static final String SALT_LN = "Salt";
    public static final String ITERATION_LN = "Iteration";
    public static final String ASSERTION_LN = "Assertion";
    public static final String ENCRYPED_ASSERTION_LN = "EncryptedAssertion";
    public static final String PW_DIGEST = "PasswordDigest";
    public static final String PW_TEXT = "PasswordText";
    public static final String PW_NONE = "PasswordNone";
    public static final String ENCRYPTED_HEADER = "EncryptedHeader";
    public static final String X509_ISSUER_SERIAL_LN = "X509IssuerSerial";
    public static final String X509_ISSUER_NAME_LN = "X509IssuerName";
    public static final String X509_SERIAL_NUMBER_LN = "X509SerialNumber";
    public static final String X509_SKI_LN = "X509SKI";
    public static final String X509_DATA_LN = "X509Data";
    public static final String X509_CERT_LN = "X509Certificate";
    public static final String KEYINFO_LN = "KeyInfo";
    public static final String KEYVALUE_LN = "KeyValue";
    public static final String TOKEN_TYPE = "TokenType";

    public static final String ELEM_ENVELOPE = "Envelope";
    public static final String ELEM_HEADER = "Header";
    public static final String ELEM_BODY = "Body";
    public static final String ATTR_MUST_UNDERSTAND = "mustUnderstand";
    public static final String ATTR_ACTOR = "actor";
    public static final String ATTR_ROLE = "role";
    public static final String NULL_NS = "Null";

    //
    // Prefixes
    //
    public static final String WSSE_PREFIX = "wsse";
    public static final String WSSE11_PREFIX = "wsse11";
    public static final String WSU_PREFIX = "wsu";
    public static final String DEFAULT_SOAP_PREFIX = "soapenv";
    public static final String SIG_PREFIX = "ds";
    public static final String ENC_PREFIX = "xenc";
    public static final String ENC11_PREFIX = "xenc11";
    public static final String C14N_EXCL_OMIT_COMMENTS_PREFIX = "ec";

    //
    // Kerberos ValueTypes
    //
    public static final String WSS_KRB_V5_AP_REQ = KERBEROS_NS11 + "#Kerberosv5_AP_REQ";
    public static final String WSS_GSS_KRB_V5_AP_REQ = KERBEROS_NS11 + "#GSS_Kerberosv5_AP_REQ";
    public static final String WSS_KRB_V5_AP_REQ1510 = KERBEROS_NS11 + "#Kerberosv5_AP_REQ1510";
    public static final String WSS_GSS_KRB_V5_AP_REQ1510 =
        KERBEROS_NS11 + "#GSS_Kerberosv5_AP_REQ1510";
    public static final String WSS_KRB_V5_AP_REQ4120 = KERBEROS_NS11 + "#Kerberosv5_AP_REQ4120";
    public static final String WSS_GSS_KRB_V5_AP_REQ4120 =
        KERBEROS_NS11 + "#GSS_Kerberosv5_AP_REQ4120";
    public static final String WSS_KRB_KI_VALUE_TYPE = KERBEROS_NS11 + "#Kerberosv5APREQSHA1";

    //
    // Misc
    //
    public static final String WSS_SAML_KI_VALUE_TYPE = SAMLTOKEN_NS + "#" + SAML_ASSERTION_ID;
    public static final String WSS_SAML2_KI_VALUE_TYPE = SAMLTOKEN_NS11 + "#" + SAML2_ASSERTION_ID;
    public static final String WSS_SAML_TOKEN_TYPE = SAMLTOKEN_NS11 + "#SAMLV1.1";
    public static final String WSS_SAML2_TOKEN_TYPE = SAMLTOKEN_NS11 + "#SAMLV2.0";
    public static final String WSS_ENC_KEY_VALUE_TYPE = SOAPMESSAGE_NS11 + "#" + ENC_KEY_VALUE_TYPE;
    public static final String PASSWORD_DIGEST = USERNAMETOKEN_NS + "#PasswordDigest";
    public static final String PASSWORD_TEXT = USERNAMETOKEN_NS + "#PasswordText";
    public static final String WSS_USERNAME_TOKEN_VALUE_TYPE =
        USERNAMETOKEN_NS + "#" + USERNAME_TOKEN_LN;
    public static final String BASE64_ENCODING = SOAPMESSAGE_NS + "#Base64Binary";

    public static final String[] URIS_SOAP_ENV = {
        URI_SOAP11_ENV,
        URI_SOAP12_ENV,
    };
}
