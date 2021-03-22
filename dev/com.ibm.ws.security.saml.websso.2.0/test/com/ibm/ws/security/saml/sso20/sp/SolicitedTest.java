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

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.provider.MapBasedConfiguration;
import org.opensaml.core.xml.NamespaceManager;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureImpl;
import org.opensaml.xmlsec.signature.support.ContentReference;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.Activator;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.security.tai.TAIResult;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import test.common.SharedOutputManager;

public class SolicitedTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();
    private static final WebAppSecurityConfig webAppSecConfig = common.getWebAppSecConfig();
    static {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
    }

    private static final BasicMessageContext<?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final BasicMessageContextBuilder<?, ?, ?> basicMessageContextBuilder = common.getBasicMessageContextBuilder();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final AcsDOMMetadataProvider metadataProvider = mockery.mock(AcsDOMMetadataProvider.class, "metadataProvider");
    private static final XMLObject metadata = mockery.mock(EntityDescriptor.class, "metadata");
    private static final XMLObject badMetadata = mockery.mock(Signature.class, "badMetadata");
    private static final IDPSSODescriptor ssoDescriptor = mockery.mock(IDPSSODescriptor.class, "ssoDescriptor");
    private static final SingleSignOnService singleSignOnService = mockery.mock(SingleSignOnService.class, "singleSignOnService");
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final Endpoint endpoint = mockery.mock(Endpoint.class, "endpoint");
    private static final ParserPool parserPool = mockery.mock(ParserPool.class, "parserPool");
   
    private static final Element element = mockery.mock(Element.class, "element");
    private static final PrintWriter out = mockery.mock(PrintWriter.class, "out");
    private static final Cache cache = common.getCache();
    private static final PrivateKey privateKey = mockery.mock(RSAPrivateKey.class, "privateKey");
    private static final Certificate certificate = mockery.mock(X509Certificate.class, "certificate");
    private static final XMLObjectBuilder<?> objectBuilder = mockery.mock(XMLObjectBuilder.class, "objectBuilder");
    private static final Marshaller marshaller = mockery.mock(Marshaller.class, "marshaller");
    private static final AcsDOMMetadataProvider badMetadataProvider = mockery.mock(AcsDOMMetadataProvider.class, "badMetadatProvider");
    private static final AuthnRequest authnRequest = mockery.mock(AuthnRequest.class, "authnRequest");
    private final static XMLObjectProviderRegistry providerRegistry = mockery.mock(XMLObjectProviderRegistry.class);
    private final static MarshallerFactory marshallerFactory = mockery.mock(MarshallerFactory.class);
    private final static XMLObjectBuilderFactory builderFactory = mockery.mock(XMLObjectBuilderFactory.class);
    @SuppressWarnings("unchecked")

    private static final String PROVIDER_ID = "b07b804c";
    private static final String DEFAULT_KS_PASS = "Liberty";

    private static List<SingleSignOnService> listSingleSignOnServices = new ArrayList<SingleSignOnService>();

    private static Solicited initiator;
    private static final Activator activator = new Activator();
    private static final QName qName = new QName("test");
    @SuppressWarnings("rawtypes")
    static BasicMessageContextBuilder<?, ?, ?> instance = new BasicMessageContextBuilder();
    
    private static Configuration configuration;

    @BeforeClass
    public static void setUp() throws Exception {
        outputMgr.trace("*=all");
        configuration = new MapBasedConfiguration();
        ConfigurationService.setConfiguration(configuration);
        configuration.register(XMLObjectProviderRegistry.class,providerRegistry,ConfigurationService.DEFAULT_PARTITION_NAME);
        
        BasicMessageContextBuilder.setInstance(basicMessageContextBuilder);
        activator.start(null);
        final String[] arrayAuthnContextClassRef = { "test" };

        mockery.checking(new Expectations() {
            {
                allowing(basicMessageContextBuilder).buildIdp(request, response, ssoService);
                will(returnValue(basicMessageContext));
                //allowing(basicMessageContext).setPeerEntityId(null);
                allowing(basicMessageContext).setPeerEntityEndpoint(singleSignOnService);
                allowing(basicMessageContext).getPeerEntityEndpoint();
                will(returnValue(endpoint));
                allowing(basicMessageContext).getSsoConfig();
                will(returnValue(ssoConfig));

                allowing(endpoint).getLocation();
                will(returnValue(with(any(String.class))));

                allowing(metadataProvider).resolveSingle(with(any(CriteriaSet.class)));
                will(returnValue((EntityDescriptor) metadata));

                allowing((EntityDescriptor) metadata).getEntityID();
                will(returnValue(null));
                allowing((EntityDescriptor) metadata).getIDPSSODescriptor(Constants.SAML20P_NS);
                will(returnValue(ssoDescriptor));

                allowing(ssoDescriptor).getSingleSignOnServices();
                will(returnValue(listSingleSignOnServices));

                allowing(singleSignOnService).getBinding();
                will(returnValue(Constants.SAML2_POST_BINDING_URI));
                allowing(singleSignOnService).getLocation();
                will(returnValue("https://localhost:8020/ibm/saml20/sp"));

                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost:8010/formlogin")));
                allowing(request).getQueryString();//
                will(returnValue(null));//
                allowing(request).getMethod();
                will(returnValue("PUT"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(8020));
                allowing(request).isSecure();//
                will(returnValue(true));//
                allowing(request).getScheme();//
                will(returnValue("https"));//
                allowing(request).setAttribute("SpSLOInProgress", "true");

                allowing(ssoService).getProviderId();
                will(returnValue(PROVIDER_ID));
                allowing(ssoService).getConfig();
                will(returnValue(ssoConfig));
                allowing(ssoConfig).getSpHostAndPort();
                will(returnValue(null));
                one(ssoService).getAcsCookieCache(PROVIDER_ID);
                will(returnValue(cache));
                allowing(ssoService).getDefaultKeyStorePassword();
                will(returnValue(DEFAULT_KS_PASS));

                one(response).setStatus(with(any(Integer.class)));
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setDateHeader(with(any(String.class)), with(any(Integer.class)));
                one(response).setContentType(with(any(String.class)));
                allowing(response).addCookie(with(any(Cookie.class)));
                one(response).getWriter();
                will(returnValue(out));

                one(out).println(with(any(String.class)));
                one(out).flush();

                allowing(ssoConfig).isForceAuthn();
                will(returnValue(with(any(Boolean.class))));
                allowing(ssoConfig).isPassive();
                will(returnValue(with(any(Boolean.class))));
                allowing(ssoConfig).getNameIDFormat();
                will(returnValue("NameIDPolicy"));
                allowing(ssoConfig).getAllowCreate();
                will(returnValue(new Boolean(true)));
                allowing(ssoConfig).getAuthnContextClassRef();
                will(returnValue(arrayAuthnContextClassRef));
                allowing(ssoConfig).getAuthnContextComparisonType();
                will(returnValue("default"));
                allowing(ssoConfig).isHttpsRequired();
                will(returnValue(with(any(Boolean.class))));

                //one(parserPool).newDocument();
                //will(returnValue(document));

                one(cache).put(with(any(String.class)), with(any(ForwardRequestInfo.class)));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
            }
        });

        initiator = new Solicited(ssoService);

    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
        BasicMessageContextBuilder.setInstance(instance);
        configuration = new MapBasedConfiguration();
        ConfigurationService.setConfiguration(configuration);
        mockery.assertIsSatisfied();    
    }

    @Before
    public void before() {
        configuration.register(XMLObjectProviderRegistry.class,providerRegistry,ConfigurationService.DEFAULT_PARTITION_NAME);
        listSingleSignOnServices.clear();
        listSingleSignOnServices.add(singleSignOnService);
    }

    @Test
    public void testSendAuthRequestToIdp_AuthnRequestsNotSigned() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(metadataProvider));
                allowing(metadataProvider).getEntityId();
                will(returnValue("entityId"));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(false));
                one(ssoService).getPrivateKey();
                will(returnValue(null));
            }
        });

        try {
            TAIResult result = initiator.sendAuthRequestToIdp(request, response);
            assertTrue("The TAIResult must not be null.", result != null);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testSendAuthRequestToIdp_AuthnRequestsIsSigned() throws KeyStoreException, CertificateException, MarshallingException {

        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(true));
                one(ssoConfig).getSignatureMethodAlgorithm();
                will(returnValue(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1));

                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                allowing(privateKey).getAlgorithm();
                will(returnValue("rsa"));
                one(ssoService).getSignatureCertificate();
                will(returnValue(certificate));

                one(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));

                one(providerRegistry).getMarshallerFactory();
                will(returnValue(marshallerFactory));
                
                allowing(marshallerFactory).registerMarshaller(with(any(QName.class)), with(any(Marshaller.class)));
                
                allowing(providerRegistry).getBuilderFactory();
                will(returnValue(builderFactory));
                
                allowing(builderFactory).registerBuilder(with(any(QName.class)), with(any(XMLObjectBuilder.class)));
                
            }
        });
        
        providerRegistry.getBuilderFactory().registerBuilder(Signature.DEFAULT_ELEMENT_NAME, objectBuilder);
        providerRegistry.getMarshallerFactory().registerMarshaller(qName, marshaller);


        try {
            initiator.sendAuthRequestToIdp(request, response);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        } catch (WebTrustAssociationFailedException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testSendAuthRequestToIdp_NullPrivateKey() throws KeyStoreException, CertificateException, WebTrustAssociationFailedException {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(true));
                one(ssoService).getPrivateKey();
                will(returnValue(null));
                //one(ssoService).getConfig();
                //will(returnValue(ssoConfig));
                one(ssoConfig).getKeyStoreRef();
                will(returnValue("unitTestKeyStoreRef"));
                one(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        try {
            initiator.sendAuthRequestToIdp(request, response);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleIdpMetadataAndLoginUrl_NullMetadataProvider() {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(null));

                atMost(2).of(ssoConfig).getIdpMetadata();
                will(returnValue("IDP"));

            }
        });

        try {
            initiator.handleIdpMetadataAndLoginUrl(basicMessageContext);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleIdpMetadataAndLoginUrl_NullIDPMetadataFile() {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(null));
                atMost(2).of(ssoConfig).getIdpMetadata();
                will(returnValue(null));
            }
        });

        try {
            initiator.handleIdpMetadataAndLoginUrl(basicMessageContext);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleIdpMetadataAndLoginUrl_BadMetadata() throws ResolverException {
        final ResolverException e = new ResolverException();

        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(badMetadataProvider));
                one(badMetadataProvider).getEntityId();
                will(returnValue("entityId"));
                one(badMetadataProvider).resolveSingle(with(any(CriteriaSet.class)));
                will(throwException(e));

            }
        });

        try {
            initiator.handleIdpMetadataAndLoginUrl(basicMessageContext);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    //@Test
    public void testHandleIdpMetadataAndLoginUrl_NotInstanceOfEntityDescriptor() throws ResolverException {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(badMetadataProvider));
                one(badMetadataProvider).getEntityId();
                will(returnValue("entityId"));
                one(badMetadataProvider).resolveSingle(with(any(CriteriaSet.class)));
                will(returnValue((EntityDescriptor)badMetadata));
                //one(badMetadata.getEntityID()
                atMost(2).of(ssoConfig).getIdpMetadata();
                will(returnValue(null));
                //allowing(request).getAttribute("FormLogoutExitPage");
                //will(returnValue(null));
            }
        });

        try {
            initiator.handleIdpMetadataAndLoginUrl(basicMessageContext);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetAuthnContextComparisonTypeEnumeration() {
        AuthnContextComparisonTypeEnumeration result = null;
        final String EXACT = "exact";
        final String MINIMUM = "minimum";
        final String MAXIMUM = "maximum";
        final String BETTER = "better";

        result = initiator.getAuthnContextComparisonTypeEnumeration(EXACT);
        assertTrue("Expected to receive the message '" + EXACT + "' but was received " + result,
                   (result != null) && (result.toString().equals(EXACT)));

        result = initiator.getAuthnContextComparisonTypeEnumeration(MINIMUM);
        assertTrue("Expected to receive the message '" + MINIMUM + "' but was received " + result,
                   (result != null) && (result.toString().equals(MINIMUM)));

        result = initiator.getAuthnContextComparisonTypeEnumeration(MAXIMUM);
        assertTrue("Expected to receive the message '" + MAXIMUM + "' but was received " + result,
                   (result != null) && (result.toString().equals(MAXIMUM)));

        result = initiator.getAuthnContextComparisonTypeEnumeration(BETTER);
        assertTrue("Expected to receive the message '" + BETTER + "' but was received " + result,
                   (result != null) && (result.toString().equals(BETTER)));
    }

    @Test
    public void testPostIdp_NullRelayState() {
        final String ERROR_MESSAGE = "RelayState, Single-Sign-On URL, and AuthnRequest must be provided";

        try {
            initiator.postIdp(request, response, PROVIDER_ID, null, null, null);
            fail("SamlException was not thrown");
        } catch (WebTrustAssociationFailedException ex) {
            assertEquals("Expected to receive the message for '" + ERROR_MESSAGE + "' but it was not received.",
                         ERROR_MESSAGE, ex.getMessage());
        }
    }

    @Test
    public void testGetAuthnRequestString_NullAuthnRequest() {
        try {
            String result = initiator.getAuthnRequestString(null);
            assertNull("Expected to receive a null value but was received " + result, result);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    //@Test
    public void testGetAuthnRequestString_BadDocument() throws XMLParserException, KeyStoreException, CertificateException {
        final XMLParserException e = new XMLParserException();
        mockery.checking(new Expectations() {
            {
                one(parserPool).newDocument();
                will(throwException(e));
                //allowing(request).getAttribute("FormLogoutExitPage");
                //will(returnValue(null));
                allowing(ssoService).getPrivateKey();
                one(providerRegistry).setParserPool(parserPool);
                one(providerRegistry).getParserPool();
                will(returnValue(parserPool));
                allowing(authnRequest).getDOM();
                will(returnValue(element));
                allowing(authnRequest).getElementQName();
                will(returnValue(qName));
                allowing(authnRequest).getParent();
                will(returnValue(null));

                allowing(ssoConfig).getKeyStoreRef();
                will(returnValue("unitTestKeyStoreRef"));
            }
        });

        try {
            initiator.getAuthnRequestString(authnRequest);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

}
