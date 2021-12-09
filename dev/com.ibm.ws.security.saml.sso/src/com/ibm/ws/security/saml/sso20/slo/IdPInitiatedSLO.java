/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.slo;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusResponseType;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.LogoutRequestMarshaller;
import org.opensaml.saml2.core.impl.LogoutResponseBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;

/**
 * http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
 * section 3.4
 */
public class IdPInitiatedSLO {
    public static final TraceComponent tc = Tr.register(IdPInitiatedSLO.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    SsoSamlService ssoService = null;
    BasicMessageContext<?, ?, ?> basicMsgCtx;
    String idpRelayState = null;
    final static String SINDEX = "sessionIndex";

    /**
     * @param service
     * @param msgCtx
     */
    public IdPInitiatedSLO(SsoSamlService service, BasicMessageContext<?, ?, ?> msgCtx) {
        ssoService = service;
        basicMsgCtx = msgCtx;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "IdPInitiatedSLO(" + service.getProviderId() + ")");
        }
    }

    /**
     * @param ssoService2
     * @param msgCtx
     * @param externalRelayState
     */
    public IdPInitiatedSLO(SsoSamlService service, BasicMessageContext<?, ?, ?> msgCtx, String externalRelayState) {
        ssoService = service;
        basicMsgCtx = msgCtx;
        idpRelayState = externalRelayState;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "IdPInitiatedSLO(" + service.getProviderId() + ")");
        }
    }

    /**
     * @param req
     * @param resp
     * @return
     */
    @FFDCIgnore({ SamlException.class })
    public void sendSLOResponseToIdp(HttpServletRequest req, HttpServletResponse resp) {

//        if (StatusCode.SUCCESS_URI.equals(getStatus(basicMsgCtx))) {
        try {
            req.setAttribute(Constants.SLOINPROGRESS, Boolean.valueOf(true));
            req.logout();
        } catch (ServletException e1) {
            //TODO Tr.error
            basicMsgCtx.getSLOResponseStatus().getStatusCode().setValue(StatusCode.RESPONDER_URI);
        }
//        }

        String idpUrl = null;
        try {

            idpUrl = handleIdpMetadataAndLogoutUrl(basicMsgCtx);

            LogoutResponse logoutResponse = buildLogoutResponse(basicMsgCtx.getInResponseTo(), req, basicMsgCtx);

            String sloResponseStr = "";
            if (basicMsgCtx.getSsoConfig().isAuthnRequestsSigned()) {
                signLogoutResponse(logoutResponse, RequestUtil.getSigningCredential(ssoService));
            }
            sloResponseStr = getSignedLogoutResponseString(logoutResponse);

            String shortRelayState = SamlUtil.generateRandom(); // no need to Base64 encode
            String relayState = Constants.SP_INITAL + shortRelayState;
            postIdp(req, resp, sloResponseStr, relayState, idpUrl);

        } catch (SamlException e1) {
            //TODO Tr.error
            handleLogoutError(req, resp);
        }
    }

    /**
     * @param req
     * @param resp
     */
    private void handleLogoutError(HttpServletRequest req, HttpServletResponse resp) {
        // TODO Auto-generated method stub
        // display error page

    }

    /**
     * find the single-logout service URL of the IdP
     *
     * @param basicMsgCtx
     * @return
     * @throws SamlException
     */
    String handleIdpMetadataAndLogoutUrl(BasicMessageContext<?, ?, ?> basicMsgCtx) throws SamlException {
        String idpUrl = null;
        MetadataProvider metadataProvider = basicMsgCtx.getMetadataProvider();
        if (metadataProvider == null) {
            // Should only happen during testing the installation
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "idp metadata file :" + basicMsgCtx.getSsoConfig().getIdpMetadata());
            }
            SsoConfig ssoConfig = ssoService.getConfig();
            String idpMetadataFile = ssoConfig.getIdpMetadata();
            String providerId = ssoService.getProviderId();
            if (idpMetadataFile == null || idpMetadataFile.isEmpty()) {
                throw new SamlException("SAML20_NO_IDP_URL_OR_METADATA",
                                //SAML20_NO_IDP_URL_OR_METADATA=CWWKS5080E: Cannot find the IdP URL because the identity provider (IdP) metadata file is not set in the service provider (SP) [{0}].
                                null, new Object[] { providerId });
            } else {
                throw new SamlException("SAML20_NO_IDP_URL_ERROR",
                                //SAML20_NO_IDP_URL_ERROR=CWWKS5079E: Cannot find the IdP URL through the identity provider (IdP) metadata file [{0}] in the service provider (SP) [{1}].
                                null, new Object[] { idpMetadataFile, providerId });
            }

        }

        XMLObject metadata = null;
        try {
            metadata = metadataProvider.getMetadata();
            // assume the metadataProvider is an entityDescriptor for now
            if (metadata instanceof EntityDescriptor) {
                EntityDescriptor entityDescriptor = (EntityDescriptor) metadata;
                String idpEntityId = entityDescriptor.getEntityID(); // output variable
                basicMsgCtx.setPeerEntityId(idpEntityId);
                IDPSSODescriptor ssoDescriptor = entityDescriptor.getIDPSSODescriptor(Constants.SAML20P_NS);
                if (ssoDescriptor != null) {
                    List<SingleLogoutService> list = ssoDescriptor.getSingleLogoutServices();
                    for (SingleLogoutService sloService : list) {
                        if (Constants.SAML2_POST_BINDING_URI.equals(sloService.getBinding())) {
                            basicMsgCtx.setPeerEntityEndpoint(sloService);
                            idpUrl = sloService.getLocation(); // output
                            break;
                        }
                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "idpLogout url:" + idpUrl + "(" + Constants.SAML2_POST_BINDING_URI + ")");
                    }
                } else {
                    SsoConfig ssoConfig = ssoService.getConfig();
                    String idpMetadataFile = ssoConfig.getIdpMetadata();
                    String providerId = ssoService.getProviderId();
                    throw new SamlException("SAML20_IDP_METADATA_PARSE_ERROR",
                                    //SAML20_IDP_METADATA_PARSE_ERROR=CWWKS5023E: The identity provider (IdP) metadata file [{0}] in the service provider (SP) [{1}] is not valid. The cause of the error is [{2}]
                                    null, new Object[] { idpMetadataFile, providerId, "No IDPSSODescriptor" });
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "ERROR: metadata is not an EntityDescriptor");
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "idp metadata file :" + basicMsgCtx.getSsoConfig().getIdpMetadata());
                }
                SsoConfig ssoConfig = ssoService.getConfig();
                String idpMetadaFile = ssoConfig.getIdpMetadata();
                String providerId = ssoService.getProviderId();
                throw new SamlException("SAML20_NO_IDP_URL_ERROR",
                                //SAML20_NO_IDP_URL_ERROR=CWWKS5079E: Cannot find the IdP URL through the identity provider (IdP) metadata file [{0}] in the service provider (SP) [{1}].
                                null, new Object[] { idpMetadaFile, providerId });
            }
        } catch (MetadataProviderException e) {
            throw new SamlException(e); // Let SamlException handles the unexpected Exception
        }
        return idpUrl;
    }

    /**
     *
     * @param inResponseToId
     * @param req
     * @param basicMsgCtx
     * @return
     * @throws SamlException
     */
    LogoutResponse buildLogoutResponse(String inResponseToId,
                                       HttpServletRequest req,
                                       BasicMessageContext<?, ?, ?> basicMsgCtx) throws SamlException {

        // If the request is valid, then build and send response to IdP
        // get the request valid status from the msgCtx, so we can send the correct status to IdP
        // also if the following call fails, then we should send error status accordingly
        LogoutResponseBuilder lorBuilder = new LogoutResponseBuilder();
        LogoutResponse response = lorBuilder.buildObject();

        response.setInResponseTo(inResponseToId);
        response.setConsent(StatusResponseType.UNSPECIFIED_CONSENT); //TODO

        //response.setProtocolBinding(Constants.SAML2_POST_BINDING_URI);
        if (basicMsgCtx == null || basicMsgCtx.getPeerEntityEndpoint() == null || basicMsgCtx.getPeerEntityEndpoint().getLocation() == null) {
            throw new SamlException("SAML20_SLOENDPOINT_NOT_IN_METADATA", null, new Object[] { ssoService.getProviderId() }); // CWWKS5214E
        }
        response.setDestination(basicMsgCtx.getPeerEntityEndpoint().getLocation());
        response.setIssueInstant(new DateTime());
        response.setVersion(SAMLVersion.VERSION_20);
        String id = SamlUtil.generateRandomID();
        response.setID(id);

        String entityUrl = RequestUtil.getEntityUrl(req,
                                                    Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                                    ssoService.getProviderId(),
                                                    ssoService.getConfig());
        response.setIssuer(getIssuer(entityUrl));

        StatusBuilder statusBuilder = new StatusBuilder();
        Status status = statusBuilder.buildObject();
        StatusCodeBuilder statusCodeBuilder = new StatusCodeBuilder();
        StatusCode statusCode = statusCodeBuilder.buildObject();

        String value = getStatus(basicMsgCtx);
        statusCode.setValue(value);

        status.setStatusCode(statusCode);
        //status.setStatusDetail(newStatusDetail);
        response.setStatus(status);
        return response;
    }

    /**
     * @param basicMsgCtx
     * @return
     */
    private String getStatus(BasicMessageContext<?, ?, ?> basicMsgCtx) {
        return basicMsgCtx.getSLOResponseStatus().getStatusCode().getValue();
        //return StatusCode.SUCCESS_URI;
    }

    /**
     * @param acsEndpointUrl
     * @return
     */
    Issuer getIssuer(String acsEndpointUrl) {
        IssuerBuilder issuerBuilder = new IssuerBuilder();
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(acsEndpointUrl);
        return issuer;
    }

    /**
     * Send the LogoutResponse to IdP
     *
     * @param req
     * @param resp
     * @param logoutResponse
     * @param relayState
     * @param idpUrl
     * @return
     */
    void postIdp(HttpServletRequest req,
                 HttpServletResponse resp,
                 String logoutResponse,
                 String relayState,
                 String idpUrl) throws SamlException {

        byte[] logoutResBytes = null;
        try {
            logoutResBytes = logoutResponse.getBytes(Constants.UTF8);
        } catch (UnsupportedEncodingException e1) {
            // error handling, UTF8 should not have errors
            SamlException samlException = new SamlException(e1); // let the SamlException to handle the Exception

            throw samlException;
        }

        String samlResponse = Base64.encodeBytes(logoutResBytes, Base64.DONT_BREAK_LINES);

        if (relayState == null || samlResponse == null || idpUrl == null) {
            // This should not happen
            throw new SamlException("RelayState, Single-Sign-On URL, and Saml Logout Response must be provided");
        }

        resp.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);

        ForwardRequestInfo requestInfo = new ForwardRequestInfo(idpUrl);
        // requestInfo.setFragmentCookieId(cachingRequestInfo.getFragmentCookieId()); //TODO
        // If IdP sends the relaystate, then add it back into response
        if (this.idpRelayState != null) {
         requestInfo.setParameter("RelayState", new String[] { this.idpRelayState }); // IdP sent one in the Logout request
        }
        // requestInfo.setParameter("RelayState", new String[] { relayState }); // IdP did not send one in the Logout request
        requestInfo.setParameter("SAMLResponse", new String[] { samlResponse });
        requestInfo.redirectPostRequest(req,
                                        resp,
                                        null, // In SP_INIT, we do not depend on cookie to store sp_init id
                                        null); // IdP requires to return RelayStae by spec

        // expect to return a form to redirect to the idp by the browser
