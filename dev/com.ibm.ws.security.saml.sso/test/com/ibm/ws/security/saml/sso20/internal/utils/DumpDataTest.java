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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.xml.namespace.QName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.metadata.provider.DOMMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.XMLObject;
import org.w3c.dom.Element;

/**
 * Unit test for {@link DumpData} class.
 */
public class DumpDataTest {

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final States STATE = mockery.states("test-execution");

    // Mocked classes
    private static final Assertion ASSERTION_MCK = mockery
                    .mock(Assertion.class);
    private static final Subject SUBJECT_MCK = mockery.mock(Subject.class);
    private static final NameID NAME_ID_MCK = mockery.mock(NameID.class);
    private static final Issuer ISSUER_MCK = mockery.mock(Issuer.class);
    private static final Element ELEMENT_MCK = mockery.mock(Element.class);
    private static final XMLObject XML_OBJECT_MCK = mockery
                    .mock(XMLObject.class);
    private static final AuthnRequest AUTHN_REQUEST_MCK = mockery
                    .mock(AuthnRequest.class);
    private static final DOMMetadataProvider DOM_METADATA_PROVIDER = mockery
                    .mock(DOMMetadataProvider.class);
    private static final HttpServletRequest HTTP_SERVLET_REQUEST_MCK = mockery
                    .mock(HttpServletRequest.class);
    private static final Part PART_MCK = mockery.mock(Part.class);
    private static final HttpSession HTTP_SESSION_MCK = mockery
                    .mock(HttpSession.class);

    // Private constants
    private static final String SUBJECT_NAME_ID_VALUE = "_ce3d2948b4cf20146dee0a0b3dd6f69b6cf86f62d7";
    private static final String ISSUER_VALUE = "http://idp.example.com/metadata.php";
    private static final String ASSERTION_SIGNATURE_REFERENCE_ID = "#pfxd4d369e8-9ea1-780c-aff8-a1d11a9862a1";
    private static final String COMMON_MESSAGE_FOR_DUMP_ASSERTION =
                    "\nSubjectID(Username):" + SUBJECT_NAME_ID_VALUE + "\n" +
                                    "Issuer:" + ISSUER_VALUE + "\n" +
                                    "isSigned:" + true + " signReferenceId:" + ASSERTION_SIGNATURE_REFERENCE_ID + "\n" +
                                    "DOM:" + ELEMENT_MCK + " @" + ELEMENT_MCK.hashCode() + ")\n";
    private static final QName QNAME = new QName("http://example.com/ns/foo", "e", "example");
    private static final String SIGNATURE_REFERENCE_ID_VALUE = "#pfx41d8ef22-e612-8c50-9960-1b16f15741b3";
    private static final String PART_CONTENT_TYPE = "text/plain";
    private static final String PART_NAME = "PartName";
    private static final String HTTP_SERVLET_REQUEST_NULL_PARAMETER = "NullParameter";
    private static final String HTTP_SERVLET_REQUEST_TEST_PARAMETER = "TestParameter";
    private static final String HTTP_SERVLET_REQUEST_TEST_PARAMETER_VALUE = "TestValue";
    private static final String[] HTTP_SERVLET_REQUEST_NULL_PARAMETER_VALUE = null;
    private static final String HTTP_SERVLET_REQUEST_HEADER = "accepted";
    private static final String HTTP_SERVLET_REQUEST_HEADER_VALUE = "*/*";
    private static final String HTTP_SERVLET_REQUEST_SESSION = "httpSession";
    private static final String HTTP_SERVLET_REQUEST_METHOD = "POST";
    private static final String HTTP_SERVLET_REQUEST_CONTENT_TYPE_XML = "text/xml";
    private static final String HTTP_SERVLET_REQUEST_CONTENT_TYPE_FORM_DATA = "multipart/form-data";
    private static final String HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID = "1Nh745JQeAT9RZvrgwZTHX10yXEvthxbJ5MPMu9GUzY4rkyyN8 WL!-2084526730";
    private static final String HTTP_SERVLET_REQUEST_QUERY_STRING = "x=1&y=1";
    private static final String HTTP_SERVLET_REQUEST_URL = "http://localhost:9080/urlinfo";
    private static final Cookie[] COOKIES = { new Cookie("1", "1") };

