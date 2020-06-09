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

package com.ibm.ws.security.saml.sso20.binding;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.encryption.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.util.DatatypeHelper;

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

// MessageContext

/**
 * Base implemention of {@link SAMLMessageContext}.
 *
 * @param <InboundMessageType>  type of inbound SAML message
 * @param <OutboundMessageType> type of outbound SAML message
 * @param <NameIdentifierType>  type of name identifier used for subjects
 */
public class BasicMessageContext<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject> extends BasicSAMLMessageContext<InboundMessageType, OutboundMessageType, NameIdentifierType> {
    // For debugging we may want to put back the DebugMessageContext
    //  Because the DebugMessageContext print more messages in the product code

    //      We want to keep DebugMessageContext for debugging in future
    //        In that case, change the opensaml BasicSAMLMessageContext to DebugMessageContext

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
    
    HttpServletRequest request;
    HttpServletResponse response;
    
    InitialRequestUtil irUtil= new InitialRequestUtil();

    static ChainingEncryptedKeyResolver encryptedKeyResolver = new ChainingEncryptedKeyResolver();
    static {
        encryptedKeyResolver.getResolverChain().add(new InlineEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new EncryptedElementTypeEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new SimpleRetrievalMethodEncryptedKeyResolver());
    };

    /** Resolver used to determine active security policy. get the trust store from idp Metadata */
    private SecurityPolicyResolver idpSecurityPolicyResolver;

    public BasicMessageContext(SsoSamlService ssoService) {
        this.ssoService = ssoService;
        this.ssoConfig = ssoService.getConfig();
    }
    
    public BasicMessageContext(SsoSamlService ssoService, HttpServletRequest request, HttpServletResponse response) {
        this.ssoService = ssoService;
        this.ssoConfig = ssoService.getConfig();
        this.request = request;
        this.response = response;
    }

    /**
     * @param ssoService
     */
    public SsoSamlService getSsoService() {
        return ssoService;
    }

    public HttpServletRequest getHttpServletRequest() {
        HttpServletRequestAdapter requestAdapter = (HttpServletRequestAdapter) getInboundMessageTransport();
        return requestAdapter.getWrappedRequest();
    }

    public Status getSLOResponseStatus() {
        return logoutResponseStatus;
    }

    public void setSLOResponseStatus(Status status) {
        logoutResponseStatus = status;
    }

    /** {@inheritDoc} */
    @Override
    public void setPeerEntityRole(QName role) {
        peerEntityRole = role;
    }

    /** {@inheritDoc} */
    @Override
    public QName getPeerEntityRole() {
        if (peerEntityRole == null) {

            if (!bSetIDPSSODescriptor) {
                setIDPSSODescriptor();
            }
            if (idpSsoDescriptor != null) {
                peerEntityRole = idpSsoDescriptor.getElementQName();
            }
        }

        if (peerEntityRole == null) {
            // default value
            peerEntityRole = IDPSSODescriptor.DEFAULT_ELEMENT_NAME;
        }
        return peerEntityRole;
    }

    /** {@inheritDoc} */
    @Override
    public EntityDescriptor getPeerEntityMetadata() {
        if (!bSetIDPSSODescriptor) {
            setIDPSSODescriptor();
        }
        return peerEntityMetadata;
    }

    /**
     *
     */
    void setIDPSSODescriptor() {
        bSetIDPSSODescriptor = true;
        SAMLObject samlMsg = getInboundSAMLMessage();
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

            MetadataProvider metadataProvider = this.getMetadataProvider();
            if (metadataProvider != null) {
                try {
                    EntityDescriptor entityDescriptor = metadataProvider.getEntityDescriptor(issuer);
                    if (entityDescriptor == null) {
                        // can not find a valid idpMetadata
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Can not find a valid IDP Metadata for issuer:"
                                         + issuer);
                        }
                        // This could happen. And if no idpMetadata found, later on,
                        // the Saml Token signature can not be verified
                        // since no trusted certificate...
                        // Unless trustEngine is specified (such as: pkixTrustEngine)
                    } else {
                        peerEntityMetadata = entityDescriptor;
                        idpSsoDescriptor = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
                    }
                } catch (MetadataProviderException e) {
                    // do nothing and let the IDPSsoDescriptor == null
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "setIDPSSODescriptor hit  MetadataProviderException", e);
                    }
                }
            } else {
                // no Metadata Provider.
                // Do nothing and let the IDPSSODescriptor == null
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No IdP metadata exists. Need to fall down to local trust store.");
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundSAMLProtocol(String protocol) {
        inboundSAMLProtocol = DatatypeHelper.safeTrimOrNullString(protocol);
    }

    /** {@inheritDoc} */
    @Override
    public String getInboundSAMLProtocol() {
        if (inboundSAMLProtocol == null) {
            SAMLObject samlMsg = getInboundSAMLMessage();
            if (samlMsg != null && samlMsg instanceof Response) {
                Response samlResponse = (Response) samlMsg;
                inboundSAMLProtocol = samlResponse.getElementQName().getNamespaceURI();
            }
        }

        return inboundSAMLProtocol;
    }

    public SecurityPolicyResolver getIdpSecurityPolicyResolver() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getSecurityPolicyResolver(mc):"
                         + idpSecurityPolicyResolver);
        }
        return idpSecurityPolicyResolver;
    }

    public void setIdpSecurityPolicyResolver(SecurityPolicyResolver resolver) {
        idpSecurityPolicyResolver = resolver;
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
                } catch(SamlException e) {
                    Tr.debug(tc,  "cannot recreate HttpRequestInfo using InitialRequest cookie", e.getMessage());
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

}