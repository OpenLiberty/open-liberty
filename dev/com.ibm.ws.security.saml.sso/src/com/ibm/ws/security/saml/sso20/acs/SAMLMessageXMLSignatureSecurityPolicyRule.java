/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.sso20.acs;

import java.util.List;

import org.opensaml.common.SAMLObject;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.binding.security.BaseSAMLXMLSignatureSecurityPolicyRule;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.xml.security.trust.TrustEngine;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.utils.SignatureMethods;

/**
 * SAML security policy rule which validates the signature (if present) on the {@link SAMLObject} which represents the
 * SAML profile message being processed.
 *
 * <p>
 * If the message is not an instance of {@link SignableSAMLObject}, then no processing is performed. If signature
 * validation is successful, and the SAML message context issuer was not previously authenticated, then the context's
 * issuer authentication state will be set to <code>true</code>.
 * </p>
 *
 * <p>
 * If an optional {@link Validator} for {@link Signature} objects is supplied, this validator will be used to validate
 * the XML Signature element prior to the actual cryptographic validation of the signature. This might for example be
 * used to enforce certain signature profile requirements or to detect signatures upon which it would be unsafe to
 * attempt cryptographic processing. When using the single argument constructuor form, the validator will default to {@link SAMLSignatureProfileValidator}.
 * </p>
 */
public class SAMLMessageXMLSignatureSecurityPolicyRule extends BaseSAMLXMLSignatureSecurityPolicyRule {

    private static TraceComponent tc = Tr.register(SAMLMessageXMLSignatureSecurityPolicyRule.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    /** Validator for XML Signature instances. */
    private final Validator<Signature> sigValidator;

    String processType = "";

    /**
     * Constructor.
     *
     * Signature pre-validator defaults to {@link SAMLSignatureProfileValidator}.
     *
     * @param engine Trust engine used to verify the signature
     */
    public SAMLMessageXMLSignatureSecurityPolicyRule(TrustEngine<Signature> engine) {
        super(engine);
        this.sigValidator = new SAMLSignatureProfileValidator();
    }

    /**
     * Constructor.
     *
     * @param engine             Trust engine used to verify the signature
     * @param signatureValidator optional pre-validator used to validate Signature elements prior to the actual
     *                               cryptographic validation operation
     */
    public SAMLMessageXMLSignatureSecurityPolicyRule(TrustEngine<Signature> engine,
                                                     Validator<Signature> signatureValidator) {
        super(engine);
        sigValidator = signatureValidator;
    }

    /** {@inheritDoc} */
    @Override
    public void evaluate(MessageContext messageContext) throws SecurityPolicyException {
        if (!(messageContext instanceof SAMLMessageContext)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid message context type, this policy rule only supports SAMLMessageContext");
            }
            return;
        }
        evaluateProfile((SAMLMessageContext<?, ?, ?>) messageContext);
        evaluateProtocol((SAMLMessageContext<?, ?, ?>) messageContext);
    }

