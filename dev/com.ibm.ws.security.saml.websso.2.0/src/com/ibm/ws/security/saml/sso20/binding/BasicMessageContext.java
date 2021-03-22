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

package com.ibm.ws.security.saml.sso20.binding;


import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.EncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

// MessageContext

/**
 * 
 *
 * @param <InboundMessageType> type of inbound SAML message
 * @param <OutboundMessageType> type of outbound SAML message
 * 
 */

@SuppressWarnings("rawtypes")
public class BasicMessageContext<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject>/* extends SAMLSOAPClientContextBuilder */{

    public static final TraceComponent tc = Tr.register(BasicMessageContext.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    SsoConfig ssoConfig;
    SsoSamlService ssoService;
    IDPSSODescriptor idpSsoDescriptor;
    QName peerEntityRole;
    EntityDescriptor peerEntityMetadata;
    String inboundSAMLProtocol;
    Assertion validatedAssertion;
    Decrypter decrypter;
    String externalRelayState;
    SsoRequest samlRequest;
    HttpRequestInfo cachedRequestInfo;
    boolean bSetIDPSSODescriptor = false;

    
    Status logoutResponseStatus;
    String inResponseTo;

    AcsDOMMetadataProvider metadataProvider = null;
    
    HttpServletRequest request;
    HttpServletResponse response;

    InitialRequestUtil irUtil = new InitialRequestUtil();
    ChainingEncryptedKeyResolver encryptedKeyResolver;
    private List<EncryptedKeyResolver> resolverChain;
    EncryptedKeyResolver inline = new InlineEncryptedKeyResolver();
    EncryptedKeyResolver encryptedelem = new EncryptedElementTypeEncryptedKeyResolver();
    EncryptedKeyResolver simple = new SimpleRetrievalMethodEncryptedKeyResolver();
    
    SAMLPeerEntityContext samlPeerEntityContext = new SAMLPeerEntityContext();

    private MessageContext<SAMLObject> messageContext;
    private Endpoint peerEntityEndpoint;
    private String inboundMessageIssuer;

    private NameID subjectNameIdentifer;

    public BasicMessageContext(SsoSamlService ssoService) {
        this.ssoService = ssoService;
        this.ssoConfig = ssoService.getConfig();
        resolverChain = Arrays.asList(inline, encryptedelem, simple);
        encryptedKeyResolver = new ChainingEncryptedKeyResolver(resolverChain);
    }

    public BasicMessageContext(SsoSamlService ssoService, HttpServletRequest request, HttpServletResponse response) {
        this.ssoService = ssoService;
        this.ssoConfig = ssoService.getConfig();
        this.request = request;
        this.response = response;
        resolverChain = Arrays.asList(inline, encryptedelem, simple);
        encryptedKeyResolver = new ChainingEncryptedKeyResolver(resolverChain);
    }

    /**
     * @param ssoService
     */
    public SsoSamlService getSsoService() {
        return ssoService;
    }

    public HttpServletRequest getHttpServletRequest() {
        return this.request;
    }
    
    public void setMetadataProvider(AcsDOMMetadataProvider acsIdpMetadataProvider) {
       this.metadataProvider = acsIdpMetadataProvider;
    }
    
    public AcsDOMMetadataProvider getMetadataProvider() {
        return this.metadataProvider;
    }

    public Status getSLOResponseStatus() {
        return logoutResponseStatus;
    }

    public void setSLOResponseStatus(Status status) {
        logoutResponseStatus = status;
    }

    public EntityDescriptor getPeerEntityMetadata() {
        if (!bSetIDPSSODescriptor) {
            setIDPSSODescriptor();
        }
        return peerEntityMetadata;
    }

    void setIDPSSODescriptor() {
        bSetIDPSSODescriptor = true;
        
        SAMLObject samlMsg = null;
        if (getMessageContext() != null) {
            samlMsg = getMessageContext().getMessage();
        }
        if (samlMsg != null && (samlMsg instanceof Response || samlMsg instanceof LogoutResponse ||
                                samlMsg instanceof LogoutRequest)) {
            String issuer = null;
            if (samlMsg instanceof Response) {
                Response samlResponse = (Response) samlMsg;
                issuer = samlResponse.getIssuer().getValue();
            } else if (samlMsg instanceof LogoutResponse) {
                LogoutResponse sloResponse = (LogoutResponse) samlMsg;
                issuer = sloResponse.getIssuer().getValue();
            } else if (samlMsg instanceof LogoutRequest) {
                LogoutRequest sloRequest = (LogoutRequest) samlMsg;
                issuer = sloRequest.getIssuer().getValue();
            }
            if (metadataProvider != null) {
//                try {
                    CriteriaSet criteriaSet = new CriteriaSet(new EntityIdCriterion(issuer));
                    EntityDescriptor entityDescriptor = null;
                    try {
                        entityDescriptor = metadataProvider.resolveSingle(criteriaSet);
                    } catch (ResolverException e) {
                      // do nothing and let the IDPSsoDescriptor == null
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                          Tr.debug(tc, "ResolverException in setIDPSSODescriptor : ", e);
                      }
                    }
                    if (entityDescriptor == null) {
                        // cannot find a valid idpMetadata
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Can not find a valid IDP Metadata for issuer:"
                                         + issuer);
                        }
                        // This could happen. And if no idpMetadata found, later on,
                        // the Saml Token signature cannot be verified
                        // since no trusted certificate...
                        // Unless trustEngine is specified (using pkixTrustEngine)
                    } else {
                        peerEntityMetadata = entityDescriptor;
                        idpSsoDescriptor = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
                    }
//                } catch (MetadataProviderException e) { // TODO: handle ResolverException?
//                    // do nothing and let the IDPSsoDescriptor == null
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                        Tr.debug(tc, "setIDPSSODescriptor hit  MetadataProviderException", e);
//                    }
//                }
            } else {
                // no Metadata Provider.
                // Do nothing and let the IDPSSODescriptor == null
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IdP metadata does not exist, fall back to local trust store.");
                }
            }
        }
    }

    public Assertion getValidatedAssertion() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getValidatedAssertion(mc):"
                         + validatedAssertion);
        }
        return validatedAssertion;
    }

    public void setValidatedAssertion(Assertion validatedAssertion) {
        this.validatedAssertion = validatedAssertion;
    }

    public UserData getUserDataIfReady() throws SamlException {
        if (validatedAssertion != null) {
            return new UserData(validatedAssertion, ssoService.getProviderId());
        }
        return null;
    }

    // Construct an decrypter according the SsoConfig
    public void setDecrypter() throws SamlException {
        if (decrypter == null) {
            Credential decryptingCredential = RequestUtil.getDecryptingCredential(ssoService);
            KeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(decryptingCredential);
            decrypter = new Decrypter(null, // symmetric
                            resolver, // asymmetric
                            encryptedKeyResolver);
            decrypter.setRootInNewDocument(true);
        }
    }

    // Construct an decrypter according the SsoConfig
    public Decrypter getDecrypter() throws SamlException {
        if (decrypter == null) {
            setDecrypter();
        }
        return decrypter; // This could be null
    }

    /**
     * @param externalRelayState
     * @param samlRequest
     * @throws SamlException
     */
    public void setAndRemoveCachedRequestInfo(String externalRelayState,
                                              SsoRequest samlRequest) throws SamlException {
        this.externalRelayState = externalRelayState;
        this.samlRequest = samlRequest;
        if (externalRelayState != null) { // has to be SP_INITI
            Cache cache = ssoService.getAcsCookieCache(samlRequest.getProviderName());
            String cacheKey = externalRelayState.substring(Constants.SP_INITAL.length());
            cachedRequestInfo = (HttpRequestInfo) cache.get(cacheKey);
            if (cachedRequestInfo == null) {
                // CWWKS5029W: Cannot find the cache data for the SAML request with the relay state [0].
                //             The same request may have been sent more than once. It is a potential hack attack.
                //Tr.error(tc, "SAML20_POTENTIAL_REPLAY_ATTACK", new Object[] { externalRelayState });
                try {
                    cachedRequestInfo = irUtil.recreateHttpRequestInfo(externalRelayState, this.request, this.response, this.ssoService);
                } catch (SamlException e) {
                    Tr.debug(tc, "cannot recreate HttpRequestInfo using InitialRequest cookie", e.getMessage());
                    throw e;
                }
                if (cachedRequestInfo == null) {
                    throw new SamlException("SAML20_POTENTIAL_REPLAY_ATTACK",
                                    //"CWWKS5030E: Cannot handle the SAML request. Make sure the communication is working properly and try the requesting procedure again.",
                                    null, // cause
                                    new Object[] { externalRelayState });
                }

            } else {
                cache.remove(cacheKey); // the cache can only be used once
                irUtil.removeCookie(externalRelayState, request, response);
            }
        }
    }

    public void setCachedRequestInfo(HttpRequestInfo requestInfo) {
        this.cachedRequestInfo = requestInfo;
    }

    /**
     * @return
     */
    public HttpRequestInfo getCachedRequestInfo() {
        return cachedRequestInfo;
    }

    /**
     * @return
     */
    public String getExternalRelayState() {
        return externalRelayState;
    }

    /**
     * @return
     */
    public SsoConfig getSsoConfig() {
        return ssoConfig;
    }

    /**
     * @param id
     */
    public void setInResponseTo(String id) {
        inResponseTo = id;
    }

    public String getInResponseTo() {
        return this.inResponseTo;
    }

    /**
     * @param messageContext
     */
    public void setMessageContext(MessageContext<SAMLObject> messageContext) {
        this.messageContext = messageContext;        
    }

    public MessageContext<SAMLObject> getMessageContext() {
        return this.messageContext;        
    }

    /**
     * @param nameID
     */
    public void setSubjectNameIdentifier(NameID nameID) {
        this.subjectNameIdentifer = nameID;
        
    }

    /**
     * @param entityEndpoint
     */
    public void setPeerEntityEndpoint(Endpoint entityEndpoint) {
        this.peerEntityEndpoint = (Endpoint) entityEndpoint;
        
    }

    /**
     * @return
     */
    public Endpoint getPeerEntityEndpoint() {
       
        return this.peerEntityEndpoint;
    }

    /**
     * @param issuer
     */
    public void setInboundSamlMessageIssuer(String issuer) {
        this.inboundMessageIssuer = issuer;
    }
    
    public String getInboundSamlMessageIssuer() {
        return this.inboundMessageIssuer;
    }

}