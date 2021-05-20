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
package com.ibm.ws.security.saml.sso20.metadata;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.security.cert.CertificateEncodingException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.provider.MapBasedConfiguration;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.IDIndex;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.SPSSODescriptorImpl;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.impl.KeyInfoImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;

import junit.framework.Assert;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import test.common.SharedOutputManager;

public class SpMetadataBuilderTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("unchecked")
    private static final XMLObjectBuilder<XMLObject> XMLObjectBuilder = mockery.mock(XMLObjectBuilder.class, "xmlObjectBuilderMockSP");
    private static final Document document = mockery.mock(Document.class, "documentSP");
    private static final Element element = mockery.mock(Element.class, "elementSP");
    private static final HttpServletRequest httpServletRequest = mockery.mock(HttpServletRequest.class, "httpServletRequestSP");
    private static final IDIndex IDIndex = mockery.mock(IDIndex.class, "IDIndexSP");
    private static final Marshaller marshaller = mockery.mock(Marshaller.class, "marshallerSP");
    private static final Node node = mockery.mock(Node.class, "node");
    private static final ParserPool parserPool = mockery.mock(ParserPool.class, "parserPoolSP");
    private static final SsoSamlService ssoService = mockery.mock(SsoSamlService.class, "ssoServiceSP");
    private static final SsoConfig ssoConfig = mockery.mock(SsoConfig.class, "ssoConfigSP");
    private static final X509Certificate xCertificate509 = mockery.mock(X509Certificate.class, "X509CertificateSP");
    private static final org.opensaml.xmlsec.signature.X509Certificate x509CertificateSaml = mockery.mock(org.opensaml.xmlsec.signature.X509Certificate.class, "X509CertificateSamlSP");
    private static final X509Data x509Data = mockery.mock(X509Data.class, "x509Data");
    private final static XMLObjectProviderRegistry providerRegistry = mockery.mock(XMLObjectProviderRegistry.class);
    private final static MarshallerFactory marshallerFactory = mockery.mock(MarshallerFactory.class);
    private final static XMLObjectBuilderFactory builderFactory = mockery.mock(XMLObjectBuilderFactory.class);
    
    private final String SERVER_NAME = "fvttest_sp.austin.ibm.com";
    private final int PORT_NUMBER = 9443;
    private final String PROVIDED_ID = "_257f9d9e9fa14962c0803903a6ccad931245264310738";
    private static final String SERVER_PROTOCOL = "https";

    private final String ELEMENT_PREFIX = "md";

    private final String NAMESPACE_URI = "http://www.w3.org/2000/xmlns/";
    private final String QUALIFIED_NAME = "xmlns:md";
    private final String VALUE = "urn:oasis:names:tc:SAML:2.0:metadata";

    static String metadataAsString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                                     +
                                     "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\"\r\n"
                                     +
                                     "entityID=\"https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20\">\r\n"
                                     +
                                     "<md:SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"\r\n"
                                     +
                                     "WantAssertionsSigned=\"true\"\r\n"
                                     +
                                     "AuthnRequestsSigned=\"true\">\r\n"
                                     +
                                     "<md:KeyDescriptor use=\"signing\">\r\n"
                                     +
                                     "<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\r\n"
                                     +
                                     "<X509Data>\r\n"
                                     +
                                     "<X509Certificate>\r\n"
                                     +
                                     "MIICBzCCAXCgAwIBAgIEQH26vjANBgkqhkiG9w0BAQQFADBIMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGVGl2b2xpMQ4wDAYDVQQLEwVUQU1lQjEYMBYGA1UEAxMPZmltZGVtby5pYm0uY29tMB4XDTA0MDQxNDIyMjcxMFoXDTE3MTIyMjIyMjcxMFowSDELMAkGA1UEBhMCVVMxDzANBgNVBAoTBlRpdm9saTEOMAwGA1UECxMFVEFNZUIxGDAWBgNVBAMTD2ZpbWRlbW8uaWJtLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAiZ0D1X6rk8\r\n"
                                     +
                                     "+ZwNBTVZt7C85m421a8A52Ksjw40t+jNvbLYDp/W66AMMYD7rB5qgniZ5K1p9W8ivM9WbPxc2u/60tFPg0e/Q/r/fxegW1K1umnay+5MaUvN3p4XUCRrfg79OvurvXQ7GZa1/\r\n"
                                     +
                                     "wOp5vBIdXzg6i9CVAqL29JGi6GYUCAwEAATANBgkqhkiG9w0BAQQFAAOBgQBXiAhxm91I4m\r\n"
                                     +
                                     "+g3YX+dyGc352TSKO8HvAIBkHHFFwIkzhNgO+zLhxg5UMkOg12X9ucW7leZ1IB0Z6+JXBrXIWmU3UPum+QxmlaE0OG9zhp9LEfzsE5+ff+7XpS0wpJklY6c+cqHj4aTGfOhSE6u7BLdI26cZNdzxdhikBMZPgdyQ==\r\n"
                                     +
                                     "</X509Certificate>\r\n"
                                     +
                                     "</X509Data>\r\n"
                                     +
                                     "</KeyInfo>\r\n"
                                     +
                                     "</md:KeyDescriptor>\r\n"
                                     +
                                     "<md:KeyDescriptor use=\"encryption\">\r\n"
                                     +
                                     "<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\r\n"
                                     +
                                     "<X509Data>\r\n"
                                     +
                                     "<X509Certificate>\r\n"
                                     +
                                     "MIICBzCCAXCgAwIBAgIEQH26vjANBgkqhkiG9w0BAQQFADBIMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGVGl2b2xpMQ4wDAYDVQQLEwVUQU1lQjEYMBYGA1UEAxMPZmltZGVtby5pYm0uY29tMB4XDTA0MDQxNDIyMjcxMFoXDTE3MTIyMjIyMjcxMFowSDELMAkGA1UEBhMCVVMxDzANBgNVBAoTBlRpdm9saTEOMAwGA1UECxMFVEFNZUIxGDAWBgNVBAMTD2ZpbWRlbW8uaWJtLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAiZ0D1X6rk8\r\n"
                                     +
                                     "+ZwNBTVZt7C85m421a8A52Ksjw40t+jNvbLYDp/W66AMMYD7rB5qgniZ5K1p9W8ivM9WbPxc2u/60tFPg0e/Q/r/fxegW1K1umnay+5MaUvN3p4XUCRrfg79OvurvXQ7GZa1/\r\n"
                                     +
                                     "wOp5vBIdXzg6i9CVAqL29JGi6GYUCAwEAATANBgkqhkiG9w0BAQQFAAOBgQBXiAhxm91I4m\r\n"
                                     +
                                     "+g3YX+dyGc352TSKO8HvAIBkHHFFwIkzhNgO+zLhxg5UMkOg12X9ucW7leZ1IB0Z6+JXBrXIWmU3UPum+QxmlaE0OG9zhp9LEfzsE5+ff+7XpS0wpJklY6c+cqHj4aTGfOhSE6u7BLdI26cZNdzxdhikBMZPgdyQ==\r\n"
                                     +
                                     "</X509Certificate>\r\n" +
                                     "</X509Data>\r\n" +
                                     "</KeyInfo>\r\n" +
                                     "<md:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\r\n" +
                                     "</md:KeyDescriptor>\r\n" +
                                     "<md:ArtifactResolutionService isDefault=\"true\"\r\n" +
                                     "index=\"0\"\r\n" +
                                     "Location=\"https://fvttest_sp.austin.ibm.com:9444/sps/FvttestSp/saml20/soap\"\r\n" +
                                     "Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"/>\r\n" +
                                     "<md:SingleLogoutService Location=\"https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/slo\"\r\n" +
                                     "Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\"/>\r\n" +
                                     "<md:SingleLogoutService Location=\"https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/slo\"\r\n" +
                                     "Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"/>\r\n" +
                                     "<md:SingleLogoutService Location=\"https://fvttest_sp.austin.ibm.com:9444/sps/FvttestSp/saml20/soap\"\r\n" +
                                     "Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"/>\r\n" +
                                     "<md:NameIDFormat>\r\n" +
                                     "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\r\n" +
                                     "</md:NameIDFormat>\r\n" +
                                     "<md:NameIDFormat>\r\n" +
                                     "urn:oasis:names:tc:SAML:2.0:nameid-format:transient\r\n" +
                                     "</md:NameIDFormat>\r\n" +
                                     "<md:NameIDFormat>\r\n" +
                                     "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\r\n" +
                                     "</md:NameIDFormat>\r\n" +
                                     "<md:NameIDFormat>\r\n" +
                                     "urn:oasis:names:tc:SAML:2.0:nameid-format:encrypted\r\n" +
                                     "</md:NameIDFormat>\r\n" +
                                     "<md:AssertionConsumerService isDefault=\"true\"\r\n" +
                                     "index=\"0\"\r\n" +
                                     "Location=\"https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/login\"\r\n" +
                                     "Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\"/>\r\n" +
                                     "<md:AssertionConsumerService index=\"1\"\r\n" +
                                     "Location=\"https://fvttest_sp.austin.ibm.com:9443/sps/FvttestSp/saml20/login\"\r\n" +
                                     "Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"/>\r\n" +
                                     "</md:SPSSODescriptor>\r\n" +
                                     "<md:Organization>\r\n" +
                                     "<md:OrganizationName xml:lang=\"en\">\r\n" +
                                     "FvtTestSP\r\n" +
                                     "</md:OrganizationName>\r\n" +
                                     "<md:OrganizationDisplayName xml:lang=\"en\">\r\n" +
                                     "FvtTestSP\r\n" +
                                     "</md:OrganizationDisplayName>\r\n" +
                                     "<md:OrganizationURL xml:lang=\"en\"/>\r\n" +
                                     "</md:Organization>\r\n" +
                                     "<md:ContactPerson contactType=\"technical\">\r\n" +
                                     "<md:Company>\r\n" +
                                     "FvtTestSP\r\n" +
                                     "</md:Company>\r\n" +
                                     "<md:GivenName/>\r\n" +
                                     "<md:SurName/>\r\n" +
                                     "<md:EmailAddress/>\r\n" +
                                     "<md:TelephoneNumber/>\r\n" +
                                     "</md:ContactPerson>\r\n" +
                                     "</md:EntityDescriptor>";

    private final static List<X509Certificate> Certificates = new ArrayList<X509Certificate>();

    static Document metadataDocument;
    static NodeList nodeList;
    static Element elementFinal;
    
    private static Configuration configuration;

    public static Document loadXML(String xmlString) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xmlString));
        Document doc = builder.parse(is);

        return doc;
    }

    /*
     * This method does some configurations required by the tests.
     */
    @BeforeClass
    public static void setUp() throws ParserConfigurationException, SAXException, IOException {
        outputMgr.trace("*=all");
        mockery.checking(new Expectations() {
            {
                allowing(ssoService).getConfig();
                will(returnValue(ssoConfig));

                allowing(ssoConfig).getSpHostAndPort();
                will(returnValue(null));

                allowing(httpServletRequest).isSecure(); //
                will(returnValue(true));//

                allowing(httpServletRequest).getScheme();//
                will(returnValue(SERVER_PROTOCOL));//
                
                allowing(providerRegistry).getParserPool();
                will(returnValue(parserPool));
                
                allowing(providerRegistry).getMarshallerFactory();
                will(returnValue(marshallerFactory));
                
                allowing(marshallerFactory).registerMarshaller(with(any(QName.class)), with(any(Marshaller.class)));
                
                allowing(marshallerFactory).getMarshaller(with(any(XMLObject.class)));
                will(returnValue(marshaller));
                
                allowing(marshallerFactory).getMarshaller(with(any(QName.class)));
                will(returnValue(marshaller));
                
                allowing(providerRegistry).getBuilderFactory();
                will(returnValue(builderFactory));
                
                allowing(builderFactory).registerBuilder(with(any(QName.class)), with(any(XMLObjectBuilder.class)));
                
            }
        });
        metadataDocument = loadXML(metadataAsString);
        nodeList = metadataDocument.getElementsByTagName("md:EntityDescriptor");
        elementFinal = (Element) nodeList.item(0);

        QName childQN = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "SPSSODescriptor", "md");
        
        
        providerRegistry.getMarshallerFactory().registerMarshaller(childQN, marshaller);
        providerRegistry.getBuilderFactory().registerBuilder(X509Data.DEFAULT_ELEMENT_NAME, XMLObjectBuilder);
        providerRegistry.getBuilderFactory().registerBuilder(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME, XMLObjectBuilder);

        Certificates.add(xCertificate509);
        configuration = new MapBasedConfiguration();
        ConfigurationService.setConfiguration(configuration);
        configuration.register(XMLObjectProviderRegistry.class,providerRegistry,ConfigurationService.DEFAULT_PARTITION_NAME);
        
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
        configuration = new MapBasedConfiguration();
        ConfigurationService.setConfiguration(configuration);
    }

    @Test
    public void builEntityDescriptorTest() throws KeyStoreException, CertificateException, CertificateEncodingException, XMLParserException, ParserConfigurationException, SAXException, IOException, SamlException {

        mockery.checking(new Expectations() {
            {
                allowing(ssoService).getProviderId();
                will(returnValue(PROVIDED_ID));

                one(httpServletRequest).getServerName();
                will(returnValue(SERVER_NAME));
                one(httpServletRequest).getServerPort();
                will(returnValue(PORT_NUMBER));

                one(ssoConfig).isWantAssertionsSigned();
                will(returnValue(true));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(true));
                one(ssoConfig).isIncludeX509InSPMetadata();
                will(returnValue(true));

                one(ssoService).getSignatureCertificate();
                will(returnValue(xCertificate509));

                allowing(builderFactory).getBuilder(with(any(Element.class)));
                will(returnValue(XMLObjectBuilder));
                allowing(builderFactory).getBuilder(with(any(QName.class)));
                will(returnValue(XMLObjectBuilder));
                
                allowing(XMLObjectBuilder).buildObject(X509Data.DEFAULT_ELEMENT_NAME);
                will(returnValue(x509Data));

                ignoring(x509Data).getParent();
                will(returnValue(null));

                allowing(x509Data).setParent(with(any(KeyInfoImpl.class)));
                allowing(x509Data).releaseParentDOM(true);
                allowing(x509Data).getIDIndex();
                will(returnValue(IDIndex));

                allowing(IDIndex).isEmpty();
                will(returnValue(true));

                allowing(x509Data).getSchemaType();
                will(returnValue(X509Data.DEFAULT_ELEMENT_NAME));

                allowing(x509Data).getElementQName();
                will(returnValue(X509Data.DEFAULT_ELEMENT_NAME));

                allowing(x509Data).getX509Certificates();
                will(returnValue(Certificates));

                allowing(XMLObjectBuilder).buildObject(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
                will(returnValue(x509CertificateSaml));

                allowing(xCertificate509).getEncoded();

                allowing(x509CertificateSaml).setValue("");

            }
        });

        SpMetadataBuilder metadataBuilder = new SpMetadataBuilder(ssoService);
        EntityDescriptor ed = metadataBuilder.buildEntityDescriptor(httpServletRequest);

        // check we are producing slo endpoint in metadata
        List sloList = ed.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol").getSingleLogoutServices();
        Iterator<SingleLogoutService> it = sloList.iterator();
        SingleLogoutService slo = it.next();
        String location = slo.getLocation();
        Assert.assertTrue("slo location: " + location + " is null or empty", location != null && location.length() > 0);
    }

    public void buildEntityDescriptorExpectations() throws KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                allowing(ssoService).getProviderId();
                will(returnValue(PROVIDED_ID));

                one(httpServletRequest).getServerName();
                will(returnValue(SERVER_NAME));
                one(httpServletRequest).getServerPort();
                will(returnValue(PORT_NUMBER));

                one(ssoConfig).isWantAssertionsSigned();
                will(returnValue(true));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(true));
                one(ssoConfig).isIncludeX509InSPMetadata();
                will(returnValue(true));
                one(ssoService).getSignatureCertificate();
                will(returnValue(xCertificate509));

                allowing(builderFactory).getBuilder(with(any(Element.class)));
                will(returnValue(XMLObjectBuilder));
                allowing(builderFactory).getBuilder(with(any(QName.class)));
                will(returnValue(XMLObjectBuilder));
                
                allowing(XMLObjectBuilder).buildObject(X509Data.DEFAULT_ELEMENT_NAME);
                will(returnValue(x509Data));

                ignoring(x509Data).getParent();
                will(returnValue(null));

                allowing(x509Data).setParent(with(any(KeyInfoImpl.class)));
                allowing(x509Data).releaseParentDOM(true);
                allowing(x509Data).getIDIndex();
                will(returnValue(IDIndex));

                allowing(IDIndex).isEmpty();
                will(returnValue(true));

                allowing(x509Data).getSchemaType();
                will(returnValue(X509Data.DEFAULT_ELEMENT_NAME));

                allowing(x509Data).getElementQName();
                will(returnValue(X509Data.DEFAULT_ELEMENT_NAME));

                allowing(x509Data).getX509Certificates();
                will(returnValue(Certificates));

                allowing(XMLObjectBuilder).buildObject(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
                will(returnValue(x509CertificateSaml));

                allowing(xCertificate509).getEncoded();

                allowing(x509CertificateSaml).setValue("");
            }
        });
    }

    @Test
    public void getEntityDescriptor() throws SamlException, XMLParserException, ParserConfigurationException, SAXException, IOException {

        mockery.checking(new Expectations() {
            {
                one(parserPool).newDocument();
                will(returnValue(metadataDocument));

                one(ssoService).getProviderId();
                will(returnValue(null));
            }
        });

        EntityDescriptorBuilder edBuilder = new EntityDescriptorBuilder();
        EntityDescriptor entityDescriptorVar = edBuilder.buildObject();
        entityDescriptorVar.setDOM(elementFinal);

        SpMetadataBuilder metadataBuilder = new SpMetadataBuilder(ssoService);
        metadataBuilder.getEntityDescriptor(entityDescriptorVar);
    }

    @Test
    public void buildSPMetadataTest() throws SamlException, ParserConfigurationException, SAXException, IOException, KeyStoreException, CertificateException, XMLParserException, MarshallingException {

        mockery.checking(new Expectations() {
            {
                buildEntityDescriptorExpectations();

                one(parserPool).newDocument();
                will(returnValue(document));

                one(document).createElementNS(elementFinal.getNamespaceURI(), elementFinal.getNodeName());
                will(returnValue(element));

                one(document).getDocumentElement();
                will(returnValue(element));

                one(document).replaceChild(element, element);
                will(returnValue(nodeList.item(0)));

                one(element).setPrefix(ELEMENT_PREFIX);
                allowing(element).hasAttributes();
                will(returnValue(false));

                one(element).getParentNode();
                will(returnValue(node));

                allowing(node).getNodeType();
                will(returnValue(elementFinal.getAttributes().item(0).getNodeType()));

                allowing(node).getParentNode();
                will(returnValue(null));

                one(element).setAttributeNS(NAMESPACE_URI, QUALIFIED_NAME, VALUE);
                allowing(element).setAttributeNS(null, "entityID",
                                                 SERVER_PROTOCOL + "://" + SERVER_NAME + ":" + String.valueOf(PORT_NUMBER) + "/ibm/saml20/" + PROVIDED_ID);

                one(marshaller).marshall(with(any(SPSSODescriptorImpl.class)), with(any(Element.class)));
                will(returnValue(element));

                //started using a real object to avoid going deeper with mocks.
                allowing(element).getOwnerDocument();
                will(returnValue(metadataDocument));

                allowing(element).getNodeType();
                will(returnValue(elementFinal.getNodeType()));

                allowing(element).getPrefix();
                will(returnValue(elementFinal.getPrefix()));

                allowing(element).getLocalName();
                will(returnValue(elementFinal.getLocalName()));

                allowing(element).getAttributes();
                will(returnValue(elementFinal.getAttributes()));

                allowing(element).getNamespaceURI();
                will(returnValue(elementFinal.getNamespaceURI()));

                one(element).getNodeName();
                will(returnValue(elementFinal.getNodeName()));

                one(element).getFirstChild();
                will(returnValue(null));

                one(element).getTagName();
                will(returnValue(elementFinal.getTagName()));

                one(element).hasChildNodes();
                will(returnValue(false));

                one(element).getNodeName();
                will(returnValue(elementFinal.getNodeName()));
            }
        });

        SpMetadataBuilder metadataBuilder = new SpMetadataBuilder(ssoService);
        metadataBuilder.buildSpMetadata(httpServletRequest);
    }

}
