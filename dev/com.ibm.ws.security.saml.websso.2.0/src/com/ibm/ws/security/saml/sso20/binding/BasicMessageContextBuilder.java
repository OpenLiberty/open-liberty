/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.binding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
//import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;
//import org.opensaml.saml2.metadata.provider.MetadataProvider;
//import org.opensaml.security.MetadataCredentialResolverFactory;
//import org.opensaml.ws.security.SecurityPolicy;
//import org.opensaml.ws.security.SecurityPolicyResolver;
//import org.opensaml.ws.security.provider.StaticSecurityPolicyResolver;
//import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.keyinfo.impl.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.KeyInfoProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.Saml20HTTPPostDecoder;
//import com.ibm.ws.security.saml.sso20.acs.AcsSecurityPolicy;
import com.ibm.ws.security.saml.sso20.acs.SAMLMessageXMLSignatureSecurityPolicyRule;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 *
 */
public class BasicMessageContextBuilder<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject> {
    private static TraceComponent tc = Tr.register(BasicMessageContextBuilder.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    @SuppressWarnings("rawtypes")
    static BasicMessageContextBuilder<?, ?, ?> instance = new BasicMessageContextBuilder();

    public static void setInstance(BasicMessageContextBuilder<?, ?, ?> instance) {
        BasicMessageContextBuilder.instance = instance;
    }

    public static BasicMessageContextBuilder<?, ?, ?> getInstance() {
        return instance;
    }

    BasicMessageContext<InboundMessageType, OutboundMessageType> getBasicMessageContext(SsoSamlService ssoService) {
        return new BasicMessageContext<InboundMessageType, OutboundMessageType>(ssoService);
    }
    
    BasicMessageContext<InboundMessageType, OutboundMessageType> getBasicMessageContext(SsoSamlService ssoService, HttpServletRequest request, HttpServletResponse response) {
        return new BasicMessageContext<InboundMessageType, OutboundMessageType>(ssoService, request, response);
    }

    public BasicMessageContext<InboundMessageType, OutboundMessageType> buildAcs(HttpServletRequest req,
                                                                                                     HttpServletResponse res,
                                                                                                     SsoSamlService ssoService,
                                                                                                     String externalRelayState,
                                                                                                     SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext = getBasicMessageContext(ssoService, req, res);
            
        basicMessageContext.setAndRemoveCachedRequestInfo(externalRelayState, samlRequest); 
        //basicMessageContext.setInboundMessageTransport(new HttpServletRequestAdapter(req)); //@AV999
        setIdpMetadaProvider(basicMessageContext);
        //setSecurityPolicyResolver(basicMessageContext);

        // 1) Parsing the samlResponse
        decodeSamlResponse(basicMessageContext, req); //@AV999

        //Comment out following codes, they are done later
        /*
         * // 2) Decrypt the encryptedAssertion
         * // TODO add code to decrypt
         *
         * // 3) Check signature with IdpSecurityPolicyResolver
         * if (!verifySignatureWithIdpMetadata(basicMessageContext)) {
         * // 4) check signature with specified trustStore
         * // TODO add code to handle local trust store
         * // a) The specified Trust Store may be able to be merged into the SecurityPolicyResolver
         * // b) The signature may have be parsed and verified.
         * // The only thing is not done is: TrustEngine
         * // We may be able to the TrustEngine in AcsSecurityPolicy
         * }
         */
        return basicMessageContext;
    }

    public BasicMessageContext<InboundMessageType, OutboundMessageType> buildSLO(HttpServletRequest req,
                                                                                                     HttpServletResponse res,
                                                                                                     SsoSamlService ssoService,
                                                                                                     String externalRelayState,
                                                                                                     SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext = getBasicMessageContext(ssoService, req, res);
        basicMessageContext.setAndRemoveCachedRequestInfo(externalRelayState, samlRequest);
        //basicMessageContext.setInboundMessageTransport(new HttpServletRequestAdapter(req)); //@AV999 TODO:
        setIdpMetadaProvider(basicMessageContext);

        decodeSamlLogoutMessage(basicMessageContext, req); //@AV999
        return basicMessageContext;
    }

    public BasicMessageContext<InboundMessageType, OutboundMessageType> buildRsSaml(HttpServletRequest req,
                                                                                                        HttpServletResponse res,
                                                                                                        SsoSamlService ssoService,
                                                                                                        String headerCpontent,
                                                                                                        SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext = getBasicMessageContext(ssoService, req, res); //@AV999
        //basicMessageContext.setInboundMessageTransport(new HttpServletRequestAdapter(req));
        //decodeSamlResponse(basicMessageContext, req);
        basicMessageContext.setMessageContext(new MessageContext<SAMLObject>()); //@AV999 major change
        return basicMessageContext;
    }

    public BasicMessageContext<InboundMessageType, OutboundMessageType> buildIdp(HttpServletRequest req,
                                                                                                     HttpServletResponse res,
                                                                                                     SsoSamlService ssoService) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext = getBasicMessageContext(ssoService);
        //basicMessageContext.setInboundMessageTransport(new HttpServletRequestAdapter(req));
        setIdpMetadaProvider(basicMessageContext);

        return basicMessageContext;
    }

