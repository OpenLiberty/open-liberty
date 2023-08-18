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
package org.apache.xml.security.stax.ext;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.validation.Schema;

import org.apache.xml.security.exceptions.XMLSecurityException;

/**
 * XMLSecurityConstants for global use
 *
 */
public class XMLSecurityConstants {

    public static final DatatypeFactory datatypeFactory;
    public static final XMLOutputFactory xmlOutputFactory;
    public static final XMLOutputFactory xmlOutputFactoryNonRepairingNs;

    private static final SecureRandom SECURE_RANDOM;
    private static JAXBContext jaxbContext;
    private static Schema schema;

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }

        xmlOutputFactory = XMLOutputFactory.newInstance();
        xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);

        xmlOutputFactoryNonRepairingNs = XMLOutputFactory.newInstance();
        xmlOutputFactoryNonRepairingNs.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
    }

    protected XMLSecurityConstants() {
    }

    /**
     * Generate bytes of the given length using the SHA1PRNG algorithm. The SecureRandom
     * instance that backs this method is cached for efficiency.
     *
     * @return a byte array of the given length
     * @throws XMLSecurityException
     */
    public static byte[] generateBytes(int length) throws XMLSecurityException {
        try {
            byte[] temp = new byte[length];
            SECURE_RANDOM.nextBytes(temp);
            return temp;
        } catch (Exception ex) {
            throw new XMLSecurityException(ex);
        }
    }

    public static synchronized void setJaxbContext(JAXBContext jaxbContext) {
        XMLSecurityConstants.jaxbContext = jaxbContext;
    }

    public static synchronized void setJaxbSchemas(Schema schema) {
        XMLSecurityConstants.schema = schema;
    }

    public static synchronized Schema getJaxbSchemas() {
        return XMLSecurityConstants.schema;
    }

    public static Unmarshaller getJaxbUnmarshaller(boolean disableSchemaValidation) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        if (!disableSchemaValidation) {
            unmarshaller.setSchema(schema);
        }
        return unmarshaller;
    }

    public enum Phase {
        PREPROCESSING,
        PROCESSING,
        POSTPROCESSING,
    }

    public enum DIRECTION {
        IN,
        OUT,
    }

    public static final String XMLINPUTFACTORY = "XMLInputFactory";

    public static final String NS_XML = "http://www.w3.org/2000/xmlns/";
    public static final String NS_XMLENC = "http://www.w3.org/2001/04/xmlenc#";
    public static final String NS_XMLENC11 = "http://www.w3.org/2009/xmlenc11#";
    public static final String NS_DSIG = "http://www.w3.org/2000/09/xmldsig#";
    public static final String NS_DSIG_MORE ="http://www.w3.org/2001/04/xmldsig-more#";
    public static final String NS_DSIG_MORE_2007_05 = "http://www.w3.org/2007/05/xmldsig-more#";
    public static final String NS_DSIG11 = "http://www.w3.org/2009/xmldsig11#";
    public static final String NS_WSSE11 = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";
    public static final String NS_XOP = "http://www.w3.org/2004/08/xop/include";

    public static final String PREFIX_XENC = "xenc";
    public static final String PREFIX_XENC11 = "xenc11";
    public static final QName TAG_xenc_EncryptedKey = new QName(NS_XMLENC, "EncryptedKey", PREFIX_XENC);
    public static final QName ATT_NULL_Id = new QName(null, "Id");
    public static final QName ATT_NULL_Type = new QName(null, "Type");
    public static final QName ATT_NULL_MimeType = new QName(null, "MimeType");
    public static final QName ATT_NULL_Encoding = new QName(null, "Encoding");

    public static final QName TAG_xenc_EncryptionMethod = new QName(NS_XMLENC, "EncryptionMethod", PREFIX_XENC);
    public static final QName ATT_NULL_Algorithm = new QName(null, "Algorithm");

    public static final QName TAG_xenc_OAEPparams = new QName(NS_XMLENC, "OAEPparams", PREFIX_XENC);

    public static final QName TAG_xenc11_MGF = new QName(NS_XMLENC11, "MGF", PREFIX_XENC11);

    public static final String PREFIX_DSIG = "dsig";
    public static final String PREFIX_DSIG_MORE_PSS = "pss";
    public static final QName TAG_dsig_KeyInfo = new QName(NS_DSIG, "KeyInfo", PREFIX_DSIG);

    public static final QName TAG_xenc_EncryptionProperties = new QName(NS_XMLENC, "EncryptionProperties", PREFIX_XENC);
    public static final QName TAG_xenc_CipherData = new QName(NS_XMLENC, "CipherData", PREFIX_XENC);
    public static final QName TAG_xenc_CipherValue = new QName(NS_XMLENC, "CipherValue", PREFIX_XENC);
    public static final QName TAG_xenc_CipherReference = new QName(NS_XMLENC, "CipherReference", PREFIX_XENC);
    public static final QName TAG_xenc_ReferenceList = new QName(NS_XMLENC, "ReferenceList", PREFIX_XENC);
    public static final QName TAG_xenc_DataReference = new QName(NS_XMLENC, "DataReference", PREFIX_XENC);
    public static final QName ATT_NULL_URI = new QName(null, "URI");

    public static final QName TAG_xenc_EncryptedData = new QName(NS_XMLENC, "EncryptedData", PREFIX_XENC);

    public static final QName TAG_xenc_Transforms = new QName(NS_XMLENC, "Transforms", PREFIX_XENC);

    public static final String PREFIX_WSSE11 = "wsse11";
    public static final QName TAG_wsse11_EncryptedHeader = new QName(NS_WSSE11, "EncryptedHeader", PREFIX_WSSE11);

    public static final QName TAG_dsig_Signature = new QName(NS_DSIG, "Signature", PREFIX_DSIG);
    public static final QName TAG_dsig_SignedInfo = new QName(NS_DSIG, "SignedInfo", PREFIX_DSIG);
    public static final QName TAG_dsig_CanonicalizationMethod = new QName(NS_DSIG, "CanonicalizationMethod", PREFIX_DSIG);
    public static final QName TAG_dsig_SignatureMethod = new QName(NS_DSIG, "SignatureMethod", PREFIX_DSIG);
    public static final QName TAG_dsig_HMACOutputLength = new QName(NS_DSIG, "HMACOutputLength", PREFIX_DSIG);
    public static final QName TAG_dsig_Reference = new QName(NS_DSIG, "Reference", PREFIX_DSIG);
    public static final QName TAG_dsig_Transforms = new QName(NS_DSIG, "Transforms", PREFIX_DSIG);
    public static final QName TAG_dsig_Transform = new QName(NS_DSIG, "Transform", PREFIX_DSIG);
    public static final QName TAG_dsig_DigestMethod = new QName(NS_DSIG, "DigestMethod", PREFIX_DSIG);
    public static final QName TAG_dsig_DigestValue = new QName(NS_DSIG, "DigestValue", PREFIX_DSIG);
    public static final QName TAG_dsig_SignatureValue = new QName(NS_DSIG, "SignatureValue", PREFIX_DSIG);
    public static final QName TAG_dsig_Manifest = new QName(NS_DSIG, "Manifest", PREFIX_DSIG);

    public static final QName TAG_dsig_X509Data = new QName(NS_DSIG, "X509Data", PREFIX_DSIG);
    public static final QName TAG_dsig_X509IssuerSerial = new QName(NS_DSIG, "X509IssuerSerial", PREFIX_DSIG);
    public static final QName TAG_dsig_X509IssuerName = new QName(NS_DSIG, "X509IssuerName", PREFIX_DSIG);
    public static final QName TAG_dsig_X509SerialNumber = new QName(NS_DSIG, "X509SerialNumber", PREFIX_DSIG);
    public static final QName TAG_dsig_X509SKI = new QName(NS_DSIG, "X509SKI", PREFIX_DSIG);
    public static final QName TAG_dsig_X509Certificate = new QName(NS_DSIG, "X509Certificate", PREFIX_DSIG);
    public static final QName TAG_dsig_X509SubjectName = new QName(NS_DSIG, "X509SubjectName", PREFIX_DSIG);

    public static final QName TAG_dsig_KeyName = new QName(NS_DSIG, "KeyName", PREFIX_DSIG);
    public static final QName TAG_dsig_KeyValue = new QName(NS_DSIG, "KeyValue", PREFIX_DSIG);
    public static final QName TAG_dsig_RSAKeyValue = new QName(NS_DSIG, "RSAKeyValue", PREFIX_DSIG);
    public static final QName TAG_dsig_Modulus = new QName(NS_DSIG, "Modulus", PREFIX_DSIG);
    public static final QName TAG_dsig_Exponent = new QName(NS_DSIG, "Exponent", PREFIX_DSIG);

    public static final QName TAG_dsig_DSAKeyValue = new QName(NS_DSIG, "DSAKeyValue", PREFIX_DSIG);
    public static final QName TAG_dsig_P = new QName(NS_DSIG, "P", PREFIX_DSIG);
    public static final QName TAG_dsig_Q = new QName(NS_DSIG, "Q", PREFIX_DSIG);
    public static final QName TAG_dsig_G = new QName(NS_DSIG, "G", PREFIX_DSIG);
    public static final QName TAG_dsig_Y = new QName(NS_DSIG, "Y", PREFIX_DSIG);
    public static final QName TAG_dsig_J = new QName(NS_DSIG, "J", PREFIX_DSIG);
    public static final QName TAG_dsig_Seed = new QName(NS_DSIG, "Seed", PREFIX_DSIG);
    public static final QName TAG_dsig_PgenCounter = new QName(NS_DSIG, "PgenCounter", PREFIX_DSIG);

    public static final String PREFIX_DSIG11 = "dsig11";
    public static final QName TAG_dsig11_ECKeyValue = new QName(NS_DSIG11, "ECKeyValue", PREFIX_DSIG11);
    public static final QName TAG_dsig11_ECParameters = new QName(NS_DSIG11, "ECParameters", PREFIX_DSIG11);
    public static final QName TAG_dsig11_NamedCurve = new QName(NS_DSIG11, "NamedCurve", PREFIX_DSIG11);
    public static final QName TAG_dsig11_PublicKey = new QName(NS_DSIG11, "PublicKey", PREFIX_DSIG11);

    public static final String NS_C14N_EXCL = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String NS_XMLDSIG_FILTER2 = "http://www.w3.org/2002/06/xmldsig-filter2";
    public static final String NS_XMLDSIG_ENVELOPED_SIGNATURE = NS_DSIG + "enveloped-signature";
    public static final String NS_XMLDSIG_SHA1 = NS_DSIG + "sha1";
    public static final String NS_XMLDSIG_HMACSHA1 = NS_DSIG + "hmac-sha1";
    public static final String NS_XMLDSIG_RSASHA1 = NS_DSIG + "rsa-sha1";
    public static final String NS_XMLDSIG_MANIFEST = NS_DSIG + "Manifest";

    public static final String NS_XMLDSIG_HMACSHA256 = NS_DSIG_MORE + "hmac-sha256";
    public static final String NS_XMLDSIG_HMACSHA384 = NS_DSIG_MORE + "hmac-sha384";
    public static final String NS_XMLDSIG_HMACSHA512 = NS_DSIG_MORE + "hmac-sha512";
    public static final String NS_XMLDSIG_RSASHA256 = NS_DSIG_MORE + "rsa-sha256";
    public static final String NS_XMLDSIG_RSASHA384 = NS_DSIG_MORE + "rsa-sha384";
    public static final String NS_XMLDSIG_RSASHA512 = NS_DSIG_MORE + "rsa-sha512";

    public static final String NS_XENC_TRIPLE_DES = NS_XMLENC + "tripledes-cbc";
    public static final String NS_XENC_AES128 = NS_XMLENC + "aes128-cbc";
    public static final String NS_XENC11_AES128_GCM = NS_XMLENC11 + "aes128-gcm";
    public static final String NS_XENC_AES192 = NS_XMLENC + "aes192-cbc";
    public static final String NS_XENC11_AES192_GCM = NS_XMLENC11 + "aes192-gcm";
    public static final String NS_XENC_AES256 = NS_XMLENC + "aes256-cbc";
    public static final String NS_XENC11_AES256_GCM = NS_XMLENC11 + "aes256-gcm";
    public static final String NS_XENC_RSA15 = NS_XMLENC + "rsa-1_5";
    public static final String NS_XENC_RSAOAEPMGF1P = NS_XMLENC + "rsa-oaep-mgf1p";
    public static final String NS_XENC11_RSAOAEP = NS_XMLENC11 + "rsa-oaep";

    public static final String NS_MGF1_SHA1 = NS_XMLENC11 + "mgf1sha1";
    public static final String NS_MGF1_SHA224 = NS_XMLENC11 + "mgf1sha224";
    public static final String NS_MGF1_SHA256 = NS_XMLENC11 + "mgf1sha256";
    public static final String NS_MGF1_SHA384 = NS_XMLENC11 + "mgf1sha384";
    public static final String NS_MGF1_SHA512 = NS_XMLENC11 + "mgf1sha512";

    public static final String NS_XENC_SHA256 = NS_XMLENC + "sha256";
    public static final String NS_XENC_SHA512 = NS_XMLENC + "sha512";

    public static final String PREFIX_C14N_EXCL = "c14nEx";
    public static final QName ATT_NULL_PrefixList = new QName(null, "PrefixList");
    public static final QName TAG_c14nExcl_InclusiveNamespaces = new QName(NS_C14N_EXCL, "InclusiveNamespaces", PREFIX_C14N_EXCL);

    public static final String NS_C14N_OMIT_COMMENTS = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
    public static final String NS_C14N_WITH_COMMENTS = NS_C14N_OMIT_COMMENTS + "#WithComments";
    public static final String NS_C14N_EXCL_OMIT_COMMENTS = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String NS_C14N_EXCL_WITH_COMMENTS = NS_C14N_EXCL_OMIT_COMMENTS + "WithComments";
    public static final String NS_C14N11_OMIT_COMMENTS = "http://www.w3.org/2006/12/xml-c14n11";
    public static final String NS_C14N11_WITH_COMMENTS = NS_C14N11_OMIT_COMMENTS + "#WithComments";

    public static final QName TAG_XOP_INCLUDE = new QName(NS_XOP, "Include", "xop");

    public static final String PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE = "PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE";
    public static final String PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION = "PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTION";
    public static final String PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTED_KEY = "PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTED_KEY";

    public static final String SIGNATURE_PARTS = "signatureParts";
    public static final String ENCRYPTION_PARTS = "encryptionParts";

    public static final Action SIGNATURE = new Action("Signature");
    public static final Action ENCRYPTION = new Action("Encryption");
    /**
     * Use {@link #ENCRYPTION} instead.
     */
    @Deprecated
    public static final Action ENCRYPT = ENCRYPTION;

    public static final QName TAG_dsigmore_RSAPSSPARAMS = new QName(NS_DSIG_MORE_2007_05, "RSAPSSParams", PREFIX_DSIG_MORE_PSS);
    public static final QName TAG_dsigmore_SALTLENGTH = new QName(NS_DSIG_MORE_2007_05, "SaltLength", PREFIX_DSIG_MORE_PSS);
    public static final QName TAG_dsigmore_TRAILERFIELD = new QName(NS_DSIG_MORE_2007_05, "TrailerField", PREFIX_DSIG_MORE_PSS);

    public static class Action extends ComparableType<Action> {
        public Action(String name) {
            super(name);
        }
    }

    public static final AlgorithmUsage Sym_Key_Wrap = new AlgorithmUsage("Sym_Key_Wrap");
    public static final AlgorithmUsage Asym_Key_Wrap = new AlgorithmUsage("Asym_Key_Wrap");
    public static final AlgorithmUsage Sym_Sig = new AlgorithmUsage("Sym_Sig");
    public static final AlgorithmUsage Asym_Sig = new AlgorithmUsage("Asym_Sig");
    public static final AlgorithmUsage Enc = new AlgorithmUsage("Enc");

    public static final AlgorithmUsage SigDig = new AlgorithmUsage("SigDig");
    public static final AlgorithmUsage EncDig = new AlgorithmUsage("EncDig");
    public static final AlgorithmUsage SigC14n = new AlgorithmUsage("SigC14n");
    public static final AlgorithmUsage SigTransform = new AlgorithmUsage("SigTransform");

    public static class AlgorithmUsage extends ComparableType<AlgorithmUsage> {
        public AlgorithmUsage(String name) {
            super(name);
        }
    }

    public enum ContentType {
        PLAIN,
        SIGNATURE,
        ENCRYPTION
    }

    public enum TransformMethod {
        XMLSecEvent,
        InputStream,
    }
}