//        try {
//            return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
//        } catch (WebTrustAssociationFailedException e) {
//            throw new SamlException(e);
//        }
    }

    /**
     * Signs the given SAML message if it a {@link SignableSAMLObject} and this encoder has signing credentials.
     *
     * @param messageContext current message context
     *
     * @throws MessageEncodingException thrown if there is a problem marshalling or signing the outbound message
     * @throws SamlException
     */
    @SuppressWarnings("unchecked")
    void signLogoutResponse(SAMLObject logoutResponse, Credential signingCredential) throws SamlException {
        SsoConfig samlConfig = ssoService.getConfig();
        if (logoutResponse instanceof SignableSAMLObject && signingCredential != null) {
            SignableSAMLObject signableMessage = (SignableSAMLObject) logoutResponse;

            XMLObjectBuilder<Signature> signatureBuilder = Configuration.getBuilderFactory().getBuilder(
                                                                                                        Signature.DEFAULT_ELEMENT_NAME);
            Signature signature = signatureBuilder.buildObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSignatureAlgorithm(samlConfig.getSignatureMethodAlgorithm());//SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);// SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1); //
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            signature.setSigningCredential(signingCredential);
            try {
                SecurityHelper.prepareSignatureParams(signature, signingCredential, null, null);
            } catch (SecurityException e) {
                throw new SamlException(e, true); // Let SamlException handles opensaml Exception
            }

            signableMessage.setSignature(signature);

            try {
                Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(signableMessage);
                if (marshaller == null) {
                    //Tr.error(tc, "SAML20_NO_MARSHALLER_FOUND", signableMessage.getElementQName());
                    // CWWKS5043E: The Web Service request failed. Cannot find a marshaller registered for [{0}] unable to marshall the Element.
                    throw new SamlException("SAML20_AUTHENTICATION_FAIL",
                                    //"CWWKS5063E: The Web Service Request failed due to the authentication is not successful.",
                                    null, new Object[] {});
                }
                marshaller.marshall(signableMessage);

                Signer.signObject(signature);
            } catch (Exception e) {
                throw new SamlException(e, true); // Let SamlException handles opensaml Exception
            }
        }
    }

    String getSignedLogoutResponseString(LogoutResponse logoutResponse) throws SamlException {
        String result = null;
        if (logoutResponse != null) {
            try {
                LogoutRequestMarshaller marshaller = new LogoutRequestMarshaller();
                Element element = marshaller.marshall(logoutResponse);
                result = XMLHelper.nodeToString(element);
            } catch (MarshallingException e) {
                throw new SamlException(e, true); // Let SamlException handles opensaml Exception
            }
        }
        return result;
    }

}
