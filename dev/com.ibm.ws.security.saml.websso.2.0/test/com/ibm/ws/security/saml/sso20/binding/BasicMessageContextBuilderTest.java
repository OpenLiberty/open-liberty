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
import javax.xml.namespace.QName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.provider.MapBasedConfiguration;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;

import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;


import org.opensaml.core.xml.io.UnmarshallingException;

import org.opensaml.messaging.context.MessageContext;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import test.common.SharedOutputManager;

@SuppressWarnings("rawtypes")
public class BasicMessageContextBuilderTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static BasicMessageContextBuilder instance;
    private static QName qnLogoutRequest;

    private final static String QN_LOCALNAME = "LogoutRequest";
    private final static String QN_NAME = "samlp:LogoutRequest";
    private final static String QN_NS_URI = "urn:oasis:names:tc:SAML:2.0:protocol";
    private final String RELAY_STATE = "RPID%3Dhttps%253A%252F%252Frelyingpartyapp%26wctx%3Dappid%253D45%2526foo%253Dbar";

    public interface MockInterface {
        BasicMessageContext<?, ?> getBasicMessageContext();

        HTTPPostDecoder getSamlHttpPostDecoder();
    }

    private static final MockInterface mockInterface = mockery.mock(MockInterface.class);

    private static final BasicMessageContext basicMessageContext = mockery.mock(BasicMessageContext.class, "basicMessageContextCB");
    private static final MessageContext messageContext = mockery.mock(MessageContext.class);
    private static final HttpServletRequest httpServletRequest = mockery.mock(HttpServletRequest.class);
    private static final HttpServletResponse httpServletResponse = mockery.mock(HttpServletResponse.class);
    private static final SsoSamlService ssoService = mockery.mock(SsoSamlService.class);
    private static final SsoRequest ssoRequest = mockery.mock(SsoRequest.class);
    private static final SsoConfig ssoConfig = mockery.mock(SsoConfig.class);
    private static final AcsDOMMetadataProvider acsDOM = mockery.mock(AcsDOMMetadataProvider.class);
    private static final HTTPPostDecoder httpPostDecoder = mockery.mock(HTTPPostDecoder.class);

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        instance = new BasicMessageContextBuilder();
        qnLogoutRequest = new QName(QN_NS_URI, QN_LOCALNAME, QN_NAME);
        
        Configuration configuration = new MapBasedConfiguration();
        ConfigurationService.setConfiguration(configuration);

        XMLObjectProviderRegistry providerRegistry = new XMLObjectProviderRegistry();
        configuration.register(XMLObjectProviderRegistry.class, providerRegistry,
                               ConfigurationService.DEFAULT_PARTITION_NAME);

    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @After
    public void isSatisfied() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void BuildAcsTest() throws SamlException, UnmarshallingException, MessageDecodingException, SecurityException {
        instance = new BasicMessageContextBuilder() {
            @Override
            BasicMessageContext<?, ?> getBasicMessageContext(SsoSamlService ssoService) {
                return mockInterface.getBasicMessageContext();
            }
            
            @Override
            BasicMessageContext<?, ?> getBasicMessageContext(SsoSamlService ssoService, HttpServletRequest req, HttpServletResponse res) {
                return mockInterface.getBasicMessageContext();
            }

            @Override
            HTTPPostDecoder getSamlHttpPostDecoder(String acsUrl, HttpServletRequest req) {
                return mockInterface.getSamlHttpPostDecoder();
            }
        };

        mockery.checking(new Expectations() {
            {
                one(mockInterface).getBasicMessageContext();
                will(returnValue(basicMessageContext));

                one(basicMessageContext).setAndRemoveCachedRequestInfo(RELAY_STATE, ssoRequest);
                //one(basicMessageContext).setInboundMessageTransport(with(any(HttpServletRequestAdapter.class)));

                allowing(basicMessageContext).getSsoConfig();
                will(returnValue(ssoConfig));

                one(ssoConfig).getIdpMetadataProvider();
                will(returnValue(acsDOM));

                one(basicMessageContext).setMetadataProvider(acsDOM);

                one(basicMessageContext).getHttpServletRequest();
                will(returnValue(httpServletRequest));

                one(basicMessageContext).getSsoService();
                will(returnValue(ssoService));

                one(ssoService).getProviderId();
                will(returnValue("sp1"));
               
                one(ssoConfig).getSpHostAndPort();
                will(returnValue("http://www.ibm.com"));

                one(mockInterface).getSamlHttpPostDecoder();
                will(returnValue(httpPostDecoder));

                one(httpPostDecoder).decode();
                
                allowing(httpPostDecoder).getMessageContext();
                will(returnValue(messageContext));
                
                allowing(basicMessageContext).setMessageContext(with(any(MessageContext.class)));

            }
        });

        instance.buildAcs(httpServletRequest, httpServletResponse, ssoService, RELAY_STATE, ssoRequest);
    }

}
