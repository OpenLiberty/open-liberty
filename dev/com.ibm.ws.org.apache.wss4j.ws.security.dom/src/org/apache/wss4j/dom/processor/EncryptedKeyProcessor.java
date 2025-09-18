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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.xml.crypto.dsig.XMLSignatureFactory;

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

    private final Provider provider;

    public EncryptedKeyProcessor() {
        this(null);
    }

    public EncryptedKeyProcessor(Provider provider) {
        this.provider = provider;
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
        if (id.length() != 0) {
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

        // Check BSP Compliance
        checkBSPCompliance(elem, encryptedKeyTransportMethod, data.getBSPEnforcer());

        //
        // Now lookup CipherValue.
        //
        Element xencCipherValue = EncryptionUtils.getCipherValueFromEncryptedData(elem);
        if (xencCipherValue == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noCipher");
        }

        Element keyInfoChildElement = getKeyInfoChildElement(elem, data);

        X509Certificate[] certs = null;
        STRParser.REFERENCE_TYPE referenceType = null;
        PublicKey publicKey = null;
        boolean symmetricKeyWrap = isSymmetricKeyWrap(encryptedKeyTransportMethod);
        if (!symmetricKeyWrap) {
            if (SecurityTokenReference.SECURITY_TOKEN_REFERENCE.equals(keyInfoChildElement.getLocalName())
                && WSConstants.WSSE_NS.equals(keyInfoChildElement.getNamespaceURI())) {
                STRParserParameters parameters = new STRParserParameters();
                parameters.setData(data);
                parameters.setStrElement(keyInfoChildElement);

                STRParser strParser = new EncryptedKeySTRParser();
                STRParserResult parserResult = strParser.parseSecurityTokenReference(parameters);

                certs = parserResult.getCertificates();
                publicKey = parserResult.getPublicKey();
                referenceType = parserResult.getCertificatesReferenceType();
            } else {
                certs = getCertificatesFromX509Data(keyInfoChildElement, data);
                if (certs == null) {
                    XMLSignatureFactory signatureFactory;
                    if (provider == null) {
                        // Try to install the Santuario Provider - fall back to the JDK provider if this does
                        // not work
                        try {
                            signatureFactory = XMLSignatureFactory.getInstance("DOM", "ApacheXMLDSig");
                        } catch (NoSuchProviderException ex) {
                            signatureFactory = XMLSignatureFactory.getInstance("DOM");
                        }
                    } else {
                        signatureFactory = XMLSignatureFactory.getInstance("DOM", provider);
                    }

                    publicKey = X509Util.parseKeyValue((Element)keyInfoChildElement.getParentNode(),
                                                       signatureFactory);
                }
            }

            if (publicKey == null && (certs == null || certs.length < 1 || certs[0] == null)) {
                throw new WSSecurityException(
                                          WSSecurityException.ErrorCode.FAILURE,
                                          "noCertsFound",
                                          new Object[] {"decryption (KeyId)"});
            }
            if (certs != null && certs.length > 0) {
                publicKey = certs[0].getPublicKey();
            }
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

        if (symmetricKeyWrap) {
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

    private PrivateKey getPrivateKey(
        RequestData data, X509Certificate[] certs, PublicKey publicKey
    ) throws WSSecurityException {
        try {
            if (certs != null) {
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
                EncryptionUtils.findEncryptedDataElement(refList.getOwnerDocument(),
                                                         wsDocInfo, uri);
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
                String jceDigestAlgorithm = "SHA-1";
                if (digestAlgorithm != null && digestAlgorithm.length() != 0) {
                    jceDigestAlgorithm = JCEMapper.translateURItoJCEID(digestAlgorithm);
                }

                MGF1ParameterSpec mgfParameterSpec = new MGF1ParameterSpec("SHA-1");
                if (WSConstants.KEYTRANSPORT_RSAOAEP_XENC11.equals(encryptedKeyTransportMethod)) {
                    String mgfAlgorithm = EncryptionUtils.getMGFAlgorithm(encryptedKeyElement);
                    if (WSConstants.MGF_SHA224.equals(mgfAlgorithm)) {
                        mgfParameterSpec = new MGF1ParameterSpec("SHA-224");
                    } else if (WSConstants.MGF_SHA256.equals(mgfAlgorithm)) {
                        mgfParameterSpec = new MGF1ParameterSpec("SHA-256");
                    } else if (WSConstants.MGF_SHA384.equals(mgfAlgorithm)) {
                        mgfParameterSpec = new MGF1ParameterSpec("SHA-384");
                    } else if (WSConstants.MGF_SHA512.equals(mgfAlgorithm)) {
                        mgfParameterSpec = new MGF1ParameterSpec("SHA-512");
                    }
                }

                PSource.PSpecified pSource = PSource.PSpecified.DEFAULT;
                byte[] pSourceBytes = EncryptionUtils.getPSource(encryptedKeyElement);
                if (pSourceBytes != null) {
                    pSource = new PSource.PSpecified(pSourceBytes);
                }

                oaepParameterSpec =
                    new OAEPParameterSpec(
                        jceDigestAlgorithm, "MGF1", mgfParameterSpec, pSource
                    );
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
                    EncryptionUtils.findEncryptedDataElement(refList.getOwnerDocument(),
                                                                    wsDocInfo, uri);
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

    private X509Certificate[] getCertificatesFromX509Data(
        Element keyInfoChildElement,
        RequestData data
    ) throws WSSecurityException {

        if (WSConstants.SIG_NS.equals(keyInfoChildElement.getNamespaceURI())
            && WSConstants.X509_DATA_LN.equals(keyInfoChildElement.getLocalName())) {
            data.getBSPEnforcer().handleBSPRule(BSPRule.R5426);

            Element x509Child = getFirstElement(keyInfoChildElement);

            if (x509Child != null && WSConstants.SIG_NS.equals(x509Child.getNamespaceURI())) {
                if (WSConstants.X509_ISSUER_SERIAL_LN.equals(x509Child.getLocalName())) {
                    DOMX509IssuerSerial issuerSerial = new DOMX509IssuerSerial(x509Child);
                    CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
                    cryptoType.setIssuerSerial(issuerSerial.getIssuer(), issuerSerial.getSerialNumber());
                    return data.getDecCrypto().getX509Certificates(cryptoType);
                } else if (WSConstants.X509_CERT_LN.equals(x509Child.getLocalName())) {
                    byte[] token = EncryptionUtils.getDecodedBase64EncodedData(x509Child);
                    if (token == null) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidCertData",
                                                      new Object[] {"0"});
                    }
                    try (InputStream in = new ByteArrayInputStream(token)) {
                        X509Certificate cert = data.getDecCrypto().loadCertificate(in);
                        if (cert != null) {
                            return new X509Certificate[]{cert};
                        }
                    } catch (IOException e) {
                        throw new WSSecurityException(
                            WSSecurityException.ErrorCode.SECURITY_TOKEN_UNAVAILABLE, e, "parseError"
                        );
                    }
                }
            }
        }

        return null;
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
    private List<WSDataRef> decryptDataRefs(Element refList, WSDocInfo docInfo,
                                            byte[] decryptedBytes, RequestData data
    ) throws WSSecurityException {
        //
        // At this point we have the decrypted session (symmetric) key. According
        // to W3C XML-Enc this key is used to decrypt _any_ references contained in
        // the reference list
        if (refList == null) {
            return null;
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
    private WSDataRef decryptDataRef(
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
            EncryptionUtils.findEncryptedDataElement(doc, docInfo, dataRefURI);
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
     * @throws WSSecurityException
     */
    private void checkBSPCompliance(
        Element elem, String encAlgo, BSPEnforcer bspEnforcer
    ) throws WSSecurityException {
        String attribute = elem.getAttributeNS(null, "Type");
        if (attribute != null && attribute.length() != 0) {
            bspEnforcer.handleBSPRule(BSPRule.R3209);
        }
        attribute = elem.getAttributeNS(null, "MimeType");
        if (attribute != null && attribute.length() != 0) {
            bspEnforcer.handleBSPRule(BSPRule.R5622);
        }
        attribute = elem.getAttributeNS(null, "Encoding");
        if (attribute != null && attribute.length() != 0) {
            bspEnforcer.handleBSPRule(BSPRule.R5623);
        }
        attribute = elem.getAttributeNS(null, "Recipient");
        if (attribute != null && attribute.length() != 0) {
            bspEnforcer.handleBSPRule(BSPRule.R5602);
        }

        // EncryptionAlgorithm must be RSA15, or RSAOEP.
        if (!(WSConstants.KEYTRANSPORT_RSA15.equals(encAlgo)
            || WSConstants.KEYTRANSPORT_RSAOAEP.equals(encAlgo)
            || WSConstants.KEYTRANSPORT_RSAOAEP_XENC11.equals(encAlgo))) {
            bspEnforcer.handleBSPRule(BSPRule.R5621);
        }
    }

}
