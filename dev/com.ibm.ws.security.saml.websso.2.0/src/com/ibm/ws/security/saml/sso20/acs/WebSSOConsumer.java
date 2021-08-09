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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.internal.utils.DumpData;

/**
 *
 */
public class WebSSOConsumer<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject> {
    private static TraceComponent tc = Tr.register(WebSSOConsumer.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    @SuppressWarnings("rawtypes")
    static WebSSOConsumer<?, ?, ?> instance = new WebSSOConsumer();

    public static WebSSOConsumer<?, ?, ?> getInstance() {
        return instance;
    }

    static void setInstance(WebSSOConsumer<?, ?, ?> instance) {
        WebSSOConsumer.instance = instance;
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ SamlException.class })
    public BasicMessageContext<InboundMessageType, OutboundMessageType> handleSAMLResponse(HttpServletRequest req,
                                                                                                               HttpServletResponse res,
                                                                                                               SsoSamlService ssoService,
                                                                                                               String externalRelayState,
                                                                                                               SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> messageContext = null;
        try {
            @SuppressWarnings("rawtypes")
            BasicMessageContextBuilder ctxBuilder = BasicMessageContextBuilder.getInstance();
            messageContext = ctxBuilder.buildAcs(req, res, ssoService, externalRelayState, samlRequest);

            // get the SAML Response
            //Response samlResponse = (Response) messageContext.getInboundMessage();
            MessageContext<SAMLObject> mc = messageContext.getMessageContext(); //v3
            Response samlResponse = (Response) mc.getMessage(); //v3
            String inboundMsgIssuer = samlResponse.getIssuer().getValue(); //v3
            messageContext.setInboundSamlMessageIssuer(inboundMsgIssuer);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "samlResponse:" + samlResponse);
                Tr.debug(tc, DumpData.dumpXMLObject(null, samlResponse, 0).toString());
            }
            
            //TODO: new config attribute to get this value?
            if (messageContext.getPeerEntityMetadata() != null) {
                String issuer = messageContext.getPeerEntityMetadata().getEntityID();
                mc.getSubcontext(SAMLPeerEntityContext.class, true).setEntityId(issuer); //v3
            }           
            mc.getSubcontext(SAMLPeerEntityContext.class, true).setRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
            mc.getSubcontext(SAMLProtocolContext.class, true).setProtocol(SAMLConstants.SAML20P_NS);
            //validate Response
            ResponseValidator<InboundMessageType, OutboundMessageType, NameIdentifierType> validator = new ResponseValidator<InboundMessageType, OutboundMessageType, NameIdentifierType>(messageContext, samlResponse);
            validator.validate();

