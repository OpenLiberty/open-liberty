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

package org.apache.wss4j.dom.processor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.token.DOMX509SKI;
import org.apache.xml.security.encryption.AgreementMethod;
import org.apache.xml.security.encryption.KeyDerivationMethod;
import org.apache.xml.security.encryption.XMLCipherUtil;
import org.apache.xml.security.encryption.keys.RecipientKeyInfo;
import org.apache.xml.security.encryption.keys.content.AgreementMethodImpl;
import org.apache.xml.security.encryption.params.KeyAgreementParameters;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.ECKeyValue;
import org.apache.xml.security.keys.content.keyvalues.KeyValueContent;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.EncryptionConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.crypto.AlgorithmSuiteValidator;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.DOMX509IssuerSerial;
import org.apache.wss4j.common.token.SecurityTokenReference;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.str.EncryptedKeySTRParser;
import org.apache.wss4j.dom.str.STRParser;
import org.apache.wss4j.dom.str.STRParserParameters;
import org.apache.wss4j.dom.str.STRParserResult;
import org.apache.wss4j.dom.util.EncryptionUtils;
import org.apache.wss4j.dom.util.SignatureUtils;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.util.X509Util;
import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.XMLCipher;

public class EncryptedKeyProcessor implements Processor {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(EncryptedKeyProcessor.class);

    public EncryptedKeyProcessor() {
    }

    public List<WSSecurityEngineResult> handleToken(
        Element elem,
        RequestData data
    ) throws WSSecurityException {
        return handleToken(elem, data, data.getAlgorithmSuite());
    }

