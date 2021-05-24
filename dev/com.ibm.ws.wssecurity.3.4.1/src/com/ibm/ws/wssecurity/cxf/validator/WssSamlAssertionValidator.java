/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.cxf.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.ext.WSSecurityException.ErrorCode;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.validate.Credential;
import org.joda.time.DateTime;
import org.opensaml.saml.common.SAMLVersion;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.ws.wssecurity.WSSecurityPolicyException;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.ws.wssecurity.token.TokenUtils;

/**
 * This class validates a SAML Assertion, which is wrapped in an "AssertionWrapper" instance.
 * It assumes that the AssertionWrapper instance has already verified the signature on the
 * assertion (done by the SAMLTokenProcessor). It verifies trust in the signature, and also
 * checks that the Subject contains a KeyInfo (and processes it) for the holder-of-key case,
 * and verifies that the Assertion is signed as well for holder-of-key.
 */
public class WssSamlAssertionValidator extends org.apache.wss4j.dom.validate.SamlAssertionValidator {
    private static final TraceComponent tc = Tr.register(WssSamlAssertionValidator.class,
                                                         WSSecurityConstants.TR_GROUP,
                                                         WSSecurityConstants.TR_RESOURCE_BUNDLE);

    List<String> audienceRestrictions = new ArrayList<String>();//null; // if no restructions, set this to null
    int iFutureTTL = 5 * 60; // 5 minutes 
    int ttl = 60 * 30; // 30 Minutes

    public WssSamlAssertionValidator(Map<String, Object> configMap) {
        // ALlow the WssSamlAssertionValidator to initialized 
        // But will fail when the validate method is called

        // always check the saml profile
        setValidateSignatureAgainstProfile(true);
        setRequireStandardSubjectConfirmationMethod(true); // we only support bearer, sender-vouches, holder-of-key
        if (configMap != null) {
            setRequiredSubjectConfirmationMethod((String) configMap.get(WSSecurityConstants.KEY_requiredSubjectConfirmationMethod));
            setRequireBearerSignature((Boolean) configMap.get(WSSecurityConstants.KEY_wantAssertionsSigned));
            iFutureTTL = ((Long) configMap.get(WSSecurityConstants.KEY_clockSkew)).intValue(); // translate to secondsalready
            setFutureTTL(iFutureTTL);
            ttl = ((Long) configMap.get(WSSecurityConstants.KEY_timeToLive)).intValue(); // translate to secondsalready
            setTtl(ttl); // translate to seconds already

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "futureTTL:" + iFutureTTL +
                             " ttl:" + ttl
                                );
            }

            String[] restrictions = (String[]) configMap.get(WSSecurityConstants.KEY_audienceRestrictions);
            if (restrictions == null) {
                //audienceRestrictions = new ArrayList<String>();//null; // no restrictions
            } else {
                audienceRestrictions = new ArrayList<String>();
                for (int iI = 0; iI < restrictions.length; iI++) {
                    audienceRestrictions.add(restrictions[iI]);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "audienceRestriction:" + restrictions[iI]);
                    }
                }
            }

        }
    }

    /**
     * Validate the credential argument. It must contain a non-null AssertionWrapper.
     * A Crypto and a CallbackHandler implementation is also required to be set.
     * 
     * @param credential the Credential to be validated
     * @param data the RequestData associated with the request
     * @throws WSSecurityException on a failed validation
     */
    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        // For now, do not allow the saml to run when wss_saml bundle is not up
        SsoService wssSamlService = TokenUtils.getCommonSsoService(SsoService.TYPE_WSS_SAML); //"wssSaml");
        if (wssSamlService == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "No wsSecuritySaml-1.1 feature is up. Make sure your server.xml has wsSecuritySaml-1.1 feature set up properly");
        }

        // set the Audience to the requestData
        // if no restructions, set this to null
        data.setAudienceRestrictions(audienceRestrictions);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, " audienceRestriction:" + audienceRestrictions);
            Tr.debug(tc, " audienceRestriction:" + audienceRestrictions.isEmpty());
        }
        return super.validate(credential, data);
    }

    /**
     * Check the Conditions of the Assertion.
     */
    @Override
    protected void checkConditions(SamlAssertionWrapper assertion) throws WSSecurityException {
        DateTime validFrom = null;
        DateTime validTill = null;
        DateTime issueInstant = null;

        if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)
            && assertion.getSaml2().getConditions() != null) {
            validFrom = assertion.getSaml2().getConditions().getNotBefore();
            validTill = assertion.getSaml2().getConditions().getNotOnOrAfter();
            issueInstant = assertion.getSaml2().getIssueInstant();
        } else if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_11)
                   && assertion.getSaml1().getConditions() != null) {
            validFrom = assertion.getSaml1().getConditions().getNotBefore();
            validTill = assertion.getSaml1().getConditions().getNotOnOrAfter();
            issueInstant = assertion.getSaml1().getIssueInstant();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "futureTTL(clockSkew):" + iFutureTTL +
                         " ttl:" + ttl);
        }

        if (validFrom != null) {
            DateTime currentTime = new DateTime();
            DateTime currentTimePlusSkew = currentTime.plusSeconds(iFutureTTL);
            if (validFrom.isAfter(currentTimePlusSkew)) {
                // The current time is before the SAML token's NotBefore assertion value; this assertion is not yet valid
                Tr.error(tc, "saml_token_not_yet_valid", validFrom, currentTime, iFutureTTL);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
            }
        }

        if (validTill != null) {
            // newly added the clockSkew
            DateTime validTillPlusSkew = validTill.plusSeconds(iFutureTTL); // add the clockSkew
            DateTime currentTime = new DateTime();
            if (validTillPlusSkew.isBeforeNow()) {
                // SAML token has expired - the NotOnOrAfter time has passed
                Tr.error(tc, "saml_token_expired", validTill, currentTime, iFutureTTL);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
            }
        } else { // move here since findbug complains
            if (issueInstant != null) {
                DateTime currentTime = new DateTime();
                DateTime earliestAllowedIssuance = currentTime.minusSeconds(ttl + iFutureTTL); // also minus the clockSkew

                if (issueInstant.isBefore(earliestAllowedIssuance)) {
                    // SAML token issued too long ago - TTL has passed
                    Tr.error(tc, "saml_token_issued_too_long_ago", issueInstant, currentTime, iFutureTTL);
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
                }
            }
        }

        // IssueInstant is not strictly in Conditions, but it has similar semantics to 
        // NotBefore, so including it here

        // Check the IssueInstant is not in the future, subject to the future TTL
        if (issueInstant != null) {
            DateTime currentTime = new DateTime();
            DateTime currentTimePlusSkew = currentTime.plusSeconds(iFutureTTL);
            if (issueInstant.isAfter(currentTimePlusSkew)) {
                // SAML token's IssueInstant assertion is in the future - the token is not yet valid
                Tr.error(tc, "saml_token_issue_instant_in_future", issueInstant, currentTime, iFutureTTL);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
            }
        }
    }

}
