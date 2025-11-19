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

package org.apache.wss4j.common.crypto;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.*;
import java.util.Set;

import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.exceptions.DERDecodingException;
import org.apache.xml.security.utils.DERDecoderUtils;
import org.apache.xml.security.utils.KeyUtils;

/**
 * Validate signature/encryption/etc. algorithms against an AlgorithmSuite policy.
 */
public class AlgorithmSuiteValidator {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(AlgorithmSuiteValidator.class);

    private final AlgorithmSuite algorithmSuite;

    public AlgorithmSuiteValidator(
        AlgorithmSuite algorithmSuite
    ) {
        this.algorithmSuite = algorithmSuite;
    }

    /**
     * Check the Signature Method
     */
    public void checkSignatureMethod(
        String signatureMethod
    ) throws WSSecurityException {
        Set<String> allowedSignatureMethods = algorithmSuite.getSignatureMethods();
        if (!allowedSignatureMethods.isEmpty()
            && !allowedSignatureMethods.contains(signatureMethod)) {
            LOG.warn(
                "SignatureMethod " + signatureMethod + " does not match required values"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }

    /**
     * Check the C14n Algorithm
     */
    public void checkC14nAlgorithm(
        String c14nAlgorithm
    ) throws WSSecurityException {
        Set<String> allowedC14nAlgorithms = algorithmSuite.getC14nAlgorithms();
        if (!allowedC14nAlgorithms.isEmpty() && !allowedC14nAlgorithms.contains(c14nAlgorithm)) {
            LOG.warn(
                "C14nMethod " + c14nAlgorithm + " does not match required value"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }

    /**
     * Check the Signature Algorithms
     */
    public void checkSignatureAlgorithms(
        XMLSignature xmlSignature
    ) throws WSSecurityException {
        // Signature Algorithm
        String signatureMethod =
            xmlSignature.getSignedInfo().getSignatureMethod().getAlgorithm();
        checkSignatureMethod(signatureMethod);

        // C14n Algorithm
        String c14nMethod =
            xmlSignature.getSignedInfo().getCanonicalizationMethod().getAlgorithm();
        checkC14nAlgorithm(c14nMethod);

        for (Object refObject : xmlSignature.getSignedInfo().getReferences()) {
            Reference reference = (Reference)refObject;
            // Digest Algorithm
            String digestMethod = reference.getDigestMethod().getAlgorithm();
            Set<String> allowedDigestAlgorithms = algorithmSuite.getDigestAlgorithms();
            if (!allowedDigestAlgorithms.isEmpty()
                    && !allowedDigestAlgorithms.contains(digestMethod)) {
                LOG.warn(
                    "DigestMethod " + digestMethod + " does not match required value"
                );
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            }

            // Transform Algorithms
            for (int i = 0; i < reference.getTransforms().size(); i++) {
                Transform transform = (Transform)reference.getTransforms().get(i);
                String algorithm = transform.getAlgorithm();
                Set<String> allowedTransformAlgorithms =
                        algorithmSuite.getTransformAlgorithms();
                if (!allowedTransformAlgorithms.isEmpty()
                        && !allowedTransformAlgorithms.contains(algorithm)) {
                    LOG.warn(
                        "Transform method " + algorithm + " does not match required value"
                    );
                    throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
                }
            }
        }
    }

    public void checkEncryptionKeyWrapAlgorithm(
        String keyWrapAlgorithm
    ) throws WSSecurityException {
        Set<String> keyWrapAlgorithms = algorithmSuite.getKeyWrapAlgorithms();
        if (!keyWrapAlgorithms.isEmpty()
            && !keyWrapAlgorithms.contains(keyWrapAlgorithm)) {
            LOG.warn(
                "The Key transport method does not match the requirement"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }
    // Liberty Change Start: Backport 4.x
    public void checkKeyAgreementMethodAlgorithm(
            String keyAgreementMethodAlgorithm
    ) throws WSSecurityException {
        Set<String> keyAgreementMethodAlgorithms = algorithmSuite.getKeyAgreementMethodAlgorithms();
        if (!keyAgreementMethodAlgorithms.isEmpty()
                && !keyAgreementMethodAlgorithms.contains(keyAgreementMethodAlgorithm)) {
            LOG.warn(
                    "The Key agreement method does not match the requirement"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }

    /**
     * Method to check the Key Derivation algorithm is on the approved list  of the
     * AlgorithmSuite configuration.
     * @param keyDerivationFunction the key derivation function to be validated
     * @throws WSSecurityException if the approved list is not empty and the key
     * derivation function is not on the list
     */
    public void checkKeyDerivationFunction(
            String keyDerivationFunction
    ) throws WSSecurityException {
        Set<String> keyDerivationFunctions = algorithmSuite.getDerivedKeyAlgorithms();
        
        for (String function : keyDerivationFunctions) {
            // Process each string element in the set
        }
        if (!keyDerivationFunctions.isEmpty()
                && !keyDerivationFunctions.contains(keyDerivationFunction)) {
            LOG.warn(
                    "The Key derivation function does not match the requirement"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }
    // Liberty Change End

    public void checkSymmetricEncryptionAlgorithm(
        String symmetricAlgorithm
    ) throws WSSecurityException {
        Set<String> encryptionMethods = algorithmSuite.getEncryptionMethods();
        if (!encryptionMethods.isEmpty()
            && !encryptionMethods.contains(symmetricAlgorithm)) {
            LOG.warn(
                "The encryption algorithm does not match the requirement"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }

    /**
     * Check the asymmetric key length
     */
    public void checkAsymmetricKeyLength(
        X509Certificate[] x509Certificates
    ) throws WSSecurityException {
        if (x509Certificates == null) {
            return;
        }

        for (X509Certificate cert : x509Certificates) {
            checkAsymmetricKeyLength(cert.getPublicKey());
        }
    }

    /**
     * Check the asymmetric key length
     */
    public void checkAsymmetricKeyLength(
        X509Certificate x509Certificate
    ) throws WSSecurityException {
        if (x509Certificate == null) {
            return;
        }

        checkAsymmetricKeyLength(x509Certificate.getPublicKey());
    }

    /**
     * Check the asymmetric key length
     */
    public void checkAsymmetricKeyLength(
        PublicKey publicKey
    ) throws WSSecurityException {
        if (publicKey == null) {
            return;
        }
        if (publicKey instanceof RSAPublicKey) {
            int modulus = ((RSAPublicKey)publicKey).getModulus().bitLength();
            if (modulus < algorithmSuite.getMinimumAsymmetricKeyLength()
                || modulus > algorithmSuite.getMaximumAsymmetricKeyLength()) {
                LOG.warn(
                    "The asymmetric key length does not match the requirement"
                );
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            }
        } else if (publicKey instanceof DSAPublicKey) {
            int length = ((DSAPublicKey)publicKey).getParams().getP().bitLength();
            if (length < algorithmSuite.getMinimumAsymmetricKeyLength()
                || length > algorithmSuite.getMaximumAsymmetricKeyLength()) {
                LOG.warn(
                    "The asymmetric key length does not match the requirement"
                );
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            }
        } else if (publicKey instanceof ECPublicKey) {
            final ECPublicKey ecpriv = (ECPublicKey) publicKey;
            final java.security.spec.ECParameterSpec spec = ecpriv.getParams();
            int length = spec.getOrder().bitLength();
            if (length < algorithmSuite.getMinimumEllipticCurveKeyLength()
                    || length > algorithmSuite.getMaximumEllipticCurveKeyLength()) {
                LOG.warn("The elliptic curve key length does not match the requirement");
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            }
        } else {
            // Liberty Change Start: Backport 4.x
            // Try with last supported key types EdEC and XDH
            int keySize = getEdECndXDHKeyLength(publicKey);
            if (keySize < algorithmSuite.getMinimumEllipticCurveKeyLength()
                    || keySize > algorithmSuite.getMaximumEllipticCurveKeyLength()) {
                LOG.warn(
                        "The asymmetric key length does not match the requirement"
                );
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            }
        }
    }

    /**
     * A generic method to determinate key length for keys x25519, x448, ed25519 and ed448 keys. Method does not rely on
     * any specific implementation of the key, but uses OID to determine the key type.
     *
     * @param publicKey the public key to check the key length
     * @return the key length in bits
     * @throws WSSecurityException if the key is not  EdEC or XDH or if length can not be determined
     */
    private int getEdECndXDHKeyLength(PublicKey publicKey) throws  WSSecurityException {
        String keyAlgorithmOId;
        try {
            keyAlgorithmOId = DERDecoderUtils.getAlgorithmIdFromPublicKey(publicKey);
        } catch (DERDecodingException e) {
            LOG.warn("Can not parse the public key to determine key size!", e);
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
        KeyUtils.KeyType keyType = KeyUtils.KeyType.getByOid(keyAlgorithmOId);
        if (keyType == null) {
            LOG.warn("An unknown public key was provided");
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }

        int edECndXDHKeyLength = 0;

        switch (keyType) {
        case ED25519:
            edECndXDHKeyLength = 256;
            break;
        case X25519:
            edECndXDHKeyLength = 256;
            break;
        case ED448:
            edECndXDHKeyLength = 456;
            break;
        case X448:
            edECndXDHKeyLength = 456;
            break;
        default:
            LOG.warn("An unknown public key was provided");
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }

        return edECndXDHKeyLength;
        // return switch (keyType) {
        //     case ED25519, X25519 -> 256;
        //     case ED448, X448 -> 456;
        //     default -> {
        //         LOG.warn(
        //                 "An unknown public key was provided"
        //         );
        //         throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        //     }
        // };
    }
    // Liberty Change End
    /**
     * Check the symmetric key length
     */
    public void checkSymmetricKeyLength(
        int secretKeyLength
    ) throws WSSecurityException {
        if (secretKeyLength < (algorithmSuite.getMinimumSymmetricKeyLength() / 8)
            || secretKeyLength > (algorithmSuite.getMaximumSymmetricKeyLength() / 8)) {
            LOG.warn(
                "The symmetric key length does not match the requirement"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }

    /**
     * Check Signature Derived Key length (in bytes)
     */
    public void checkSignatureDerivedKeyLength(
        int derivedKeyLength
    ) throws WSSecurityException {
        int requiredKeyLength = algorithmSuite.getSignatureDerivedKeyLength();
        if (requiredKeyLength > 0 && (derivedKeyLength / 8) != requiredKeyLength) {
            LOG.warn(
                "The signature derived key length of " + derivedKeyLength + " does not match"
                + " the requirement of " + requiredKeyLength
            );
        }
    }

    /**
     * Check Encryption Derived Key length (in bytes)
     */
    public void checkEncryptionDerivedKeyLength(
        int derivedKeyLength
    ) throws WSSecurityException {
        int requiredKeyLength = algorithmSuite.getEncryptionDerivedKeyLength();
        if (requiredKeyLength > 0 && (derivedKeyLength / 8) != requiredKeyLength) {
            LOG.warn(
                "The encryption derived key length of " + derivedKeyLength + " does not match"
                + " the requirement of " + requiredKeyLength
            );
        }
    }

    /**
     * Check Derived Key algorithm
     */
    public void checkDerivedKeyAlgorithm(
        String algorithm
    ) throws WSSecurityException {
        Set<String> derivedKeyAlgorithms = algorithmSuite.getDerivedKeyAlgorithms();
        if (!derivedKeyAlgorithms.isEmpty()
            && !derivedKeyAlgorithms.contains(algorithm)) {
            LOG.warn(
                "The Derived Key Algorithm does not match the requirement"
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
    }

}
