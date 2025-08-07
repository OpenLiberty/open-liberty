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
package org.apache.wss4j.stax.ext;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

/**
 * WSSConstants for global use
 */
public class WSSConstants extends XMLSecurityConstants {

    protected WSSConstants() {
    }

    public static final String TRANSPORT_SECURITY_ACTIVE = "transportSecurityActive";

    public static final String TIMESTAMP_PROCESSED = "TimestampProcessed";

    public static final String PROP_ALLOW_RSA15_KEYTRANSPORT_ALGORITHM = "secureProcessing.AllowRSA15KeyTransportAlgorithm";
    public static final String PROP_ALLOW_USERNAMETOKEN_NOPASSWORD = "secureProcessing.AllowUsernameTokenNoPassword";

    public static final String NS_WSSE10 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String NS_WSSE11 = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";
    public static final String NS_WSU10 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String NS_SOAP11 = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String NS_SOAP12 = "http://www.w3.org/2003/05/soap-envelope";

    public static final String NS_WST = "http://schemas.xmlsoap.org/ws/2005/02/trust";
    public static final String NS_WST_05_12 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    public static final String NS_WSC_SCT = "http://schemas.xmlsoap.org/ws/2005/02/sc/sct";

    public static final String NS_SAML = "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String NS_SAML2 = "urn:oasis:names:tc:SAML:2.0:assertion";

    public static final String PREFIX_SOAPENV = "soap";
    public static final String TAG_SOAP_ENVELOPE_LN = "Envelope";
    public static final String TAG_SOAP_HEADER_LN = "Header";
    public static final String TAG_SOAP_BODY_LN = "Body";

    public static final QName TAG_SOAP11_ENVELOPE = new QName(NS_SOAP11, TAG_SOAP_ENVELOPE_LN, PREFIX_SOAPENV);
    public static final QName TAG_SOAP11_HEADER = new QName(NS_SOAP11, TAG_SOAP_HEADER_LN, PREFIX_SOAPENV);
    public static final QName TAG_SOAP11_BODY = new QName(NS_SOAP11, TAG_SOAP_BODY_LN, PREFIX_SOAPENV);
    public static final QName ATT_SOAP11_ACTOR = new QName(NS_SOAP11, "actor", PREFIX_SOAPENV);
    public static final QName ATT_SOAP11_MUST_UNDERSTAND = new QName(NS_SOAP11, "mustUnderstand", PREFIX_SOAPENV);

    public static final QName TAG_SOAP12_ENVELOPE = new QName(NS_SOAP12, TAG_SOAP_ENVELOPE_LN, PREFIX_SOAPENV);
    public static final QName TAG_SOAP12_HEADER = new QName(NS_SOAP12, TAG_SOAP_HEADER_LN, PREFIX_SOAPENV);
    public static final QName TAG_SOAP12_BODY = new QName(NS_SOAP12, TAG_SOAP_BODY_LN, PREFIX_SOAPENV);
    public static final QName ATT_SOAP12_ROLE = new QName(NS_SOAP12, "role", PREFIX_SOAPENV);
    public static final QName ATT_SOAP12_MUST_UNDERSTAND = new QName(NS_SOAP12, "mustUnderstand", PREFIX_SOAPENV);

    public static final String PREFIX_WSSE = "wsse";
    public static final String PREFIX_WSSE11 = "wsse11";
    public static final QName TAG_WSSE_SECURITY = new QName(NS_WSSE10, "Security", PREFIX_WSSE);

    public static final QName TAG_WSSE_SECURITY_TOKEN_REFERENCE = new QName(NS_WSSE10, "SecurityTokenReference", PREFIX_WSSE);
    public static final QName TAG_WSSE_REFERENCE = new QName(NS_WSSE10, "Reference", PREFIX_WSSE);
    public static final QName ATT_WSSE_USAGE = new QName(NS_WSSE10, "Usage", PREFIX_WSSE);
    public static final QName ATT_WSSE11_TOKEN_TYPE = new QName(NS_WSSE11, "TokenType", PREFIX_WSSE11);

