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

package org.apache.wss4j.dom.validate;

import java.time.Instant;
import java.util.List;

import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.handler.RequestData;
import org.joda.time.DateTime;
import org.opensaml.saml.common.SAMLVersion;

/**
 * This class validates a SAML Assertion, which is wrapped in an "SamlAssertionWrapper" instance.
 * It assumes that the SamlAssertionWrapper instance has already verified the signature on the
 * assertion (done by the SAMLTokenProcessor). It verifies trust in the signature, and also
 * checks that the Subject contains a KeyInfo (and processes it) for the holder-of-key case,
 * and verifies that the Assertion is signed as well for holder-of-key.
 */
public class SamlAssertionValidator extends SignatureTrustValidator {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SamlAssertionValidator.class);

    /**
     * The time in seconds in the future within which the NotBefore time of an incoming
     * Assertion is valid. The default is 60 seconds.
     */
    private int futureTTL = 60;

    /**
     * The time in seconds within which a SAML Assertion is valid, if it does not contain
     * a NotOnOrAfter Condition. The default is 30 minutes.
     */
    private int ttl = 60 * 30;

    /**
     * Whether to validate the signature of the Assertion (if it exists) against the
     * relevant profile. Default is true.
     */
    private boolean validateSignatureAgainstProfile = true;

    /**
     * If this is set, then the value must appear as one of the Subject Confirmation Methods
     */
    private String requiredSubjectConfirmationMethod;

    /**
     * If this is set, at least one of the standard Subject Confirmation Methods *must*
     * be present in the assertion (Bearer / SenderVouches / HolderOfKey).
     */
    private boolean requireStandardSubjectConfirmationMethod = true;

    /**
     * If this is set, an Assertion with a Bearer SubjectConfirmation Method must be
     * signed
     */
    private boolean requireBearerSignature = true;

    /**
     * Set the time in seconds in the future within which the NotBefore time of an incoming
     * Assertion is valid. The default is 60 seconds.
     */
    public void setFutureTTL(int newFutureTTL) {
        futureTTL = newFutureTTL;
    }

    /**
     * Validate the credential argument. It must contain a non-null SamlAssertionWrapper.
     * A Crypto and a CallbackHandler implementation is also required to be set.
     *
     * @param credential the Credential to be validated
     * @param data the RequestData associated with the request
     * @throws WSSecurityException on a failed validation
     */
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null || credential.getSamlAssertion() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
        }
        SamlAssertionWrapper samlAssertion = credential.getSamlAssertion();

        // Check the Subject Confirmation requirements
        verifySubjectConfirmationMethod(samlAssertion);

        // Check conditions
        checkConditions(samlAssertion, data.getAudienceRestrictions());

        // Check the AuthnStatements of the assertion (if any)
        checkAuthnStatements(samlAssertion);

        // Check OneTimeUse Condition
        checkOneTimeUse(samlAssertion, data);

        // Validate the assertion against schemas/profiles
        validateAssertion(samlAssertion);

        // Verify trust on the signature
        if (samlAssertion.isSigned()) {
            verifySignedAssertion(samlAssertion, data);
        }
        return credential;
    }

    /**
     * Check the Subject Confirmation method requirements
     */
    protected void verifySubjectConfirmationMethod(
        SamlAssertionWrapper samlAssertion
    ) throws WSSecurityException {

        List<String> methods = samlAssertion.getConfirmationMethods();
        if (methods == null || methods.isEmpty()) {
            if (requiredSubjectConfirmationMethod != null) {
                LOG.warn("A required subject confirmation method was not present");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "invalidSAMLsecurity");
            } else if (requireStandardSubjectConfirmationMethod) {
                LOG.warn("A standard subject confirmation method was not present");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "invalidSAMLsecurity");
            }
        }

        boolean signed = samlAssertion.isSigned();
        boolean requiredMethodFound = false;
        boolean standardMethodFound = false;
        if (methods != null) {
            for (String method : methods) {
                if (OpenSAMLUtil.isMethodHolderOfKey(method)) {
                    if (samlAssertion.getSubjectKeyInfo() == null) {
                        LOG.warn("There is no Subject KeyInfo to match the holder-of-key subject conf method");
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noKeyInSAMLToken");
                    }

                    // The assertion must have been signed for HOK
                    if (!signed) {
                        LOG.warn("A holder-of-key assertion must be signed");
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
                    }
                    standardMethodFound = true;
                }

                if (method != null) {
                    if (method.equals(requiredSubjectConfirmationMethod)) {
                        requiredMethodFound = true;
                    }
                    if (SAML2Constants.CONF_BEARER.equals(method)
                        || SAML1Constants.CONF_BEARER.equals(method)) {
                        standardMethodFound = true;
                        if (requireBearerSignature && !signed) {
                            LOG.warn("A Bearer Assertion was not signed");
                            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                                          "invalidSAMLsecurity");
                        }
                    } else if (SAML2Constants.CONF_SENDER_VOUCHES.equals(method)
                        || SAML1Constants.CONF_SENDER_VOUCHES.equals(method)) {
                        standardMethodFound = true;
                    }
                }
            }
        }

        if (!requiredMethodFound && requiredSubjectConfirmationMethod != null) {
            LOG.warn("A required subject confirmation method was not present");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "invalidSAMLsecurity");
        }

        if (!standardMethodFound && requireStandardSubjectConfirmationMethod) {
            LOG.warn("A standard subject confirmation method was not present");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                      "invalidSAMLsecurity");
        }
    }

    /**
     * Verify trust in the signature of a signed Assertion. This method is separate so that
     * the user can override if if they want.
     * @param samlAssertion The signed Assertion
     * @param data The RequestData context
     * @return A Credential instance
     * @throws WSSecurityException
     */
    protected Credential verifySignedAssertion(
        SamlAssertionWrapper samlAssertion,
        RequestData data
    ) throws WSSecurityException {
        Credential trustCredential = new Credential();
        SAMLKeyInfo samlKeyInfo = samlAssertion.getSignatureKeyInfo();
        trustCredential.setPublicKey(samlKeyInfo.getPublicKey());
        trustCredential.setCertificates(samlKeyInfo.getCerts());
        return super.validate(trustCredential, data);
    }

    /**
     * Check the Conditions of the Assertion.
     */
    protected void checkConditions(
        SamlAssertionWrapper samlAssertion, List<String> audienceRestrictions
    ) throws WSSecurityException {
        checkConditions(samlAssertion);
        samlAssertion.checkAudienceRestrictions(audienceRestrictions);
    }

    /**
     * Check the Conditions of the Assertion.
     */
    protected void checkConditions(SamlAssertionWrapper samlAssertion) throws WSSecurityException {
        samlAssertion.checkConditions(futureTTL);
        samlAssertion.checkIssueInstant(futureTTL, ttl);
    }

    /**
     * Check the AuthnStatements of the Assertion (if any)
     */
    protected void checkAuthnStatements(SamlAssertionWrapper samlAssertion) throws WSSecurityException {
        samlAssertion.checkAuthnStatements(futureTTL);
    }

    /**
     * Check the "OneTimeUse" Condition of the Assertion. If this is set then the Assertion
     * is cached (if a cache is defined), and must not have been previously cached
     */
    protected void checkOneTimeUse(
        SamlAssertionWrapper samlAssertion, RequestData data
    ) throws WSSecurityException {
        if (samlAssertion.getSamlVersion().equals(SAMLVersion.VERSION_20)
            && samlAssertion.getSaml2().getConditions() != null
            && samlAssertion.getSaml2().getConditions().getOneTimeUse() != null
            && data.getSamlOneTimeUseReplayCache() != null) {
            String identifier = samlAssertion.getId();

            ReplayCache replayCache = data.getSamlOneTimeUseReplayCache();
            if (replayCache.contains(identifier)) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY,
                    "badSamlToken",
                    new Object[] {"A replay attack has been detected"});
            }

            DateTime expires = samlAssertion.getSaml2().getConditions().getNotOnOrAfter();
            if (expires != null) {
                Instant zonedExpires = Instant.ofEpochMilli(expires.getMillis());
                replayCache.add(identifier, zonedExpires);
            } else {
                replayCache.add(identifier);
            }

            replayCache.add(identifier);
        }
    }

    /**
     * Validate the samlAssertion against schemas/profiles
     */
    protected void validateAssertion(SamlAssertionWrapper samlAssertion) throws WSSecurityException {
        if (validateSignatureAgainstProfile) {
            samlAssertion.validateSignatureAgainstProfile();
        }
    }

    /**
     * Whether to validate the signature of the Assertion (if it exists) against the
     * relevant profile. Default is true.
     */
    public boolean isValidateSignatureAgainstProfile() {
        return validateSignatureAgainstProfile;
    }

    /**
     * Whether to validate the signature of the Assertion (if it exists) against the
     * relevant profile. Default is true.
     */
    public void setValidateSignatureAgainstProfile(boolean validateSignatureAgainstProfile) {
        this.validateSignatureAgainstProfile = validateSignatureAgainstProfile;
    }

    public String getRequiredSubjectConfirmationMethod() {
        return requiredSubjectConfirmationMethod;
    }

    public void setRequiredSubjectConfirmationMethod(String requiredSubjectConfirmationMethod) {
        this.requiredSubjectConfirmationMethod = requiredSubjectConfirmationMethod;
    }

    public boolean isRequireStandardSubjectConfirmationMethod() {
        return requireStandardSubjectConfirmationMethod;
    }

    public void setRequireStandardSubjectConfirmationMethod(boolean requireStandardSubjectConfirmationMethod) {
        this.requireStandardSubjectConfirmationMethod = requireStandardSubjectConfirmationMethod;
    }

    public boolean isRequireBearerSignature() {
        return requireBearerSignature;
    }

    public void setRequireBearerSignature(boolean requireBearerSignature) {
        this.requireBearerSignature = requireBearerSignature;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

}
