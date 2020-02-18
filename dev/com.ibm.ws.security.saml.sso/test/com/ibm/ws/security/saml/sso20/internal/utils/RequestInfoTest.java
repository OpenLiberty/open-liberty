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

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

/**
 * Unit test for the {@link ForwardRequestInfo} class.
 */
public class RequestInfoTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    public final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    //Mocked classes
    private final HttpServletRequest HTTP_SERVLET_REQUEST_MCK = mockery.mock(SRTServletRequest.class);
    private final IExtendedRequest IEXTENDED_REQUEST_MCK = mockery.mock(IExtendedRequest.class);
    private final HttpServletResponse HTTP_SERVLET_RESPONSE_MCK = mockery.mock(HttpServletResponse.class);
    private final ForwardRequestInfo REQUEST_INFO_MCK = mockery.mock(ForwardRequestInfo.class);

    //Constants
    private static final String HTTP_SERVLET_REQUEST_CONTENT_TYPE = "text/html";
    private static final String HTTP_SERVLET_REQUEST_QUERY_STRING = "value=1";
    private static final String HTTP_SERVLET_REQUEST_REQUEST_URL = "http://localhost:8080/Test";
    private static final String HTTP_SERVLET_REQUEST_REQUEST_FRAGMENT = "#print";
    private static final String HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_FRAGMENT = HTTP_SERVLET_REQUEST_REQUEST_URL + HTTP_SERVLET_REQUEST_REQUEST_FRAGMENT;
    private static final String HTTP_SERVLET_REQUEST_URL_WITH_FRAGMENT = HTTP_SERVLET_REQUEST_REQUEST_URL + HTTP_SERVLET_REQUEST_REQUEST_FRAGMENT;
    private static final String HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY = HTTP_SERVLET_REQUEST_REQUEST_URL + "?" + HTTP_SERVLET_REQUEST_QUERY_STRING;
    private static final String NEW_HTTP_SERVLET_REQUEST_REQUEST_URL = HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY + "&test=true";
    private static final String HTTP_SERVLET_REQUEST_UNKNOWN_METHOD = "UNKNOWN";
    private static final HashMap map = new HashMap();

    @BeforeClass
    public static void setUp() throws Exception {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDownClass() {
        //mockery.assertIsSatisfied();
        outputMgr.trace("*=all=disabled");
    }

    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    /**
     * Test that {@link ForwardRequestInfo#RequestInfo(HttpServletRequest)} should be debugged if the method of the {@link HttpServletRequest} is unknown.
     * Fails if it does not set the <code>requestUrl, method and strInResponseToId</code> fields.
     */
    @Test
    public void constructorWithHttpServletRequestShouldDebugIfRequestMethodIsUnknown() {
        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY)));

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();//
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY)));//
                one(HTTP_SERVLET_REQUEST_MCK).getQueryString();//
                will(returnValue(HTTP_SERVLET_REQUEST_QUERY_STRING));//

                one(HTTP_SERVLET_REQUEST_MCK).getMethod();
                will(returnValue(HTTP_SERVLET_REQUEST_UNKNOWN_METHOD));

                allowing(HTTP_SERVLET_REQUEST_MCK).setAttribute("SpSLOInProgress", "true");
                allowing(HTTP_SERVLET_REQUEST_MCK).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        HttpRequestInfo requestInfo = null;
        try {
            requestInfo = new HttpRequestInfo(HTTP_SERVLET_REQUEST_MCK);
        } catch (SamlException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY.toString(), requestInfo.getRequestUrl());
        Assert.assertEquals(HTTP_SERVLET_REQUEST_UNKNOWN_METHOD, requestInfo.method); //No getter for method field
        Assert.assertEquals(33, requestInfo.getInResponseToId().length()); //Should be any string with 33 characters
    }

    /**
     * Test that {@link ForwardRequestInfo#RequestInfo(HttpServletRequest)} should invoke the {@link ForwardRequestInfo#initGet(HttpServletRequest)} method.
     * Fails if it does not set the <code>requestUrl, method, strInResponseToId and queryString</code> fields
     */
    @Test
    public void constructorWithHttpServletRequestShouldInvokeInitGetMethod() {
        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY)));

                one(HTTP_SERVLET_REQUEST_MCK).getRequestURL();//
                will(returnValue(new StringBuffer(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY)));//
                one(HTTP_SERVLET_REQUEST_MCK).getQueryString();//
                will(returnValue(HTTP_SERVLET_REQUEST_QUERY_STRING));//

                one(HTTP_SERVLET_REQUEST_MCK).getMethod();
                will(returnValue(ForwardRequestInfo.METHOD_GET));
                allowing(HTTP_SERVLET_REQUEST_MCK).setAttribute("SpSLOInProgress", "true");
                allowing(HTTP_SERVLET_REQUEST_MCK).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
            }
        });

        HttpRequestInfo requestInfo = null;
        try {
            requestInfo = new HttpRequestInfo(HTTP_SERVLET_REQUEST_MCK);
        } catch (SamlException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY.toString(), requestInfo.getRequestUrl());
        Assert.assertEquals(ForwardRequestInfo.METHOD_GET, requestInfo.method); //No getter for method field
        Assert.assertEquals(33, requestInfo.getInResponseToId().length()); //Should be any string with 33 characters
    }

    /**
     * Test if {@link ForwardRequestInfo#RequestInfo(String)} set the <code>method and requestUrl</code> fields.
     */
    @Test
    public void constructorWithRequestUri() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        Assert.assertEquals(ForwardRequestInfo.METHOD_POST, requestInfo.method);
        Assert.assertEquals(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY, requestInfo.getRequestUrl());
    }

    /**
     * Test if {@link ForwardRequestInfo#RequestInfo(String, String)} set the <code>method, requestUrl and queryString</code> fields.
     */
    @Test
    public void constructorWithRequestUrlAndQueryStringShouldSetFields() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY, HTTP_SERVLET_REQUEST_QUERY_STRING);

        Assert.assertEquals(ForwardRequestInfo.METHOD_GET, requestInfo.method);
        Assert.assertEquals(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY, requestInfo.getRequestUrl());
        Assert.assertEquals(HTTP_SERVLET_REQUEST_QUERY_STRING, requestInfo.getQueryString());
    }

    /**
     * Test if {@link ForwardRequestInfo#getFragmentCookieId()} initialize the <code>fragmentCookieId</code> field with a Random String with 8 characters if it hasn't been
     * initialized.
     * Fails if the returned value is null or if it has more than 8 characters.
     */
    @Test
    public void getFragmentCookieIdShouldGenerateFragmentCookieIdIfNull() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        Assert.assertNotNull(requestInfo.getFragmentCookieId());

        Assert.assertEquals(8, requestInfo.getFragmentCookieId().length());
    }

    /**
     * Test that {@link ForwardRequestInfo#getNewArray(String[])} returns a new array with the values of the provided.
     * Fails if does not return the expected array.
     */
    @Test
    public void getNewArrayShouldReturnNewArrayFromProvidedStringArray() throws UnsupportedEncodingException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        String[] array = { "a", "b", "c" };
        String[] expectedAarray = { "a", "b", "c", null };

        Assert.assertEquals(4, requestInfo.getNewArray(array).length);
        Assert.assertArrayEquals(expectedAarray, requestInfo.getNewArray(array));

    }

    /**
     * Test that {@link ForwardRequestInfo#getNewArray(String[])} returns a new array when the provided parameter is null.
     * Fails if the array length is different to 1 and the item is not null.
     */
    @Test
    public void getNewArrayShouldReturnNewArrayIfProvidedParameterIsNull() throws UnsupportedEncodingException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        Assert.assertEquals(1, requestInfo.getNewArray(null).length);
        Assert.assertNull(requestInfo.getNewArray(null)[0]);
    }

    /**
     * Test that {@link ForwardRequestInfo#getNewArray(String[])} return an array as follow:
     * <li>If the key is found in the parameters field, then it will create a new array with all the items in that array and append the new one provided in the value
     * parameter.</li>
     */
    @Test
    public void getStringArrayShouldAppendNewItemInArray() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        //Setting a parameter to the parameters field
        requestInfo.setParameter("a", new String[] { "1" });

        //getStringArray method return an array with the values of the key (if it's found) plus the one to be added.
        Assert.assertArrayEquals(new String[] { "1", "2" }, requestInfo.getStringArray("a", "2"));

    }

    /**
     * Test that {@link ForwardRequestInfo#handleFragmentCookies()} return the expected String.
     */
    @Test
    public void handleFragmentCookiesShouldReturnJavascriptString() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        String expected = "\n<SCRIPT type=\"TEXT/JAVASCRIPT\" language=\"JavaScript\">\n" +
                          "document.cookie = '" + Constants.COOKIE_NAME_SAML_FRAGMENT +
                          requestInfo.getFragmentCookieId() +
                          "=' + encodeURIComponent(window.location.href) + '; Path=/;" +
                          "';\n</SCRIPT>\n";

        Assert.assertEquals(expected, requestInfo.handleFragmentCookies());

    }

    /**
     * Test that {@link ForwardRequestInfo#handleParameter(String, String)} put an array as follow:
     * <li>If the encodedKey is found as a key in the parameters field, then it will create a new array with all the items in the values of that key and append the new value
     * provided in encodedValue parameter.
     * parameter.</li>
     */
    @Test
    public void handleParameterShouldAddNewItem() throws UnsupportedEncodingException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        requestInfo.setParameter("a", new String[] { "1" });

        requestInfo.handleParameter("a", "2");

        Assert.assertTrue(requestInfo.parameters.containsKey("a"));
        Assert.assertArrayEquals(new String[] { "1", "2" }, requestInfo.parameters.get("a"));
    }

    /**
     * Test that {@link ForwardRequestInfo#parseQueryString(String)} follow next rules:
     * <li>Put different key/values in different elements of a map.</li>
     * <li>Put the string after equals symbol as an string array value and the other part as key.</li>
     * <li>If string query does not have value and just key, then create an array with an empty item.</li<
     */
    @Test
    public void parseQueryStringTest() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        String stringQuery = "a=1&b";

        HashMap<String, String[]> test = requestInfo.parseQueryString(stringQuery);

        Assert.assertEquals(2, test.size());

        Assert.assertTrue(test.containsKey("a"));
        Assert.assertArrayEquals(new String[] { "1" }, test.get("a"));

        Assert.assertTrue(test.containsKey("b"));
        Assert.assertArrayEquals(new String[0], test.get("b"));
    }

    /**
     * Test that {@link ForwardRequestInfo#queryStringToParameters(String)} does not perform any operation if the provided parameter is null or empty.
     * Fails if any exception or if the parameters field is different to null.
     */
    @Test
    public void queryStringToParametersShouldDoAnythingIfQueryIsNullOrEmpty() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        try {
            requestInfo.queryStringToParameters(null);
            requestInfo.queryStringToParameters("");

            Assert.assertTrue(requestInfo.parameters == null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    /**
     * Test that {@link ForwardRequestInfo#queryStringToParameters()} initialize the parameters field with no items.
     * Fails if the parameters field is not initialized or empty.
     */
    @Test
    public void queryStringToParametersShouldInitializeParametersField() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL);
        try {
            requestInfo.queryStringToParameters();

            Assert.assertTrue(requestInfo.parameters.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    /**
     * Test that {@link ForwardRequestInfo#queryStringToParameters(String)} should set both parameters in the {@link ForwardRequestInfo#handleParameter(String, String)} method if
     * the query have
     * key and value.
     */
    @Test
    public void queryStringToParametersShouldInvokeHandleParameterWithKeyAndValueIfQueryContainsHasKeyAndProperty() throws UnsupportedEncodingException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        requestInfo.setParameter("a", new String[] { "1" });

        requestInfo.queryStringToParameters("a=2");

        Assert.assertArrayEquals(new String[] { "1", "2" }, requestInfo.parameters.get("a"));
    }

    /**
     * Test that {@link ForwardRequestInfo#queryStringToParameters(String)} should set an empty value in the {@link ForwardRequestInfo#handleParameter(String, String)} method if
     * the provided
     * query have only a key and not parameters.
     */
    @Test
    public void queryStringToParametersShouldInvokeHandleParameterWithNoValueIfQueryJustHaveKey() throws UnsupportedEncodingException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        requestInfo.setParameter("a", new String[] { "1" });

        requestInfo.queryStringToParameters("a");

        Assert.assertArrayEquals(new String[] { "1", "" }, requestInfo.parameters.get("a"));
    }

    /**
     * Test that {@link ForwardRequestInfo#queryStringToParameters()} set the specified key and values from the requestUrl.
     * Fails if the key/value were not set in the properties map.
     */
    @Test
    public void queryStringToParametersShouldQueryFromRequestURL() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);
        try {
            requestInfo.queryStringToParameters();

            Assert.assertTrue(requestInfo.parameters.containsKey("value"));
            Assert.assertArrayEquals(new String[] { "1" }, requestInfo.parameters.get("value"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    /**
     * Test the Test the {@link ForwardRequestInfo#redirectCachedHttpRequest(HttpServletRequest, HttpServletResponse, String, String)} method.
     */
    @Test
    public void redirectCachedRequestNoFragmentTest() throws IOException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL, HTTP_SERVLET_REQUEST_QUERY_STRING);

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_RESPONSE_MCK).sendRedirect(HTTP_SERVLET_REQUEST_REQUEST_URL + "?" + HTTP_SERVLET_REQUEST_QUERY_STRING);
                //exactly(2).of(HTTP_SERVLET_RESPONSE_MCK).setHeader(with(any(String.class)), with(any(String.class)));

                //one(HTTP_SERVLET_RESPONSE_MCK).setDateHeader(with(any(String.class)), with(any(Long.class)));
                //one(HTTP_SERVLET_RESPONSE_MCK).setContentType(with(any(String.class)));

                //ByteArrayOutputStream baos = new ByteArrayOutputStream();

                //one(HTTP_SERVLET_RESPONSE_MCK).getWriter();
                //will(returnValue(new PrintWriter(baos)));
            }
        });

        try {
            requestInfo.redirectCachedHttpRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }
    }

    /**
     * Test the {@link ForwardRequestInfo#redirectGetRequest(HttpServletRequest, HttpServletResponse, String, String, boolean)} method.
     */
    @Test
    public void redirectGetRequestTest() throws IOException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        mockery.checking(new Expectations() {
            {
                one(HTTP_SERVLET_RESPONSE_MCK).sendRedirect(HTTP_SERVLET_REQUEST_REQUEST_URL + "?" + HTTP_SERVLET_REQUEST_QUERY_STRING);
                exactly(2).of(HTTP_SERVLET_RESPONSE_MCK).setHeader(with(any(String.class)), with(any(String.class)));

                one(HTTP_SERVLET_RESPONSE_MCK).setDateHeader(with(any(String.class)), with(any(Long.class)));
                one(HTTP_SERVLET_RESPONSE_MCK).setContentType(with(any(String.class)));
            }
        });

        try {
            requestInfo.redirectGetRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK, null, null, false);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }

        Assert.assertEquals(ForwardRequestInfo.METHOD_GET, requestInfo.method); //No getter for method field.
    }

    /**
     * Test the {@link ForwardRequestInfo#redirectPostRequest(HttpServletRequest, HttpServletResponse, String, String)} method.
     */
    @Test
    public void redirectPostRequestTest() throws IOException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        mockery.checking(new Expectations() {
            {
                exactly(2).of(HTTP_SERVLET_RESPONSE_MCK).setHeader(with(any(String.class)), with(any(String.class)));

                one(HTTP_SERVLET_RESPONSE_MCK).setDateHeader(with(any(String.class)), with(any(Long.class)));
                one(HTTP_SERVLET_RESPONSE_MCK).setContentType(with(any(String.class)));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                one(HTTP_SERVLET_RESPONSE_MCK).getWriter();
                will(returnValue(new PrintWriter(baos)));
            }
        });

        try {
            requestInfo.redirectPostRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }

        Assert.assertEquals(ForwardRequestInfo.METHOD_POST, requestInfo.method); //No getter for method field.
    }

    /**
     * Test that @link {@link ForwardRequestInfo#redirectRequest(HttpServletRequest, HttpServletResponse, String, String)} append the properties
     * to a {@link StringBuffer}.
     */
    @Test
    public void redirectRequestShouldAppendParameters() throws SamlException, IOException {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_FRAGMENT);

        //Setting parameters
        requestInfo.setParameter("a", new String[] { "1" });
        requestInfo.setParameter("b", new String[0]);

        mockery.checking(new Expectations() {
            {
                exactly(2).of(HTTP_SERVLET_RESPONSE_MCK).setHeader(with(any(String.class)), with(any(String.class)));

                one(HTTP_SERVLET_RESPONSE_MCK).setDateHeader(with(any(String.class)), with(any(Long.class)));
                one(HTTP_SERVLET_RESPONSE_MCK).setContentType(with(any(String.class)));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                one(HTTP_SERVLET_RESPONSE_MCK).getWriter();
                will(returnValue(new PrintWriter(baos)));
            }
        });

        requestInfo.redirectRequest(HTTP_SERVLET_REQUEST_MCK, HTTP_SERVLET_RESPONSE_MCK, null, null);
    }

    /**
     * Test that {@link ForwardRequestInfo#safeCompare(int, int)} method compares safely two parameters.
     * Fails if any exception is thrown.
     */
    @Test
    public void safeCompareIntTest() {
        try {
            Assert.assertTrue(ForwardRequestInfo.safeCompare(1, 1));
            Assert.assertFalse(ForwardRequestInfo.safeCompare(1, 2));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }

    }

    /**
     * Test that {@link ForwardRequestInfo#safeCompare(String, String)} method compares safely two parameters.
     * Fails if any exception is thrown.
     */
    @Test
    public void safeCompareStringsTest() {
        String value1 = null;
        String value2 = null;

        try {
            Assert.assertFalse(ForwardRequestInfo.safeCompare("", null));
            Assert.assertFalse(ForwardRequestInfo.safeCompare(null, ""));
            Assert.assertTrue(ForwardRequestInfo.safeCompare("", ""));
            Assert.assertTrue(ForwardRequestInfo.safeCompare(value1, value2));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown: " + e.getMessage());
        }

    }

    /**
     * Test if {@link ForwardRequestInfo#setParameter(String, String[])} initialize the <code>parameters</code> field if it hasn't been initialized.
     */
    @Test
    public void setParameterShouldInitiliazeParametersFieldIfNotItilializedPreviously() {
        ForwardRequestInfo requestInfo = new ForwardRequestInfo(HTTP_SERVLET_REQUEST_REQUEST_URL_WITH_QUERY);

        Assert.assertNull(requestInfo.parameters); //No getter for this field.

        requestInfo.setParameter("a", new String[] { "1" });

        Assert.assertNotNull(requestInfo.parameters); //No getter for this field.

    }

}