    public boolean decodeSamlResponse(BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext, HttpServletRequest req) throws SamlException {
        String acsUrl = RequestUtil.getAcsUrl(basicMessageContext.getHttpServletRequest(),
                                              Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                              basicMessageContext.getSsoService().getProviderId(),
                                              basicMessageContext.getSsoConfig());
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "acsUrl:", acsUrl);
        }
        
        
        try {
            HTTPPostDecoder samlMessageDecoder = getSamlHttpPostDecoder(acsUrl, req);
            //samlMessageDecoder.decode(basicMessageContext); //@AV999 important change
            samlMessageDecoder.decode();
            basicMessageContext.setMessageContext(samlMessageDecoder.getMessageContext());
        } catch (Exception e) {
            // includes these 2 declared exception
            // Exception MessageDecodingException mde
            // But the  org.opensaml.xml.security.SecurityException se should not happen, since we only decode and parse the samlResponse here
            //
            // That pretty much nothing we can do/say in this kind of failure
            // We may want to return false, so it can be handled by the specified TrustStore
            //throw decodeError(e, basicMessageContext); //@AV999
            throw decodeError(e);
        }
        return true;
    }

    public boolean decodeSamlLogoutMessage(BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext, HttpServletRequest req) throws SamlException {
        String sloUrl = RequestUtil.getSloUrl(basicMessageContext.getHttpServletRequest(),
                                              Constants.SAML20_CONTEXT_PATH, // "/ibm/saml20/"
                                              basicMessageContext.getSsoService().getProviderId(),
                                              basicMessageContext.getSsoConfig());
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SLO Url:", sloUrl);
        }
        
        try {
            HTTPPostDecoder samlMessageDecoder = getSamlHttpPostDecoder(sloUrl, req); //@AV999
            //samlMessageDecoder.decode(basicMessageContext); //@AV999 important change
            samlMessageDecoder.decode();
            basicMessageContext.setMessageContext(samlMessageDecoder.getMessageContext());
        } catch (Exception e) {
            // includes these 2 declared exception
            // Exception MessageDecodingException mde
            // But the  org.opensaml.xml.security.SecurityException se should not happen, since we only decode and parse the samlResponse here
            //
            // That pretty much nothing we can do/say in this kind of failure
            // We may want to return false, so it can be handled by the specified TrustStore
            //throw decodeError(e, basicMessageContext); //@AV999
            throw decodeError(e);
        }
        return true;
    }

    HTTPPostDecoder getSamlHttpPostDecoder(String acsUrl, HttpServletRequest req) throws ComponentInitializationException {
        HTTPPostDecoder decoder = new Saml20HTTPPostDecoder(acsUrl);
        decoder.setHttpServletRequest(req);
        decoder.setParserPool(XMLObjectProviderRegistrySupport.getParserPool());
        decoder.initialize();
        //TODO @AV999 initialize()?
        return decoder;
    }

