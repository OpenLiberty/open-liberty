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

import java.util.List;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.binding.security.impl.BaseSAMLXMLSignatureSecurityHandler;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.keyinfo.KeyInfoCriterion;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.impl.BaseSignatureTrustEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.utils.SignatureMethods;

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

public class SAMLMessageXMLSignatureSecurityPolicyRule extends BaseSAMLXMLSignatureSecurityHandler {

    private static TraceComponent tc = Tr.register(SAMLMessageXMLSignatureSecurityPolicyRule.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    /** Validator for XML Signature instances. */
    //private final Validator<Signature> sigValidator; //v3
    private SignaturePrevalidator signaturePrevalidator;

    String processType = "";

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
        //ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this); //TODO:
        signaturePrevalidator = validator;
    }

    
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
    }
    

    // @FFDCIgnore({SecurityPolicyException.class}) //TODO: ignore new exception type
    public void evaluateProfile(BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        processType = "Profile";
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
    
    public void evaluateAssertion(BasicMessageContext<?, ?> samlMsgCtx, Assertion assertion) throws MessageHandlerException {
        processType = "Profile";

        if (!assertion.isSigned()) {
            return; //caller is resonsible to check if signature is required
        }

        evaluate(samlMsgCtx, assertion);

    }
    
    public void evaluateProtocol(BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        processType = "Protocol";
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

    public void evaluateResponse(BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        processType = "Protocol";
        //SAMLObject samlMsg = samlMsgCtx.getInboundSAMLMessage(); //v2
        SAMLObject samlMsg = samlMsgCtx.getMessageContext().getMessage(); //v3
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

    public void evaluate(BasicMessageContext<?, ?> samlMsgCtx, SignableSAMLObject assertion) throws MessageHandlerException {
        Signature signature = assertion.getSignature();
        evaluateSignatureMethod(samlMsgCtx, signature);
        performPreValidation(signature);
        doEvaluate(signature, assertion, samlMsgCtx);
    }

    protected void evaluateSignatureMethod(BasicMessageContext<?, ?> samlMsgCtx, Signature signature) throws MessageHandlerException {
        @SuppressWarnings("rawtypes")
        String configMethod = ((BasicMessageContext) samlMsgCtx).getSsoConfig().getSignatureMethodAlgorithm();
        String messageMethod = signature.getSignatureAlgorithm();
        if (SignatureMethods.toInteger(messageMethod) < SignatureMethods.toInteger(configMethod)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Required signature method from configuration is " + configMethod);
                Tr.debug(tc, "Received signature method is " + messageMethod);
            }
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
    
    protected void doEvaluate(Signature signature, SignableSAMLObject signableObject, BasicMessageContext<?, ?> samlMsgCtx) throws MessageHandlerException {
        //String contextIssuer = samlMsgCtx.getInboundMessageIssuer(); //v2
        String contextIssuer = samlMsgCtx.getInboundSamlMessageIssuer();
        MessageContext<SAMLObject> messageContext = samlMsgCtx.getMessageContext();
        SAMLPeerEntityContext peerContext = getSAMLPeerEntityContext();
        
        //String contextIssuer = peerContext.getEntityId(); // v3 this will not work in rs saml flow
        //SAMLObject samlMsg = messageContext.getMessage();
        
        if (contextIssuer != null) {
            String msgType = signableObject.getElementQName().toString();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting to verify signature on signed SAML " + processType + " message using context issuer message type: " + msgType);
            }
            final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
//                if (samlMsgCtx.isPKIXTrustEngineConfigured()) {
//                    contextIssuer = null; // TODO: is there a better way???
//                }
                Thread.currentThread().setContextClassLoader(SignatureValidator.class.getClassLoader());
                if (evaluate(signature, contextIssuer, messageContext)) { //v3
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Validation of " + processType + " message signature succeeded, message type: " + msgType);
                    }
                    if (!peerContext.isAuthenticated()) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Authentication via " + processType + " message signature succeeded for context issuer entity ID " +
                                         contextIssuer);
                        }
                        peerContext.setAuthenticated(true);
                    }
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Validation of " + processType + " message signature failed for context issuer '" + contextIssuer
                                     + "', message type: " + msgType);
                    }
                    throw new MessageHandlerException("Validation of " + processType + " message signature failed");
                }
            }catch (Exception e){
                throw new MessageHandlerException("Validation of " + processType + " message signature failed");           
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader); 
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Context issuer unavailable, can not attempt SAML " + processType + " message signature validation");
            }
            throw new MessageHandlerException("Context issuer unavailable, can not validate signature");
        }
    }

    /**
     * @param signature
     * @param contextIssuer
     * @param messageContext
     * @return
     */
    private boolean mevaluate(Signature signature, String entityID, MessageContext messageContext) {
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
               
            } catch (SecurityException e) {
               
            }
        } catch (MessageHandlerException e) {
       
        }
        return false;
    }

    /**
     * Get the validator used to perform pre-validation on Signature tokens.
     *
     * @return the configured Signature validator, or null
     */

    protected SignaturePrevalidator getSignaturePrevalidator() {
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
            } catch (SignatureException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, processType + " message signature failed signature pre-validation", e);
                }
                throw new MessageHandlerException(processType + " message signature failed signature pre-validation", e);
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, processType + " message signature failed without pre-validation");
            }
            throw new MessageHandlerException(processType + " message signature failed signature pre-validation");
        }
    }

}