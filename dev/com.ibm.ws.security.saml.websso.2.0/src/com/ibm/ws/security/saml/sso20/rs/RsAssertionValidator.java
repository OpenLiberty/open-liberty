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
package com.ibm.ws.security.saml.sso20.rs;

import java.util.Date;
import java.util.List;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.acs.AssertionValidator;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;

/**
 *
 */
public class RsAssertionValidator extends AssertionValidator {
    private static TraceComponent tc = Tr.register(RsAssertionValidator.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    protected SsoSamlService ssoSamlService = null;

    public RsAssertionValidator(BasicMessageContext<?, ?> context, Assertion assertion) {
        super(context, assertion);
        ssoSamlService = context.getSsoService();
    }

    @Override
    public void validateAssertion() throws SamlException {

        //1. validate issuer
        validateIssuer(true);
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

    @SuppressWarnings("unchecked")
    @Override
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

    @Override
    protected void verifyAudience(List<AudienceRestriction> audienceRestrictions) throws SamlException {

        boolean bAudienceOk = false;
        String[] audiences = ssoSamlService.getConfig().getAudiences();
        if (audiences == null || audiences.length == 0) {
            bAudienceOk = true;
        } else {
            for (String audience : audiences) {
                if (audience.equals(Constants.ANY_AUDIENCE)) {
                    bAudienceOk = true;
                    break;
                }
            }
        }
        if (bAudienceOk)
            return; // any audience is OK
        SamlException lastException = null;
        for (AudienceRestriction audienceRestriction : audienceRestrictions) {
            for (Audience aud : audienceRestriction.getAudiences()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Audience=" + aud.getAudienceURI());
                }
                for (String audience : audiences) {
                    if (audience.equals(aud.getAudienceURI())) {
                        return;
                    }
                }
                lastException = new SamlException("SAML20_AUDIENCE_UNKNOWN_ERR",
                                //SAML20_AUDIENCE_UNKNOWN_ERR=CWWKS5060E: The Conditions contain an invalid Audience attribute [{0}]. The expected Audience attribute is [{1}].
                                null,
                                new Object[] { aud.getAudienceURI(), audiences[0] });
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
}