//    boolean verifySignatureWithIdpMetadata(BasicMessageContext<InboundMessageType, OutboundMessageType, NameIdentifierType> basicMessageContext) throws SamlException {
//        try {
//            SecurityPolicyResolver idpPolicyResolver = basicMessageContext.getIdpSecurityPolicyResolver();
//            if (idpPolicyResolver != null) {
//                Iterable<SecurityPolicy> securityPolicies = idpPolicyResolver.resolve(basicMessageContext);
//                if (securityPolicies != null) {
//                    for (SecurityPolicy policy : securityPolicies) {
//                        if (policy != null) {
//                            if (tc.isDebugEnabled()) {
//                                Tr.debug(tc, "Evaluating security policy of type '{}' for decoded message", policy.getClass().getName());
//                            }
//                            policy.evaluate(basicMessageContext);
//                        }
//                    }
//                } else {
//                    if (tc.isDebugEnabled()) {
//                        Tr.debug(tc, "No security policy resolved for this message context, no security policy evaluation attempted");
//                    }
//                }
//            }
//        } catch (SecurityException e) {
//            // Let the SamlException handle the opensaml exception
//            throw new SamlException(e);
//        }
//
//        return true;
//    }

//    /**
//     * @param messageContext
//     */
//    void setSecurityPolicyResolver(BasicMessageContext<InboundMessageType, OutboundMessageType, NameIdentifierType> messageContext) {
//        AcsSecurityPolicy acsSecurityPolicy = new AcsSecurityPolicy();
//        //acsSecurityPolicy.add(new SAML2AuthnRequestsSignedRule()); // AuthRequest
//        MetadataProvider metadataProvider = messageContext.getMetadataProvider();
//        MetadataCredentialResolverFactory factory = MetadataCredentialResolverFactory.getFactory();
//        InlineX509DataProvider x509DataProvider = new InlineX509DataProvider();
//        List<KeyInfoProvider> providers = new ArrayList<KeyInfoProvider>();
//        providers.add(x509DataProvider);
//        BasicProviderKeyInfoCredentialResolver keyInfoCredResolver = new BasicProviderKeyInfoCredentialResolver(providers);
//        TrustEngine<Signature> engine = new ExplicitKeySignatureTrustEngine(factory.getInstance(metadataProvider), keyInfoCredResolver);
//        acsSecurityPolicy.add(new SAMLMessageXMLSignatureSecurityPolicyRule(engine));
//        messageContext.setIdpSecurityPolicyResolver(new StaticSecurityPolicyResolver(acsSecurityPolicy));
//    }

    BasicMessageContext<InboundMessageType, OutboundMessageType> setIdpMetadaProvider(BasicMessageContext<InboundMessageType, OutboundMessageType> messageContext) throws SamlException {
        SsoConfig samlConfig = messageContext.getSsoConfig();
        AcsDOMMetadataProvider acsIdpMetadataProvider = samlConfig.getIdpMetadataProvider();
        messageContext.setMetadataProvider(acsIdpMetadataProvider);
        return messageContext;
    }

    //SAML20_DECODE_SAML_RESPONSE_FAILURE=CWWKS5017E: The request failed. The SAML Token issued by the SAML Web SSO Version 2.0 provider can not be decoded or parsed. It is very likely a communication glitch happened.
    //SAML20_DECODE_SAML_RESPONSE_FAILURE_LOG=CWWKS5018E: The SAML Token can not be decoded or parsed. Error message is [{1}]. Exception name is [{2}].
    public static SamlException decodeError(Exception e/*,
                                            SAMLMessageContext<?, ?, ?> msgContext @AV999 */) {
        String msg = e.getMessage();
        String className = e.getClass().getName();
        SamlException result = new SamlException("SAML20_DECODE_SAML_RESPONSE_FAILURE_LOG",
                        //"CWWKS5018E: The SAML Token cannot be decoded or parsed. The Error message is [" + msg +
                        //                "]. The exception name is [" + className +
                        //                "].",
                        e, // cause
                        new Object[] { msg, className });
        return result;
    }
}
