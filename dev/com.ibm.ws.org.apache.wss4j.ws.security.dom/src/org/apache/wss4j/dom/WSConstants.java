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

package org.apache.wss4j.dom;

import javax.xml.namespace.QName;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.derivedKey.ConversationConstants;

/**
 * Constants in WS-Security spec.
 */
public final class WSConstants extends WSS4JConstants {

    //
    // QNames for the security header elements
    //

    /**
     * <code>wsse:BinarySecurityToken</code> as defined by WS Security specification
     */
    public static final QName BINARY_TOKEN =
        new QName(WSConstants.WSSE_NS, WSConstants.BINARY_TOKEN_LN);
    /**
     * <code>wsse:UsernameToken</code> as defined by WS Security specification
     */
    public static final QName USERNAME_TOKEN =
        new QName(WSConstants.WSSE_NS, WSConstants.USERNAME_TOKEN_LN);
    /**
     * <code>wsu:Timestamp</code> as defined by OASIS WS Security specification,
     */
    public static final QName TIMESTAMP =
        new QName(WSConstants.WSU_NS, WSConstants.TIMESTAMP_TOKEN_LN);
    /**
     * <code>wsse11:signatureConfirmation</code> as defined by OASIS WS Security specification,
     */
    public static final QName SIGNATURE_CONFIRMATION =
        new QName(WSConstants.WSSE11_NS, WSConstants.SIGNATURE_CONFIRMATION_LN);
    /**
     * <code>ds:Signature</code> as defined by XML Signature specification,
     * enhanced by WS Security specification
     */
    public static final QName SIGNATURE =
        new QName(WSConstants.SIG_NS, WSConstants.SIG_LN);
    /**
     * <code>xenc:EncryptedKey</code> as defined by XML Encryption specification,
     * enhanced by WS Security specification
     */
    public static final QName ENCRYPTED_KEY =
        new QName(WSConstants.ENC_NS, WSConstants.ENC_KEY_LN);
    /**
     * <code>xenc:EncryptedData</code> as defined by XML Encryption specification,
     * enhanced by WS Security specification
     */
    public static final QName ENCRYPTED_DATA =
        new QName(WSConstants.ENC_NS, WSConstants.ENC_DATA_LN);
    /**
     * <code>xenc:ReferenceList</code> as defined by XML Encryption specification,
     */
    public static final QName REFERENCE_LIST =
        new QName(WSConstants.ENC_NS, WSConstants.REF_LIST_LN);
    /**
     * <code>saml:Assertion</code> as defined by SAML v1.1 specification
     */
    public static final QName SAML_TOKEN =
        new QName(WSConstants.SAML_NS, WSConstants.ASSERTION_LN);

    /**
     * <code>saml:Assertion</code> as defined by SAML v2.0 specification
     */
    public static final QName SAML2_TOKEN =
        new QName(WSConstants.SAML2_NS, WSConstants.ASSERTION_LN);

    /**
     * <code>saml:EncryptedAssertion</code> as defined by SAML v2.0 specification
     */
    public static final QName ENCRYPTED_ASSERTION =
        new QName(WSConstants.SAML2_NS, WSConstants.ENCRYPED_ASSERTION_LN);

    /**
     * <code>wsc:DerivedKeyToken</code> as defined by WS-SecureConversation specification
     */
    public static final QName DERIVED_KEY_TOKEN_05_02 =
        new QName(ConversationConstants.WSC_NS_05_02, ConversationConstants.DERIVED_KEY_TOKEN_LN);

    /**
     * <code>wsc:SecurityContextToken</code> as defined by WS-SecureConversation specification
     */
    public static final QName SECURITY_CONTEXT_TOKEN_05_02 =
        new QName(ConversationConstants.WSC_NS_05_02, ConversationConstants.SECURITY_CONTEXT_TOKEN_LN);

    /**
     * <code>wsc:DerivedKeyToken</code> as defined by WS-SecureConversation specification in WS-SX
     */
    public static final QName DERIVED_KEY_TOKEN_05_12 =
        new QName(ConversationConstants.WSC_NS_05_12, ConversationConstants.DERIVED_KEY_TOKEN_LN);

    /**
     * <code>wsc:SecurityContextToken</code> as defined by WS-SecureConversation specification in
     * WS-SX
     */
    public static final QName SECURITY_CONTEXT_TOKEN_05_12 =
        new QName(ConversationConstants.WSC_NS_05_12, ConversationConstants.SECURITY_CONTEXT_TOKEN_LN);

    //
    // Fault codes defined in the WSS 1.1 spec under section 12, Error handling
    //

    /**
     * An unsupported token was provided
     */
    public static final QName UNSUPPORTED_SECURITY_TOKEN =
        new QName(WSSE_NS, "UnsupportedSecurityToken");

    /**
     * An unsupported signature or encryption algorithm was used
     */
    public static final QName UNSUPPORTED_ALGORITHM =
        new QName(WSSE_NS, "UnsupportedAlgorithm");