    // @FFDCIgnore({SecurityPolicyException.class})
    public void evaluateProfile(SAMLMessageContext<?, ?, ?> samlMsgCtx) throws SecurityPolicyException {
        processType = "Profile";
        SAMLObject samlMsg = samlMsgCtx.getInboundSAMLMessage();
        if (!(samlMsg instanceof SignableSAMLObject)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Extracted SAML message was not a SignableSAMLObject, can not process signature");
            }
            return;
        }
        if (samlMsg instanceof Response) { // checking the assertion
            Response samlResponse = (Response) samlMsg;
            List<Assertion> assertions = samlResponse.getAssertions();
            for (Assertion assertion : assertions) {
                if (assertion instanceof SignableSAMLObject) {
                    if (!assertion.isSigned()) {
                        // This should not happen
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "A SAML assertion is not signed." +
                                         " We do not allow this kind of situation");
                        }
                    }
                    evaluate(samlMsgCtx, assertion);
                }
            }
        }
    }

    public void evaluateAssertion(SAMLMessageContext<?, ?, ?> samlMsgCtx, Assertion assertion) throws SecurityPolicyException {
        processType = "Profile";

        if (!assertion.isSigned()) {
            return; //caller is resonsible to check if signature is required
        }

        evaluate(samlMsgCtx, assertion);

    }

    public void evaluateProtocol(SAMLMessageContext<?, ?, ?> samlMsgCtx) throws SecurityPolicyException {
        processType = "Protocol";
        SAMLObject samlMsg = samlMsgCtx.getInboundSAMLMessage();
        if (!(samlMsg instanceof SignableSAMLObject)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Extracted SAML message was not a SignableSAMLObject, can not process signature");
            }
            return;
        }
        SignableSAMLObject signableObject = (SignableSAMLObject) samlMsg;
        if (!signableObject.isSigned()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SAML protocol message was not signed, skipping XML signature processing");
            }
            return;
        }
        evaluate(samlMsgCtx, signableObject);
    }

    public void evaluateResponse(SAMLMessageContext<?, ?, ?> samlMsgCtx) throws SecurityPolicyException {
        processType = "Protocol";
        SAMLObject samlMsg = samlMsgCtx.getInboundSAMLMessage();
        if (!(samlMsg instanceof SignableSAMLObject)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Extracted SAML message was not a SignableSAMLObject, can not process signature");
            }
            return;
        }
        SignableSAMLObject signableObject = (SignableSAMLObject) samlMsg;
        if (!signableObject.isSigned()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SAML protocol message was not signed, skipping XML signature processing");
            }
            return;
        }

        evaluate(samlMsgCtx, signableObject);

    }

    public void evaluate(SAMLMessageContext<?, ?, ?> samlMsgCtx, SignableSAMLObject assertion) throws SecurityPolicyException {
        Signature signature = assertion.getSignature();
        evaluateSignatureMethod(samlMsgCtx, signature);
        performPreValidation(signature);
        doEvaluate(signature, assertion, samlMsgCtx);
    }

    protected void evaluateSignatureMethod(SAMLMessageContext<?, ?, ?> samlMsgCtx, Signature signature) throws SecurityPolicyException {
        @SuppressWarnings("rawtypes")
        String configMethod = ((BasicMessageContext) samlMsgCtx).getSsoConfig().getSignatureMethodAlgorithm();
        String messageMethod = signature.getSignatureAlgorithm();
        if (SignatureMethods.toInteger(messageMethod) < SignatureMethods.toInteger(configMethod)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Required signature method is " + configMethod);
                Tr.debug(tc, "Received signature method is " + messageMethod);
            }
            throw new SecurityPolicyException("The server is configured with the signature method " + configMethod
                                              + " but the received SAML assertion is signed with the signature method "
                                              + messageMethod + ", the signature method provided is weaker than the required.");
        }
    }

    /**
     * Perform cryptographic validation and trust evaluation on the Signature token using the configured Signature trust
     * engine.
     *
     * @param signature      the signature which is being evaluated
     * @param signableObject the signable object which contained the signature
     * @param samlMsgCtx     the SAML message context being processed
     * @throws SecurityPolicyException thrown if the signature fails validation
     */
    protected void doEvaluate(Signature signature, SignableSAMLObject signableObject, SAMLMessageContext<?, ?, ?> samlMsgCtx) throws SecurityPolicyException {

        String contextIssuer = samlMsgCtx.getInboundMessageIssuer();
        if (contextIssuer != null) {
            String msgType = signableObject.getElementQName().toString();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting to verify signature on signed SAML " + processType + " message using context issuer message type: " + msgType);
            }

            if (evaluate(signature, contextIssuer, samlMsgCtx)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Validation of " + processType + " message signature succeeded, message type: " + msgType);
                }
                if (!samlMsgCtx.isInboundSAMLMessageAuthenticated()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Authentication via " + processType + " message signature succeeded for context issuer entity ID " +
                                     contextIssuer);
                    }
                    samlMsgCtx.setInboundSAMLMessageAuthenticated(true);
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Validation of " + processType + " message signature failed for context issuer '" + contextIssuer
                                 + "', message type: " + msgType);
                }
                throw new SecurityPolicyException("Validation of " + processType + " message signature failed");
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Context issuer unavailable, can not attempt SAML " + processType + " message signature validation");
            }
            throw new SecurityPolicyException("Context issuer unavailable, can not validate signature");
        }
    }

    /**
     * Get the validator used to perform pre-validation on Signature tokens.
     *
     * @return the configured Signature validator, or null
     */
    protected Validator<Signature> getSignaturePrevalidator() {
        return sigValidator;
    }

    /**
     * Perform pre-validation on the Signature token.
     *
     * @param signature the signature to evaluate
     * @throws SecurityPolicyException thrown if the signature element fails pre-validation
     */
    protected void performPreValidation(Signature signature) throws SecurityPolicyException {
        if (getSignaturePrevalidator() != null) {
            try {
                getSignaturePrevalidator().validate(signature);
            } catch (ValidationException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, processType + " message signature failed signature pre-validation", e);
                }
                throw new SecurityPolicyException(processType + " message signature failed signature pre-validation", e);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, processType + " message signature failed without pre-validation");
            }
            throw new SecurityPolicyException(processType + " message signature failed signature pre-validation");
        }
    }

}