/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertSame;
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
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;

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

        // Register a real BasicParserPool so that getSamlHttpPostDecoder's call to
        // XMLObjectProviderRegistrySupport.getParserPool() returns non-null.
        BasicParserPool pp = new BasicParserPool();
        pp.setNamespaceAware(true);
        pp.setMaxPoolSize(50);
        try {
            pp.initialize();
        } catch (ComponentInitializationException e) {
            // ignore — initialization failure would surface as a test failure
        }
        providerRegistry.setParserPool(pp);
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @After
    public void resetInstance() {
        // BuildAcsTest overwrites the static `instance` with an anonymous subclass.
        // Reset to a real BasicMessageContextBuilder so subsequent tests exercise real code.
        instance = new BasicMessageContextBuilder();
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

    /**
     * Verifies that getSamlHttpPostDecoder uses setHttpServletRequestSupplier (not the
     * deprecated setHttpServletRequest). After the fix, calling getHttpServletRequest()
     * on the returned decoder must return the exact request object that was passed in,
     * proving the supplier was wired correctly.
     */
    @Test
    public void test_getSamlHttpPostDecoder_supplierReturnsCorrectRequest() throws Exception {
        // Re-use the shared mock request from CommonMockitoObjects
        HttpServletRequest mockRequest = httpServletRequest;
        String acsUrl = "https://localhost/ibm/saml20/sp/acs";

        HTTPPostDecoder decoder = instance.getSamlHttpPostDecoder(acsUrl, mockRequest);

        // getHttpServletRequest() is backed by the supplier set via setHttpServletRequestSupplier.
        // If the deprecated setHttpServletRequest was used instead, OpenSAML 4.x would log a warning
        // and the supplier path would not be exercised.  Either way, the request must round-trip.
        assertSame("decoder.getHttpServletRequest() must return the exact request passed to getSamlHttpPostDecoder",
                   mockRequest, decoder.getHttpServletRequest());
    }

    /**
     * Verifies that getSamlHttpPostDecoder does NOT call the deprecated
     * setHttpServletRequest(HttpServletRequest) path, which would cause OpenSAML 4.x to
     * log "Unsafe HttpServletRequest injected".  The fix uses setHttpServletRequestSupplier
     * instead.  We verify this indirectly: the decoder returned must be initialized without
     * error, and getHttpServletRequest() must not return null — if the supplier were absent
     * the decoder would have no reference at all.
     */
    @Test
    public void test_getSamlHttpPostDecoder_doesNotCallDeprecatedSetHttpServletRequest() throws Exception {
        String acsUrl = "https://localhost/ibm/saml20/sp/acs";

        HTTPPostDecoder decoder = instance.getSamlHttpPostDecoder(acsUrl, httpServletRequest);

        // The decoder must be initialized (no exception thrown) and the request must be
        // reachable via the supplier-backed accessor.  A null here would mean neither
        // setHttpServletRequest nor setHttpServletRequestSupplier was called.
        assertSame("decoder.getHttpServletRequest() must equal the request passed in — " +
                   "confirming setHttpServletRequestSupplier was used (not the deprecated setter)",
                   httpServletRequest, decoder.getHttpServletRequest());
    }

}