    /**
     * An error was discovered processing the <Security> header
     */
    public static final QName INVALID_SECURITY = new QName(WSSE_NS, "InvalidSecurity");

    /**
     * An invalid security token was provided
     */
    public static final QName INVALID_SECURITY_TOKEN = new QName(WSSE_NS, "InvalidSecurityToken");

    /**
     * The security token could not be authenticated or authorized
     */
    public static final QName FAILED_AUTHENTICATION = new QName(WSSE_NS, "FailedAuthentication");

    /**
     * The signature or decryption was invalid
     */
    public static final QName FAILED_CHECK = new QName(WSSE_NS, "FailedCheck");

    /**
     * Referenced security token could not be retrieved
     */
    public static final QName SECURITY_TOKEN_UNAVAILABLE = new QName(WSSE_NS, "SecurityTokenUnavailable");

    /**
     * The message has expired
     */
    public static final QName MESSAGE_EXPIRED = new QName(WSSE_NS, "MessageExpired");

    /*
     * Constants used to configure WSS4J
     */

    /**
     * Sets the {@link
     * org.apache.wss4j.dom.message.WSSecSignature#build(Document, Crypto, WSSecHeader)
     * } method to send the signing certificate as a <code>BinarySecurityToken</code>.
     * <p/>
     * The signing method takes the signing certificate, converts it to a
     * <code>BinarySecurityToken</code>, puts it in the security header,
     * and inserts a <code>Reference</code> to the binary security token
     * into the <code>wsse:SecurityReferenceToken</code>. Thus the whole
     * signing certificate is transfered to the receiver.
     * The X509 profile recommends to use {@link #ISSUER_SERIAL} instead
     * of sending the whole certificate.
     * <p/>
     * Please refer to WS Security specification X509 1.1 profile, chapter 3.3.2
     * and to WS Security SOAP Message security 1.1 specification, chapter 7.2
     * <p/>
     * Note: only local references to BinarySecurityToken are supported
     */
    public static final int BST_DIRECT_REFERENCE = 1;

    /**
     * Sets the {@link
     *org.apache.wss4j.dom.message.WSSecSignature#build(Crypto)
     *} or the {@link
     *org.apache.wss4j.dom.message.WSSecEncrypt#build(Crypto, SecretKey)
     * } method to send the issuer name and the serial number of a certificate to
     * the receiver.
     * <p/>
     * In contrast to {@link #BST_DIRECT_REFERENCE} only the issuer name
     * and the serial number of the signing certificate are sent to the
     * receiver. This reduces the amount of data being sent. The encryption
     * method uses the public key associated with this certificate to encrypt
     * the symmetric key used to encrypt data. 
     * The name format will delimit unicode characters with a '\' which is not compatible with Microsoft's WCF stack.
     * To send issuer name with format that is compatible with WCF and Java use {@link #ISSUER_SERIAL_QUOTE_FORMAT}
     * <p/>
     * Please refer to WS Security specification X509 1.1 profile, chapter 3.3.3
     */
    public static final int ISSUER_SERIAL = 2;

    /**
     * Sets the {@link
     * org.apache.wss4j.dom.message.WSSecSignature#build(Document, Crypto, WSSecHeader)
     * } or the {@link
     * org.apache.wss4j.dom.message.WSSecEncrypt#build(Document, Crypto, WSSecHeader)
     * }method to send the certificate used to encrypt the symmetric key.
     * <p/>
     * The encryption method uses the public key associated with this certificate
     * to encrypt the symmetric key used to encrypt data. The certificate is
     * converted into a <code>KeyIdentifier</code> token and sent to the receiver.
     * Thus the complete certificate data is transferred to receiver.
     * The X509 profile recommends to use {@link #ISSUER_SERIAL} instead
     * of sending the whole certificate.
     * <p/>
     * Please refer to WS Security SOAP Message security 1.1 specification,
     * chapter 7.3. Note that this is a NON-STANDARD method. The standard way to refer to
     * an X.509 Certificate via a KeyIdentifier is to use {@link #SKI_KEY_IDENTIFIER}
     */
    public static final int X509_KEY_IDENTIFIER = 3;

    /**
     * Sets the {@link
     * org.apache.wss4j.dom.message.WSSecSignature#build(Document, Crypto, WSSecHeader)
     * } method to send a <code>SubjectKeyIdentifier</code> to identify
     * the signing certificate.
     * <p/>
     * Refer to WS Security specification X509 1.1 profile, chapter 3.3.1
     */
    public static final int SKI_KEY_IDENTIFIER = 4;

    /**
     * Embeds a keyinfo/key name into the EncryptedData element.
     * <p/>
     */
    @Deprecated
    public static final int EMBEDDED_KEYNAME = 5;

    /**
     * Embeds a keyinfo/wsse:SecurityTokenReference into EncryptedData element.
     */
    @Deprecated
    public static final int EMBED_SECURITY_TOKEN_REF = 6;

