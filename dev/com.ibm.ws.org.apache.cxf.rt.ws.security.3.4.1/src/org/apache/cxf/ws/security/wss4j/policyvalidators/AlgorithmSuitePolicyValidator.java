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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.principal.WSDerivedKeyTokenPrincipal;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.transform.STRTransform;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.xml.security.transforms.Transforms;

/**
 * Validate results corresponding to the processing of a Signature, EncryptedKey or
 * EncryptedData structure against an AlgorithmSuite policy.
 */
//No Liberty Change, but needed to recompile due to Liberty change in MessageImpl.
public class AlgorithmSuitePolicyValidator extends AbstractSecurityPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.ALGORITHM_SUITE.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.ALGORITHM_SUITE.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        for (AssertionInfo ai : ais) {
            AlgorithmSuite algorithmSuite = (AlgorithmSuite)ai.getAssertion();
            ai.setAsserted(true);

            boolean valid = validatePolicy(ai, algorithmSuite, parameters.getResults().getResults());
            if (valid) {
                String namespace = algorithmSuite.getAlgorithmSuiteType().getNamespace();
                String name = algorithmSuite.getAlgorithmSuiteType().getName();
                Collection<AssertionInfo> algSuiteAis =
                    parameters.getAssertionInfoMap().get(new QName(namespace, name));
                if (algSuiteAis != null) {
                    for (AssertionInfo algSuiteAi : algSuiteAis) {
                        algSuiteAi.setAsserted(true);
                    }
                }

                PolicyUtils.assertPolicy(parameters.getAssertionInfoMap(),
                                         new QName(algorithmSuite.getName().getNamespaceURI(),
                                                   algorithmSuite.getC14n().name()));
            } else if (ai.isAsserted()) {
                ai.setNotAsserted("Error in validating AlgorithmSuite policy");
            }
        }
    }

    private boolean validatePolicy(
        AssertionInfo ai, AlgorithmSuite algorithmPolicy, List<WSSecurityEngineResult> results
    ) {

        for (WSSecurityEngineResult result : results) {
            Integer action = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (WSConstants.SIGN == action
                && !checkSignatureAlgorithms(result, algorithmPolicy, ai)) {
                return false;
            } else if (WSConstants.ENCR == action
                && !checkEncryptionAlgorithms(result, algorithmPolicy, ai)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check the Signature Algorithms
     */
    private boolean checkSignatureAlgorithms(
        WSSecurityEngineResult result,
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        String signatureMethod =
            (String)result.get(WSSecurityEngineResult.TAG_SIGNATURE_METHOD);
        if (!algorithmPolicy.getAlgorithmSuiteType().getAsymmetricSignature().equals(signatureMethod)
            && !algorithmPolicy.getAlgorithmSuiteType().getSymmetricSignature().equals(signatureMethod)) {
            ai.setNotAsserted(
                "The signature method does not match the requirement"
            );
            return false;
        }
        String c14nMethod =
            (String)result.get(WSSecurityEngineResult.TAG_CANONICALIZATION_METHOD);
        if (!algorithmPolicy.getC14n().getValue().equals(c14nMethod)) {
            ai.setNotAsserted(
                "The c14n method does not match the requirement"
            );
            return false;
        }

        List<WSDataRef> dataRefs =
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
        if (!checkDataRefs(dataRefs, algorithmPolicy, ai)) {
            return false;
        }

        return checkKeyLengths(result, algorithmPolicy, ai, true);
    }

    /**
     * Check the individual signature references
     */
    private boolean checkDataRefs(
        List<WSDataRef> dataRefs,
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        AlgorithmSuiteType algorithmSuiteType = algorithmPolicy.getAlgorithmSuiteType();
        for (WSDataRef dataRef : dataRefs) {
            String digestMethod = dataRef.getDigestAlgorithm();
            if (!algorithmSuiteType.getDigest().equals(digestMethod)) {
                ai.setNotAsserted(
                    "The digest method does not match the requirement"
                );
                return false;
            }

            List<String> transformAlgorithms = dataRef.getTransformAlgorithms();
            // Only a max of 2 transforms per reference is allowed
            if (transformAlgorithms == null || transformAlgorithms.size() > 2) {
                ai.setNotAsserted("The transform algorithms do not match the requirement");
                return false;
            }
            for (String transformAlgorithm : transformAlgorithms) {
                if (!(algorithmPolicy.getC14n().getValue().equals(transformAlgorithm)
                    || WSS4JConstants.C14N_EXCL_OMIT_COMMENTS.equals(transformAlgorithm)
                    || STRTransform.TRANSFORM_URI.equals(transformAlgorithm)
                    || Transforms.TRANSFORM_ENVELOPED_SIGNATURE.equals(transformAlgorithm)
                    || WSS4JConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS.equals(transformAlgorithm)
                    || WSS4JConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS.equals(transformAlgorithm))) {
                    ai.setNotAsserted("The transform algorithms do not match the requirement");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check the Encryption Algorithms
     */
    private boolean checkEncryptionAlgorithms(
        WSSecurityEngineResult result,
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        AlgorithmSuiteType algorithmSuiteType = algorithmPolicy.getAlgorithmSuiteType();
        String transportMethod =
            (String)result.get(WSSecurityEngineResult.TAG_ENCRYPTED_KEY_TRANSPORT_METHOD);
        if (transportMethod != null
            && !algorithmSuiteType.getSymmetricKeyWrap().equals(transportMethod)
            && !algorithmSuiteType.getAsymmetricKeyWrap().equals(transportMethod)) {
            ai.setNotAsserted(
                "The Key transport method does not match the requirement"
            );
            return false;
        }

        List<WSDataRef> dataRefs =
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
        if (dataRefs != null) {
            for (WSDataRef dataRef : dataRefs) {
                String encryptionAlgorithm = dataRef.getAlgorithm();
                if (!algorithmSuiteType.getEncryption().equals(encryptionAlgorithm)) {
                    ai.setNotAsserted(
                        "The encryption algorithm does not match the requirement"
                    );
                    return false;
                }
            }
        }

        return checkKeyLengths(result, algorithmPolicy, ai, false);
    }

    /**
     * Check the key lengths of the secret and public keys.
     */
    private boolean checkKeyLengths(
        WSSecurityEngineResult result,
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai,
        boolean signature
    ) {
        PublicKey publicKey = (PublicKey)result.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
        if (publicKey != null && !checkPublicKeyLength(publicKey, algorithmPolicy, ai)) {
            return false;
        }

        X509Certificate x509Cert =
            (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        if (x509Cert != null && !checkPublicKeyLength(x509Cert.getPublicKey(), algorithmPolicy, ai)) {
            return false;
        }

        AlgorithmSuiteType algorithmSuiteType = algorithmPolicy.getAlgorithmSuiteType();
        byte[] secret = (byte[])result.get(WSSecurityEngineResult.TAG_SECRET);
        if (signature) {
            Principal principal = (Principal)result.get(WSSecurityEngineResult.TAG_PRINCIPAL);
            if (principal instanceof WSDerivedKeyTokenPrincipal) {
                int requiredLength = algorithmSuiteType.getSignatureDerivedKeyLength();
                if (secret == null || secret.length != (requiredLength / 8)) {
                    ai.setNotAsserted(
                        "The signature derived key length does not match the requirement"
                    );
                    return false;
                }
            } else if (secret != null
                && (secret.length < (algorithmSuiteType.getMinimumSymmetricKeyLength() / 8)
                    || secret.length > (algorithmSuiteType.getMaximumSymmetricKeyLength() / 8))) {
                ai.setNotAsserted(
                    "The symmetric key length does not match the requirement"
                );
                return false;
            }
        } else if (secret != null
            && (secret.length < (algorithmSuiteType.getMinimumSymmetricKeyLength() / 8)
                || secret.length > (algorithmSuiteType.getMaximumSymmetricKeyLength() / 8))) {
            ai.setNotAsserted(
                "The symmetric key length does not match the requirement"
            );
            return false;
        }

        return true;
    }

    /**
     * Check the public key lengths
     */
    private boolean checkPublicKeyLength(
        PublicKey publicKey,
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        AlgorithmSuiteType algorithmSuiteType = algorithmPolicy.getAlgorithmSuiteType();
        if (publicKey instanceof RSAPublicKey) {
            int modulus = ((RSAPublicKey)publicKey).getModulus().bitLength();
            if (modulus < algorithmSuiteType.getMinimumAsymmetricKeyLength()
                || modulus > algorithmSuiteType.getMaximumAsymmetricKeyLength()) {
                ai.setNotAsserted(
                    "The asymmetric key length does not match the requirement"
                );
                return false;
            }
        } else if (publicKey instanceof DSAPublicKey) {
            int length = ((DSAPublicKey)publicKey).getParams().getP().bitLength();
            if (length < algorithmSuiteType.getMinimumAsymmetricKeyLength()
                || length > algorithmSuiteType.getMaximumAsymmetricKeyLength()) {
                ai.setNotAsserted(
                    "The asymmetric key length does not match the requirement"
                );
                return false;
            }
        } else {
            ai.setNotAsserted(
                "An unknown public key was provided"
            );
            return false;
        }

        return true;
    }

}
