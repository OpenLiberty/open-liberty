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
package com.ibm.ws.security.saml.sso20.internal.utils;

import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.namespace.QName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;

import test.common.SharedOutputManager;

public class MsgCtxUtilTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final CommonMockObjects common = new CommonMockObjects();
    private final Mockery mockery = common.getMockery();

    private final BasicMessageContext<?, ?, ?> basicMessageContext = common.getBasicMessageContext();
    private final Issuer issuer = common.getIssuer();
    private final SsoConfig ssoConfig = common.getSsoConfig();
    private final ParserPool parserPool = mockery.mock(StaticBasicParserPool.class, "parserPool");
    private final Document document = mockery.mock(Document.class, "document");
    private final Element element = mockery.mock(Element.class, "element");
    private final Unmarshaller unmarshaller = mockery.mock(Unmarshaller.class, "unmarshaller");
    private final XMLObject mDataTemp = mockery.mock(XMLObject.class, "mDataTemp");

    private static final String PATH_IDP_METADATA = "IdpMetadata.xml";

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
    }

    @Before
    public void before() {
        Configuration.setParserPool(parserPool);
        mockery.checking(new Expectations() {
            {
                allowing(basicMessageContext).getSsoConfig();
                will(returnValue(ssoConfig));

                allowing(ssoConfig).getProviderId();
                will(returnValue(with(any(String.class))));

                one(mDataTemp).getElementQName();
                will(returnValue(new QName("test")));
                one(mDataTemp).getDOM();
                will(returnValue(null));
                one(mDataTemp).hasChildren();
                will(returnValue(false));
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        Configuration.setParserPool(null);
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testParseIdpMetadataProvider() throws XMLParserException, UnmarshallingException {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getIdpMetadata();
                will(returnValue("test" + File.separator + "resources" + File.separator + "IdpMetadata" + File.separator + PATH_IDP_METADATA));

                one(parserPool).parse(with(any(FileInputStream.class)));
                will(returnValue(document));

                one(document).getDocumentElement();
                will(returnValue(element));

                one(element).getAttributeNodeNS(with(any(String.class)), with(any(String.class)));
                will(returnValue(null));

                atMost(2).of(element).getNamespaceURI();
                will(returnValue("NamespaceURI"));

                atMost(2).of(element).getLocalName();
                will(returnValue("LocalName"));

                atMost(2).of(element).getPrefix();
                will(returnValue("Prefix"));

                one(unmarshaller).unmarshall(element);
                will(returnValue(mDataTemp));

                one(mDataTemp).releaseDOM();
                one(mDataTemp).releaseChildrenDOM(true);
            }
        });

        try {
            QName name = XMLHelper.getNodeQName(element);
            Configuration.getUnmarshallerFactory().registerUnmarshaller(name, unmarshaller);

            AcsDOMMetadataProvider result = MsgCtxUtil.parseIdpMetadataProvider(ssoConfig);
            assertTrue("A null value was received.", result != null);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testParseIdpMetadataProvider_NullStringIdpMetadata() {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getIdpMetadata();
                will(returnValue(null));
            }
        });

        try {
            AcsDOMMetadataProvider metadataProvider = MsgCtxUtil.parseIdpMetadataProvider(ssoConfig);
            assertTrue("Expected to receive a null value.", metadataProvider == null);
        } catch (SamlException e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    @Test
    public void testParseIdpMetadataProvider_ThrowsXMLParserException() throws XMLParserException {
        final XMLParserException pex = new XMLParserException();
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getIdpMetadata();
                will(returnValue("test" + File.separator + "resources" + File.separator + "IdpMetadata" + File.separator + PATH_IDP_METADATA));

                one(parserPool).parse(with(any(FileInputStream.class)));
                will(throwException(pex));
            }
        });

        try {
            MsgCtxUtil.parseIdpMetadataProvider(ssoConfig);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testParseIdpMetadataProvider_NullParserPool() throws XMLParserException {
        Configuration.setParserPool(null);
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getIdpMetadata();
                will(returnValue("test" + File.separator + "resources" + File.separator + "IdpMetadata" + File.separator + PATH_IDP_METADATA));
            }
        });

        try {
            MsgCtxUtil.parseIdpMetadataProvider(ssoConfig);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }

    }

    @Test
    public void testParseIdpMetadataProvider_ThrowsUnmarshallingException() throws XMLParserException, UnmarshallingException {
        final UnmarshallingException ue = new UnmarshallingException();
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getIdpMetadata();
                will(returnValue("test" + File.separator + "resources" + File.separator + "IdpMetadata" + File.separator + PATH_IDP_METADATA));

                one(parserPool).parse(with(any(FileInputStream.class)));
                will(returnValue(document));

                one(document).getDocumentElement();
                will(returnValue(element));

                one(element).getAttributeNodeNS(with(any(String.class)), with(any(String.class)));
                will(returnValue(null));

                atMost(2).of(element).getNamespaceURI();
                will(returnValue("NamespaceURI"));

                atMost(2).of(element).getLocalName();
                will(returnValue("LocalName"));

                atMost(2).of(element).getPrefix();
                will(returnValue("Prefix"));

                one(unmarshaller).unmarshall(element);
                will(throwException(ue));
            }
        });

        try {
            QName name = XMLHelper.getNodeQName(element);
            Configuration.getUnmarshallerFactory().registerUnmarshaller(name, unmarshaller);

            MsgCtxUtil.parseIdpMetadataProvider(ssoConfig);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testParseIdpMetadataProvider_InexistentFile() {
        mockery.checking(new Expectations() {
            {
                one(ssoConfig).getIdpMetadata();
                will(returnValue("inexistent_file"));
            }
        });

        try {
            AcsDOMMetadataProvider result = MsgCtxUtil.parseIdpMetadataProvider(ssoConfig);
            assertTrue("Expected to receive a null value.", result == null);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testTryTrustedIssuers() {
        final String ISSUER_VALUE = "issuerValue";
        final String[] trustedIssuers = { ISSUER_VALUE };

        mockery.checking(new Expectations() {
            {
                one(issuer).getValue();
                will(returnValue(ISSUER_VALUE));

                one(ssoConfig).getPkixTrustedIssuers();
                will(returnValue(trustedIssuers));
            }
        });

        boolean result = MsgCtxUtil.tryTrustedIssuers(issuer, basicMessageContext);
        assertTrue("Expected to receive a true value.", result == true);
    }
}