    /**
     * <code>UT_SIGNING</code> is used internally only to set a specific Signature
     * behavior.
     *
     * The signing token is constructed from values in the UsernameToken according
     * to WS-Trust specification.
     */
    public static final int UT_SIGNING = 7;

    /**
     * <code>THUMPRINT_IDENTIFIER</code> is used to set the specific key identifier
     * ThumbprintSHA1.
     *
     * This identifier uses the SHA-1 digest of a security token to
     * identify the security token. Please refer to chapter 7.2 of the OASIS WSS 1.1
     * specification.
     *
     */
    public static final int THUMBPRINT_IDENTIFIER = 8;

    /**
     * <code>CUSTOM_SYMM_SIGNING</code> is used internally only to set a
     * specific Signature behavior.
     *
     * The signing key, reference id and value type are set externally.
     */
    public static final int CUSTOM_SYMM_SIGNING = 9;

    /**
     * <code>ENCRYPTED_KEY_SHA1_IDENTIFIER</code> is used to set the specific key identifier
     * EncryptedKeySHA1.
     *
     * This identifier uses the SHA-1 digest of a security token to
     * identify the security token. Please refer to chapter 7.3 of the OASIS WSS 1.1
     * specification.
     */
    public static final int ENCRYPTED_KEY_SHA1_IDENTIFIER = 10;

    /**
     * <code>CUSTOM_SYMM_SIGNING_DIRECT</code> is used internally only to set a
     * specific Signature behavior.
     *
     * The signing key, reference id and value type are set externally.
     */
    public static final int CUSTOM_SYMM_SIGNING_DIRECT = 11;

    /**
     * <code>CUSTOM_KEY_IDENTIFIER</code> is used to set a KeyIdentifier to
     * a particular ID
     *
     * The reference id and value type are set externally.
     */
    public static final int CUSTOM_KEY_IDENTIFIER = 12;

    /**
     * <code>KEY_VALUE</code> is used to set a ds:KeyInfo/ds:KeyValue element to refer to
     * either an RSA or DSA public key.
     */
    public static final int KEY_VALUE = 13;

    /**
     * <code>ENDPOINT_KEY_IDENTIFIER</code> is used to specify service endpoint as public key
     * identifier.
     *
     * Constant is useful in case of symmetric holder of key, where token service can determine
     * target service public key to encrypt shared secret.
     */
    public static final int ENDPOINT_KEY_IDENTIFIER = 14;


    /**
     *Sets the {@link org.apache.wss4j.dom.message.WSSecSignature#build(Crypto)}
     * or the {@link org.apache.wss4j.dom.message.WSSecEncrypt#build(Crypto, SecretKey)}
     * method to send the issuer name and the serial number of a certificate to the receiver.
     *<p/>
     * In contrast to {@link #BST_DIRECT_REFERENCE} only the issuer name
     * and the serial number of the signing certificate are sent to the
     * receiver. This reduces the amount of data being sent. The encryption
     * method uses the public key associated with this certificate to encrypt
     * the symmetric key used to encrypt data.
     * The issuer name format will use a quote delimited Rfc 2253 format if necessary which is recognized by the Microsoft's WCF stack.
     * It also places a space before each subsequent RDN also required for WCF interoperability.
     * In addition, this format is know to be correctly interpreted by Java.
     *<p/>
     *Please refer to WS Security specification X509 1.1 profile, chapter 3.3.3
     * <p/>
     */
    public static final int ISSUER_SERIAL_QUOTE_FORMAT = 15;

    /*
     * The following values are bits that can be combined to form a set.
     * Be careful when adding new constants.
     */
    public static final int NO_SECURITY = 0;
    public static final int UT = 0x1; // perform UsernameToken
    public static final int SIGN = 0x2; // Perform Signature
    public static final int ENCR = 0x4; // Perform Encryption

    public static final int ST_UNSIGNED = 0x8; // perform SAMLToken unsigned
    public static final int ST_SIGNED = 0x10; // perform SAMLToken signed

    public static final int TS = 0x20; // insert Timestamp
    public static final int UT_SIGN = 0x40; // perform signature with UT secret key
    public static final int SC = 0x80;      // this is a SignatureConfirmation

    public static final int NO_SERIALIZE = 0x100;
    public static final int SERIALIZE = 0x200;
    public static final int SCT = 0x400; //SecurityContextToken
    public static final int DKT = 0x800; //DerivedKeyToken
    public static final int BST = 0x1000; //BinarySecurityToken
    public static final int UT_NOPASSWORD = 0x2000; // perform UsernameToken
    public static final int CUSTOM_TOKEN = 0x4000; // perform a Custom Token action
    public static final int DKT_SIGN = 0x8000; // Perform Signature with a Derived Key
    public static final int DKT_ENCR = 0x10000; // Perform Encryption with a Derived Key

    private WSConstants() {
        super();
    }

}
