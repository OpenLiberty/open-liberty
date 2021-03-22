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
package com.ibm.ws.security.saml.sso20.slo;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml.saml2.core.impl.LogoutRequestMarshaller;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml.saml2.core.impl.SessionIndexBuilder;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.opensaml.xmlsec.signature.support.SignerProvider;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;
import com.ibm.ws.security.sso.common.saml.propagation.SamlCommonUtil;
import com.ibm.wsspi.security.tai.TAIResult;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 * http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
 * section 3.4
 */
public class SPInitiatedSLO {
    public static final TraceComponent tc = Tr.register(SPInitiatedSLO.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    SsoSamlService ssoService = null;
    Subject subject = null;
    final static String SINDEX = "sessionIndex";

    /**
     * @param service
     */
    public SPInitiatedSLO(SsoSamlService service, Subject sub) {
        ssoService = service;
        subject = sub;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SLOSolicited(" + service.getProviderId() + ")");
        }
    }

    /**
     * @param req
     * @param resp
     * @return
     * @throws WebTrustAssociationFailedException
     * @throws SamlException
     */
    @FFDCIgnore({ SamlException.class })
    public void buildandSendSLORequest(HttpServletRequest req, HttpServletResponse resp) throws SamlException {

        // need to redirect to the Idp
        BasicMessageContext<?, ?> basicMsgCtx = BasicMessageContextBuilder.getInstance().buildIdp(req, resp, ssoService);
        String idpSLOUrl = handleIdpMetadataAndLogoutUrl(basicMsgCtx);

        //check whether we need to enforce ssl communication with IdP
        if (ssoService.getConfig() != null) {
            if (idpSLOUrl != null && ssoService.getConfig().isHttpsRequired() && !(idpSLOUrl.startsWith("https"))) {
                throw new SamlException("SAML20_IDP_PROTOCOL_NOT_HTTPS", null, new Object[] { idpSLOUrl });
            }
        }

        HttpRequestInfo cachingRequestInfo = new HttpRequestInfo(req);
        req.setAttribute(Constants.SP_INITIATED_SLO_IN_PROGRESS, "true"); // let FormLogoutExtensionProcessor know were handling it.
        cachingRequestInfo.restorePostParams(req);
        LogoutRequest logoutRequest = buildLogoutRequest(cachingRequestInfo.getInResponseToId(), req, basicMsgCtx);
        String sloRequestStr = null;
        try {
            if (basicMsgCtx.getSsoConfig().isAuthnRequestsSigned()) {
                signLogoutRequest(logoutRequest, RequestUtil.getSigningCredential(ssoService));
            }
            sloRequestStr = getLogoutRequestString(logoutRequest);
        } catch (SamlException e) {
            // error handling
            // This should not happen unless some unexpected data in the logout request
            throw e;
        }
        String shortRelayState = SamlUtil.generateRandom(); // no need to Base64 encode
        String relayState = Constants.SP_INITAL + shortRelayState;
        RequestUtil.cacheRequestInfo(shortRelayState, ssoService, cachingRequestInfo); // cache with shorRelayState
        basicMsgCtx.setCachedRequestInfo(cachingRequestInfo);
        TAIResult result = postIdp(req, resp, sloRequestStr, relayState, idpSLOUrl, cachingRequestInfo); // send out with the long relayState
        //return result;
    }

