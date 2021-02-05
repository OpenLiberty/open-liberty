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
import java.util.Set;

//import java.util.function.Predicate;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SignableSAMLObject;
//import org.opensaml.saml.common.binding.SAMLMessageContext; //@AV999
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;

//import org.opensaml.common.binding.security.BaseSAMLXMLSignatureSecurityPolicyRule;
import org.opensaml.saml.common.binding.security.impl.BaseSAMLXMLSignatureSecurityHandler; //PolicyRule=Handler
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.PKIXValidationInformation;
//import org.opensaml.ws.message.MessageContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.xmlsec.keyinfo.KeyInfoCriterion;
//import org.opensaml.ws.security.SecurityPolicyException;
//import org.opensaml.xml.security.trust.TrustEngine;
//import org.opensaml.xml.signature.Signature;
//import org.opensaml.xml.validation.ValidationException;
//import org.opensaml.xml.validation.Validator;
//import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.SignerProvider;
import org.opensaml.xmlsec.signature.support.impl.BaseSignatureTrustEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.utils.SignatureMethods;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

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
//@AV999 TODO: change the name? to SAMLProtocolMessageXMLSignatureSecurityHandler?
public class SAMLMessageXMLSignatureSecurityPolicyRule extends BaseSAMLXMLSignatureSecurityHandler {

