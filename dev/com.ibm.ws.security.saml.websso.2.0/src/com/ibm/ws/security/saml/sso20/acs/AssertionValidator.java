/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.acs;

import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Condition;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.OneTimeUse;
import org.opensaml.saml.saml2.core.ProxyRestriction;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

import org.opensaml.security.trust.TrustEngine;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.utils.MsgCtxUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 *
 */
public class AssertionValidator {
    private static TraceComponent tc = Tr.register(AssertionValidator.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    @SuppressWarnings("rawtypes")
    protected BasicMessageContext context = null;
    protected Assertion assertion = null;
    protected long clockSkewAllowed = 0; //Need make it configurable

    public AssertionValidator(BasicMessageContext<?, ?> context, Assertion assertion) {
        this.assertion = assertion;
        this.context = context;
        clockSkewAllowed = context.getSsoConfig().getClockSkew();
    }

    public void validateAssertion() throws SamlException {

        //1. validate issuer
        validateIssuer(false);
        //2. Verify any signatures present on the assertion(s) or the response
        validateSignature();
        //3. Verify subject
        //A. the Recipient attribute in any bearer <SubjectConfirmationData> matches the
        //assertion consumer service URL to which the <Response> or artifact was delivered
        //B. the NotOnOrAfter attribute in any bearer <SubjectConfirmationData> has not
        // passed, subject to allowable clock skew between the providers
        //c. the InResponseTo attribute in the bearer <SubjectConfirmationData> equals the ID
        //of its original <AuthnRequest> message, unless the response is unsolicited (see Section 4.1.5 ), in
        //which case the attribute MUST NOT be present  

        //D. If any bearer <SubjectConfirmationData> includes an Address attribute, the service provider
        //MAY check the user agent's client address against it.
        verifySubject();
        //4. Validate Conditions
        verifyConditions();
        //5. validate authnStatement
        verifyAuthnStatement();
        return;
    }

    protected void validateIssuer(boolean isRsSaml) throws SamlException {
        Issuer samlIssuer = this.assertion.getIssuer();
        MsgCtxUtil.validateIssuer(samlIssuer, context, isRsSaml);
    }

    protected void validateSignature() throws SamlException {
        this.context.getMessageContext().getSubcontext(SAMLPeerEntityContext.class, true).setAuthenticated(false);
        if (this.assertion.getSignature() != null) {
            verifyAssertionSignature();
        }
        if (this.context.getSsoConfig().isWantAssertionsSigned() &&
            !this.context.getMessageContext().getSubcontext(SAMLPeerEntityContext.class, true).isAuthenticated()) {
            throw new SamlException("SAML20_ASSERTION_SIGNATURE_NOT_VERIFIED_ERR",
                            null,
                            new Object[] {});
        }
    }

    protected void verifyAssertionSignature() throws SamlException {
        try {
            TrustEngine<Signature> trustEngine = MsgCtxUtil.getTrustedEngine(context);
            SignatureValidationParameters sigValParams = new SignatureValidationParameters();
            sigValParams.setSignatureTrustEngine((SignatureTrustEngine) trustEngine); //TODO
            this.context.getMessageContext().getSubcontext(SecurityParametersContext.class, true).setSignatureValidationParameters(sigValParams);
            SAMLMessageXMLSignatureSecurityPolicyRule signatureRule = new SAMLMessageXMLSignatureSecurityPolicyRule();
            try {
                signatureRule.initialize();
            } catch (ComponentInitializationException e) {
                throw new SamlException("SAML20_ASSERTION_SIGNATURE_FAIL_ERR",
                                        //The SAML Assertion Signature is not trusted or invalid with exception [{0}].
                                        e,
                                        new Object[] { e
                                        });
            }
            signatureRule.invoke(this.context.getMessageContext()); //we may not need this?
            signatureRule.evaluateAssertion(this.context, this.assertion);
        } catch (MessageHandlerException e) {
            throw new SamlException("SAML20_ASSERTION_SIGNATURE_FAIL_ERR",
                            //The SAML Assertion Signature is not trusted or invalid with exception [{0}].
                            e,
                            new Object[] { e
                            });
        }
    }

    @SuppressWarnings("unchecked")
    protected void verifySubject() throws SamlException {
        Subject subject = this.assertion.getSubject();
        String method = null;
        for (SubjectConfirmation confirmation : subject.getSubjectConfirmations()) {

            if (SubjectConfirmation.METHOD_BEARER.equals(confirmation.getMethod())) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Subject Confirmation:", confirmation.getMethod());
                }

                SubjectConfirmationData data = confirmation.getSubjectConfirmationData();

                if (data == null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "There is no SubjectConfirmationData");
                    }
                    throw new SamlException("SAML20_ELEMENT_ERR",
                                    // "SAML20_SUBJECT_DATA_ERR=CWWKS5050E: The SAML Assertion does not contain SubjectConfirmationData element.",
                                    null, new Object[] { "SubjectConfirmationData" });
                }