    public List<WSSecurityEngineResult> handleToken(
        Element elem,
        RequestData data,
        AlgorithmSuite algorithmSuite
    ) throws WSSecurityException {
        LOG.debug("Found encrypted key element");

        // See if this key has already been processed. If so then just return the result
        String id = elem.getAttributeNS(null, "Id");
        if (!id.isEmpty()) {
             WSSecurityEngineResult result = data.getWsDocInfo().getResult(id);
             if (result != null
                 && WSConstants.ENCR == (Integer)result.get(WSSecurityEngineResult.TAG_ACTION)
             ) {
                 return Collections.singletonList(result);
             }
        }

        if (data.getCallbackHandler() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCallback");
        }
        //
        // lookup xenc:EncryptionMethod, get the Algorithm attribute to determine
        // how the key was encrypted. Then check if we support the algorithm
        //
        String encryptedKeyTransportMethod = X509Util.getEncAlgo(elem);
        if (encryptedKeyTransportMethod == null) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, "noEncAlgo"
            );
        }
        if (WSConstants.KEYTRANSPORT_RSA15.equals(encryptedKeyTransportMethod)
            && !data.isAllowRSA15KeyTransportAlgorithm()
            && (algorithmSuite == null
              || !algorithmSuite.getKeyWrapAlgorithms().contains(WSConstants.KEYTRANSPORT_RSA15))) {
            LOG.debug(
                "The Key transport method does not match the requirement"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }

        Element keyInfoChildElement = getKeyInfoChildElement(elem, data);
        boolean isDHKeyWrap = isDiffieHellmanKeyWrap(keyInfoChildElement);
        // Check BSP Compliance
        checkBSPCompliance(elem, encryptedKeyTransportMethod, isDHKeyWrap, data.getBSPEnforcer());

        //
        // Now lookup CipherValue.
        //
        Element xencCipherValue = EncryptionUtils.getCipherValueFromEncryptedData(elem);
        if (xencCipherValue == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noCipher");
        }

        X509Certificate[] certs = null;
        STRParser.REFERENCE_TYPE referenceType = null;
        PublicKey publicKey = null;
        boolean symmetricKeyWrap = isSymmetricKeyWrap(encryptedKeyTransportMethod);
        AgreementMethod agreementMethod = null;
        KeyDerivationMethod keyDerivationMethod = null;
        if (isDHKeyWrap) {
            // get key agreement method value
            agreementMethod = getAgreementMethodFromElement(keyInfoChildElement);
            keyDerivationMethod = getKeyDerivationFunction(agreementMethod);
            //  get the recipient key info element
            keyInfoChildElement = getRecipientKeyInfoChildElement(agreementMethod);
            if (keyInfoChildElement == null) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY, "noRecipientSecTokRef"
                );
            }
        }
        if (!symmetricKeyWrap || isDHKeyWrap) {
            CertificateResult certificateResult = getPublicKey(keyInfoChildElement, data);
            certs = certificateResult.getCerts();
            publicKey = certificateResult.getPublicKey();
            referenceType = certificateResult.getCertificatesReferenceType();
        }

        // Check for compliance against the defined AlgorithmSuite
        if (algorithmSuite != null) {
            AlgorithmSuiteValidator algorithmSuiteValidator = new
                AlgorithmSuiteValidator(algorithmSuite);

            if (!symmetricKeyWrap) {
                algorithmSuiteValidator.checkAsymmetricKeyLength(publicKey);
            }
            algorithmSuiteValidator.checkEncryptionKeyWrapAlgorithm(
                encryptedKeyTransportMethod
            );
            if (agreementMethod != null) {
                algorithmSuiteValidator.checkKeyAgreementMethodAlgorithm(
                        agreementMethod.getAlgorithm()
                );
            }
            if (keyDerivationMethod != null) {
                algorithmSuiteValidator.checkKeyDerivationFunction(
                        keyDerivationMethod.getAlgorithm()
                );
            }
        }

        byte[] encryptedEphemeralKey = null;
        byte[] decryptedBytes = null;
        Element refList =
            XMLUtils.getDirectChildElement(elem, "ReferenceList", WSConstants.ENC_NS);

        // Get the key bytes from CipherValue directly or via an attachment
        String xopUri = EncryptionUtils.getXOPURIFromCipherValue(xencCipherValue);
        if (xopUri != null && xopUri.startsWith("cid:")) {
            encryptedEphemeralKey = WSSecurityUtil.getBytesFromAttachment(xopUri, data);
        } else {
            encryptedEphemeralKey = EncryptionUtils.getDecodedBase64EncodedData(xencCipherValue);
        }

        if (isDHKeyWrap) {
            PrivateKey privateKey = getPrivateKey(data, certs, publicKey);
            decryptedBytes = getDiffieHellmanDecryptedBytes(data, agreementMethod,
                    encryptedKeyTransportMethod, encryptedEphemeralKey, privateKey);
        } else if (symmetricKeyWrap) {
            decryptedBytes = getSymmetricDecryptedBytes(data, data.getWsDocInfo(), keyInfoChildElement, refList);
        } else {
            PrivateKey privateKey = getPrivateKey(data, certs, publicKey);
            decryptedBytes = getAsymmetricDecryptedBytes(data, data.getWsDocInfo(), encryptedKeyTransportMethod,
                                                         encryptedEphemeralKey, refList,
                                                         elem, privateKey);
        }

        List<WSDataRef> dataRefs = decryptDataRefs(refList, data.getWsDocInfo(), decryptedBytes, data);

        WSSecurityEngineResult result = new WSSecurityEngineResult(
                WSConstants.ENCR,
                decryptedBytes,
                encryptedEphemeralKey,
                dataRefs,
                certs
            );
        result.put(
            WSSecurityEngineResult.TAG_ENCRYPTED_KEY_TRANSPORT_METHOD,
            encryptedKeyTransportMethod
        );
        result.put(WSSecurityEngineResult.TAG_TOKEN_ELEMENT, elem);
        String tokenId = elem.getAttributeNS(null, "Id");
        if (tokenId.length() != 0) {
            result.put(WSSecurityEngineResult.TAG_ID, tokenId);
        }
        if (referenceType != null) {
            result.put(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE, referenceType);
        }
        if (publicKey != null) {
            result.put(WSSecurityEngineResult.TAG_PUBLIC_KEY, publicKey);
        }
        data.getWsDocInfo().addResult(result);
        data.getWsDocInfo().addTokenElement(elem);
        return Collections.singletonList(result);
    }

    /**
     * Resolve the KeyInfoType child element to locate the public key (with the X509Certificate chain if given )
     * to use to decrypt the EncryptedKey.
     *
     * @param keyValueElement The element to get the child element from
     * @param data            The RequestData context
     * @return The CertificateResult object containing the public key and optionally X509Certificate chain
     * @throws WSSecurityException an error occurred when trying to resolve the key info
     */
    private CertificateResult getPublicKey(Element keyValueElement, RequestData data) throws WSSecurityException {
        CertificateResult.Builder builder = CertificateResult.Builder.create();

        if (SecurityTokenReference.SECURITY_TOKEN_REFERENCE.equals(keyValueElement.getLocalName())
                && WSConstants.WSSE_NS.equals(keyValueElement.getNamespaceURI())) {
            STRParserParameters parameters = new STRParserParameters();
            parameters.setData(data);
            parameters.setStrElement(keyValueElement);

            STRParser strParser = new EncryptedKeySTRParser();
            STRParserResult result = strParser.parseSecurityTokenReference(parameters);
            builder.certificates(result.getCertificates());
            builder.publicKey(result.getPublicKey());
            builder.certificatesReferenceType(result.getCertificatesReferenceType());
        } else {
            X509Certificate[] certs = getCertificatesFromX509Data(keyValueElement, data);
            builder.certificates(certs);
            if (certs == null || certs.length == 0) {
                PublicKey publicKey = getPublicKeyFromKeyValue(keyValueElement);
                builder.publicKey(publicKey);
            }
        }
        return builder.build();
    }

    private PublicKey getPublicKeyFromKeyValue(Element keyValueElement) throws WSSecurityException {
        PublicKey publicKey = null;
        KeyValueContent keyValue;
        try {
            Element keyValueChild = getFirstElement(keyValueElement);
            if (keyValueChild == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "unsupportedKeyInfo");
            }
            switch (keyValueChild.getLocalName()) {
                case "ECKeyValue":
                    keyValue = new ECKeyValue(keyValueChild, Constants.SignatureSpec11NS);
                    break;
                case "RSAKeyValue":
                    keyValue = new RSAKeyValue(keyValueChild, Constants.SignatureSpecNS);
                    break;
                case "DSAKeyValue":
                    keyValue = new DSAKeyValue(keyValueChild, Constants.SignatureSpecNS);
                    break;
                default:
                    throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "unsupportedKeyInfo");
            }

            publicKey = keyValue.getPublicKey();
        } catch (XMLSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "unsupportedKeyInfo");
        }
        return publicKey;
    }

    private PrivateKey getPrivateKey(
        RequestData data, X509Certificate[] certs, PublicKey publicKey
    ) throws WSSecurityException {
        try {
            if (certs != null && certs.length > 0) {
                return data.getDecCrypto().getPrivateKey(certs[0], data.getCallbackHandler());
            }
            return data.getDecCrypto().getPrivateKey(publicKey, data.getCallbackHandler());
        } catch (WSSecurityException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        }
    }

    private static byte[] getSymmetricDecryptedBytes(
        RequestData data,
        WSDocInfo wsDocInfo,
        Element keyInfoChildElement,
        Element refList
    ) throws WSSecurityException {
        // Get the (first) encryption algorithm
        String uri = getFirstDataRefURI(refList);
        String algorithmURI = null;
        if (uri != null) {
            Element ee =
                EncryptionUtils.findEncryptedDataElement(wsDocInfo, uri);
            algorithmURI = X509Util.getEncAlgo(ee);
        }
        return X509Util.getSecretKey(keyInfoChildElement, algorithmURI, data.getCallbackHandler());
    }

    private static byte[] getAsymmetricDecryptedBytes(
        RequestData data,
        WSDocInfo wsDocInfo,
        String encryptedKeyTransportMethod,
        byte[] encryptedEphemeralKey,
        Element refList,
        Element encryptedKeyElement,
        PrivateKey privateKey
    ) throws WSSecurityException {
        if (data.getDecCrypto() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noDecCryptoFile");
        }
        String cryptoProvider = data.getDecCrypto().getCryptoProvider();
        Cipher cipher = KeyUtils.getCipherInstance(encryptedKeyTransportMethod, cryptoProvider);
        try {
            OAEPParameterSpec oaepParameterSpec = null;
            if (WSConstants.KEYTRANSPORT_RSAOAEP.equals(encryptedKeyTransportMethod)
                || WSConstants.KEYTRANSPORT_RSAOAEP_XENC11.equals(encryptedKeyTransportMethod)) {
                // Get the DigestMethod if it exists
                String digestAlgorithm = EncryptionUtils.getDigestAlgorithm(encryptedKeyElement);
                String mgfAlgorithm = EncryptionUtils.getMGFAlgorithm(encryptedKeyElement);
                byte[] pSourceBytes = EncryptionUtils.getPSource(encryptedKeyElement);
                oaepParameterSpec = XMLCipherUtil.constructOAEPParameters(encryptedKeyTransportMethod,
                        digestAlgorithm, mgfAlgorithm, pSourceBytes);
            }

            if (oaepParameterSpec == null) {
                cipher.init(Cipher.UNWRAP_MODE, privateKey);
            } else {
                cipher.init(Cipher.UNWRAP_MODE, privateKey, oaepParameterSpec);
            }
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        }

        try {
            String keyAlgorithm = JCEMapper.translateURItoJCEID(encryptedKeyTransportMethod);
            return cipher.unwrap(encryptedEphemeralKey, keyAlgorithm, Cipher.SECRET_KEY).getEncoded();
        } catch (IllegalStateException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        } catch (Exception ex) {
            return getRandomKey(refList, wsDocInfo);
        }
    }

    /**
     * Method decrypts encryptedEphemeralKey using Key Agreement algorithm to derive symmetric key
     * for decryption of the key.
     *
     * @param data RequestData context
     * @param agreementMethod AgreementMethod element
     * @param encryptedKeyTransportMethod Algorithm used to encrypt the key
     * @param encryptedEphemeralKey Encrypted ephemeral/transport key
     * @param privateKey Private key of the recipient
     * @return Decrypted bytes of the ephemeral/transport key
     * @throws WSSecurityException if the key decryption fails
     */
    private static byte[] getDiffieHellmanDecryptedBytes(
            RequestData data,
            AgreementMethod agreementMethod,
            String encryptedKeyTransportMethod,
            byte[] encryptedEphemeralKey,
            PrivateKey privateKey
    ) throws WSSecurityException {

        SecretKey kek;
        try {
            KeyAgreementParameters parameterSpec = XMLCipherUtil.constructRecipientKeyAgreementParameters(
                    encryptedKeyTransportMethod, agreementMethod, privateKey);

            kek = org.apache.xml.security.utils.KeyUtils.aesWrapKeyWithDHGeneratedKey(parameterSpec);
        } catch (XMLSecurityException ex) {
            LOG.debug("Error occurred while resolving the Diffie Hellman key: " + ex.getMessage());
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        }

        String cryptoProvider = data.getDecCrypto().getCryptoProvider();
        Cipher cipher = KeyUtils.getCipherInstance(encryptedKeyTransportMethod, cryptoProvider);

        try {
            cipher.init(Cipher.UNWRAP_MODE, kek);
            String keyAlgorithm = JCEMapper.translateURItoJCEID(encryptedKeyTransportMethod);
            return cipher.unwrap(encryptedEphemeralKey, keyAlgorithm, Cipher.SECRET_KEY).getEncoded();
        } catch (InvalidKeyException | NoSuchAlgorithmException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, ex);
        }
    }

    /**
     * if keyInfo element contains AgreementMethod element then check if it is supported EC Diffie-Hellman key agreement algorithm
     *
     * @param keyInfoChildElement The KeyInfo child element
     * @return true if AgreementMethod element is present and DH algorithm supported and false if AgreementMethod element is not present
     * @throws WSSecurityException if AgreementMethod element is present but DH algorithm is not supported
     */
    private boolean isDiffieHellmanKeyWrap(Element keyInfoChildElement) throws WSSecurityException {
        if (EncryptionConstants._TAG_AGREEMENTMETHOD.equals(keyInfoChildElement.getLocalName())
                && WSConstants.ENC_NS.equals(keyInfoChildElement.getNamespaceURI())) {
            String algorithmURI = keyInfoChildElement.getAttributeNS(null, "Algorithm");
            // Only ECDH_ES is supported for AgreementMethod
            if (!(WSConstants.AGREEMENT_METHOD_ECDH_ES.equals(algorithmURI)
                || WSConstants.AGREEMENT_METHOD_X25519.equals(algorithmURI)
                || WSConstants.AGREEMENT_METHOD_X448.equals(algorithmURI))) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM,
                        "unknownAlgorithm", new Object[]{algorithmURI});
            }
            return true;
        }
        return false;
    }

    /**
     * Parse keyInfo content to AgreementMethod object.
     *
     * @param keyInfoChildElement The KeyInfo child element containing AgreementMethod data.
     * @return the {@link AgreementMethod} object.
     * @throws WSSecurityException if AgreementMethod element is invalid.
     */
    private AgreementMethod getAgreementMethodFromElement(Element keyInfoChildElement) throws WSSecurityException {
        try {
            return new AgreementMethodImpl(keyInfoChildElement);
        } catch (XMLSecurityException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, ex);
        }
    }

    /**
     * Get the RecipientKeyInfo child element from the AgreementMethod element.
     *
     * @param agreementMethod The AgreementMethod element
     * @return the RecipientKeyInfo child element which contains the recipient's public key.
     * @throws WSSecurityException if the agreementMethod is null or RecipientKeyInfo element can not be retrieved.
     */
    private Element getRecipientKeyInfoChildElement(AgreementMethod agreementMethod) throws WSSecurityException {
        if (agreementMethod == null) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY, "noAgreementMethod"
            );
        }
        try {
            RecipientKeyInfo recipientKeyInfo = agreementMethod.getRecipientKeyInfo();
            if (recipientKeyInfo == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noRecipientKeyInfo");
            }
            Element receiverKeyInfoElement = recipientKeyInfo.getElement();
            return getFirstElement(receiverKeyInfoElement);
        } catch (XMLSecurityException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, ex);
        }
    }

    private static boolean isSymmetricKeyWrap(String transportAlgorithm) {
        return XMLCipher.AES_128_KeyWrap.equals(transportAlgorithm)
            || XMLCipher.AES_192_KeyWrap.equals(transportAlgorithm)
            || XMLCipher.AES_256_KeyWrap.equals(transportAlgorithm)
            || XMLCipher.TRIPLEDES_KeyWrap.equals(transportAlgorithm)
            || XMLCipher.CAMELLIA_128_KeyWrap.equals(transportAlgorithm)
            || XMLCipher.CAMELLIA_192_KeyWrap.equals(transportAlgorithm)
            || XMLCipher.CAMELLIA_256_KeyWrap.equals(transportAlgorithm)
            || XMLCipher.SEED_128_KeyWrap.equals(transportAlgorithm);
    }

    /**
     * Generates a random secret key using the algorithm specified in the
     * first DataReference URI
     */
    private static byte[] getRandomKey(Element refList, WSDocInfo wsDocInfo) throws WSSecurityException {
        try {
            String alg = "AES";
            int size = 16;
            String uri = getFirstDataRefURI(refList);

            if (uri != null) {
                Element ee =
                    EncryptionUtils.findEncryptedDataElement(wsDocInfo, uri);
                String algorithmURI = X509Util.getEncAlgo(ee);
                alg = JCEMapper.getJCEKeyAlgorithmFromURI(algorithmURI);
                size = KeyUtils.getKeyLength(algorithmURI);
            }
            KeyGenerator kgen = KeyGenerator.getInstance(alg);
            kgen.init(size * 8);
            SecretKey k = kgen.generateKey();
            return k.getEncoded();
        } catch (Throwable ex) {
            // Fallback to just using AES to avoid attacks on EncryptedData algorithms
            try {
                KeyGenerator kgen = KeyGenerator.getInstance("AES");
                kgen.init(128);
                SecretKey k = kgen.generateKey();
                return k.getEncoded();
            } catch (NoSuchAlgorithmException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, e);
            }
        }
    }

    private static String getFirstDataRefURI(Element refList) {
        // Lookup the references that are encrypted with this key
        if (refList != null) {
            for (Node node = refList.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (Node.ELEMENT_NODE == node.getNodeType()
                        && WSConstants.ENC_NS.equals(node.getNamespaceURI())
                        && "DataReference".equals(node.getLocalName())) {
                    String dataRefURI = ((Element) node).getAttributeNS(null, "URI");
                    return XMLUtils.getIDFromReference(dataRefURI);
                }
            }
        }
        return null;
    }

    private Element getKeyInfoChildElement(
        Element xencEncryptedKey, RequestData data
    ) throws WSSecurityException {
        Element keyInfo =
            XMLUtils.getDirectChildElement(xencEncryptedKey, "KeyInfo", WSConstants.SIG_NS);
        if (keyInfo != null) {
            Element strElement = null;

            int result = 0;
            Node node = keyInfo.getFirstChild();
            while (node != null) {
                if (Node.ELEMENT_NODE == node.getNodeType()) {
                    result++;
                    strElement = (Element)node;
                }
                node = node.getNextSibling();
            }
            if (result != 1) {
                data.getBSPEnforcer().handleBSPRule(BSPRule.R5424);
            }

            if (strElement == null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY, "noSecTokRef"
                );
            }

            return strElement;
        } else {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noKeyinfo");
        }
    }

    /**
     * Method retrieved the KeyDerivationMethod child element from the AgreementMethod object.
     *
     * @param agreementMethod object containing th KeyDerivationMethod.
     * @return the {@link KeyDerivationMethod} object or null if no KeyDerivation element is specified in the agreementMethod.
     * @throws WSSecurityException if KeyDerivationMethod can not be parsed from Element structure.
     */
    private KeyDerivationMethod getKeyDerivationFunction(AgreementMethod agreementMethod) throws WSSecurityException {
        if (agreementMethod == null) {
            return null;
        }
        try {
            return agreementMethod.getKeyDerivationMethod();
        } catch (XMLSecurityException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, ex);
        }
    }

    private X509Certificate[] getCertificatesFromX509Data(
        Element keyInfoChildElement,
        RequestData data
    ) throws WSSecurityException {
        X509Certificate[] certs = new X509Certificate[0];

        if (WSConstants.SIG_NS.equals(keyInfoChildElement.getNamespaceURI())
            && WSConstants.X509_DATA_LN.equals(keyInfoChildElement.getLocalName())) {
            data.getBSPEnforcer().handleBSPRule(BSPRule.R5426);

            Element issuerSerialElement = XMLUtils.getDirectChildElement(keyInfoChildElement, WSS4JConstants.X509_ISSUER_SERIAL_LN,
                    WSS4JConstants.SIG_NS);
            if (issuerSerialElement != null) {
                DOMX509IssuerSerial issuerSerial = new DOMX509IssuerSerial(issuerSerialElement);
                CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
                cryptoType.setIssuerSerial(issuerSerial.getIssuer(), issuerSerial.getSerialNumber());
                certs = data.getDecCrypto().getX509Certificates(cryptoType);
            }

            Element skiElement = XMLUtils.getDirectChildElement(keyInfoChildElement, WSS4JConstants.X509_SKI_LN, WSS4JConstants.SIG_NS);
            if (skiElement != null && certs.length == 0) {
                DOMX509SKI x509SKI = new DOMX509SKI(skiElement);
                CryptoType cryptoType = new CryptoType(CryptoType.TYPE.SKI_BYTES);
                cryptoType.setBytes(x509SKI.getSKIBytes());
                certs = data.getDecCrypto().getX509Certificates(cryptoType);
            }

            Element x509CertElement = XMLUtils.getDirectChildElement(keyInfoChildElement,
                WSS4JConstants.X509_CERT_LN, WSS4JConstants.SIG_NS);
            if (x509CertElement != null && certs.length == 0) {
                byte[] token = EncryptionUtils.getDecodedBase64EncodedData(x509CertElement);
                if (token == null || token.length == 0) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidCertData",
                                                  new Object[] {"0"});
                }
                try (InputStream in = new ByteArrayInputStream(token)) {
                    X509Certificate cert = data.getDecCrypto().loadCertificate(in);
                    if (cert != null) {
                        certs = new X509Certificate[]{cert};
                    }
                } catch (IOException e) {
                    throw new WSSecurityException(
                        WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "parseError"
                    );
                }
            }
        }

        return certs;
    }

    private Element getFirstElement(Element element) {
        for (Node currentChild = element.getFirstChild();
             currentChild != null;
             currentChild = currentChild.getNextSibling()
        ) {
            if (Node.ELEMENT_NODE == currentChild.getNodeType()) {
                return (Element) currentChild;
            }
        }
        return null;
    }

    /**
     * Decrypt all data references
     */
    protected List<WSDataRef> decryptDataRefs(Element refList, WSDocInfo docInfo,
                                            byte[] decryptedBytes, RequestData data
    ) throws WSSecurityException {
        //
        // At this point we have the decrypted session (symmetric) key. According
        // to W3C XML-Enc this key is used to decrypt _any_ references contained in
        // the reference list
        if (refList == null) {
            return Collections.emptyList();
        }

        List<WSDataRef> dataRefs = new ArrayList<>();
        for (Node node = refList.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (Node.ELEMENT_NODE == node.getNodeType()
                    && WSConstants.ENC_NS.equals(node.getNamespaceURI())
                    && "DataReference".equals(node.getLocalName())) {
                String dataRefURI = ((Element) node).getAttributeNS(null, "URI");
                dataRefURI = XMLUtils.getIDFromReference(dataRefURI);

                WSDataRef dataRef =
                    decryptDataRef(refList.getOwnerDocument(), dataRefURI, docInfo, decryptedBytes, data);
                dataRefs.add(dataRef);
            }
        }

        return dataRefs;
    }

    /**
     * Decrypt an EncryptedData element referenced by dataRefURI
     */
    protected WSDataRef decryptDataRef(
        Document doc,
        String dataRefURI,
        WSDocInfo docInfo,
        byte[] decryptedData,
        RequestData data
    ) throws WSSecurityException {
        LOG.debug("found data reference: {}", dataRefURI);
        //
        // Find the encrypted data element referenced by dataRefURI
        //
        Element encryptedDataElement =
            EncryptionUtils.findEncryptedDataElement(docInfo, dataRefURI);
        if (encryptedDataElement != null && data.isRequireSignedEncryptedDataElements()) {
            List<WSSecurityEngineResult> signedResults =
                docInfo.getResultsByTag(WSConstants.SIGN);
            SignatureUtils.verifySignedElement(encryptedDataElement, signedResults);
        }
        //
        // Prepare the SecretKey object to decrypt EncryptedData
        //
        String symEncAlgo = X509Util.getEncAlgo(encryptedDataElement);

        // EncryptionAlgorithm cannot be null
        if (symEncAlgo == null) {
            LOG.warn("No encryption algorithm was specified in the request");
            throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, "badEncAlgo",
                                          new Object[] {null});

        }
        // EncryptionAlgorithm must be 3DES, or AES128, or AES256
        if (!WSConstants.TRIPLE_DES.equals(symEncAlgo)
            && !WSConstants.AES_128.equals(symEncAlgo)
            && !WSConstants.AES_128_GCM.equals(symEncAlgo)
            && !WSConstants.AES_256.equals(symEncAlgo)
            && !WSConstants.AES_256_GCM.equals(symEncAlgo)) {
            data.getBSPEnforcer().handleBSPRule(BSPRule.R5620);
        }

        SecretKey symmetricKey = null;
        try {
            symmetricKey = KeyUtils.prepareSecretKey(symEncAlgo, decryptedData);
        } catch (IllegalArgumentException ex) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, ex, "badEncAlgo",
                new Object[] {symEncAlgo});
        }

        // Check for compliance against the defined AlgorithmSuite
        AlgorithmSuite algorithmSuite = data.getAlgorithmSuite();
        if (algorithmSuite != null) {
            AlgorithmSuiteValidator algorithmSuiteValidator = new
                AlgorithmSuiteValidator(algorithmSuite);

            algorithmSuiteValidator.checkSymmetricKeyLength(symmetricKey.getEncoded().length);
            algorithmSuiteValidator.checkSymmetricEncryptionAlgorithm(symEncAlgo);
        }

        return EncryptionUtils.decryptEncryptedData(
            doc, dataRefURI, encryptedDataElement, symmetricKey, symEncAlgo, data.getAttachmentCallbackHandler(),
            data.getEncryptionSerializer()
        );
    }

    /**
     * A method to check that the EncryptedKey is compliant with the BSP spec.
     * @throws WSSecurityException if the EncryptedKey is not BSP compliant
     */
    private void checkBSPCompliance(
            Element elem, String encAlgo,
            boolean useKeyWrap,
            BSPEnforcer bspEnforcer
    ) throws WSSecurityException {
        String attribute = elem.getAttributeNS(null, "Type");
        if (attribute != null && !attribute.isEmpty()) {
            bspEnforcer.handleBSPRule(BSPRule.R3209);
        }
        attribute = elem.getAttributeNS(null, "MimeType");
        if (attribute != null && !attribute.isEmpty()) {
            bspEnforcer.handleBSPRule(BSPRule.R5622);
        }
        attribute = elem.getAttributeNS(null, "Encoding");
        if (attribute != null && !attribute.isEmpty()) {
            bspEnforcer.handleBSPRule(BSPRule.R5623);
        }
        attribute = elem.getAttributeNS(null, "Recipient");
        if (attribute != null && !attribute.isEmpty()) {
            bspEnforcer.handleBSPRule(BSPRule.R5602);
        }

        if (useKeyWrap) {
            if (!(WSConstants.KEYWRAP_AES128.equals(encAlgo)
                    || WSConstants.KEYWRAP_AES192.equals(encAlgo)
                    || WSConstants.KEYWRAP_AES256.equals(encAlgo)
                    || WSConstants.KEYWRAP_TRIPLEDES.equals(encAlgo))) {
                bspEnforcer.handleBSPRule(BSPRule.R5625);
            }
        } else {
            // EncryptionAlgorithm must be RSA15, or RSAOEP.
            if (!(WSConstants.KEYTRANSPORT_RSA15.equals(encAlgo)
                    || WSConstants.KEYTRANSPORT_RSAOAEP.equals(encAlgo)
                    || WSConstants.KEYTRANSPORT_RSAOAEP_XENC11.equals(encAlgo))) {
                bspEnforcer.handleBSPRule(BSPRule.R5621);
            }
        }
    }
}
