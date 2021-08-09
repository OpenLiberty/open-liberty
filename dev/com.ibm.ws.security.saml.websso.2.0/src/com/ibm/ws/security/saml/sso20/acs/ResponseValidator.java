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

import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.saml2.PropagationHelper;
import com.ibm.websphere.security.saml2.Saml20Token;
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
public class ResponseValidator<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject> {
    private static TraceComponent tc = Tr.register(ResponseValidator.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    BasicMessageContext<InboundMessageType, OutboundMessageType> context;
    Response samlResponse;
    LogoutResponse samlLogoutResponse;
    LogoutRequest samlLogoutRequest;
    String issuer;
    long clockSkewAllowed = 0;
    StatusBuilderUtil statusBuilderUtil;
    final static String SINDEX = "sessionIndex";

    public ResponseValidator(BasicMessageContext<InboundMessageType, OutboundMessageType> context, Response samlResponse) {
        this.context = context;
        this.samlResponse = samlResponse;
        issuer = samlResponse.getIssuer().getValue();
        clockSkewAllowed = context.getSsoConfig().getClockSkew();
        //timeValidater = new CurrentDateTimeValidater(context.getSsoConfig().getClockSkew()); // using the current date time
    }

    public ResponseValidator(BasicMessageContext<InboundMessageType, OutboundMessageType> context, LogoutResponse samlLogoutResponse) {
        this.context = context;
        this.samlLogoutResponse = samlLogoutResponse;
        issuer = samlLogoutResponse.getIssuer().getValue();
        clockSkewAllowed = context.getSsoConfig().getClockSkew();
        statusBuilderUtil = new StatusBuilderUtil();
    }

    public ResponseValidator(BasicMessageContext<InboundMessageType, OutboundMessageType> context, LogoutRequest samlLogoutRequest) {
        this.context = context;
        this.samlLogoutRequest = samlLogoutRequest;
        issuer = samlLogoutRequest.getIssuer().getValue();
        clockSkewAllowed = context.getSsoConfig().getClockSkew();
        statusBuilderUtil = new StatusBuilderUtil();
    }

    public boolean validate() throws SamlException {
        boolean valid = true;

        //1. validate response status
        validateStatus(); // if it's in error it should have thrown SamlException

        // Let's make sure the assertion is there
        if (samlResponse.getAssertions().isEmpty() && samlResponse.getEncryptedAssertions().isEmpty()) {
            // no assertions
            // No need to check further
            throw new SamlException("SAML20_SP_NO_ASSERTION_ERROR",
                            //"CWWKS5009E: The SAML Response does not contain any SAML Assertions.);",
                            null, // cause
                            new Object[] { issuer });
        }

        //2. validate response version
        validateVersion();

        //3. validate InResponse for SP-initiated @liangch
        //A.  validate at redirectToRelayState() in com/ibm/ws/security/saml/sso20/acs/AcsHandler
        //B.  Must not contain InResponseTo for unsolicited SSO
        validateInResponseTo();

        //4. validate IssueInstant
        //Clock skew has to be added!
        validateIssueInstant();

        //5. validate Destination (match to AssertionConsumerServicesURL)
        //  (optional)
        validateDestination();

        //6. validate Issuer in Response
        validateIssuer();
        // TODO add code to compare with the issuer in the idpMetadata

        //7. validate Response's Signature
        // (optional on samlp:Response) (required on the assertion)
        // http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
        // section 5
        // opensaml may have done this for us. FAT ought to be able to confirm it.
        validateResponseSignature();

        return valid;
    }

    public boolean validateLogoutResponse() throws SamlException {
        boolean valid = true;

        //1. validate status - already did this
        // validateLogoutStatus(); //

        //2. validate response version
        validateVersion();

        //3. validate InResponse for SP-initiated
        //A.  validated relayState already
        //B.  validate InResponseTo
        validateInResponseTo();

        //4. validate IssueInstant
        //Clock skew has to be added!
        validateIssueInstant();

        //5. validate Destination (match to AssertionConsumerServicesURL)
        //  (optional)
        validateDestination();

        //6. validate Issuer in Response
        validateIssuer();
        // TODO add code to compare with the issuer in the idpMetadata

        //7. validate Response's Signature
        // (optional on samlp:Response) (required on the assertion)
        // http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
        // section 5
        // opensaml may have done this for us. FAT ought to be able to confirm it.
        validateResponseSignature();

        return valid;
    }

    public boolean validateLogoutRequest() {
        boolean valid = true;

        //1. validate NameID.
        if (!validateNameID()) {
            return false;
        }

        //2. validate SessionIndex
        if (!validateSessionIndex()) {
            return false;
        }

        //3. validate logout request version
        try {
            if (!validateVersion()) {
                return false;
            }
            //4. validate IssueInstant
            //Clock skew has to be added!
            validateIssueInstant();

            //5. validate Destination (match to AssertionConsumerServicesURL)
            //  (optional)
            validateDestination();

            //6. validate Issuer in LogoutRequest
            validateIssuer();

            //7. validate Response's Signature
            // (optional on samlp:Response) (required on the assertion)
            // http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
            // section 5
            // opensaml may have done this for us.
            validateResponseSignature();
        } catch (SamlException e) {
            statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.REQUESTER); //v3 - it was StatusCode.REQUESTER_URI
            return false;
        }

        return valid;
    }

