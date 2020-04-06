/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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
import java.io.Writer;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

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
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.impl.AuthnRequestImpl;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.NamespaceManager;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.signature.ContentReference;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.impl.SignatureImpl;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;

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
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.security.tai.TAIResult;

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

    private static final BasicMessageContext<?, ?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final BasicMessageContextBuilder<?, ?, ?> basicMessageContextBuilder = common.getBasicMessageContextBuilder();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final MetadataProvider metadataProvider = common.getMetadataProvider();
    private static final XMLObject metadata = mockery.mock(EntityDescriptor.class, "metadata");
    private static final XMLObject badMetadata = mockery.mock(Signature.class, "badMetadata");
    private static final IDPSSODescriptor ssoDescriptor = mockery.mock(IDPSSODescriptor.class, "ssoDescriptor");
    private static final SingleSignOnService singleSignOnService = mockery.mock(SingleSignOnService.class, "singleSignOnService");
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final Endpoint endpoint = mockery.mock(Endpoint.class, "endpoint");
    private static final ParserPool parserPool = mockery.mock(ParserPool.class, "parserPool");
    private static final Document document = mockery.mock(Document.class, "document");
    private static final Element element = mockery.mock(Element.class, "element");
    private static final DOMImplementation implementation = mockery.mock(DOMImplementation.class, "implementation");
    private static final DOMImplementationLS implementationLS = mockery.mock(DOMImplementationLS.class, "implementationLS");
    private static final LSSerializer serializer = mockery.mock(LSSerializer.class, "serializer");
    private static final LSOutput serializerOut = mockery.mock(LSOutput.class, "serializerOut");
    private static final PrintWriter out = mockery.mock(PrintWriter.class, "out");
    private static final Cache cache = common.getCache();
    private static final PrivateKey privateKey = mockery.mock(RSAPrivateKey.class, "privateKey");
    private static final Certificate certificate = mockery.mock(X509Certificate.class, "certificate");
    private static final PublicKey publicKey = mockery.mock(PublicKey.class, "publicKey");
    private static final XMLObjectBuilder<?> objectBuilder = mockery.mock(XMLObjectBuilder.class, "objectBuilder");
    private static final Signature signatureImpl = mockery.mock(SignatureImpl.class, "signatureImpl");
    private static final KeyInfo keyInfo = mockery.mock(KeyInfo.class, "keyInfo");
    private static final NamespaceManager namespaceManager = mockery.mock(NamespaceManager.class, "namespaceManager");
    private static final Marshaller marshaller = mockery.mock(Marshaller.class, "marshaller");
    private static final MetadataProvider badMetadataProvider = mockery.mock(MetadataProvider.class, "badMetadataProvider");
    private static final AuthnRequest authnRequest = mockery.mock(AuthnRequest.class, "authnRequest");

    private static final String PROVIDER_ID = "b07b804c";

    private static List<SingleSignOnService> listSingleSignOnServices = new ArrayList<SingleSignOnService>();
    private static List<ContentReference> listContentReference = new ArrayList<ContentReference>();

    private static Solicited initiator;
    private static final Activator activator = new Activator();
    private static final QName qName = new QName("test");
    @SuppressWarnings("rawtypes")
    static BasicMessageContextBuilder<?, ?, ?> instance = new BasicMessageContextBuilder();

    @BeforeClass
    public static void setUp() throws Exception {
        outputMgr.trace("*=all");
        Configuration.setGlobalSecurityConfiguration(null);
        BasicMessageContextBuilder.setInstance(basicMessageContextBuilder);
        activator.start(null);
        final String[] arrayAuthnContextClassRef = { "test" };

        mockery.checking(new Expectations() {
            {
                allowing(basicMessageContextBuilder).buildIdp(request, response, ssoService);
                will(returnValue(basicMessageContext));
                allowing(basicMessageContext).setPeerEntityId(null);
                allowing(basicMessageContext).setPeerEntityEndpoint(singleSignOnService);
                allowing(basicMessageContext).getPeerEntityEndpoint();
                will(returnValue(endpoint));
                allowing(basicMessageContext).getSsoConfig();
                will(returnValue(ssoConfig));

                allowing(endpoint).getLocation();
                will(returnValue(with(any(String.class))));

                allowing(metadataProvider).getMetadata();
                will(returnValue(metadata));

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

                one(response).setStatus(with(any(Integer.class)));
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setDateHeader(with(any(String.class)), with(any(Integer.class)));
                one(response).setContentType(with(any(String.class)));
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

                allowing(parserPool).newDocument();
                will(returnValue(document));

                allowing(document).createElementNS(with(any(String.class)), with(any(String.class)));
                will(returnValue(element));
                allowing(document).getDocumentElement();
                will(returnValue(null));
                allowing(document).appendChild(element);
                one(document).getImplementation();
                will(returnValue(implementation));
                allowing(document).createTextNode(with(any(String.class)));
                will(returnValue(null));

                one(element).setPrefix(with(any(String.class)));
                allowing(element).hasAttributes();
                will(returnValue(false));
                allowing(element).getParentNode();
                will(returnValue(null));
                allowing(element).setAttributeNS(with(any(String.class)), with(any(String.class)), with(any(String.class)));
                allowing(element).setIdAttributeNS(with(any(String.class)), with(any(String.class)), with(any(Boolean.class)));
                allowing(element).getOwnerDocument();
                will(returnValue(document));
                allowing(element).getNamespaceURI();
                will(returnValue("NamespaceURI"));
                allowing(element).getLocalName();
                will(returnValue("LocalName"));
                allowing(element).getPrefix();
                will(returnValue("Prefix"));
                allowing(element).appendChild(element);
                allowing(element).appendChild(null);
                allowing(element).setPrefix(with(any(String.class)));

                one(implementation).getFeature(with(any(String.class)), with(any(String.class)));
                will(returnValue(implementationLS));
                one(implementationLS).createLSSerializer();
                will(returnValue(serializer));
                one(implementationLS).createLSOutput();
                will(returnValue(serializerOut));

                one(serializer).setFilter(with(any(LSSerializerFilter.class)));
                one(serializer).write(with(any(Element.class)), with(any(LSOutput.class)));
                one(serializerOut).setCharacterStream(with(any(Writer.class)));

                one(cache).put(with(any(String.class)), with(any(ForwardRequestInfo.class)));

                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
            }
        });

        initiator = new Solicited(ssoService);

    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
        BasicMessageContextBuilder.setInstance(instance);
        mockery.assertIsSatisfied();
    }

    @Before
    public void before() {
        Configuration.setParserPool(parserPool);
        listSingleSignOnServices.clear();
        listSingleSignOnServices.add(singleSignOnService);
    }

    @Test
    public void testSendAuthRequestToIdp_AuthnRequestsNotSigned() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(false));
            }
        });

        try {
            TAIResult result = initiator.sendAuthRequestToIdp(request, response);
            assertTrue("The TAIResult must not be null.", result != null);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testSendAuthRequestToIdp_AuthnRequestsIsSigned() throws KeyStoreException, CertificateException, MarshallingException {
        Configuration.getBuilderFactory().registerBuilder(Signature.DEFAULT_ELEMENT_NAME, objectBuilder);
        Configuration.getMarshallerFactory().registerMarshaller(qName, marshaller);

        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(true));
                one(ssoConfig).getSignatureMethodAlgorithm();
                will(returnValue(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1));

                one(objectBuilder).buildObject(Signature.DEFAULT_ELEMENT_NAME);
                will(returnValue(signatureImpl));

                one(signatureImpl).setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
                one(signatureImpl).setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
                one(signatureImpl).setSigningCredential(with(any(Credential.class)));
                one(signatureImpl).getSignatureAlgorithm();
                will(returnValue(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1));
                one(signatureImpl).getCanonicalizationAlgorithm();
                will(returnValue(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS));
                one(signatureImpl).getKeyInfo();
                will(returnValue(keyInfo));
                allowing(signatureImpl).getContentReferences();
                will(returnValue(listContentReference));
                one(signatureImpl).hasParent();
                will(returnValue(false));
                one(signatureImpl).setParent(with(any(AuthnRequestImpl.class)));
                one(signatureImpl).getIDIndex();
                will(returnValue(null));
                one(signatureImpl).getNamespaceManager();
                will(returnValue(namespaceManager));
                allowing(signatureImpl).getElementQName();
                will(returnValue(qName));
                one(signatureImpl).getSchemaType();
                will(returnValue(qName));
                one((SignatureImpl) signatureImpl).getXMLSignature();
                will(returnValue(null));

                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                one(ssoService).getSignatureCertificate();
                will(returnValue(certificate));

                atMost(2).of(certificate).getPublicKey();
                will(returnValue(publicKey));

                one(namespaceManager).getNonVisibleNamespaces();
                will(returnValue(null));

                one(marshaller).marshall(signatureImpl, element);
                will(returnValue(element));

                allowing(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        try {
            initiator.sendAuthRequestToIdp(request, response);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        } catch (WebTrustAssociationFailedException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
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
                allowing(request).getAttribute("FormLogoutExitPage");
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

                allowing(request).getAttribute("FormLogoutExitPage");
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
    public void testHandleIdpMetadataAndLoginUrl_NullIDPMetadataFile() {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(null));
                atMost(2).of(ssoConfig).getIdpMetadata();
                will(returnValue(null));
                allowing(request).getAttribute("FormLogoutExitPage");
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
    public void testHandleIdpMetadataAndLoginUrl_BadMetadata() throws MetadataProviderException {
        final MetadataProviderException e = new MetadataProviderException();

        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(badMetadataProvider));
                one(badMetadataProvider).getMetadata();
                will(throwException(e));
                allowing(request).getAttribute("FormLogoutExitPage");
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
    public void testHandleIdpMetadataAndLoginUrl_NotInstanceOfEntityDescriptor() throws MetadataProviderException {
        mockery.checking(new Expectations() {
            {
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(badMetadataProvider));
                one(badMetadataProvider).getMetadata();
                will(returnValue(badMetadata));

                atMost(2).of(ssoConfig).getIdpMetadata();
                will(returnValue(null));
                allowing(request).getAttribute("FormLogoutExitPage");
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
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testGetAuthnRequestString_BadDocument() throws XMLParserException {
        final XMLParserException e = new XMLParserException();
        final ParserPool badParserPool = mockery.mock(ParserPool.class, "badParserPool");
        Configuration.setParserPool(badParserPool);

        mockery.checking(new Expectations() {
            {
                one(badParserPool).newDocument();
                will(throwException(e));
                allowing(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
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