    public static final QName TAG_WSSE_KEY_IDENTIFIER = new QName(NS_WSSE10, "KeyIdentifier", PREFIX_WSSE);
    public static final QName ATT_NULL_ENCODING_TYPE = new QName(null, "EncodingType");
    public static final QName ATT_NULL_VALUE_TYPE = new QName(null, "ValueType");

    public static final QName TAG_WSSE_BINARY_SECURITY_TOKEN = new QName(NS_WSSE10, "BinarySecurityToken", PREFIX_WSSE);
    public static final String PREFIX_WSU = "wsu";
    public static final QName ATT_WSU_ID = new QName(NS_WSU10, "Id", PREFIX_WSU);

    public static final QName TAG_WSSE11_ENCRYPTED_HEADER = new QName(NS_WSSE11, "EncryptedHeader", PREFIX_WSSE11);

    public static final QName TAG_WSSE_TRANSFORMATION_PARAMETERS = new QName(NS_WSSE10, "TransformationParameters", PREFIX_WSSE);

    public static final QName TAG_WSU_TIMESTAMP = new QName(NS_WSU10, "Timestamp", PREFIX_WSU);
    public static final QName TAG_WSU_CREATED = new QName(NS_WSU10, "Created", PREFIX_WSU);
    public static final QName TAG_WSU_EXPIRES = new QName(NS_WSU10, "Expires", PREFIX_WSU);

    public static final String NS10_SOAPMESSAGE_SECURITY =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0";
    public static final String NS11_SOAPMESSAGE_SECURITY =
        "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1";

    public static final String NS_X509TOKEN_PROFILE =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0";

    public static final String NS_X509_V3_TYPE = NS_X509TOKEN_PROFILE + "#X509v3";
    public static final String NS_X509_PKIPATH_V1 = NS_X509TOKEN_PROFILE + "#X509PKIPathv1";
    public static final String NS_X509_SKI = NS_X509TOKEN_PROFILE + "#X509SubjectKeyIdentifier";
    public static final String NS_THUMBPRINT = NS11_SOAPMESSAGE_SECURITY + "#ThumbprintSHA1";

    public static final String NS_ENCRYPTED_KEY_SHA1 = NS11_SOAPMESSAGE_SECURITY + "#EncryptedKeySHA1";

    public static final String SOAPMESSAGE_NS10_BASE64_ENCODING = NS10_SOAPMESSAGE_SECURITY + "#Base64Binary";

    public static final QName TAG_WSSE_USERNAME_TOKEN = new QName(NS_WSSE10, "UsernameToken", PREFIX_WSSE);
    public static final QName TAG_WSSE_USERNAME = new QName(NS_WSSE10, "Username", PREFIX_WSSE);
    public static final QName TAG_WSSE_PASSWORD = new QName(NS_WSSE10, "Password", PREFIX_WSSE);
    public static final QName TAG_WSSE_NONCE = new QName(NS_WSSE10, "Nonce", PREFIX_WSSE);
    public static final QName TAG_WSSE11_SALT = new QName(NS_WSSE11, "Salt", PREFIX_WSSE11);
    public static final QName TAG_WSSE11_ITERATION = new QName(NS_WSSE11, "Iteration", PREFIX_WSSE11);

    public static final String NS_USERNAMETOKEN_PROFILE11 =
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0";
    public static final String NS_PASSWORD_DIGEST = NS_USERNAMETOKEN_PROFILE11 + "#PasswordDigest";
    public static final String NS_PASSWORD_TEXT = NS_USERNAMETOKEN_PROFILE11 + "#PasswordText";
    public static final String NS_USERNAMETOKEN_PROFILE_USERNAME_TOKEN = NS_USERNAMETOKEN_PROFILE11 + "#UsernameToken";

    public static final QName TAG_WSSE11_SIG_CONF = new QName(NS_WSSE11, "SignatureConfirmation", PREFIX_WSSE11);
    public static final QName ATT_NULL_VALUE = new QName(null, "Value");

    public static final String NS_C14N_EXCL = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String PREFIX_C14N_EXCL = "c14nEx";

