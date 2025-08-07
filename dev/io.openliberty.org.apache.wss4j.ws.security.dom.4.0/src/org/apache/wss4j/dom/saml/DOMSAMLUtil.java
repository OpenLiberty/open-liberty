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

package org.apache.wss4j.dom.saml;

import java.security.MessageDigest;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.WSDerivedKeyTokenPrincipal;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.w3c.dom.Element;

/**
 * Some SAML Utility methods only for use in the DOM code.
 */
public final class DOMSAMLUtil {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(DOMSAMLUtil.class);

    private DOMSAMLUtil() {
        // complete
    }

    public static void validateSAMLResults(
        WSHandlerResult handlerResults,
        Certificate[] tlsCerts,
        Element body
    ) throws WSSecurityException {
        List<WSSecurityEngineResult> samlResults = new ArrayList<>();
        if (handlerResults.getActionResults().containsKey(WSConstants.ST_SIGNED)) {
            samlResults.addAll(handlerResults.getActionResults().get(WSConstants.ST_SIGNED));
        }
        if (handlerResults.getActionResults().containsKey(WSConstants.ST_UNSIGNED)) {
            samlResults.addAll(handlerResults.getActionResults().get(WSConstants.ST_UNSIGNED));
        }

        if (samlResults.isEmpty()) {
            return;
        }

        List<WSSecurityEngineResult> signedResults = new ArrayList<>();
        if (handlerResults.getActionResults().containsKey(WSConstants.SIGN)) {
            signedResults.addAll(handlerResults.getActionResults().get(WSConstants.SIGN));
        }
        if (handlerResults.getActionResults().containsKey(WSConstants.UT_SIGN)) {
            signedResults.addAll(handlerResults.getActionResults().get(WSConstants.UT_SIGN));
        }

        for (WSSecurityEngineResult samlResult : samlResults) {
            SamlAssertionWrapper assertionWrapper =
                (SamlAssertionWrapper)samlResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);

            if (!checkHolderOfKey(assertionWrapper, signedResults, tlsCerts)) {
                LOG.warn("Assertion fails holder-of-key requirements");
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            }
            if (!checkSenderVouches(assertionWrapper, tlsCerts, body, signedResults)) {
                LOG.warn("Assertion fails sender-vouches requirements");
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
            }
        }

    }

    /**
     * Check the holder-of-key requirements against the received assertion. The subject
     * credential of the SAML Assertion must have been used to sign some portion of
     * the message, thus showing proof-of-possession of the private/secret key. Alternatively,
     * the subject credential of the SAML Assertion must match a client certificate credential
     * when 2-way TLS is used.
     * @param assertionWrapper the SAML Assertion wrapper object
     * @param signedResults a list of all of the signed results
     */
    public static boolean checkHolderOfKey(
        SamlAssertionWrapper assertionWrapper,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        boolean isHolderOfKey = false;
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
                isHolderOfKey = true;
                break;
            }
        }

        if (isHolderOfKey) {
            if (tlsCerts == null && (signedResults == null || signedResults.isEmpty())) {
                return false;
            }
            SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
            if (!compareCredentials(subjectKeyInfo, signedResults, tlsCerts)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare the credentials of the assertion to the credentials used in 2-way TLS or those
     * used to verify signatures.
     * Return true on a match
     * @param subjectKeyInfo the SAMLKeyInfo object
     * @param signedResults a list of all of the signed results
     * @return true if the credentials of the assertion were used to verify a signature
     */
    public static boolean compareCredentials(
        SAMLKeyInfo subjectKeyInfo,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        X509Certificate[] subjectCerts = subjectKeyInfo.getCerts();
        PublicKey subjectPublicKey = subjectKeyInfo.getPublicKey();
        byte[] subjectSecretKey = subjectKeyInfo.getSecret();

        //
        // Try to match the TLS certs first
        //
        if (tlsCerts != null && tlsCerts.length > 0 && subjectCerts != null
            && subjectCerts.length > 0 && tlsCerts[0].equals(subjectCerts[0])) {
            return true;
        } else if (tlsCerts != null && tlsCerts.length > 0 && subjectPublicKey != null
            && tlsCerts[0].getPublicKey().equals(subjectPublicKey)) {
            return true;
        }

        if (subjectPublicKey == null && subjectCerts != null && subjectCerts.length > 0) {
            subjectPublicKey = subjectCerts[0].getPublicKey();
        }

        //
        // Now try the message-level signatures
        //
        for (WSSecurityEngineResult signedResult : signedResults) {
            X509Certificate[] certs =
                (X509Certificate[])signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
            PublicKey publicKey =
                (PublicKey)signedResult.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
            byte[] secretKey =
                (byte[])signedResult.get(WSSecurityEngineResult.TAG_SECRET);
            if (certs != null && certs.length > 0 && subjectCerts != null
                && subjectCerts.length > 0 && certs[0].equals(subjectCerts[0])) {
                return true;
            }
            if (publicKey != null && publicKey.equals(subjectPublicKey)) {
                return true;
            }
            if (checkSecretKey(secretKey, subjectSecretKey, signedResult)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkSecretKey(
        byte[] secretKey,
        byte[] subjectSecretKey,
        WSSecurityEngineResult signedResult
    ) {
        if (secretKey != null && subjectSecretKey != null) {
            if (MessageDigest.isEqual(secretKey, subjectSecretKey)) {
                return true;
            } else {
                Principal principal =
                    (Principal)signedResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                if (principal instanceof WSDerivedKeyTokenPrincipal) {
                    secretKey = ((WSDerivedKeyTokenPrincipal)principal).getSecret();
                    if (MessageDigest.isEqual(secretKey, subjectSecretKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check the sender-vouches requirements against the received assertion. The SAML
     * Assertion and the SOAP Body must be signed by the same signature.
     */
    public static boolean checkSenderVouches(
        SamlAssertionWrapper assertionWrapper,
        Certificate[] tlsCerts,
        Element body,
        List<WSSecurityEngineResult> signed
    ) {
        //
        // If we have a 2-way TLS connection, then we don't have to check that the
        // assertion + SOAP body are signed
        //
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        }

        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        boolean isSenderVouches = false;
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
                isSenderVouches = true;
                break;
            }
        }

        if (isSenderVouches) {
            if (signed == null || signed.isEmpty()) {
                return false;
            }
            if (!checkAssertionAndBodyAreSigned(assertionWrapper, body, signed)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if there is a signature which references the Assertion and the SOAP Body.
     * @param assertionWrapper the SamlAssertionWrapper object
     * @param body The SOAP body
     * @param signed The List of signed results
     * @return true if there is a signature which references the Assertion and the SOAP Body.
     */
    private static boolean checkAssertionAndBodyAreSigned(
        SamlAssertionWrapper assertionWrapper,
        Element body,
        List<WSSecurityEngineResult> signed
    ) {
        for (WSSecurityEngineResult signedResult : signed) {
            @SuppressWarnings("unchecked")
            List<WSDataRef> sl =
                (List<WSDataRef>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                );
            boolean assertionIsSigned = false;
            boolean bodyIsSigned = false;
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    Element se = dataRef.getProtectedElement();
                    if (se == assertionWrapper.getElement()) {
                        assertionIsSigned = true;
                    }
                    if (se == body) {
                        bodyIsSigned = true;
                    }
                    if (assertionIsSigned && bodyIsSigned) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}
