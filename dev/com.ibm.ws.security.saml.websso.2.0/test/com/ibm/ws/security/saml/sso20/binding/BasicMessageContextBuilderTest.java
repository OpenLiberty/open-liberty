/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.binding;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

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
import com.ibm.ws.security.saml.sso20.common.CommonMockitoObjects;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import test.common.SharedOutputManager;

@SuppressWarnings("rawtypes")
public class BasicMessageContextBuilderTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

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

    private static CommonMockitoObjects mockitoObjects;
    private static MockInterface mockInterface;
    private static BasicMessageContext basicMessageContext;
    private static MessageContext messageContext;
    private static HttpServletRequest httpServletRequest;
    private static HttpServletResponse httpServletResponse;
    private static SsoSamlService ssoService;
    private static SsoRequest ssoRequest;
    private static SsoConfig ssoConfig;
    private static AcsDOMMetadataProvider acsDOM;
    private static HTTPPostDecoder httpPostDecoder;

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        instance = new BasicMessageContextBuilder();
        qnLogoutRequest = new QName(QN_NS_URI, QN_LOCALNAME, QN_NAME);
        
        // Initialize Mockito objects
        mockitoObjects = new CommonMockitoObjects();
        mockInterface = mock(MockInterface.class);
        basicMessageContext = mockitoObjects.getBasicMessageContext();
        messageContext = mockitoObjects.getMessageContext();
        httpServletRequest = mockitoObjects.getServletRequest();
        httpServletResponse = mockitoObjects.getServletResponse();
        ssoService = mockitoObjects.getSsoService();
        ssoRequest = mockitoObjects.getSsoRequest();
        ssoConfig = mockitoObjects.getSsoConfig();
        acsDOM = mock(AcsDOMMetadataProvider.class);
        httpPostDecoder = mock(HTTPPostDecoder.class);
        
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

        // Setup mock behavior
        when(mockInterface.getBasicMessageContext()).thenReturn(basicMessageContext);
        when(basicMessageContext.getSsoConfig()).thenReturn(ssoConfig);
        when(ssoConfig.getIdpMetadataProvider()).thenReturn(acsDOM);
        when(basicMessageContext.getHttpServletRequest()).thenReturn(httpServletRequest);
        when(basicMessageContext.getSsoService()).thenReturn(ssoService);
        when(ssoService.getProviderId()).thenReturn("sp1");
        when(ssoConfig.getSpHostAndPort()).thenReturn("http://www.ibm.com");
        when(mockInterface.getSamlHttpPostDecoder()).thenReturn(httpPostDecoder);
        when(httpPostDecoder.getMessageContext()).thenReturn(messageContext);

        // Execute the method under test
        instance.buildAcs(httpServletRequest, httpServletResponse, ssoService, RELAY_STATE, ssoRequest);
        
        // Verify interactions
        verify(basicMessageContext).setAndRemoveCachedRequestInfo(RELAY_STATE, ssoRequest);
        verify(ssoConfig).getIdpMetadataProvider();
        verify(basicMessageContext).setMetadataProvider(acsDOM);
        verify(basicMessageContext).getHttpServletRequest();
        verify(basicMessageContext).getSsoService();
        verify(ssoService).getProviderId();
        verify(ssoConfig).getSpHostAndPort();
        verify(httpPostDecoder).decode();
        // We don't verify setMessageContext because it uses any() matcher and is called multiple times
    }

}
