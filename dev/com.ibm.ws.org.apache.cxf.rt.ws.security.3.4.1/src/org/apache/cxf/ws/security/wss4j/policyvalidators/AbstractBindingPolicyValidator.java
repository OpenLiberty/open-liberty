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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.PKIPathSecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.Timestamp;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding;
import org.apache.wss4j.policy.model.AbstractSymmetricAsymmetricBinding.ProtectionOrder;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.EncryptionToken;
import org.apache.wss4j.policy.model.ProtectionToken;
import org.apache.wss4j.policy.model.SignatureToken;
import org.apache.wss4j.policy.model.X509Token;

/**
 * Some abstract functionality for validating a security binding.
 */
public abstract class AbstractBindingPolicyValidator implements SecurityPolicyValidator {

    private static final QName SIG_QNAME = new QName(WSConstants.SIG_NS, WSConstants.SIG_LN);

    /**
     * Validate a Timestamp
     * @param includeTimestamp whether a Timestamp must be included or not
     * @param transportBinding whether the Transport binding is in use or not
     * @param signedResults the signed results list
     * @param message the Message object
     * @return whether the Timestamp policy is valid or not
     */
    protected boolean validateTimestamp(
        boolean includeTimestamp,
        boolean transportBinding,
        WSHandlerResult results,
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        List<WSSecurityEngineResult> timestampResults =
            results.getActionResults().get(WSConstants.TS);

        // Check whether we received a timestamp and compare it to the policy
        if (includeTimestamp && (timestampResults == null || timestampResults.size() != 1)) {
            return false;
        } else if (!includeTimestamp) {
            return timestampResults == null || timestampResults.isEmpty();
        }

        // At this point we received a (required) Timestamp. Now check that it is integrity protected.
        if (transportBinding) {
            return true;
        } else if (!signedResults.isEmpty()) {
            Timestamp timestamp =
                (Timestamp)timestampResults.get(0).get(WSSecurityEngineResult.TAG_TIMESTAMP);
            for (WSSecurityEngineResult signedResult : signedResults) {
                List<WSDataRef> dataRefs =
                    CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                for (WSDataRef dataRef : dataRefs) {
                    if (timestamp.getElement() == dataRef.getProtectedElement()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Validate the entire header and body signature property.
     */
    protected boolean validateEntireHeaderAndBodySignatures(
        List<WSSecurityEngineResult> signedResults
    ) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> dataRefs =
                    CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (dataRefs != null) {
                for (WSDataRef dataRef : dataRefs) {
                    String xpath = dataRef.getXpath();
                    if (xpath != null) {
                        String[] nodes = xpath.split("/");
                        // envelope/Body || envelope/Header/header || envelope/Header/wsse:Security/header
                        if (nodes.length < 3 || nodes.length > 5) {
                            return false;
                        }

                        if (!(nodes[2].contains("Header") || nodes[2].contains("Body"))) {
                            return false;
                        }

                        if (nodes.length == 5 && !nodes[3].contains("Security")) {
                            return false;
                        }

                        if (nodes.length == 4 && nodes[2].contains("Body")) {
                            return false;
                        }

                    }
                }
            }
        }
        return true;
    }

    /**
     * Check various properties set in the policy of the binding
     */
    protected boolean checkProperties(
        AbstractSymmetricAsymmetricBinding binding,
        AssertionInfo ai,
        AssertionInfoMap aim,
        WSHandlerResult results,
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        // Check the IncludeTimestamp
        if (!validateTimestamp(binding.isIncludeTimestamp(), false, results, signedResults, message)) {
            String error = "Received Timestamp does not match the requirements";
            ai.setNotAsserted(error);
            return false;
        }
        String namespace = binding.getName().getNamespaceURI();
        PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.INCLUDE_TIMESTAMP));

        // Check the EntireHeaderAndBodySignatures property
        if (binding.isOnlySignEntireHeadersAndBody()
            && !validateEntireHeaderAndBodySignatures(signedResults)) {
            String error = "OnlySignEntireHeadersAndBody does not match the requirements";
            ai.setNotAsserted(error);
            return false;
        }
        PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));