    /**
     * find the single-logout service URL of the IdP
     *
     * @param basicMsgCtx
     * @return
     * @throws SamlException
     */
    String handleIdpMetadataAndLogoutUrl(BasicMessageContext<?, ?> basicMsgCtx) throws SamlException {
        String idpUrl = null;
        //MetadataProvider metadataProvider = basicMsgCtx.getMetadataProvider();
        AcsDOMMetadataProvider metadataProvider = basicMsgCtx.getMetadataProvider();
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

        //XMLObject metadata = null;
        EntityDescriptor metadata2 = null; //v3
        String entityID = metadataProvider.getEntityId(); //TODO: we need a config attribute
        CriteriaSet criteriaSet;
        try {
            criteriaSet = new CriteriaSet(new EntityIdCriterion(entityID));
            metadata2 = metadataProvider.resolveSingle(criteriaSet);
            
            // TODO: make sure that we have an EntityDescriptor
            if (metadata2 != null) {
                EntityDescriptor entityDescriptor = (EntityDescriptor) metadata2;
                String idpEntityId = entityDescriptor.getEntityID(); // output variable
                //basicMsgCtx.setPeerEntityId(idpEntityId);
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
                // Should only happen during testing the installation
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
        } catch (ResolverException e) {
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
     */
    LogoutRequest buildLogoutRequest(String inResponseToId,
                                     HttpServletRequest req,
                                     BasicMessageContext<?, ?> basicMsgCtx) throws SamlException {

        LogoutRequestBuilder lorBuilder = new LogoutRequestBuilder();
        LogoutRequest request = lorBuilder.buildObject();

        request.setID(inResponseToId);
        String acsEndpointUrl = RequestUtil.getAcsUrl(req,
                                                      Constants.SAML20_CONTEXT_PATH,
                                                      ssoService.getProviderId(),
                                                      ssoService.getConfig());

        //request.setAssertionConsumerServiceURL(acsEndpointUrl);
        //request.setProtocolBinding(Constants.SAML2_POST_BINDING_URI);
        request.setVersion(SAMLVersion.VERSION_20);

        //request.setForceAuthn(samlConfig.isForceAuthn());
        //request.setIsPassive(samlConfig.isPassive());
        //request.setNameIDPolicy(buildNameIdPolicy(samlConfig));
        String entityUrl = RequestUtil.getEntityUrl(req,
                                                    Constants.SAML20_CONTEXT_PATH,
                                                    ssoService.getProviderId(),
                                                    ssoService.getConfig());
        request.setIssuer(getIssuer(entityUrl));
        request.setIssueInstant(new DateTime());

        if (basicMsgCtx == null || basicMsgCtx.getPeerEntityEndpoint() == null || basicMsgCtx.getPeerEntityEndpoint().getLocation() == null) {
            throw new SamlException("SAML20_SLOENDPOINT_NOT_IN_METADATA", null, new Object[] { ssoService.getProviderId() }); // CWWKS5214E
        }
        request.setDestination(basicMsgCtx.getPeerEntityEndpoint().getLocation());

        NameIDBuilder nidBuilder = new NameIDBuilder();
        NameID nid = nidBuilder.buildObject();
        String session = null;
        //nid.setNameQualifier(arg0);
        Saml20Token saml20token = SamlCommonUtil.getSaml20TokenFromSubject(subject, true); //PropagationHelper.getSaml20Token();
        if (saml20token != null) {
            NameID originalnid = (NameID) saml20token.getProperties().get("NameID");
            nid.setFormat(saml20token.getSAMLNameIDFormat())/* "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress") */;
            nid.setValue(saml20token.getSAMLNameID())/* "user1@adfsidp1.rtp.raleigh.ibm.com") */;
            nid.setSPNameQualifier(originalnid.getSPNameQualifier());
            nid.setNameQualifier(originalnid.getNameQualifier());
            session = (String) saml20token.getProperties().get(SINDEX);
        } else {
            //TODO - there is no point in continuing if there is no valid saml token
            throw new SamlException("LOGOUT_CANNOT_FIND_SAMLTOKEN"); // CWWKS5216E
        }
        request.setNameID(nid);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "session index = ", session);
        }
        SessionIndexBuilder siBuilder = new SessionIndexBuilder();
        SessionIndex si = siBuilder.buildObject();
        si.setSessionIndex(session);
        request.getSessionIndexes().add(si);

        return request;
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
     * @param samlConfig
     * @return
     */
    NameIDPolicy buildNameIdPolicy(SsoConfig samlConfig) {
        NameIDPolicyBuilder nameIDPolicyBuilder = new NameIDPolicyBuilder();
        NameIDPolicy nameIDPolicy = nameIDPolicyBuilder.buildObject();
        // debugging
        String strNameIDPolicy = samlConfig.getNameIDFormat();
        if (strNameIDPolicy != null && !strNameIDPolicy.isEmpty()) {
            nameIDPolicy.setFormat(strNameIDPolicy);
        }
        Boolean allowCreate = samlConfig.getAllowCreate();
        if (allowCreate != null) {
            nameIDPolicy.setAllowCreate(allowCreate);
        }
        return nameIDPolicy;
    }

    /**
     * Send the authnRequest to IdP
     *
     * @param req
     * @param resp
     * @param LogoutRequest
     * @param relayState
     * @param idpUrl
     * @param cachingRequestInfo
     * @return
     * @throws SamlException
     */
    TAIResult postIdp(HttpServletRequest req,
                      HttpServletResponse resp,
                      String logoutRequest,
                      String relayState,
                      String idpUrl,
                      HttpRequestInfo cachingRequestInfo) throws SamlException {

        byte[] logoutReqBytes = null;
        try {
            logoutReqBytes = logoutRequest.getBytes(Constants.UTF8);
        } catch (UnsupportedEncodingException e1) {
            // error handling, UTF8 should not have errors
            SamlException samlException = new SamlException(e1); // let the SamlException to handle the Exception

            throw samlException;
        }

        String samlRequest = Base64Support.encode(logoutReqBytes, Base64Support.UNCHUNKED); //v3

        if (relayState == null || samlRequest == null || idpUrl == null) {
            // This should not happen
            throw new SamlException("RelayState, Single-Sign-On URL, and Saml Logout Request must be provided");
        }

//        try {
//            req.logout();
//        } catch (ServletException e1) {
//            //throw new SamlException(e1); //TODO
//        }
        resp.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);

        ForwardRequestInfo requestInfo = new ForwardRequestInfo(idpUrl);
        requestInfo.setFragmentCookieId(cachingRequestInfo.getFragmentCookieId());
        requestInfo.setParameter("RelayState", new String[] { relayState });
        requestInfo.setParameter("SAMLRequest", new String[] { samlRequest });
        requestInfo.redirectPostRequest(req,
                                        resp,
                                        null, // In SP_INIT, we do not depend on cookie to store sp_init id
                                        null); // IdP requires to return RelayState by spec

        // expect to return a form to redirect to the idp by the browser
        try {
            return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        } catch (WebTrustAssociationFailedException e) {
            throw new SamlException(e);
        } //
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
    void signLogoutRequest(SAMLObject logoutRequest, Credential signingCredential) throws SamlException {
        SsoConfig samlConfig = ssoService.getConfig();
        if (logoutRequest instanceof SignableSAMLObject && signingCredential != null) {
            SignableSAMLObject signableMessage = (SignableSAMLObject) logoutRequest;
            
            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
            XMLObjectBuilder<Signature> signatureBuilder = (XMLObjectBuilder<Signature>)builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME);
            Signature signature = signatureBuilder.buildObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSignatureAlgorithm(samlConfig.getSignatureMethodAlgorithm());//SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);// SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1); //
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            signature.setSigningCredential(signingCredential);
            signableMessage.setSignature(signature);
            final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(signableMessage);//v3
                if (marshaller == null) {
                    throw new SamlException("SAML20_AUTHENTICATION_FAIL",
                                    //"CWWKS5063E: The Web Service Request failed due to the authentication is not successful.",
                                    null, new Object[] {});
                }
                marshaller.marshall(signableMessage);
                Thread.currentThread().setContextClassLoader(SignerProvider.class.getClassLoader());
                Signer.signObject(signature);
            } catch (Exception e) {
                throw new SamlException(e, true); // Let SamlException handles opensaml Exception
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader); 
            }
        }
    }

    String getLogoutRequestString(LogoutRequest logoutRequest) throws SamlException {
        String result = null;
        if (logoutRequest != null) {
            try {
                LogoutRequestMarshaller marshaller = new LogoutRequestMarshaller();
                Element element = marshaller.marshall(logoutRequest);
                //result = XMLHelper.nodeToString(element);
                result = SerializeSupport.nodeToString(element);//v3
            } catch (MarshallingException e) {
                throw new SamlException(e, true); // Let SamlException handles opensaml Exception
            }
        }
        return result;
    }

}
