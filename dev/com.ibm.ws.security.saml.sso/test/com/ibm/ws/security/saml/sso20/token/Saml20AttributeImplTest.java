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
package com.ibm.ws.security.saml.sso20.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.Namespace;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 */
public class Saml20AttributeImplTest {

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final static String ATTR_NAME = "urn:oid:1.3.6.1.4.1.5923.1.1.1.1";
    private final static String ATTR_NAME_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
    private final static String ATTR_FRIENDLY_NAME = "eduPersonAffiliation";

    private final static String NAMESPACE_URI = "urn:oasis:names:tc:SAML:2.0:assertion";
    private final static String NAMESPACE_PREFIX = "saml";
    private final static String LOCAL_NAME = "Attribute";

    private final static List<String> VALUES_AS_STRING = Arrays.asList("\nmember\nstaff\n");

    private final Attribute Attribute = mockery.mock(Attribute.class);
    private static final List<XMLObject> attributesValues = new ArrayList<XMLObject>();
    private final Namespace Namespace = mockery.mock(Namespace.class);
    @SuppressWarnings("rawtypes")
    private final Set set = new HashSet();

    final static String xmlAttribute = "<saml:Assertion\r\n" +
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

    final String serializedValueExpected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><saml:Attribute xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" FriendlyName=\"eduPersonAffiliation\" Name=\"urn:oid:1.3.6.1.4.1.5923.1.1.1.1\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\" x500:Encoding=\"LDAP\" xmlns:x500=\"urn:oasis:names:tc:SAML:2.0:profiles:attribute:X500\">\r\n"
                                           +
                                           "<saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">member</saml:AttributeValue>\r\n"
                                           +
                                           "<saml:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">staff</saml:AttributeValue>"
                                           + "\r\n" +
                                           "</saml:Attribute>";

    static Document document = null;
    static NodeList nodes = null;
    static Element element = null;

    @BeforeClass
    public static void setUp() throws ParserConfigurationException, SAXException, IOException {
        document = loadXML(xmlAttribute);
        nodes = document.getElementsByTagName("saml:Attribute");
        element = (Element) nodes.item(0);
    }

    public static Document loadXML(String xmlString) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource is = new InputSource(new StringReader(xmlString));
        Document doc = builder.parse(is);

        return doc;
    }

    @SuppressWarnings("unchecked")
    public void initTest() throws ParserConfigurationException, SAXException, IOException {

        mockery.checking(new Expectations() {
            {

                one(Attribute).getName();
                will(returnValue(element.getAttribute("Name")));

                one(Attribute).getNameFormat();
                will(returnValue(element.getAttribute("NameFormat")));

                one(Attribute).getFriendlyName();
                will(returnValue(element.getAttribute("FriendlyName")));

                attributesValues.add(Attribute);
                one(Attribute).getAttributeValues();
                will(returnValue(attributesValues));

                QName QName = XSString.TYPE_NAME;
                one(Attribute).getSchemaType();
                will(returnValue(QName));

                set.add(Namespace);
                one(Attribute).getNamespaces();
                will(returnValue(set));

                one(Namespace).getNamespaceURI();
                will(returnValue(element.getNamespaceURI()));
                one(Namespace).getNamespacePrefix();
                will(returnValue(element.getPrefix()));

                one(Attribute).getAttributeValues();
                will(returnValue(attributesValues));

                one(Attribute).getDOM();
                will(returnValue(element));

                one(Attribute).getDOM();
                will(returnValue(element));

            }
        });

        assertEquals("Namespace URI is is not the expected one.", NAMESPACE_URI, element.getNamespaceURI());
        assertEquals("Local name is is not the expected one.", LOCAL_NAME, element.getLocalName());
        assertEquals("Prefix is is not the expected one.", NAMESPACE_PREFIX, element.getPrefix());

    }

    @Test
    public void constructorAttributeTest() throws ParserConfigurationException, SAXException, IOException {
        mockery.checking(new Expectations() {
            {
                try {

                    one(Attribute).detach();
                    one(Attribute).getDOM();
                    will(returnValue(element));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        initTest();

        final Saml20AttributeImpl Saml20AttributeImpl = new Saml20AttributeImpl(Attribute);
        assertNotNull("There must be an attribute", Saml20AttributeImpl);

        assertEquals("Name is not the expected one.", ATTR_NAME, Saml20AttributeImpl.getName());
        assertEquals("Name format is not the expected one.", ATTR_NAME_FORMAT, Saml20AttributeImpl.getNameFormat());
        assertEquals("Friendly name is not the expected one.", ATTR_FRIENDLY_NAME, Saml20AttributeImpl.getFriendlyName());
        assertEquals("String values are not the expected.", VALUES_AS_STRING, Saml20AttributeImpl.getValuesAsString());

        Document serializedValue = null;
        serializedValue = loadXML(Saml20AttributeImpl.getSerializedValues().get(0));
        Document expectedValue = null;
        expectedValue = loadXML(serializedValueExpected);

        assertTrue("Serialized is not the expected one.", expectedValue.isEqualNode(serializedValue));
    }

}
