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
package com.ibm.ws.security.saml.sso20.sp;

import java.io.UnsupportedEncodingException;
import java.util.List;

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
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnRequestMarshaller;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.opensaml.xmlsec.signature.support.SignerProvider;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;
import com.ibm.wsspi.security.tai.TAIResult;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 * http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf
 * section 3.4
 */
public class Solicited {
    public static final TraceComponent tc = Tr.register(Solicited.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    SsoSamlService ssoService = null;
    InitialRequestUtil irUtil = new InitialRequestUtil();

    /**
     * @param service
     */
    public Solicited(SsoSamlService service) {
        ssoService = service;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Solicited(" + service.getProviderId() + ")");
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
    public TAIResult sendAuthRequestToIdp(HttpServletRequest req, HttpServletResponse resp) throws WebTrustAssociationFailedException, SamlException {

        // need to redirect to the Idp
        BasicMessageContext<?, ?> basicMsgCtx = BasicMessageContextBuilder.getInstance().buildIdp(req, resp, ssoService);
        String idpUrl = handleIdpMetadataAndLoginUrl(basicMsgCtx);

        //check whether we need to enforce ssl communication with IdP
        if (ssoService.getConfig() != null) {
            if (idpUrl != null && ssoService.getConfig().isHttpsRequired() && !(idpUrl.startsWith("https"))) {
                throw new SamlException("SAML20_IDP_PROTOCOL_NOT_HTTPS",
                                null,
                                new Object[] { idpUrl });
            }
        }
        String shortRelayState = SamlUtil.generateRandom(); // no need to Base64 encode
        HttpRequestInfo cachingRequestInfo = new HttpRequestInfo(req);
        AuthnRequest authnRequest = buildAuthnRequest(cachingRequestInfo.getInResponseToId(), req, basicMsgCtx);
        String strAuthnRequest = "";
        try {
            if (basicMsgCtx.getSsoConfig().isAuthnRequestsSigned()) {
                signAuthnRequest(authnRequest, RequestUtil.getSigningCredential(ssoService));
            }
            strAuthnRequest = getAuthnRequestString(authnRequest);
        } catch (SamlException e) {
            // error handling
            // This should not happen unless some unexpected data happens in the AuthnRequest
            throw e;
        }
        String relayState = Constants.SP_INITAL + shortRelayState;
        RequestUtil.cacheRequestInfo(shortRelayState, ssoService, cachingRequestInfo); // cache with shorRelayState
        irUtil.handleSerializingInitialRequest(req, resp, Constants.SP_INITAL, shortRelayState, cachingRequestInfo, ssoService);
        TAIResult result = postIdp(req, resp, strAuthnRequest, relayState, idpUrl, cachingRequestInfo); // send out with the long relayState
        return result;
    }

    /**
     * find the single-sign-on service URL of the IdP
     * 
     * @param basicMsgCtx
     * @return
     * @throws SamlException
     */
    String handleIdpMetadataAndLoginUrl(BasicMessageContext<?, ?> basicMsgCtx) throws SamlException {
        String idpUrl = null;
        //MetadataProvider metadataProvider = basicMsgCtx.getMetadataProvider(); //
        AcsDOMMetadataProvider metadataProvider = basicMsgCtx.getMetadataProvider();//v3
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
                                null,
                                new Object[] { providerId });
            } else {
                throw new SamlException("SAML20_NO_IDP_URL_ERROR",
                                //SAML20_NO_IDP_URL_ERROR=CWWKS5079E: Cannot find the IdP URL through the identity provider (IdP) metadata file [{0}] in the service provider (SP) [{1}].
                                null,
                                new Object[] { idpMetadataFile, providerId });
            }

        }