                if (data.getNotBefore() != null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "There is a NotBefore");
                    }
                    throw new SamlException("SAML20_SUBJECT_NOTBEFORE_ERR",
                                    // "NotBefore attribute is not allowed inside SubjectConfirmationData element.");
                                    //"SAML20_SUBJECT_NOTBEFORE_ERR=CWWKS5051E: NotBefore attribute is not allowed inside SubjectConfirmationData element."
                                    null,
                                    new Object[] {});
                }

                if (data.getNotOnOrAfter() == null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "NotOnOrAfter attribute is required inside SubjectConfirmationData element.");
                    }
                    throw new SamlException("SAML20_ELEMENT_ATTR_ERR",
                                    //"SAML20_SUBJECT_NOTON_ERR=CWWKS5052E: NotOnOrAfter attribute inside SubjectConfirmationData is required."
                                    null,
                                    new Object[] { "NotOnOrAfter", "SubjectConfirmationData" });
                }

                if (data.getNotOnOrAfter().plus(clockSkewAllowed).isBeforeNow()) {
                    throw new SamlException("SAML20_SUBJECT_NOTONAFTER_ERR",
                                    //"SAML20_SUBJECT_NOTONAFTER_ERR=CWWKS5053E: NotOnOrAfter  [{0}] in SubjectConfirmationData passed current time [{1}]"
                                    null,
                                    new Object[] { data.getNotOnOrAfter(), new Date(), (clockSkewAllowed / 1000) });
                }

                // Validate in response to
                // The method will take care of:
                //   1) If InResponseTo is not null, it has to be sp_init and has to match the id in ReuqestInfo
                //   2) If inResponseTo is null, it has to be sp-unsolicited
                RequestUtil.validateInResponseTo(context, data.getInResponseTo());

                String acsEndpointUrl = RequestUtil.getAcsUrl(this.context.getHttpServletRequest(),
                                                              Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                                              this.context.getSsoService().getProviderId(),
                                                              this.context.getSsoConfig());

                if (data.getRecipient() == null) {
                    throw new SamlException("SAML20_ELEMENT_ATTR_ERR",
                                    // "SAML20_SUBJECT_NO_REC_ERR=CWWKS5054E: The Recipient attribute inside SubjectConfirmationData is required"
                                    null,
                                    new Object[] { "Recipient", "SubjectConfirmationData" });
                    // } else if (!context.getLocalEntityId().equals(data.getRecipient())) { //Need add it to M
                } else if (!acsEndpointUrl.equals(data.getRecipient())) {

                    throw new SamlException("SAML20_SUBJECT_NO_REC_MATCH_ERR",
                                    // SAML20_SUBJECT_NO_REC_MATCH_ERR=CWWKS5055E: The Recipient [{0}] does not match this AssertionConsumerService [{1}]
                                    null,
                                    new Object[] { data.getRecipient(), acsEndpointUrl });
                }

                this.context.setSubjectNameIdentifier(subject.getNameID());
                return;

            } else {
                method = confirmation.getMethod();
            }

        }
        throw new SamlException("SAML20_NO_BEARER_FOUND",
                        //"The subject confirmation method urn:oasis:names:tc:SAML:2.0:cm:bearer is required.");
                        //SAML20_NO_BEARER_FOUND=CWWKSS5065E: Cannot find a valid Assertion with proper SubjectConfirmationData.
                        null,
                        new Object[] { method });
    }

    protected void verifyConditions() throws SamlException {
        Conditions conditions = this.assertion.getConditions();

        if (conditions == null || conditions.getAudienceRestrictions().size() == 0) {
            throw new SamlException("SAML20_ELEMENT_ERR",
                            //SAML20_SUBJECT_NO_AUD_ERR=CWWKS5056E: The Assertion must contain AudienceRestriction element.
                            null,
                            new Object[] { "AudienceRestriction" });
        }

        if (conditions.getNotBefore() != null) {
            if (conditions.getNotBefore().minus(clockSkewAllowed).isAfterNow()) {
                throw new SamlException("SAML20_SUBJECT_NOBEFORE_ERR",
                                // "SAML20_SUBJECT_NOBEFORE_ERR=CWWKS5057E: The Assertion must not be accepted before [{0}] condition. The current time is [{1}].
                                null,
                                new Object[] { conditions.getNotBefore(), new Date(), (clockSkewAllowed / 1000) });
            }
        }

        if (conditions.getNotOnOrAfter() != null) {
            if (conditions.getNotOnOrAfter().plus(clockSkewAllowed).isBeforeNow()) {
                throw new SamlException("SAML20_SUBJECT_NOAFTER_ERR",
                                //SAML20_SUBJECT_NOAFTER_ERR=CWWKS5058E: The Assertion must not be accepted after [{0}] condition. The current time is [{1}].
                                null,
                                new Object[] { conditions.getNotOnOrAfter(), new Date(), (clockSkewAllowed / 1000) });
            }
        }

        for (Condition condition : conditions.getConditions()) {
            QName conditionQName = condition.getElementQName();

            if (conditionQName.equals(AudienceRestriction.DEFAULT_ELEMENT_NAME)) {
                verifyAudience(conditions.getAudienceRestrictions());
            } else if (conditionQName.equals(OneTimeUse.DEFAULT_ELEMENT_NAME)) {
                //ignore it, we implement replayAttack prevention

            } else if (conditionQName.equals(ProxyRestriction.DEFAULT_ELEMENT_NAME)) {
                //ignore it

            } else {

                //other not processed condition
                throw new SamlException("SAML20_CONDITION_UNKNOWN_ERR",
                                //SAML20_CONDITION_UNKNOWN_ERR=CWWKS5059E: The Conditions element must not contain unknown attribute [{0}].
                                null,
                                new Object[] { conditionQName });
            }
        }
    }

    protected void verifyAudience(List<AudienceRestriction> audienceRestrictions) throws SamlException {
        //TODO fix it to use metadata's entityId
        String audienceUrl = RequestUtil.getEntityUrl(this.context.getHttpServletRequest(),
                                                      Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                                      this.context.getSsoService().getProviderId(),
                                                      this.context.getSsoConfig());

        SamlException lastException = null;
        for (AudienceRestriction audienceRestriction : audienceRestrictions) {

            for (Audience aud : audienceRestriction.getAudiences()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Audience=" + aud.getAudienceURI());
                }
                if (audienceUrl.equals(aud.getAudienceURI())) {
                    return;
                }
                else {
                    lastException = new SamlException("SAML20_AUDIENCE_UNKNOWN_ERR",
                                    //SAML20_AUDIENCE_UNKNOWN_ERR=CWWKS5060E: The Conditions contain an invalid Audience attribute [{0}]. The expected Audience attribute is [{1}].
                                    null,
                                    new Object[] { aud.getAudienceURI(), audienceUrl });
                }
            }

        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Invalid audience");
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new SamlException("SAML20_ELEMENT_ATTR_ERR",
                        //SAML20_AUDIENCE_NO_ERR=CWWKS5061E: The Conditions element must contain Audience attribute.
                        null,
                        new Object[] { "Audience", "Conditions" });
    }

    protected void verifyAuthnStatement() throws SamlException {
        List<AuthnStatement> authns = this.assertion.getAuthnStatements();
        for (AuthnStatement statement : authns) {

            if (statement.getSessionNotOnOrAfter() != null &&
                statement.getSessionNotOnOrAfter().plus(clockSkewAllowed).isBeforeNow()) {
                throw new SamlException("SAML20_SESSION_ERR",

                                //SAML20_SESSION_ERR=CWWKS5062E: The Session in AuthnStatement element is invalid after [{0}]. The current time is [{1}]
                                null,
                                new Object[] { statement.getSessionNotOnOrAfter(), new Date(), (clockSkewAllowed / 1000) });
            }

            //TODO Verify AuthnContext for solicited sso if request contains AuthnContextClassReference
        }
    }

}