    @AfterClass
    public static void tearDownClass() {
        mockery.assertIsSatisfied();
    }

    /**
     * This method test if the {@link DumpData#dumpAssertion(StringBuffer, Assertion, int)} method can
     * dump a child assertion invoking itself to do it. Fails if the method does
     * not return the expected String.
     */
    @Test
    public void dumpAssertionShouldInvokeItselfIfOneOrMoreChildOfAssertionIsAssertion() {
        final List<XMLObject> list = new ArrayList<XMLObject>();
        list.add(ASSERTION_MCK);

        // Parent assertion expectations
        loadAssertionExpectations();

        mockery.checking(new Expectations() {
            {
                // This add an Assertion to be used as a child element of the
                // parent.
                one(ASSERTION_MCK).getOrderedChildren();
                will(returnValue(list));
            }
        });

        // Child assertion expectations
        loadAssertionExpectations();

        mockery.checking(new Expectations() {
            {
                // This return an empty xml object list to define that there's
                // no other elements.
                one(ASSERTION_MCK).getOrderedChildren();
                will(returnValue(new ArrayList<XMLObject>()));
            }
        });

        String message = COMMON_MESSAGE_FOR_DUMP_ASSERTION;

        String messageToBeAppended = "**SubjectID(Username):"
                                     + SUBJECT_NAME_ID_VALUE + "\n" + "  Issuer:" + ISSUER_VALUE
                                     + "\n" + "  isSigned:" + true + " signReferenceId:"
                                     + ASSERTION_SIGNATURE_REFERENCE_ID + "\n" + "  DOM:"
                                     + ELEMENT_MCK + " @" + ELEMENT_MCK.hashCode() + ")\n";

        String expected = message + messageToBeAppended;

        Assert.assertEquals(expected,
                            DumpData.dumpAssertion(null, ASSERTION_MCK, 0).toString());
    }

    /**
     * This method test if the {@link DumpData#dumpAssertion(StringBuffer, Assertion, int)} avoid
     * dumping other child elements of the assertion that are not assertions.
     * Fails if the method does not return the expected String.
     */
    @Test
    public void dumpAssertionShouldNotInvokeItselfIfChildOfAssertionIsNotAssertion() {
        final List<XMLObject> list = new ArrayList<XMLObject>();
        list.add(XML_OBJECT_MCK);

        // Parent assertion expectations
        loadAssertionExpectations();

        mockery.checking(new Expectations() {
            {
                // This add an XmlObject to be used as a child element of the
                // parent.
                one(ASSERTION_MCK).getOrderedChildren();
                will(returnValue(list));
            }
        });

        String expected = COMMON_MESSAGE_FOR_DUMP_ASSERTION;

        Assert.assertEquals(expected,
                            DumpData.dumpAssertion(null, ASSERTION_MCK, 0).toString());
    }

    /**
     * This method test if the {@link DumpData#dumpMetadata(DOMMetadataProvider)} method catch a {@link MetadataProviderException}. Fails if the exception is not handled
     * or if the message is not equals as the expected.
     */
    @Test
    public void dumpMetadataShouldCatchMetadataProviderException()
                    throws MetadataProviderException {

        mockery.checking(new Expectations() {
            {
                one(DOM_METADATA_PROVIDER).getMetadata();
                will(throwException((new MetadataProviderException())));
            }
        });

        Assert.assertEquals("\nfound an null XMLObject\n", DumpData
                        .dumpMetadata(DOM_METADATA_PROVIDER).toString());

    }

    /**
     * This method test if the {@link DumpData#dumpMetadata(DOMMetadataProvider)} method can dump a
     * metadata. Fails if the the message is not equals as the expected.
     */
    @Test
    public void dumpMetadataShouldDumpMetadataXmlObject()
                    throws MetadataProviderException {

        mockery.checking(new Expectations() {
            {
                one(DOM_METADATA_PROVIDER).getMetadata();
                will(returnValue(XML_OBJECT_MCK));
                one(XML_OBJECT_MCK).getElementQName();
                will(returnValue(QNAME));

                one(XML_OBJECT_MCK).getDOM();
                will(returnValue(ELEMENT_MCK));

                one(XML_OBJECT_MCK).hasChildren();
                will(returnValue(false));
            }
        });

        String message = "\n" + QNAME.getPrefix() + ":" + QNAME.getLocalPart()
                         + "(" + QNAME.getNamespaceURI() + ")\n";
        String messageToBeAppended = "DOM:" + ELEMENT_MCK + " @"
                                     + ELEMENT_MCK.hashCode() + ")\n";

        String expected = message + messageToBeAppended;

        Assert.assertEquals(expected,
                            DumpData.dumpMetadata(DOM_METADATA_PROVIDER).toString());

    }