    public static final QName TAG_WST_BINARY_SECRET = new QName(NS_WST, "BinarySecret");
    public static final QName TAG_WST0512_BINARY_SECRET = new QName(NS_WST_05_12, "BinarySecret");

    public static final String SOAPMESSAGE_NS10_STR_TRANSFORM = NS10_SOAPMESSAGE_SECURITY + "#STR-Transform";
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

    public static final QName TAG_SAML_ASSERTION = new QName(NS_SAML, "Assertion");
    public static final QName TAG_SAML2_ASSERTION = new QName(NS_SAML2, "Assertion");
    public static final QName TAG_SAML2_ENCRYPTED_ASSERTION = new QName(NS_SAML2, "EncryptedAssertion");

    public static final String NS_SAML10_TOKEN_PROFILE = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0";
    public static final String NS_SAML11_TOKEN_PROFILE = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1";
    public static final String NS_SAML10_TYPE = NS_SAML10_TOKEN_PROFILE + "#SAMLAssertionID";
    public static final String NS_SAML20_TYPE = NS_SAML11_TOKEN_PROFILE + "#SAMLID";
    public static final String NS_SAML11_TOKEN_PROFILE_TYPE = NS_SAML11_TOKEN_PROFILE + "#SAMLV1.1";
    public static final String NS_SAML20_TOKEN_PROFILE_TYPE = NS_SAML11_TOKEN_PROFILE + "#SAMLV2.0";

    public static final String NS_KERBEROS11_TOKEN_PROFILE = "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1#";
    public static final String NS_GSS_KERBEROS5_AP_REQ = NS_KERBEROS11_TOKEN_PROFILE + "GSS_Kerberosv5_AP_REQ";
    public static final String NS_GSS_KERBEROS5_AP_REQ1510 = NS_KERBEROS11_TOKEN_PROFILE + "GSS_Kerberosv5_AP_REQ1510";
    public static final String NS_GSS_KERBEROS5_AP_REQ4120 = NS_KERBEROS11_TOKEN_PROFILE + "GSS_Kerberosv5_AP_REQ4120";
    public static final String NS_KERBEROS5_AP_REQ = NS_KERBEROS11_TOKEN_PROFILE + "Kerberosv5_AP_REQ";
    public static final String NS_KERBEROS5_AP_REQ_SHA1 = NS_KERBEROS11_TOKEN_PROFILE + "Kerberosv5APREQSHA1";
    public static final String NS_KERBEROS5_AP_REQ1510 = NS_KERBEROS11_TOKEN_PROFILE + "Kerberosv5_AP_REQ1510";
    public static final String NS_KERBEROS5_AP_REQ4120 = NS_KERBEROS11_TOKEN_PROFILE + "Kerberosv5_AP_REQ4120";


    public static final QName ATT_NULL_ASSERTION_ID = new QName(null, "AssertionID");
    public static final QName ATT_NULL_ID = new QName(null, "ID");


    public static final String NS_WSC_05_02 = "http://schemas.xmlsoap.org/ws/2005/02/sc";
    public static final String NS_WSC_05_12 = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512";
    public static final String PREFIX_WSC = "wsc";

    public static final QName TAG_WSC0502_SCT = new QName(NS_WSC_05_02, "SecurityContextToken", PREFIX_WSC);
    public static final QName TAG_WSC0512_SCT = new QName(NS_WSC_05_12, "SecurityContextToken", PREFIX_WSC);
    public static final QName TAG_WSC0502_IDENTIFIER = new QName(NS_WSC_05_02, "Identifier", PREFIX_WSC);
    public static final QName TAG_WSC0512_IDENTIFIER = new QName(NS_WSC_05_12, "Identifier", PREFIX_WSC);