        // Check whether the signatures were encrypted or not
        if (binding.isEncryptSignature() && !isSignatureEncrypted(results.getResults())) {
            ai.setNotAsserted("The signature is not protected");
            return false;
        }
        PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.ENCRYPT_SIGNATURE));
        PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.PROTECT_TOKENS));

        /*
        // Check ProtectTokens
        if (binding.isTokenProtection() && !isTokenProtected(results, signedResults)) {
            ai.setNotAsserted("The token protection property is not valid");
            return false;
        }
        */

        return true;
    }

    /**
     * Check the Protection Order of the binding
     */
    protected boolean checkProtectionOrder(
        AbstractSymmetricAsymmetricBinding binding,
        AssertionInfoMap aim,
        AssertionInfo ai,
        List<WSSecurityEngineResult> results
    ) {
        ProtectionOrder protectionOrder = binding.getProtectionOrder();
        String namespace = binding.getName().getNamespaceURI();

        if (protectionOrder == ProtectionOrder.EncryptBeforeSigning) {
            if (!binding.isProtectTokens() && isSignedBeforeEncrypted(results)) {
                ai.setNotAsserted("Not encrypted before signed");
                return false;
            }
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.ENCRYPT_BEFORE_SIGNING));
        } else if (protectionOrder == ProtectionOrder.SignBeforeEncrypting) {
            if (isEncryptedBeforeSigned(results)) {
                ai.setNotAsserted("Not signed before encrypted");
                return false;
            }
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.SIGN_BEFORE_ENCRYPTING));
        }
        return true;
    }

    /**
     * Check to see if a signature was applied before encryption.
     * Note that results are stored in the reverse order.
     */
    private boolean isSignedBeforeEncrypted(List<WSSecurityEngineResult> results) {
        boolean signed = false;
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            List<WSDataRef> el =
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));

            // Don't count an endorsing signature
            if (actInt.intValue() == WSConstants.SIGN && el != null
                && !(el.size() == 1 && el.get(0).getName().equals(SIG_QNAME))) {
                signed = true;
            }
            if (actInt.intValue() == WSConstants.ENCR && el != null) {
                return signed;
            }
        }
        return false;
    }

    /**
     * Check to see if encryption was applied before signature.
     * Note that results are stored in the reverse order.
     */
    private boolean isEncryptedBeforeSigned(List<WSSecurityEngineResult> results) {
        boolean encrypted = false;
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            List<WSDataRef> el =
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));

            if (actInt.intValue() == WSConstants.ENCR && el != null) {
                encrypted = true;
            }
            // Don't count an endorsing signature
            if (actInt.intValue() == WSConstants.SIGN && el != null
                && !(el.size() == 1 && el.get(0).getName().equals(SIG_QNAME))) {
                return encrypted;
            }
        }
        return false;
    }

    /**
     * Check the derived key requirement.
     */
    protected boolean checkDerivedKeys(
        AbstractTokenWrapper tokenWrapper,
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        AbstractToken token = tokenWrapper.getToken();
        boolean isDerivedKeys = token.getDerivedKeys() == DerivedKeys.RequireDerivedKeys;
        // If derived keys are not required then just return
        if (!(token instanceof X509Token && isDerivedKeys)) {
            return true;
        }
        if (tokenWrapper instanceof EncryptionToken
            && !hasDerivedKeys && !encryptedResults.isEmpty()) {
            return false;
        } else if (tokenWrapper instanceof SignatureToken
            && !hasDerivedKeys && !signedResults.isEmpty()) {
            return false;
        } else if (tokenWrapper instanceof ProtectionToken
            && !hasDerivedKeys && !(signedResults.isEmpty() || encryptedResults.isEmpty())) {
            return false;
        }
        return true;
    }

    /**
     * Check whether the token protection policy is followed. In other words, check that the
     * signature token was itself signed.
     */
    protected boolean isTokenProtected(
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        for (WSSecurityEngineResult result : signedResults) {

            // Get the Token result that was used for the signature
            WSSecurityEngineResult tokenResult = findCorrespondingToken(result, results);
            if (tokenResult == null) {
                return false;
            }

            // Now go through what was signed and see if the token itself was signed
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)result.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            boolean found = false;
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    Element referenceElement = dataRef.getProtectedElement();
                    if (referenceElement != null
                        && referenceElement.equals(tokenResult.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT))) {
                        found = true;
                    }
                }
            }
            if (!found) {
                return false;
            }

        }
        return true;
    }

    /**
     * Find the token corresponding to either the X509Certificate or PublicKey used to sign
     * the "signatureResult" argument.
     */
    private WSSecurityEngineResult findCorrespondingToken(
        WSSecurityEngineResult signatureResult,
        List<WSSecurityEngineResult> results
    ) {
        // See what was used to sign this result
        X509Certificate cert =
            (X509Certificate)signatureResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        PublicKey publicKey =
            (PublicKey)signatureResult.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);

        for (WSSecurityEngineResult token : results) {
            Integer actInt = (Integer)token.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt == WSConstants.SIGN) {
                continue;
            }

            BinarySecurity binarySecurity =
                (BinarySecurity)token.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
            PublicKey foundPublicKey =
                (PublicKey)token.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
            if (binarySecurity instanceof X509Security
                || binarySecurity instanceof PKIPathSecurity) {
                X509Certificate foundCert =
                    (X509Certificate)token.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (foundCert.equals(cert)) {
                    return token;
                }
            } else if (actInt.intValue() == WSConstants.ST_SIGNED
                || actInt.intValue() == WSConstants.ST_UNSIGNED) {
                SamlAssertionWrapper assertionWrapper =
                    (SamlAssertionWrapper)token.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                SAMLKeyInfo samlKeyInfo = assertionWrapper.getSubjectKeyInfo();
                if (samlKeyInfo != null) {
                    X509Certificate[] subjectCerts = samlKeyInfo.getCerts();
                    PublicKey subjectPublicKey = samlKeyInfo.getPublicKey();
                    if ((cert != null && subjectCerts != null
                        && cert.equals(subjectCerts[0]))
                        || (subjectPublicKey != null && subjectPublicKey.equals(publicKey))) {
                        return token;
                    }
                }
            } else if (publicKey != null && publicKey.equals(foundPublicKey)) {
                return token;
            }
        }
        return null;
    }

    /**
     * Check whether the primary Signature (and all SignatureConfirmation) elements were encrypted
     */
    protected boolean isSignatureEncrypted(List<WSSecurityEngineResult> results) {
        boolean foundPrimarySignature = false;
        for (int i = results.size() - 1; i >= 0; i--) {
            WSSecurityEngineResult result = results.get(i);
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.SIGN && !foundPrimarySignature) {
                foundPrimarySignature = true;
                Element sigElement =
                    (Element)result.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                if (sigElement == null || !isElementEncrypted(sigElement, results)) {
                    return false;
                }
            } else if (actInt.intValue() == WSConstants.SC) {
                Element sigElement =
                    (Element)result.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                if (sigElement == null || !isElementEncrypted(sigElement, results)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if the given Element was encrypted
     */
    private boolean isElementEncrypted(Element element, List<WSSecurityEngineResult> results) {
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.ENCR) {
                List<WSDataRef> el =
                    CastUtils.cast((List<?>)wser.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (el != null) {
                    for (WSDataRef r : el) {
                        Element protectedElement = r.getProtectedElement();
                        if (element.equals(protectedElement)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected void assertDerivedKeys(AbstractToken token, AssertionInfoMap aim) {
        DerivedKeys derivedKeys = token.getDerivedKeys();
        if (derivedKeys != null) {
            PolicyUtils.assertPolicy(aim, new QName(token.getName().getNamespaceURI(), derivedKeys.name()));
        }
    }
}
