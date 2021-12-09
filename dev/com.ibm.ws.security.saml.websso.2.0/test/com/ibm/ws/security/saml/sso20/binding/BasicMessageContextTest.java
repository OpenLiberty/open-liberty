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

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.core.config.Configuration;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.provider.MapBasedConfiguration;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.NamespaceManager;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.config.DecryptionParserPool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import test.common.SharedOutputManager;

@SuppressWarnings("rawtypes")
public class BasicMessageContextTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final DateTime date = new DateTime(2015, 6, 12, 15, 23, 0, 0);
    private static List<XMLObject> listXMLObjects = new ArrayList<XMLObject>();

    private static final Cache cache = mockery.mock(Cache.class);
    private static final SsoSamlService ssoService = mockery.mock(SsoSamlService.class, "SsoServiceCTX");
    private static final SsoConfig ssoConfig = mockery.mock(SsoConfig.class, "SsoConfigCTX");
    private static final SsoRequest ssoRequest = mockery.mock(SsoRequest.class, "SsoRequestCTX");
    private static final ForwardRequestInfo requestInfo = mockery.mock(ForwardRequestInfo.class, "RequestInfoCTX");

    private static final QName qName = mockery.mock(QName.class, "QNameCTX");
    private static final Assertion assertion = mockery.mock(Assertion.class);
    private static final PrivateKey privateKey = mockery.mock(PrivateKey.class);
    private static final Certificate certificate = mockery.mock(Certificate.class);
    private static final X509Certificate x509certificate = mockery.mock(X509Certificate.class);
    private static final XMLObject xmlObject = mockery.mock(XMLObject.class);
    private static final Response response = mockery.mock(Response.class);
    private static final Issuer issuer = mockery.mock(Issuer.class);
    private static final EntityDescriptor entityDescriptor = mockery.mock(EntityDescriptor.class);
    private static final AcsDOMMetadataProvider acsmetadataProvider = mockery.mock(AcsDOMMetadataProvider.class);
    
    private static final NamespaceManager nsManager = mockery.mock(NamespaceManager.class, "nsManager");

    private static BasicMessageContext instance;
    private static Element finalElement;
    
    private static Configuration configuration = null;
    private static XMLObjectProviderRegistry providerRegistry;
    
    private static final InitialRequestUtil irutil = mockery.mock(InitialRequestUtil.class);

    private final String RELAY_STATE = "RPID%3Dhttps%253A%252F%252Frelyingpartyapp%26wctx%3Dappid%253D45%2526foo%253Dbar";
    private final String CACHE_VALUE = "s%253A%252F%252Frelyingpartyapp%26wctx%3Dappid%253D45%2526foo%253Dbar";
    private final String PROVIDED_ID = "_257f9d9e9fa14962c0803903a6ccad931245264310738";

    private final String ENTITY_DES_ID = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
    private final String SUPPORTED_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";
    private final String IDP_NAME = "https://idp.example.org/SAML2";

    private final String metadataFile = "Metadata.xml";

    final static String elementAsString = "<saml:Assertion\r\n" +
                                          "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\r\n" +
                                          "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" +
                                          "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
                                          "ID=\"b07b804c-7c29-ea16-7300-4f3d6f7928ac\"\r\n" +
                                          "Version=\"2.0\"\r\n" +
                                          "IssueInstant=\"2004-12-05T09:22:05\">\r\n" +
                                          "<saml:Issuer>https://idp.example.org/SAML2</saml:Issuer>\r\n" +
                                          "<ds:Signature\r\n" +
                                          "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">...</ds:Signature>\r\n" +
                                          "<saml:Subject>\r\n" +
                                          "<saml:NameID\r\n" +
                                          "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">\r\n" +
                                          "3f7b3dcf-1674-4ecd-92c8-1544f346baf8\r\n" +
                                          "</saml:NameID>\r\n" +
                                          "<saml:SubjectConfirmation\r\n" +
                                          "Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\r\n" +
                                          "<saml:SubjectConfirmationData\r\n" +
                                          "InResponseTo=\"aaf23196-1773-2113-474a-fe114412ab72\"\r\n" +
                                          "Recipient=\"https://sp.example.com/SAML2/SSO/POST\"\r\n" +
                                          "NotOnOrAfter=\"2004-12-05T09:27:05\"/>\r\n" +
                                          "</saml:SubjectConfirmation>\r\n" +
                                          "</saml:Subject>\r\n" +
                                          "<saml:Conditions\r\n" +
                                          "NotBefore=\"2004-12-05T09:17:05\"\r\n" +
                                          "NotOnOrAfter=\"2004-12-05T09:27:05\">\r\n" +
                                          "<saml:AudienceRestriction>\r\n" +
                                          "<saml:Audience>https://sp.example.com/SAML2</saml:Audience>\r\n" +
                                          "</saml:AudienceRestriction>\r\n" +
                                          "</saml:Conditions>\r\n" +
                                          "<saml:AuthnStatement\r\n" +
                                          "AuthnInstant=\"2004-12-05T09:22:00\"\r\n" +
                                          "SessionIndex=\"b07b804c-7c29-ea16-7300-4f3d6f7928ac\">\r\n" +
                                          "<saml:AuthnContext>\r\n" +
                                          "<saml:AuthnContextClassRef>\r\n" +
                                          "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport\r\n" +
                                          "</saml:AuthnContextClassRef>\r\n" +
                                          "</saml:AuthnContext>\r\n" +
                                          "</saml:AuthnStatement>\r\n" +
                                          "<saml:AttributeStatement>\r\n" +
                                          "<saml:Attribute\r\n" +
                                          "xmlns:x500=\"urn:oasis:names:tc:SAML:2.0:profiles:attribute:X500\"\r\n" +
                                          "x500:Encoding=\"LDAP\"\r\n" +
                                          "NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"\r\n" +
                                          "Name=\"urn:oid:1.3.6.1.4.1.5923.1.1.1.1\"\r\n" +
                                          "FriendlyName=\"eduPersonAffiliation\">\r\n" +
                                          "<saml:AttributeValue\r\n" +
                                          "xsi:type=\"xs:string\">member</saml:AttributeValue>\r\n" +
                                          "<saml:AttributeValue\r\n" +
                                          "xsi:type=\"xs:string\">staff</saml:AttributeValue>\r\n" +
                                          "</saml:Attribute>\r\n" +
                                          "</saml:AttributeStatement>\r\n" +
                                          "</saml:Assertion>";

    public static Document loadXML(String xmlString) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xmlString));
        Document doc = builder.parse(is);

        return doc;
    }

    @BeforeClass
    public static void testBasicMessageContextTest() throws ParserConfigurationException, SAXException, IOException {
        outputMgr.trace("*=all");
        final Set<Namespace> namespaces = new HashSet<Namespace>();
        mockery.checking(new Expectations() {
            {
                one(ssoService).getConfig();
                will(returnValue(ssoConfig));
                allowing(assertion).getNamespaceManager();//
                will(returnValue(nsManager));//
                allowing(nsManager).getAllNamespacesInSubtreeScope();//
                will(returnValue(namespaces));//
            }
        });

        listXMLObjects.add(xmlObject);
        instance = new BasicMessageContext(ssoService);
        finalElement = loadXML(elementAsString).getDocumentElement();
        
        configuration = new MapBasedConfiguration();
        ConfigurationService.setConfiguration(configuration);

        providerRegistry = new XMLObjectProviderRegistry();
        configuration.register(XMLObjectProviderRegistry.class, providerRegistry,
                               ConfigurationService.DEFAULT_PARTITION_NAME);
        BasicParserPool pp = new BasicParserPool();
        pp.setNamespaceAware(true);
        pp.setMaxPoolSize(50);
        try {
            pp.initialize();
        } catch (ComponentInitializationException e) {
           
        }
        ConfigurationService.register(DecryptionParserPool.class, new DecryptionParserPool(pp));
        configuration.register(DecryptionParserPool.class, new DecryptionParserPool(pp), ConfigurationService.DEFAULT_PARTITION_NAME);
        providerRegistry.setParserPool(pp);
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void getUserDataIfReadyNullTest() throws SamlException, ParserConfigurationException, SAXException, IOException {
        instance.setValidatedAssertion(null);
        instance.getUserDataIfReady();
    }

    @Test
    public void getMetadataProviderTest() {

        instance.getMetadataProvider();
    }

    @Test
    public void getSsoServiceTest() {
        instance.getSsoService();
    }

    @Test
    public void getSsoConfigTest() {
        instance.getSsoConfig();
    }



    @Test
    public void getPeerEntityMetadataTest() {
        instance.getPeerEntityMetadata();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setIDPSSODescriptorIfEntityIsNullTest() throws SamlException, ResolverException {
        
        mockery.checking(new Expectations() {
            {
                allowing(response).getIssuer();
                will(returnValue(issuer));

                allowing(issuer).getValue();
                will(returnValue(ENTITY_DES_ID));

                allowing(acsmetadataProvider).resolveSingle(with(any(CriteriaSet.class)));
                //allowing(metadataProvider).getEntityDescriptor(ENTITY_DES_ID);
                will(returnValue(null));

                allowing(response).getID();
                will(returnValue(ENTITY_DES_ID));

                allowing(acsmetadataProvider).getMetadataFilename();
                will(returnValue(metadataFile));
            }
        });

        //instance.setMessage(response);
        instance.setMetadataProvider(acsmetadataProvider);
        instance.setIDPSSODescriptor();

    }

    @SuppressWarnings("unchecked")
    @Test
    public void setIDPSSODescriptorTest() throws SamlException, ResolverException {
        
        mockery.checking(new Expectations() {
            {
                one(response).getIssuer();
                will(returnValue(issuer));

                one(issuer).getValue();
                will(returnValue(ENTITY_DES_ID));

                allowing(acsmetadataProvider).resolveSingle(with(any(CriteriaSet.class)));
                will(returnValue(entityDescriptor));

                one(entityDescriptor).getIDPSSODescriptor(SUPPORTED_PROTOCOL);

            }
        });

        instance.setMetadataProvider(acsmetadataProvider);
        instance.setIDPSSODescriptor();
    }


    @Test
    public void setValidateAssertionTest() {
        instance.setValidatedAssertion(assertion);
    }

    @Test
    public void getValidateAssertionTest() {
        instance.getValidatedAssertion();
    }

    @Test
    public void getUserDataIfReadyTest() throws SamlException, ParserConfigurationException, SAXException, IOException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getProviderId();
                will(returnValue(PROVIDED_ID));

                one(assertion).getDOM();
                will(returnValue(finalElement));

                one(assertion).detach();
                one(assertion).getID();
                will(returnValue(PROVIDED_ID));

                one(assertion).getElementQName();
                will(returnValue(qName));

                one(assertion).getIssueInstant();
                will(returnValue(date));

                one(assertion).getOrderedChildren();

            }
        });
        instance.getUserDataIfReady();
    }

    @Test
    public void getDecrypterNullTest() throws SamlException, KeyStoreException, CertificateException {
        XMLObjectProviderRegistrySupport.getParserPool();
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                allowing(ssoService).getSignatureCertificate();
                will(returnValue(x509certificate));
            }
        });

        instance.getDecrypter();
    }

    @Test
    public void setDecrypterTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                allowing(ssoService).getSignatureCertificate();
                will(returnValue(x509certificate));
            }
        });
        instance.setDecrypter();
    }

    @Test
    public void getDecrypterTest() throws SamlException, KeyStoreException, CertificateException {
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getProviderName();
                will(returnValue(IDP_NAME));

                one(ssoService).getPrivateKey();
                will(returnValue(privateKey));
                
                allowing(ssoService).getSignatureCertificate();
                will(returnValue(certificate));
            }
        });

        instance.getDecrypter();
    }

    @Test
    public void setAndRemoveCachedRequestInfoTest() throws SamlException {
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getProviderName();
                will(returnValue(IDP_NAME));

                one(ssoService).getAcsCookieCache(IDP_NAME);
                will(returnValue(cache));

                one(cache).get(CACHE_VALUE);
                will(returnValue(requestInfo));

                one(cache).remove(CACHE_VALUE);
        
            }
        });

        instance.setAndRemoveCachedRequestInfo(RELAY_STATE, ssoRequest);
    }

    @Test(expected = SamlException.class)
    public void setAndRemoveCachedRequestInfoNullTest() throws SamlException {
        
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getProviderName();
                will(returnValue(IDP_NAME));

                one(ssoService).getAcsCookieCache(IDP_NAME);
                will(returnValue(cache));

                one(cache).get(CACHE_VALUE);
                will(returnValue(null));
                
                one(irutil).recreateHttpRequestInfo(RELAY_STATE, null, null, ssoService);
                will(throwException(new SamlException("")));
                             
            }
        });
        instance.irUtil = irutil;
        instance.setAndRemoveCachedRequestInfo(RELAY_STATE, ssoRequest);
    }

    @Test
    public void getChachedInfoRequestInfo() {
        instance.getCachedRequestInfo();
    }

    @Test
    public void getExternalRelayState() {
        instance.getExternalRelayState();
    }

}