    public static final QName TAG_WSC0502_DKT = new QName(NS_WSC_05_02, "DerivedKeyToken", PREFIX_WSC);
    public static final QName TAG_WSC0512_DKT = new QName(NS_WSC_05_12, "DerivedKeyToken", PREFIX_WSC);
    public static final QName TAG_WSC0502_PROPERTIES = new QName(NS_WSC_05_02, "Properties", PREFIX_WSC);
    public static final QName TAG_WSC0512_PROPERTIES = new QName(NS_WSC_05_12, "Properties", PREFIX_WSC);
    public static final QName TAG_WSC0502_LENGTH = new QName(NS_WSC_05_02, "Length", PREFIX_WSC);
    public static final QName TAG_WSC0512_LENGTH = new QName(NS_WSC_05_12, "Length", PREFIX_WSC);
    public static final QName TAG_WSC0502_GENERATION = new QName(NS_WSC_05_02, "Generation", PREFIX_WSC);
    public static final QName TAG_WSC0512_GENERATION = new QName(NS_WSC_05_12, "Generation", PREFIX_WSC);
    public static final QName TAG_WSC0502_OFFSET = new QName(NS_WSC_05_02, "Offset", PREFIX_WSC);
    public static final QName TAG_WSC0512_OFFSET = new QName(NS_WSC_05_12, "Offset", PREFIX_WSC);
    public static final QName TAG_WSC0502_LABEL = new QName(NS_WSC_05_02, "Label", PREFIX_WSC);
    public static final QName TAG_WSC0512_LABEL = new QName(NS_WSC_05_12, "Label", PREFIX_WSC);
    public static final QName TAG_WSC0502_NONCE = new QName(NS_WSC_05_02, "Nonce", PREFIX_WSC);
    public static final QName TAG_WSC0512_NONCE = new QName(NS_WSC_05_12, "Nonce", PREFIX_WSC);

    public static final String P_SHA_1 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";
    public static final String P_SHA_1_2005_12 = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/dk/p_sha1";
    public static final String WS_SEC_CONV_DEFAULT_LABEL = "WS-SecureConversation";

    public static final String NS_WSS_ENC_KEY_VALUE_TYPE = NS11_SOAPMESSAGE_SECURITY + "#EncryptedKey";

    public static final String PROP_USE_THIS_TOKEN_ID_FOR_KERBEROS = "PROP_USE_THIS_TOKEN_ID_FOR_KERBEROS";
    public static final String PROP_USE_THIS_TOKEN_ID_FOR_DERIVED_KEY = "PROP_USE_THIS_TOKEN_ID_FOR_DERIVED_KEY";
    public static final String PROP_USE_THIS_TOKEN_ID_FOR_SECURITYCONTEXTTOKEN = "PROP_USE_THIS_TOKEN_ID_FOR_SECURITYCONTEXTTOKEN";
    public static final String PROP_USE_THIS_TOKEN_ID_FOR_CUSTOM_TOKEN = "PROP_USE_THIS_TOKEN_ID_FOR_CUSTOM_TOKEN";

    public static final String PROP_TIMESTAMP_SECURITYEVENT = "PROP_TIMESTAMP";

    public static final String PROP_ENCRYPTED_DATA_REFS = "PROP_ENCRYPTED_DATA_REFS";

    public static final Action TIMESTAMP = new Action(ConfigurationConstants.TIMESTAMP);
    public static final Action USERNAMETOKEN = new Action(ConfigurationConstants.USERNAME_TOKEN);
    public static final Action USERNAMETOKEN_SIGNED = new Action(ConfigurationConstants.USERNAME_TOKEN_SIGNATURE);
    public static final Action SIGNATURE_CONFIRMATION = new Action("SignatureConfirmation");
    public static final Action SIGNATURE_WITH_DERIVED_KEY = new Action("SignatureWithDerivedKey");
    public static final Action ENCRYPTION_WITH_DERIVED_KEY = new Action("EncryptionWithDerivedKey");
    @Deprecated
    public static final Action ENCRYPT_WITH_DERIVED_KEY = ENCRYPTION_WITH_DERIVED_KEY;
    public static final Action SAML_TOKEN_SIGNED = new Action(ConfigurationConstants.SAML_TOKEN_SIGNED);
    public static final Action SAML_TOKEN_UNSIGNED = new Action(ConfigurationConstants.SAML_TOKEN_UNSIGNED);
    public static final Action SIGNATURE_WITH_KERBEROS_TOKEN = new Action("SignatureWithKerberosToken");
    public static final Action ENCRYPTION_WITH_KERBEROS_TOKEN = new Action("EncryptionWithKerberosToken");
    @Deprecated
    public static final Action ENCRYPT_WITH_KERBEROS_TOKEN = ENCRYPTION_WITH_KERBEROS_TOKEN;
    public static final Action KERBEROS_TOKEN = new Action("KerberosToken");
    public static final Action CUSTOM_TOKEN = new Action("CustomToken");