    private static TraceComponent tc = Tr.register(SAMLMessageXMLSignatureSecurityPolicyRule.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    /** Validator for XML Signature instances. */
    //private final Validator<Signature> sigValidator; //@AV999 major change
    private SignaturePrevalidator signaturePrevalidator;

    String processType = "";
// @AV999
//    /**
//     * Constructor.
//     *
//     * Signature pre-validator defaults to {@link SAMLSignatureProfileValidator}.
//     *
//     * @param engine Trust engine used to verify the signature
//     */
//    public SAMLMessageXMLSignatureSecurityPolicyRule(TrustEngine<Signature> engine) {
//        super(engine);
//        this.sigValidator = new SAMLSignatureProfileValidator();
//    }
//
//    /**
//     * Constructor.
//     *
//     * @param engine             Trust engine used to verify the signature
//     * @param signatureValidator optional pre-validator used to validate Signature elements prior to the actual
//     *                               cryptographic validation operation
//     */
//    public SAMLMessageXMLSignatureSecurityPolicyRule(TrustEngine<Signature> engine,
//                                                     Validator<Signature> signatureValidator) {
//        super(engine);
//        sigValidator = signatureValidator;
//    }

    /**
     * Constructor.
     * 
     * Signature prevalidator defaults to {@link SAMLSignatureProfileValidator}.
     * 
     */
    public SAMLMessageXMLSignatureSecurityPolicyRule() {
        setSignaturePrevalidator(new SAMLSignatureProfileValidator());
    }
    
    /**
     * Set the prevalidator for XML Signature instances.
     * 
     * @param validator The prevalidator to set.
     */
    public void setSignaturePrevalidator(final SignaturePrevalidator validator) {
        //ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this); //@AV999 TODO: look into this
        signaturePrevalidator = validator;
    }

// @AV999
//    /** {@inheritDoc} */
//    @Override
//    public void evaluate(MessageContext messageContext) throws MessageHandlerException {
//        if (!(messageContext instanceof SAMLMessageContext)) {
//            if (tc.isDebugEnabled()) {
//                Tr.debug(tc, "Invalid message context type, this policy rule only supports SAMLMessageContext");
//            }
//            return;
//        }
//        evaluateProfile((SAMLMessageContext<?, ?, ?>) messageContext);
//        evaluateProtocol((SAMLMessageContext<?, ?, ?>) messageContext);
//    }
    
    /** {@inheritDoc} */
    @Override
    public void doInvoke(final MessageContext messageContext) throws MessageHandlerException {

        final Object samlMsg = messageContext.getMessage();
        if (!(samlMsg instanceof SignableSAMLObject)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "{} Extracted SAML message was not a SignableSAMLObject, cannot process signature");
            }
            return;
        }
        final SignableSAMLObject signableObject = (SignableSAMLObject) samlMsg;
        if (!signableObject.isSigned()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "{} SAML protocol message was not signed, skipping XML signature processing");
            }
            return;
        }
        //@AV999 
        //final Signature signature = signableObject.getSignature();

        //performPrevalidation(signature);

        //doEvaluate(signature, signableObject, messageContext);
    }
    

    // @FFDCIgnore({SecurityPolicyException.class})
    //@AV999
    //public void evaluateProfile(SAMLMessageContext<?, ?, ?> samlMsgCtx) throws SecurityPolicyException {
    public void evaluateProfile(BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        processType = "Profile";
        //SAMLObject samlMsg = samlMsgCtx.getInboundSAMLMessage(); //@AV999
        SAMLObject samlMsg = samlMsgCtx.getMessageContext().getMessage();
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
    //@AV999
    //public void evaluateAssertion(SAMLMessageContext<?, ?, ?> samlMsgCtx, Assertion assertion) throws SecurityPolicyException {
    public void evaluateAssertion(BasicMessageContext<?, ?> samlMsgCtx, Assertion assertion) throws MessageHandlerException {
        processType = "Profile";

        if (!assertion.isSigned()) {
            return; //caller is resonsible to check if signature is required
        }

        evaluate(samlMsgCtx, assertion);

    }
//@AV999
    public void evaluateProtocol(BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        processType = "Protocol";
        //SAMLObject samlMsg = samlMsgCtx.getInboundSAMLMessage(); //@AV999 major change
        SAMLObject samlMsg = samlMsgCtx.getMessageContext().getMessage();
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

    //@AV999
    //public void evaluateResponse(SAMLMessageContext<?, ?, ?> samlMsgCtx) throws SecurityPolicyException {
    public void evaluateResponse(BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        processType = "Protocol";
        //SAMLObject samlMsg = samlMsgCtx.getInboundSAMLMessage(); //@AV999
        SAMLObject samlMsg = samlMsgCtx.getMessageContext().getMessage();
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

    //@AV999
    //public void evaluate(SAMLMessageContext<?, ?, ?> samlMsgCtx, SignableSAMLObject assertion) throws SecurityPolicyException {
    public void evaluate(BasicMessageContext<?, ?> samlMsgCtx, SignableSAMLObject assertion) throws MessageHandlerException {
        Signature signature = assertion.getSignature();
        evaluateSignatureMethod(samlMsgCtx, signature);
        performPreValidation(signature);
        doEvaluate(signature, assertion, samlMsgCtx);
    }

    //@AV999
    //protected void evaluateSignatureMethod(SAMLMessageContext<?, ?, ?> samlMsgCtx, Signature signature) throws SecurityPolicyException {
    protected void evaluateSignatureMethod(BasicMessageContext<?, ?> samlMsgCtx, Signature signature) throws MessageHandlerException {
        @SuppressWarnings("rawtypes")
        String configMethod = ((BasicMessageContext) samlMsgCtx).getSsoConfig().getSignatureMethodAlgorithm();
        String messageMethod = signature.getSignatureAlgorithm();
        if (SignatureMethods.toInteger(messageMethod) < SignatureMethods.toInteger(configMethod)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Required signature method is " + configMethod);
                Tr.debug(tc, "Received signature method is " + messageMethod);
            }
            //@AV999
//            throw new SecurityPolicyException("The server is configured with the signature method " + configMethod
//                                              + " but the received SAML assertion is signed with the signature method "+ messageMethod + ", the signature method provided is weaker than the required.");
              throw new MessageHandlerException("The server is configured with the signature method " + configMethod
          + " but the received SAML assertion is signed with the signature method "
          + messageMethod + ", the signature method provided is weaker than the required.");
        
        }
    }
    protected SAMLPeerEntityContext getPeerContext() {
        return getSAMLPeerEntityContext();
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
    //@AV999
    //protected void doEvaluate(Signature signature, SignableSAMLObject signableObject, SAMLMessageContext<?, ?, ?> samlMsgCtx) throws SecurityPolicyException {
    protected void doEvaluate(Signature signature, SignableSAMLObject signableObject, BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        //String contextIssuer = samlMsgCtx.getInboundMessageIssuer(); //@AV999
        String contextIssuer = samlMsgCtx.getInboundSamlMessageIssuer();
        MessageContext<SAMLObject> messageContext = samlMsgCtx.getMessageContext();
        SAMLPeerEntityContext peerContext = getSAMLPeerEntityContext();
        //messageContext.getSubcontext(SAMLPeerEntityContext.class, true)
        //String contextIssuer = peerContext.getEntityId(); // @AV999 this will not work in rs saml flow
        //SAMLObject samlMsg = messageContext.getMessage();
        
        if (contextIssuer != null) {
            String msgType = signableObject.getElementQName().toString();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting to verify signature on signed SAML " + processType + " message using context issuer message type: " + msgType);
            }
            final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
//                if (samlMsgCtx.isPKIXTrustEngineConfigured()) {
//                    contextIssuer = null; //@AV999, TODO: is there a better way???
//                }
                Thread.currentThread().setContextClassLoader(SignatureValidator.class.getClassLoader());
                if (evaluate(signature, contextIssuer, messageContext)) { //@AV999
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Validation of " + processType + " message signature succeeded, message type: " + msgType);
                    }
                    if (!peerContext.isAuthenticated()) {
                    //if (!samlMsgCtx.isInboundSAMLMessageAuthenticated()) { //@AV999
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Authentication via " + processType + " message signature succeeded for context issuer entity ID " +
                                         contextIssuer);
                        }
                        peerContext.setAuthenticated(true);
                        //samlMsgCtx.setInboundSAMLMessageAuthenticated(true); //@AV999
                    }
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Validation of " + processType + " message signature failed for context issuer '" + contextIssuer
                                     + "', message type: " + msgType);
                    }
                    //@AV999
                    //throw new SecurityPolicyException("Validation of " + processType + " message signature failed");
                    throw new MessageHandlerException("Validation of " + processType + " message signature failed");
                }
            }catch (Exception e){
              //@AV999
                //throw new SecurityPolicyException("Validation of " + processType + " message signature failed");
                throw new MessageHandlerException("Validation of " + processType + " message signature failed");
            
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader); 
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Context issuer unavailable, can not attempt SAML " + processType + " message signature validation");
            }
            //throw new SecurityPolicyException("Context issuer unavailable, can not validate signature"); //@AV999
            throw new MessageHandlerException("Context issuer unavailable, can not validate signature");
        }
    }

    /**
     * @param signature
     * @param contextIssuer
     * @param messageContext
     * @return
     */
    private boolean myevaluate(Signature signature, String entityID, MessageContext messageContext) {
        try {
            final CriteriaSet criteriaSet = buildCriteriaSet(entityID, messageContext);
            try {
                final KeyInfoCriterion keyInfoCriteria = new KeyInfoCriterion(signature.getKeyInfo());
                final CriteriaSet keyInfoCriteriaSet = new CriteriaSet(keyInfoCriteria);
                Iterable<Credential> kiCredIter = ((BaseSignatureTrustEngine)getTrustEngine()).getKeyInfoResolver().resolve(keyInfoCriteriaSet);
                //return getTrustEngine().validate(signature, criteriaSet);
                Credential kiCred = kiCredIter.iterator().next();
                //evaluateTrust(kiCred, trustBasis);
                return getTrustEngine().validate(signature, criteriaSet);
               
            } catch (ResolverException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                e.printStackTrace();
            }
        } catch (MessageHandlerException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param kiCred
     * @param trustBasis
     */
    private void evaluateTrust(Credential kiCred, Pair<Set<String>, Iterable<PKIXValidationInformation>> validationPair) {
        // TODO Auto-generated method stub
        
    }

    /**
     * Get the validator used to perform pre-validation on Signature tokens.
     *
     * @return the configured Signature validator, or null
     */
    //@AV999
    //protected Validator<Signature> getSignaturePrevalidator() {
    protected SignaturePrevalidator getSignaturePrevalidator() {
        //return sigValidator; //@AV999
        return signaturePrevalidator;
    }

    /**
     * Perform pre-validation on the Signature token.
     *
     * @param signature the signature to evaluate
     * @throws SecurityPolicyException thrown if the signature element fails pre-validation
     */
    protected void performPreValidation(Signature signature) throws MessageHandlerException {
        if (getSignaturePrevalidator() != null) {
            try {
                getSignaturePrevalidator().validate(signature);
            } catch (SignatureException e) { //catch (ValidationException e) { //@AV999
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, processType + " message signature failed signature pre-validation", e);
                }
                //@AV999
                throw new MessageHandlerException(processType + " message signature failed signature pre-validation", e);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, processType + " message signature failed without pre-validation");
            }
            //@AV999
            throw new MessageHandlerException(processType + " message signature failed signature pre-validation");
        }
    }

}