    /**
     * This method test if the {@link DumpData#dumpRequestInfo(HttpServletRequest)} method append a
     * message if an exception is throw. Fails if the message does not contains
     * the expected text.
     */
    @Test
    public void dumpRequestInfoShouldAppendExceptionMessage() {
        STATE.become("dumpRequestInfoShouldAppendExceptionMessage");

        mockery.checking(new Expectations() {
            {
                allowing(HTTP_SERVLET_REQUEST_MCK);
                will(throwException(new Exception()));
                when(STATE.is("dumpRequestInfoShouldAppendExceptionMessage"));
            }
        });

        Assert.assertTrue(DumpData.dumpRequestInfo(HTTP_SERVLET_REQUEST_MCK)
                        .contains("Hit unexpect exception:"));

        STATE.become("test-execution");
    }

    /**
     * This method test if the {@link DumpData#dumpRequestInfo(HttpServletRequest)} method append a
     * message if the content type of the HttpServletRequest is equals to
     * multipart/form-data. Fails if the message does not contains the expected
     * text.
     */
    @Test
    public void dumpRequestInfoShouldAppendMessageIfHttpServletRequestContentTypeIsMultipartFormData()
                    throws IOException, ServletException {

        final List<Part> parts = new ArrayList<Part>();
        parts.add(PART_MCK);

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getCookies();
                will(returnValue(null));
                one(HTTP_SERVLET_REQUEST_MCK).getHeaderNames();
                will(returnValue(null));
                one(HTTP_SERVLET_REQUEST_MCK).getParameterNames();
                will(returnValue(null));

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_URL)));
                one(HTTP_SERVLET_REQUEST_MCK).getQueryString();
                will(returnValue(HTTP_SERVLET_REQUEST_QUERY_STRING));
                one(HTTP_SERVLET_REQUEST_MCK).getRequestedSessionId();
                will(returnValue(HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID));
                one(HTTP_SERVLET_REQUEST_MCK).getSession(true);
                will(returnValue(HTTP_SESSION_MCK));
                one(HTTP_SERVLET_REQUEST_MCK).getContentType();
                will(returnValue(HTTP_SERVLET_REQUEST_CONTENT_TYPE_FORM_DATA));

                one(HTTP_SERVLET_REQUEST_MCK).getParts();
                will(returnValue(parts));
                one(PART_MCK).getName();
                will(returnValue(PART_NAME));
                one(PART_MCK).getContentType();
                will(returnValue(PART_CONTENT_TYPE));

                one(HTTP_SERVLET_REQUEST_MCK).getMethod();
                will(returnValue(HTTP_SERVLET_REQUEST_METHOD));
            }
        });

        String messageToBeAppended = "part:" + PART_NAME + "->"
                                     + PART_CONTENT_TYPE + "\n";

        String message = "requestUrl:" + HTTP_SERVLET_REQUEST_URL + "\n"
                         + "queryString:" + HTTP_SERVLET_REQUEST_QUERY_STRING + "\n"
                         + "sessionId:" + HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID
                         + "\n" + "session:" + HTTP_SERVLET_REQUEST_SESSION + "\n"
                         + "contentType:" + HTTP_SERVLET_REQUEST_CONTENT_TYPE_FORM_DATA
                         + "\n" + "method:" + HTTP_SERVLET_REQUEST_METHOD + "\n";
        String expected = message + messageToBeAppended;

        Assert.assertEquals(expected,
                            DumpData.dumpRequestInfo(HTTP_SERVLET_REQUEST_MCK));
    }

    /**
     * This method test if the {@link DumpData#dumpRequestInfo(HttpServletRequest)} method append a
     * message if the HttpServletRequest has cookies. Fails if the message is
     * not equals as the expected.
     */
    @Test
    public void dumpRequestInfoShouldAppendMessageIfHttpServletRequestHasCookies() {
        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getCookies();
                will(returnValue(COOKIES));
                one(HTTP_SERVLET_REQUEST_MCK).getHeaderNames();
                will(returnValue(null));
                one(HTTP_SERVLET_REQUEST_MCK).getParameterNames();
                will(returnValue(null));

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_URL)));
                one(HTTP_SERVLET_REQUEST_MCK).getQueryString();
                will(returnValue(HTTP_SERVLET_REQUEST_QUERY_STRING));
                one(HTTP_SERVLET_REQUEST_MCK).getRequestedSessionId();
                will(returnValue(HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID));
                one(HTTP_SERVLET_REQUEST_MCK).getSession(true);
                will(returnValue(HTTP_SESSION_MCK));
                one(HTTP_SERVLET_REQUEST_MCK).getContentType();
                will(returnValue(HTTP_SERVLET_REQUEST_CONTENT_TYPE_XML));
                one(HTTP_SERVLET_REQUEST_MCK).getMethod();
                will(returnValue(HTTP_SERVLET_REQUEST_METHOD));
            }
        });

        String messageToBeAppended = "cookie " + COOKIES[0] + "\n";

        String message = "requestUrl:" + HTTP_SERVLET_REQUEST_URL + "\n"
                         + "queryString:" + HTTP_SERVLET_REQUEST_QUERY_STRING + "\n"
                         + "sessionId:" + HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID
                         + "\n" + "session:" + HTTP_SERVLET_REQUEST_SESSION + "\n"
                         + "contentType:" + HTTP_SERVLET_REQUEST_CONTENT_TYPE_XML + "\n"
                         + "method:" + HTTP_SERVLET_REQUEST_METHOD + "\n";
        String expected = messageToBeAppended + message;

        Assert.assertEquals(expected,
                            DumpData.dumpRequestInfo(HTTP_SERVLET_REQUEST_MCK));
    }

    /**
     * This method test if the {@link DumpData#dumpRequestInfo(HttpServletRequest)} method append a
     * message if the HttpServletRequest has header names. Fails if the message
     * is not equals as the expected..
     */
    @Test
    public void dumpRequestInfoShouldAppendMessageIfHttpServletRequestHasHeaderNames() {
        List<String> list = new ArrayList<String>();
        list.add(HTTP_SERVLET_REQUEST_HEADER);

        final Enumeration<String> enumeration = Collections.enumeration(list);

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getCookies();
                will(returnValue(null));
                one(HTTP_SERVLET_REQUEST_MCK).getHeaderNames();
                will(returnValue(enumeration));

                one(HTTP_SERVLET_REQUEST_MCK)
                                .getHeader(with(any(String.class)));
                will(returnValue(HTTP_SERVLET_REQUEST_HEADER_VALUE));

                one(HTTP_SERVLET_REQUEST_MCK).getParameterNames();
                will(returnValue(null));

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_URL)));
                one(HTTP_SERVLET_REQUEST_MCK).getQueryString();
                will(returnValue(HTTP_SERVLET_REQUEST_QUERY_STRING));
                one(HTTP_SERVLET_REQUEST_MCK).getRequestedSessionId();
                will(returnValue(HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID));
                one(HTTP_SERVLET_REQUEST_MCK).getSession(true);
                will(returnValue(HTTP_SESSION_MCK));
                one(HTTP_SERVLET_REQUEST_MCK).getContentType();
                will(returnValue(HTTP_SERVLET_REQUEST_CONTENT_TYPE_XML));
                one(HTTP_SERVLET_REQUEST_MCK).getMethod();
                will(returnValue(HTTP_SERVLET_REQUEST_METHOD));
            }
        });

        String messageToBeAppended = "header " + HTTP_SERVLET_REQUEST_HEADER
                                     + ":" + HTTP_SERVLET_REQUEST_HEADER_VALUE + "\n";

        String message = "requestUrl:" + HTTP_SERVLET_REQUEST_URL + "\n"
                         + "queryString:" + HTTP_SERVLET_REQUEST_QUERY_STRING + "\n"
                         + "sessionId:" + HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID
                         + "\n" + "session:" + HTTP_SERVLET_REQUEST_SESSION + "\n"
                         + "contentType:" + HTTP_SERVLET_REQUEST_CONTENT_TYPE_XML + "\n"
                         + "method:" + HTTP_SERVLET_REQUEST_METHOD + "\n";
        String expected = messageToBeAppended + message;

        Assert.assertEquals(expected,
                            DumpData.dumpRequestInfo(HTTP_SERVLET_REQUEST_MCK));
    }

    /**
     * This method test if the {@link DumpData#dumpRequestInfo(HttpServletRequest)} method append a
     * message if the HttpServletRequest has parameter names. Fails if the
     * message is not equals as the expected.
     */
    @Test
    public void dumpRequestInfoShouldAppendMessageIfHttpServletRequestHasParameterNames() {
        // This list is used to verify the two different messages to append
        List<String> list = new ArrayList<String>();
        list.add(HTTP_SERVLET_REQUEST_NULL_PARAMETER);
        list.add(HTTP_SERVLET_REQUEST_TEST_PARAMETER);

        final Enumeration<String> enumeration = Collections.enumeration(list);

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getCookies();
                will(returnValue(null));
                one(HTTP_SERVLET_REQUEST_MCK).getHeaderNames();
                will(returnValue(null));

                one(HTTP_SERVLET_REQUEST_MCK).getParameterNames();
                will(returnValue(enumeration));

                one(HTTP_SERVLET_REQUEST_MCK).getParameterValues(
                                                                 with(any(String.class)));
                will(returnValue(HTTP_SERVLET_REQUEST_NULL_PARAMETER_VALUE));

                one(HTTP_SERVLET_REQUEST_MCK).getParameterValues(
                                                                 with(any(String.class)));
                will(returnValue(new String[] { HTTP_SERVLET_REQUEST_TEST_PARAMETER_VALUE }));

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_URL)));
                one(HTTP_SERVLET_REQUEST_MCK).getQueryString();
                will(returnValue(HTTP_SERVLET_REQUEST_QUERY_STRING));
                one(HTTP_SERVLET_REQUEST_MCK).getRequestedSessionId();
                will(returnValue(HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID));
                one(HTTP_SERVLET_REQUEST_MCK).getSession(true);
                will(returnValue(HTTP_SESSION_MCK));
                one(HTTP_SERVLET_REQUEST_MCK).getContentType();
                will(returnValue(HTTP_SERVLET_REQUEST_CONTENT_TYPE_XML));
                one(HTTP_SERVLET_REQUEST_MCK).getMethod();
                will(returnValue(HTTP_SERVLET_REQUEST_METHOD));

            }
        });

        String messageToBeAppended = "parameter "
                                     + HTTP_SERVLET_REQUEST_NULL_PARAMETER + ":null or empty\n"
                                     + "parameter " + HTTP_SERVLET_REQUEST_TEST_PARAMETER + ":"
                                     + HTTP_SERVLET_REQUEST_TEST_PARAMETER_VALUE + "\n";

        String message = "requestUrl:" + HTTP_SERVLET_REQUEST_URL + "\n"
                         + "queryString:" + HTTP_SERVLET_REQUEST_QUERY_STRING + "\n"
                         + "sessionId:" + HTTP_SERVLET_REQUEST_REQUESTED_SESSION_ID
                         + "\n" + "session:" + HTTP_SERVLET_REQUEST_SESSION + "\n"
                         + "contentType:" + HTTP_SERVLET_REQUEST_CONTENT_TYPE_XML + "\n"
                         + "method:" + HTTP_SERVLET_REQUEST_METHOD + "\n";
        String expected = messageToBeAppended + message;

        Assert.assertEquals(expected,
                            DumpData.dumpRequestInfo(HTTP_SERVLET_REQUEST_MCK));
    }

    /**
     * This method test if the {@link DumpData#dumpXMLObject(StringBuffer, XMLObject, int)} method
     * append a message if the DOM of the xml object is not null. Fail if the
     * message cannot be found.
     */
    @Test
    public void dumpXmlObjectShouldAppendMessageIfDomOfXmlObjectIsNotNull() {
        mockery.checking(new Expectations() {
            {
                one(XML_OBJECT_MCK).getElementQName();
                will(returnValue(QNAME));

                one(XML_OBJECT_MCK).getDOM();
                will(returnValue(ELEMENT_MCK));

                one(XML_OBJECT_MCK).hasChildren();
                will(returnValue(false));
            }
        });

        String message = "\n" + QNAME.getPrefix() + ":" + QNAME.getLocalPart()
                         + "(" + QNAME.getNamespaceURI() + ")\n";
        String messageToBeAppended = "DOM:" + ELEMENT_MCK + " @"
                                     + ELEMENT_MCK.hashCode() + ")\n";

        String expected = message + messageToBeAppended;

        Assert.assertEquals(expected,
                            DumpData.dumpXMLObject(null, XML_OBJECT_MCK, 0).toString());
    }

    /**
     * This method verify if {@link DumpData#dumpXMLObject(StringBuffer, XMLObject, int)} method
     * append a message when {@link XMLObject#getDOM()} return a null reference.
     * Fails if it's not appended the message.
     */
    @Test
    public void dumpXmlObjectShouldAppendMessageIfDomOfXmlObjectIsNull() {
        mockery.checking(new Expectations() {
            {
                one(XML_OBJECT_MCK).getElementQName();
                will(returnValue(QNAME));

                one(XML_OBJECT_MCK).getDOM();
                will(returnValue(null));
                one(XML_OBJECT_MCK).hasChildren();
                will(returnValue(false));

            }
        });

        String message = "\n" + QNAME.getPrefix() + ":" + QNAME.getLocalPart()
                         + "(" + QNAME.getNamespaceURI() + ")\n";
        String messageToBeAppended = "DOM is null";

        String expected = message + messageToBeAppended;

        Assert.assertEquals(expected,
                            DumpData.dumpXMLObject(null, XML_OBJECT_MCK, 0).toString());

    }

    /**
     * This method test if the {@link DumpData#dumpXMLObject(StringBuffer, XMLObject, int)} method
     * append a message if the xml object has children. Fail if the message is
     * not equals as the expected.
     */
    @Test
    public void dumpXmlObjectShouldAppendMessageIfXmlObjectHasChildren() {
        final List<XMLObject> list = new ArrayList<XMLObject>();
        list.add(null);

        mockery.checking(new Expectations() {
            {
                one(XML_OBJECT_MCK).getElementQName();
                will(returnValue(QNAME));

                one(XML_OBJECT_MCK).getDOM();
                will(returnValue(ELEMENT_MCK));

                one(XML_OBJECT_MCK).hasChildren();
                will(returnValue(true));

                // Adding a new xml object as a child
                one(XML_OBJECT_MCK).getOrderedChildren();
                will(returnValue(list));
            }
        });

        String message = "\n" + QNAME.getPrefix() + ":" + QNAME.getLocalPart()
                         + "(" + QNAME.getNamespaceURI() + ")\n" + "DOM:" + ELEMENT_MCK
                         + " @" + ELEMENT_MCK.hashCode() + ")\n";

        String messageToBeAppended = "==found an null XMLObject\n";

        String expected = message + messageToBeAppended;

        Assert.assertEquals(expected,
                            DumpData.dumpXMLObject(null, XML_OBJECT_MCK, 0).toString());
    }

    /**
     * This method test if the {@link DumpData#dumpXMLObject(StringBuffer, XMLObject, int)} method
     * append a message if the xml object used as parameter is an instance of
     * SignableSAMLObject. Fail if the message is not equals as the expected.
     */
    @Test
    public void dumpXmlObjectShouldAppendMessageIfXmlObjectIsInstanceOfSignableSamlObject() {
        mockery.checking(new Expectations() {
            {
                one(AUTHN_REQUEST_MCK).getElementQName();
                will(returnValue(QNAME));

                one(AUTHN_REQUEST_MCK).getDOM();
                will(returnValue(ELEMENT_MCK));

                one(AUTHN_REQUEST_MCK).isSigned();
                will(returnValue(true));

                one(AUTHN_REQUEST_MCK).getSignatureReferenceID();
                will(returnValue(SIGNATURE_REFERENCE_ID_VALUE));

                one(AUTHN_REQUEST_MCK).hasChildren();
                will(returnValue(false));
            }
        });

        String message = "\n" + QNAME.getPrefix() + ":" + QNAME.getLocalPart()
                         + "(" + QNAME.getNamespaceURI() + ")\n" + "DOM:" + ELEMENT_MCK
                         + " @" + ELEMENT_MCK.hashCode() + ")\n";

        String messageToBeAppended = "isSigned:" + true + " id:"
                                     + SIGNATURE_REFERENCE_ID_VALUE + ")\n";

        String expected = message + messageToBeAppended;

        Assert.assertEquals(expected,
                            DumpData.dumpXMLObject(null, AUTHN_REQUEST_MCK, 0).toString());
    }

    /**
     * This method verify if {@link DumpData#dumpXMLObject(StringBuffer, XMLObject, int)} method
     * append a message when a null XmlObject is provided as parameter. Fails if
     * it's not appended the message.
     */
    @Test
    public void dumpXmlObjectShouldReturnMessageIfXmlObjectIsNull() {
        Assert.assertEquals("\nfound an null XMLObject\n", DumpData
                        .dumpXMLObject(null, null, 0).toString());
    }

    /**
     * This method verify if {@link DumpData#identString(int, boolean)} method
     * does not perform any indentation if the value of the indentation
     * parameter equals zero.
     */
    @Test
    public void identStringShouldReturnEmptyIndentationIfIdentIsEqualsToZero() {
        String indentation = DumpData.identString(0, false);

        Assert.assertEquals(0, indentation.length());
    }

    /**
     * This method verify if {@link DumpData#identString(int, boolean)} method
     * generate an space based and asterisk indentation. Fails if it does not
     * return the expected characters.
     */
    @Test
    public void identStringShouldReturnIndentationWithAsterisk() {
        // Create indentation of 6 characters
        String indentation = DumpData.identString(3, true);

        Assert.assertTrue(indentation.contentEquals(new StringBuffer("    **")));
        Assert.assertEquals(6, indentation.length());
    }

    /**
     * This method verify if {@link DumpData#identString(int, boolean)} method
     * generate an space based and equals indentation. Fails if it does not
     * return the expected characters.
     */
    @Test
    public void identStringShouldReturnIndentationWithEquals() {
        // Create indentation of 6 characters
        String indentation = DumpData.identString(3, false);

        Assert.assertTrue(indentation.contentEquals(new StringBuffer("    ==")));
        Assert.assertEquals(6, indentation.length());
    }

    /**
     * This method verify if {@link DumpData#identString(int)} method generate
     * an space based indentation. Fails if it does not return the expected
     * spaces.
     */
    @Test
    public void identStringWithSpacesShouldReturnIndentation() {
        // Create indentation of 6 characters
        String indentation = DumpData.identString(3);

        Assert.assertTrue(indentation.contentEquals(new StringBuffer("      ")));
        Assert.assertEquals(6, indentation.length());
    }

    /**
     * Load assertions for the {@link DumpDataTest#dumpAssertionShouldInvokeItselfIfOneOrMoreChildrenOfAssertionAreAssertion()} method.
     */
    private void loadAssertionExpectations() {
        mockery.checking(new Expectations() {
            {
                one(ASSERTION_MCK).getSubject();
                will(returnValue(SUBJECT_MCK));
                one(SUBJECT_MCK).getNameID();
                will(returnValue(NAME_ID_MCK));
                one(NAME_ID_MCK).getValue();
                will(returnValue(SUBJECT_NAME_ID_VALUE));

                one(ASSERTION_MCK).getIssuer();
                will(returnValue(ISSUER_MCK));
                one(ISSUER_MCK).getValue();
                will(returnValue(ISSUER_VALUE));

                one(ASSERTION_MCK).isSigned();
                will(returnValue(true));
                one(ASSERTION_MCK).getSignatureReferenceID();
                will(returnValue(ASSERTION_SIGNATURE_REFERENCE_ID));

                exactly(2).of(ASSERTION_MCK).getDOM();
                will(returnValue(ELEMENT_MCK));
            }
        });
    }
}