    public static final AlgorithmUsage COMP_KEY = new AlgorithmUsage("Comp_Key");
    public static final AlgorithmUsage ENC_KD = new AlgorithmUsage("ENC_KD");
    public static final AlgorithmUsage SIG_KD = new AlgorithmUsage("SIG_KD");
    public static final AlgorithmUsage SOAP_NORM = new AlgorithmUsage("Soap_Norm");
    public static final AlgorithmUsage STR_TRANS = new AlgorithmUsage("STR_Trans");
    public static final AlgorithmUsage XPATH = new AlgorithmUsage("XPath");

    public enum DerivedKeyTokenReference {
        DirectReference,
        EncryptedKey,
        SecurityContextToken,
    }

    public enum UsernameTokenPasswordType {
        PASSWORD_NONE(null),
        PASSWORD_TEXT(NS_PASSWORD_TEXT),
        PASSWORD_DIGEST(NS_PASSWORD_DIGEST);

        private final String namespace;
        private static final Map<String, UsernameTokenPasswordType> LOOKUP = new HashMap<>();

        static {
            for (UsernameTokenPasswordType u : EnumSet.allOf(UsernameTokenPasswordType.class)) {
                LOOKUP.put(u.getNamespace(), u);
            }
        }

        UsernameTokenPasswordType(String namespace) {
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

        public static UsernameTokenPasswordType getUsernameTokenPasswordType(String namespace) {
            return LOOKUP.get(namespace);
        }
    }

    public static final List<QName> SOAP_11_BODY_PATH = new ArrayList<>(2);
    public static final List<QName> SOAP_12_BODY_PATH = new ArrayList<>(2);
    public static final List<QName> SOAP_11_HEADER_PATH = new ArrayList<>(2);
    public static final List<QName> SOAP_12_HEADER_PATH = new ArrayList<>(2);
    public static final List<QName> SOAP_11_WSSE_SECURITY_HEADER_PATH = new ArrayList<>(3);
    public static final List<QName> SOAP_12_WSSE_SECURITY_HEADER_PATH = new ArrayList<>(3);

    static {
        SOAP_11_BODY_PATH.add(WSSConstants.TAG_SOAP11_ENVELOPE);
        SOAP_11_BODY_PATH.add(WSSConstants.TAG_SOAP11_BODY);

        SOAP_12_BODY_PATH.add(WSSConstants.TAG_SOAP12_ENVELOPE);
        SOAP_12_BODY_PATH.add(WSSConstants.TAG_SOAP12_BODY);

        SOAP_11_HEADER_PATH.add(WSSConstants.TAG_SOAP11_ENVELOPE);
        SOAP_11_HEADER_PATH.add(WSSConstants.TAG_SOAP11_HEADER);

        SOAP_12_HEADER_PATH.add(WSSConstants.TAG_SOAP12_ENVELOPE);
        SOAP_12_HEADER_PATH.add(WSSConstants.TAG_SOAP12_HEADER);

        SOAP_11_WSSE_SECURITY_HEADER_PATH.addAll(SOAP_11_HEADER_PATH);
        SOAP_11_WSSE_SECURITY_HEADER_PATH.add(WSSConstants.TAG_WSSE_SECURITY);

        SOAP_12_WSSE_SECURITY_HEADER_PATH.addAll(SOAP_12_HEADER_PATH);
        SOAP_12_WSSE_SECURITY_HEADER_PATH.add(WSSConstants.TAG_WSSE_SECURITY);

    }
}