    /**
     *
     */
    private boolean validateSessionIndex() {
        boolean valid = false;
        if (samlLogoutRequest != null) {
            Saml20Token saml20token = PropagationHelper.getSaml20Token();
            if (saml20token == null) {
                statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.RESPONDER); //v3
                return false;
            }
            String session = (String) saml20token.getProperties().get(SINDEX);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "SessionIndex in the current saml token  = ", session);
            }
            List<SessionIndex> list = samlLogoutRequest.getSessionIndexes();

            if (list.isEmpty() || session == null) {
                if (list.isEmpty() && session == null) { // no session index
                    return true;
                }
                statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.REQUESTER); //v3
                return false;
            }

            Iterator<SessionIndex> it = list.iterator();
            while (it.hasNext()) {
                if (session.equals(it.next().getSessionIndex())) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.REQUESTER); //v3
            }
        }
        return valid;
    }

    /**
     * @throws SamlException
     *
     */
    private boolean validateNameID() {
        boolean valid = true;
        if (samlLogoutRequest != null) {
            String principal = getNameIDValue();
            //String principal = samlLogoutRequest.getNameID().getValue();
            Saml20Token saml20token = PropagationHelper.getSaml20Token();
            if (saml20token == null) {
                // Even if we cannot validate this, we send success response to the IdP since the runtime cannot access authenticated data
                // meaning user is already logged out.
                // make sure that we can verify the signature though...
                try {
                    validateResponseSignature();
                } catch (SamlException e) {
                    statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.REQUESTER); //v3
                    return false;
                }
                statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.SUCCESS); //v3
                return false;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "NameID in the saml logout request = ", principal);
                Tr.debug(tc, "NameID in the current saml token  = ", saml20token.getSAMLNameID());
            }
            if (principal == null || !principal.equals(saml20token.getSAMLNameID())) {
                //TODO Tr.error
                statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.REQUESTER); //v3
                return false;
            }
        }
        return valid;
    }

    /**
     * @return
     */
    private String getNameIDValue() {
        String nameId = null;
        if (isNameIDEncrypted()) {
            try {
                nameId = decryptNameID();

            } catch (Exception e) {
                //TODO Tr.error
            }
        } else {
            nameId = samlLogoutRequest.getNameID().getValue();
        }
        return nameId;
    }

    /**
     * @return
     * @throws SamlException
     * @throws DecryptionException
     */
    private String decryptNameID() throws SamlException, DecryptionException {
        Decrypter decrypter = this.context.getDecrypter();
        SAMLObject samlobj = decrypter.decrypt(samlLogoutRequest.getEncryptedID());
        if (samlobj != null && samlobj instanceof NameID) {
            return ((NameID) samlobj).getValue();
        }
        return null;
    }

    /**
     * @return
     */
    private boolean isNameIDEncrypted() {
        return (samlLogoutRequest.getEncryptedID() != null);
    }

    /**
     * A. validate at redirectToRelayState() in com/ibm/ws/security/saml/sso20/acs/AcsHandler
     * B. Must not contain InResponseTo for unsolicited SSO
     *
     * @throws SamlException
     */
    void validateInResponseTo() throws SamlException {
        if (samlResponse != null) {
            RequestUtil.validateInResponseTo(context, samlResponse.getInResponseTo());
        } else if (samlLogoutResponse != null) {
            RequestUtil.validateInResponseTo(context, samlLogoutResponse.getInResponseTo());
        }
    }

    protected boolean validateStatus() throws SamlException {

        boolean valid = true;
        StatusCode requiredStatusCode = samlResponse.getStatus().getStatusCode();
        String statusCode = requiredStatusCode.getValue();

        if (!StatusCode.SUCCESS.equals(statusCode)) { //v3
            valid = false;
            String message = statusCode;
            StatusMessage statusMessage = samlResponse.getStatus().getStatusMessage();
            if (statusMessage != null) {
                message = statusMessage.getMessage();
            } else {
                StatusCode subStatusCode = requiredStatusCode.getStatusCode();
                if (subStatusCode != null) {
                    message = subStatusCode.getValue();
                }
            }
            // We may want to generate different messages base on the status code
            throw new SamlException("SAML20_SP_BAD_SAML_RESPONSE_ERROR", // NLS msg key
                            //"CWWKS5008E: The SAML Response from IdP provider [" + issuer +
                            //                "] contains failure Status code: [" + statusCode +
                            //                "], and Status Message:[" + message + "]", // default message
                            null, // Cause
                            new Object[] { issuer, statusCode, message } // parameters for NLS Message
            ); // Error handling
        }
        return valid;
    }

    public Status validateLogoutStatus() throws SamlException {

        StatusCode requiredStatusCode = samlLogoutResponse.getStatus().getStatusCode();
        String statusCode = requiredStatusCode.getValue();

        if (!StatusCode.SUCCESS.equals(statusCode)) {
            //valid = false;
            String message = statusCode;
            StatusMessage statusMessage = samlLogoutResponse.getStatus().getStatusMessage();
            if (statusMessage != null) {
                message = statusMessage.getMessage();
            } else {
                StatusCode subStatusCode = requiredStatusCode.getStatusCode();
                if (subStatusCode != null) {
                    message = subStatusCode.getValue();
                }
            }
            // We may want to generate different messages base on the status code
//            throw new SamlException("SAML20_SP_BAD_SAML_RESPONSE_ERROR", // NLS msg key
//                            //"CWWKS5008E: The SAML Response from IdP provider [" + issuer +
//                            //                "] contains failure Status code: [" + statusCode +
//                            //                "], and Status Message:[" + message + "]", // default message
//                            null, // Cause
//                            new Object[] { issuer, statusCode, message } // parameters for NLS Message
//            ); // Error handling
        }
        return samlLogoutResponse.getStatus();
    }

    /**
     *
     */
    boolean validateVersion() throws SamlException {

        SAMLVersion version = null;
        boolean isLogoutMessage = false;

        if (samlResponse != null) {
            version = samlResponse.getVersion();
        } else if (samlLogoutResponse != null) {
            version = samlLogoutResponse.getVersion();
            isLogoutMessage = true;
        } else if (samlLogoutRequest != null) {
            version = samlLogoutRequest.getVersion();
            isLogoutMessage = true;
        }
        int majorVersion = 0;
        int minorVersion = 0;
        if (version != null) {
            majorVersion = version.getMajorVersion();
            minorVersion = version.getMinorVersion();
        }
        if (majorVersion != 2 || minorVersion != 0) {
            if (isLogoutMessage) {
                statusBuilderUtil.setStatus(this.context.getSLOResponseStatus(), StatusCode.REQUESTER); //v3
                return false;
            } else {
                throw new SamlException("SAML20_SP_BAD_VERSION_ERROR",
                                //"CWWKS5010E: The SAML Response issued by the IdP provider [" + issuer +
                                //                "] has an invalid version, which is " + majorVersion +
                                //                "." + minorVersion + ".  The version must be 2.0",
                                null, // cause
                                new Object[] { version.toString() } // parameters for NLS Message
                ); // Error handling
            }
        }
        return true;
    }

    /**
     * The destination is optional. Only evaluate when it exists
     */
    protected boolean validateDestination() throws SamlException {
        String destination = null;
        if (samlResponse != null) {
            destination = samlResponse.getDestination();
        } else if (samlLogoutResponse != null) {
            destination = samlLogoutResponse.getDestination();
        } else if (samlLogoutRequest != null) {
            destination = samlLogoutRequest.getDestination();
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "destination is '" + destination + "'");
        }
        if (destination == null) {
            return true;
        }
        // destination exists, evaluate further
        String urlString = null;
        if (samlResponse != null) {
            urlString = RequestUtil.getAcsUrl(this.context.getHttpServletRequest(),

                                              Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                              this.context.getSsoService().getProviderId(),
                                              this.context.getSsoConfig());
        } else if (samlLogoutResponse != null || samlLogoutRequest != null) {
            urlString = RequestUtil.getSloUrl(this.context.getHttpServletRequest(), Constants.SAML20_CONTEXT_PATH,
                                              this.context.getSsoService().getProviderId(), this.context.getSsoConfig());
        }

        if (urlString.equals(destination)) {
            return true;
        } else {
            // error handling
            throw new SamlException("SAML20_RESPONSE_BAD_DESTINATION", // NLS msg key
                            //"CWWKS5012E: The SAML Response from IdP provider [" +
                            //                issuer + "] has unexpected destination [" +
                            //                destination + "]. The expected destination is [" +
                            //                urlString + "].",
                            null, // Cause
                            new Object[] { destination, urlString } // parameters for NLS Message
            ); // Error handling
        }

    }

    protected boolean validateIssuer() throws SamlException {
        Issuer samlIssuer = null;
        if (samlResponse != null) {
            samlIssuer = this.samlResponse.getIssuer();
        } else if (samlLogoutResponse != null) {
            samlIssuer = samlLogoutResponse.getIssuer();
        } else if (samlLogoutRequest != null) {
            samlIssuer = samlLogoutRequest.getIssuer();
        }

        return MsgCtxUtil.validateIssuer(samlIssuer, context, false); // not rsSaml
    }

    /**
     *
     */
    protected boolean validateIssueInstant() throws SamlException {

        DateTime jodaTime = null;
        if (samlResponse != null) {
            jodaTime = samlResponse.getIssueInstant();
        } else if (samlLogoutResponse != null) {
            jodaTime = samlLogoutResponse.getIssueInstant();
        } else if (samlLogoutRequest != null) {
            jodaTime = samlLogoutRequest.getIssueInstant();
        }

        if (jodaTime != null) {
            if (jodaTime.plus(clockSkewAllowed).isAfterNow() &&
                jodaTime.minus(clockSkewAllowed).isBeforeNow()) {
                return true;
            }
        }

        // check date time is within laterOkTime and EarlierTime

        // CWWKS5011E: The SAML Response issued by the Identity Provider [{0}] failed.
        // Its issue instant is [{1}] and not issued between [{2}] and [{3}].
        DateTime currentTime = new DateTime();
        throw new SamlException("SAML20_RESPONSE_BAD_ISSUE_TIME", // NLS msg key
                        //"CWWKS5011E: The SAML assertion with samlID: [" + samlResponse.getID() +
                        //                "] must be issued between " + jodaTime.minus(clockSkewAllowed)
                        //                + " and " + jodaTime.plus(clockSkewAllowed) + ", but it was issued at " + jodaTime,
                        null, // Cause
                        new Object[] { jodaTime,
                                       currentTime,
                                       clockSkewAllowed / 1000 //milliseconds to seconds
                        } // parameters for NLS Message
        );
    }

    protected void validateResponseSignature() throws SamlException {
        if ((samlResponse != null && samlResponse.getSignature() != null) ||
            (samlLogoutResponse != null && samlLogoutResponse.getSignature() != null) ||
            (samlLogoutRequest != null && samlLogoutRequest.getSignature() != null)) {
            verifyResponseSignature();
        }
    }

    protected void verifyResponseSignature() throws SamlException {
        try {
            TrustEngine<Signature> trustEngine = MsgCtxUtil.getTrustedEngine(context);
            SignatureValidationParameters sigValParams = new SignatureValidationParameters();
            sigValParams.setSignatureTrustEngine((SignatureTrustEngine) trustEngine);
            this.context.getMessageContext().getSubcontext(SecurityParametersContext.class, true).setSignatureValidationParameters(sigValParams); 
            SAMLMessageXMLSignatureSecurityPolicyRule signatureRule = new SAMLMessageXMLSignatureSecurityPolicyRule(); //v3
            try {
                signatureRule.initialize();
            } catch (ComponentInitializationException e) {
                throw new SamlException("SAML20_SIGNATURE_NOT_VERIFIED_ERR",
                                        //The SAML Assertion Signature is not trusted or invalid with exception [{0}].
                                        e,
                                        new Object[] { e
                                        });
            }
            
            signatureRule.invoke(this.context.getMessageContext()); //TODO v3
            signatureRule.evaluateProtocol(this.context);
            if (!signatureRule.getPeerContext().isAuthenticated()) {   //v3
                throw new SamlException("SAML20_SIGNATURE_NOT_VERIFIED_ERR",
                                //SAML20_SIGNARURE_NO_VERIFIED_ERR=CWWKS5046E: The SAML response message Signature is not verified.
                                null, new Object[] {});
            }
        } catch (MessageHandlerException e) { 
            throw new SamlException(e); // Let the SamlException handle the opensaml exception
            //"SAML Response Signature is not trusted or invalid. The signature validation fails with exception: " + e.getCause());
        }

    }

}