            //decrypt EncryptedAssertion, and add decrypted assertion to Assertion list
            List<Assertion> assertions = decryptEncryptedAssertion(samlResponse, messageContext);
            //Search for first valid assertion without exception
            Assertion validatedAssertion = null;
            Exception lastException = null;
            // Find the valid assertion as defined in profile
            for (Assertion assertion : assertions) {
                if (assertion.getAuthnStatements().size() > 0 && assertion.getSubject() != null) {
                    try {
                        AssertionValidator assertionValidator = new AssertionValidator(messageContext, assertion);
                        assertionValidator.validateAssertion();
                        validatedAssertion = assertion;
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Found valid Asserion " + assertion.getID());
                        }
                        break;
                    } catch (Exception e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Invalid Asserion " + assertion.getID());
                        }
                        lastException = e;
                    }
                } else {
                    if (assertion.getSubject() == null) {
                        lastException = new SamlException("SAML20_ELEMENT_ERR",
                                        // SAML20_NO_SUBJECT_ERR=CWWKS5042E: The SAML Assertion MUST contain a Subject element.
                                        null, new Object[] { "Subject" });
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Assertion " + assertion.getID() + " does not contain Subject");
                        }
                    } else if (assertion.getAuthnStatements().size() == 0) {
                        lastException = new SamlException("SAML20_ELEMENT_ERR",
                                        // SAML20_NO_AUTHN_STATEMENT_ERR=CWWKS5043E: The SAML Assertion MUST contain a AuthnStatement element.
                                        null, new Object[] { "AuthnStatement" });
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Assertion " + assertion.getID() + " does not contain AuthnStatement");
                        }
                    }
                }
            }

            if (validatedAssertion != null) {
                messageContext.setValidatedAssertion(validatedAssertion);
            } else {
                throw lastException;
            }

        } catch (SamlException e) {
            throw e; // this had been handled already
        } catch (Exception e) {
            // unexpected error handling
            throw new SamlException(e); // let the samlException handle the unexpected Exception
        }
        return messageContext;
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ SamlException.class })
    public BasicMessageContext<InboundMessageType, OutboundMessageType> handleSAMLLogoutResponse(HttpServletRequest req,
                                                                                                                     HttpServletResponse res,
                                                                                                                     SsoSamlService ssoService,
                                                                                                                     String externalRelayState,
                                                                                                                     SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> messageContext = null;
        try {
            @SuppressWarnings("rawtypes")
            BasicMessageContextBuilder ctxBuilder = BasicMessageContextBuilder.getInstance();
            messageContext = ctxBuilder.buildSLO(req, res, ssoService, externalRelayState, samlRequest);
            MessageContext<SAMLObject> mc = messageContext.getMessageContext();
            
            // get the SAML Response
            LogoutResponse samlLogoutResponse = (LogoutResponse) messageContext.getMessageContext().getMessage(); //v3
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "saml logoutResponse:" + samlLogoutResponse);
                Tr.debug(tc, DumpData.dumpXMLObject(null, samlLogoutResponse, 0).toString());
            }
            messageContext.setInboundSamlMessageIssuer(samlLogoutResponse.getIssuer().getValue());
            // TODO: new config attribute to get this value?
            if (messageContext.getPeerEntityMetadata() != null) {
                String issuer = messageContext.getPeerEntityMetadata().getEntityID();
                mc.getSubcontext(SAMLPeerEntityContext.class, true).setEntityId(issuer);
            }           
            mc.getSubcontext(SAMLPeerEntityContext.class, true).setRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
            mc.getSubcontext(SAMLProtocolContext.class, true).setProtocol(SAMLConstants.SAML20P_NS);
            //validate LogoutResponse - first check for status and then look at other data
            ResponseValidator<InboundMessageType, OutboundMessageType, NameIdentifierType> validator = new ResponseValidator<InboundMessageType, OutboundMessageType, NameIdentifierType>(messageContext, samlLogoutResponse);
            messageContext.setSLOResponseStatus(validator.validateLogoutStatus());
            if (StatusCode.SUCCESS.equals(messageContext.getSLOResponseStatus().getStatusCode().getValue())) {
                validator.validateLogoutResponse();
            }

        } catch (SamlException e) {
            throw e; // this had been handled already
        } catch (Exception e) {
            // unexpected error handling
            throw new SamlException(e); // let the samlException handle the unexpected Exception
        }
        return messageContext;
    }

    @SuppressWarnings("unchecked")
    public BasicMessageContext<InboundMessageType, OutboundMessageType> handleSAMLLogoutRequest(HttpServletRequest req,
                                                                                                                    HttpServletResponse res,
                                                                                                                    SsoSamlService ssoService,
                                                                                                                    String externalRelayState,
                                                                                                                    SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> messageContext = null;
        @SuppressWarnings("rawtypes")
        BasicMessageContextBuilder ctxBuilder = BasicMessageContextBuilder.getInstance();
        messageContext = ctxBuilder.buildSLO(req, res, ssoService, externalRelayState, samlRequest);
        MessageContext<SAMLObject> mc = messageContext.getMessageContext();
        
        // get the SAML Request
        LogoutRequest samlLogoutRequest = (LogoutRequest) messageContext.getMessageContext().getMessage();
        messageContext.setInboundSamlMessageIssuer(samlLogoutRequest.getIssuer().getValue());
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "saml logoutRequest:" + samlLogoutRequest);
            Tr.debug(tc, "saml logoutRequest ID :" + samlLogoutRequest.getID());
            Tr.debug(tc, DumpData.dumpXMLObject(null, samlLogoutRequest, 0).toString());
        }
        //TODO: new config attribute to get this value?
        if (messageContext.getPeerEntityMetadata() != null) {
            String issuer = messageContext.getPeerEntityMetadata().getEntityID();
            mc.getSubcontext(SAMLPeerEntityContext.class, true).setEntityId(issuer);
        }           
        mc.getSubcontext(SAMLPeerEntityContext.class, true).setRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
        mc.getSubcontext(SAMLProtocolContext.class, true).setProtocol(SAMLConstants.SAML20P_NS);
        // Status initialization
        StatusBuilderUtil statusBuilderUtil = new StatusBuilderUtil();
        messageContext.setSLOResponseStatus(statusBuilderUtil.buildStatus());

        //validate LogoutRequest
        ResponseValidator<InboundMessageType, OutboundMessageType, NameIdentifierType> validator = new ResponseValidator<InboundMessageType, OutboundMessageType, NameIdentifierType>(messageContext, samlLogoutRequest);
        if (validator.validateLogoutRequest()) {
            statusBuilderUtil.setStatus(messageContext.getSLOResponseStatus(), StatusCode.SUCCESS);
        }
        messageContext.setInResponseTo(samlLogoutRequest.getID());

        return messageContext;
    }

    List<Assertion> decryptEncryptedAssertion(Response samlResponse, BasicMessageContext<?, ?> context) throws SamlException {
        List<Assertion> assertionList = samlResponse.getAssertions();

        // Decrypt EncryptedAssertion if there is any
        List<EncryptedAssertion> encryptedAssertionList = samlResponse.getEncryptedAssertions();
        if (encryptedAssertionList.size() > 0) {
            int numberOfAssertions = samlResponse.getAssertions().size() + samlResponse.getEncryptedAssertions().size();
            assertionList = new ArrayList<Assertion>(numberOfAssertions);
            assertionList.addAll(samlResponse.getAssertions());
            Decrypter decrypter = context.getDecrypter();
            for (EncryptedAssertion eA : encryptedAssertionList) {
                try {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "encryptedAssertion:" + eA + " decrypter:" + decrypter);
                    }
                    Assertion decryptedAssertion = decrypter.decrypt(eA); //decrypt it!
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "decryptedAssertion:" + decryptedAssertion);
                    }
                    assertionList.add(decryptedAssertion);
                    samlResponse.getAssertions().add(decryptedAssertion);
                } catch (Exception e) {
                    throw new SamlException(e);
                }
            }
        }
        return assertionList;
        //return samlResponse.getAssertions();
    }

}