        //XMLObject metadata = null;
        EntityDescriptor metadata2 = null; //v3
        String entityID = metadataProvider.getEntityId(); //TODO: we need a config attribute
        CriteriaSet criteriaSet;
        try {
            criteriaSet = new CriteriaSet(new EntityIdCriterion(entityID));
            metadata2 = metadataProvider.resolveSingle(criteriaSet);
            // make sure that we have EntityDescriptor
            if (metadata2 != null) {
                EntityDescriptor entityDescriptor = metadata2; //v3
                String idpEntityId = entityDescriptor.getEntityID(); // output variable
                //basicMsgCtx.setPeerEntityId(idpEntityId);
                IDPSSODescriptor ssoDescriptor = entityDescriptor.getIDPSSODescriptor(Constants.SAML20P_NS);
                if (ssoDescriptor != null) {
                    List<SingleSignOnService> list = ssoDescriptor.getSingleSignOnServices();
                    for (SingleSignOnService ssoService : list) {
                        if (Constants.SAML2_POST_BINDING_URI.equals(ssoService.getBinding())) {
                            basicMsgCtx.setPeerEntityEndpoint(ssoService); //v3
                            idpUrl = ssoService.getLocation(); // output
                            break;
                        }
                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "idpLogin url:" + idpUrl + "(" + Constants.SAML2_POST_BINDING_URI + ")");
                    }
                } else {
                    SsoConfig ssoConfig = ssoService.getConfig();
                    String idpMetadataFile = ssoConfig.getIdpMetadata();
                    String providerId = ssoService.getProviderId();
                    throw new SamlException("SAML20_IDP_METADATA_PARSE_ERROR",
                                    //SAML20_IDP_METADATA_PARSE_ERROR=CWWKS5023E: The identity provider (IdP) metadata file [{0}] in the service provider (SP) [{1}] is not valid. The cause of the error is [{2}]
                                    null,
                                    new Object[] { idpMetadataFile, providerId, "No IDPSSODescriptor" });
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
                                null,
                                new Object[] { idpMetadaFile, providerId });
            }
        } catch (ResolverException e) {
            throw new SamlException(e); // Let SamlException handles the unexpected Exception
        }
        return idpUrl;
    }

    // "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" \n"
    // + "AssertionConsumerServiceURL=\"" + assertionConsumerServiceURL + "\" \n" + "Destination=\"" + destination + "\" \n"
    // + "ID=\"" + "_" + generateRequestID() + "\" \n" + "IssueInstant=\"" + issueInstant + "\" \n"
    // + "ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" \n" + "Version=\"2.0\" \n" + "> \n"
    // + "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" + acs + "</saml:Issuer> \n"
    // + "<samlp:NameIDPolicy AllowCreate=\"1\" /> \n" + "</samlp:AuthnRequest>"
    /**
     * 
     * @param inResponseToId
     * @param req
     * @param basicMsgCtx
     * @return
     */
    AuthnRequest buildAuthnRequest(String inResponseToId,
                                   HttpServletRequest req,
                                   BasicMessageContext<?, ?> basicMsgCtx) {
        SsoConfig samlConfig = ssoService.getConfig();
        AuthnRequestBuilder arBuilder = new AuthnRequestBuilder();
        AuthnRequest request = arBuilder.buildObject();

        request.setID(inResponseToId);
        String acsEndpointUrl = RequestUtil.getAcsUrl(req,
                                                      Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                                      ssoService.getProviderId(),
                                                      ssoService.getConfig());
        request.setAssertionConsumerServiceURL(acsEndpointUrl);
        request.setProtocolBinding(Constants.SAML2_POST_BINDING_URI);
        request.setVersion(SAMLVersion.VERSION_20);

        request.setForceAuthn(samlConfig.isForceAuthn());
        request.setIsPassive(samlConfig.isPassive());
        request.setNameIDPolicy(buildNameIdPolicy(samlConfig));
        String entityUrl = RequestUtil.getEntityUrl(req,
                                                    Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                                    ssoService.getProviderId(),
                                                    ssoService.getConfig());
        request.setIssuer(getIssuer(entityUrl));
        request.setIssueInstant(new DateTime());
        request.setDestination(basicMsgCtx.getPeerEntityEndpoint().getLocation());
        RequestedAuthnContext requestedAuthnContext = buildRequestedAuthnContext();
        if (requestedAuthnContext != null) {
            request.setRequestedAuthnContext(requestedAuthnContext);
        }

        return request;
    }

    /**
     * @return
     */
    RequestedAuthnContext buildRequestedAuthnContext() {
        RequestedAuthnContext requestedAuthnContext = null;
        SsoConfig ssoConfig = ssoService.getConfig();
        String[] authnContextClassRefs = ssoConfig.getAuthnContextClassRef();
        if (authnContextClassRefs != null && authnContextClassRefs.length > 0) {
            RequestedAuthnContextBuilder requestedAuthnContextBuilder = new RequestedAuthnContextBuilder();
            requestedAuthnContext = requestedAuthnContextBuilder.buildObject();
            addAuthnContextClassRef(requestedAuthnContext, authnContextClassRefs);
            requestedAuthnContext.setComparison(getAuthnContextComparisonTypeEnumeration(ssoConfig.getAuthnContextComparisonType()));
        }
        return requestedAuthnContext;
    }

    /**
     * @param authnContextComparisonType
     * @return
     */
    AuthnContextComparisonTypeEnumeration getAuthnContextComparisonTypeEnumeration(String authnContextComparisonType) {
        if ("exact".equals(authnContextComparisonType)) {
            return AuthnContextComparisonTypeEnumeration.EXACT;
        } else if ("minimum".equals(authnContextComparisonType)) {
            return AuthnContextComparisonTypeEnumeration.MINIMUM;
        } else if ("maximum".equals(authnContextComparisonType)) {
            return AuthnContextComparisonTypeEnumeration.MAXIMUM;
        } else if ("better".equals(authnContextComparisonType)) {
            return AuthnContextComparisonTypeEnumeration.BETTER;
        } else { // default
            return AuthnContextComparisonTypeEnumeration.EXACT;
        }
    }

    /**
     * @param authnContextClassRefs
     * @return
     */
    void addAuthnContextClassRef(RequestedAuthnContext requestedAuthnContext,
                                 String[] authnContextClassRefs) {
        AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
        List<AuthnContextClassRef> listAuthnContextClassRef = requestedAuthnContext.getAuthnContextClassRefs();
        int iCnt = 0;
        for (String strAuthnContextClassRef : authnContextClassRefs) {
            AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject();
            authnContextClassRef.setAuthnContextClassRef(strAuthnContextClassRef);
            listAuthnContextClassRef.add(iCnt++, authnContextClassRef);
        }
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
            nameIDPolicy.setAllowCreate(allowCreate.booleanValue());
        }
        return nameIDPolicy;
    }

    /**
     * Send the authnRequest to IdP
     * 
     * @param req
     * @param resp
     * @param AuthnRequest
     * @param relayState
     * @param idpUrl
     * @param cachingRequestInfo
     * @return
     * @throws WebTrustAssociationFailedException
     */
    TAIResult postIdp(HttpServletRequest req,
                      HttpServletResponse resp,
                      String AuthnRequest,
                      String relayState,
                      String idpUrl,
                      HttpRequestInfo cachingRequestInfo)
                    throws WebTrustAssociationFailedException {

        byte[] authnReqBytes = null;
        try {
            authnReqBytes = AuthnRequest.getBytes(Constants.UTF8);
        } catch (UnsupportedEncodingException e1) {
            // error handling, UTF8 should not have errors
            SamlException samlException = new SamlException(e1); // let the SamlException to handle the Exception
            WebTrustAssociationFailedException wtfae = new WebTrustAssociationFailedException(samlException.getMessage());
            wtfae.initCause(samlException);
            throw wtfae;
        }

        //String samlRequest = Base64.encodeBytes(authnReqBytes, Base64.DONT_BREAK_LINES);
        String samlRequest = Base64Support.encode(authnReqBytes, Base64Support.UNCHUNKED); //v3

        if (relayState == null || samlRequest == null || idpUrl == null) {
            // This should not happen
            throw new WebTrustAssociationFailedException("RelayState, Single-Sign-On URL, and AuthnRequest must be provided");
        }

        try {
            //resp.setStatus(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            resp.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);

            ForwardRequestInfo requestInfo = new ForwardRequestInfo(idpUrl);
            requestInfo.setFragmentCookieId(cachingRequestInfo.getFragmentCookieId());
            requestInfo.setParameter("RelayState", new String[] { relayState });
            requestInfo.setParameter("SAMLRequest", new String[] { samlRequest });
            requestInfo.redirectPostRequest(req,
                                            resp,
                                            null, // In SP_INIT, we do not depend on cookie to store sp_init id
                                            null); // IdP requires to return RelayStae by spec
        } catch (SamlException e) {
            WebTrustAssociationFailedException wtafe = new WebTrustAssociationFailedException(e.getMessage());
            wtafe.initCause(e);
            throw wtafe;
        }
        // expect to return a form to redirect to the idp by the browser
        //return TAIResult.create(HttpServletResponse.SC_FORBIDDEN); //
        // change to 401 because admincenter intercepts 403
        return TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED); 
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
    void signAuthnRequest(SAMLObject authnRequest, Credential signingCredential) throws SamlException {
        SsoConfig samlConfig = ssoService.getConfig();
        //QName qName = Signature.DEFAULT_ELEMENT_NAME;       
        if (authnRequest instanceof SignableSAMLObject && signingCredential != null) {
            SignableSAMLObject signableMessage = (SignableSAMLObject) authnRequest;
            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();//v3
            XMLObjectBuilder<Signature> signatureBuilder = (XMLObjectBuilder<Signature>)builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME);
            Signature signature = signatureBuilder.buildObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSignatureAlgorithm(samlConfig.getSignatureMethodAlgorithm());//SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);// SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1); //               
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            signature.setSigningCredential(signingCredential);
            signableMessage.setSignature(signature);

            final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader(); 
            try {
                //Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(signableMessage);
                Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(signableMessage);//v3
                if (marshaller == null) {
                    throw new SamlException("SAML20_AUTHENTICATION_FAIL",
                                    //"CWWKS5063E: The Web Service Request failed due to the authentication is not successful.",
                                    null,
                                    new Object[] {});
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

    String getAuthnRequestString(AuthnRequest authnRequest) throws SamlException {
        String result = null;
        if (authnRequest != null) {
            try {
                AuthnRequestMarshaller marshaller = new AuthnRequestMarshaller();
                Element element = marshaller.marshall(authnRequest);
                //result = XMLHelper.nodeToString(element);
                result = SerializeSupport.nodeToString(element); //v3
            } catch (MarshallingException e) {
                throw new SamlException(e, true); // Let SamlException handles opensaml Exception
            }
        }
        return result;
    }

}
