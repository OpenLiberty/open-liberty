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



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.Saml20HTTPPostDecoder;
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
        //basicMessageContext.setInboundMessageTransport(new HttpServletRequestAdapter(req)); //v2
        setIdpMetadaProvider(basicMessageContext);

        // 1) Parsing the samlResponse
        decodeSamlResponse(basicMessageContext, req);

        return basicMessageContext;
    }

    public BasicMessageContext<InboundMessageType, OutboundMessageType> buildSLO(HttpServletRequest req,
                                                                                                     HttpServletResponse res,
                                                                                                     SsoSamlService ssoService,
                                                                                                     String externalRelayState,
                                                                                                     SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext = getBasicMessageContext(ssoService, req, res);
        basicMessageContext.setAndRemoveCachedRequestInfo(externalRelayState, samlRequest);
        //basicMessageContext.setInboundMessageTransport(new HttpServletRequestAdapter(req)); //v2
        setIdpMetadaProvider(basicMessageContext);

        decodeSamlLogoutMessage(basicMessageContext, req);
        return basicMessageContext;
    }

    public BasicMessageContext<InboundMessageType, OutboundMessageType> buildRsSaml(HttpServletRequest req,
                                                                                                        HttpServletResponse res,
                                                                                                        SsoSamlService ssoService,
                                                                                                        String headerCpontent,
                                                                                                        SsoRequest samlRequest) throws SamlException {
        BasicMessageContext<InboundMessageType, OutboundMessageType> basicMessageContext = getBasicMessageContext(ssoService, req, res); //v3
        //basicMessageContext.setInboundMessageTransport(new HttpServletRequestAdapter(req));
        //decodeSamlResponse(basicMessageContext, req);
        basicMessageContext.setMessageContext(new MessageContext<SAMLObject>()); //v3
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
            //samlMessageDecoder.decode(basicMessageContext); //v2
            samlMessageDecoder.decode();
            basicMessageContext.setMessageContext(samlMessageDecoder.getMessageContext());//v3?
        } catch (Exception e) {
            // We may want to return false, so it can be handled by the specified TrustStore
            //throw decodeError(e, basicMessageContext); //v2
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
            HTTPPostDecoder samlMessageDecoder = getSamlHttpPostDecoder(sloUrl, req); //v3
            //samlMessageDecoder.decode(basicMessageContext); //v2
            samlMessageDecoder.decode();
            basicMessageContext.setMessageContext(samlMessageDecoder.getMessageContext());
        } catch (Exception e) {
            // We may want to return false, so it can be handled by the specified TrustStore
            //throw decodeError(e, basicMessageContext); //v2
            throw decodeError(e);
        }
        return true;
    }

    HTTPPostDecoder getSamlHttpPostDecoder(String acsUrl, HttpServletRequest req) throws ComponentInitializationException {
        HTTPPostDecoder decoder = new Saml20HTTPPostDecoder(acsUrl);
        decoder.setHttpServletRequest(req);
        decoder.setParserPool(XMLObjectProviderRegistrySupport.getParserPool());
        decoder.initialize();
        return decoder;
    }

    BasicMessageContext<InboundMessageType, OutboundMessageType> setIdpMetadaProvider(BasicMessageContext<InboundMessageType, OutboundMessageType> messageContext) throws SamlException {
        SsoConfig samlConfig = messageContext.getSsoConfig();
        AcsDOMMetadataProvider acsIdpMetadataProvider = samlConfig.getIdpMetadataProvider();
        messageContext.setMetadataProvider(acsIdpMetadataProvider);
        return messageContext;
    }

    public static SamlException decodeError(Exception e) {
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
