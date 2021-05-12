/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.params;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import javax.xml.ws.http.HTTPException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class ParamsTest {

    @Server("com.ibm.ws.jaxrs.fat.params")
    public static LibertyServer server;

    private static final String paramwar = "params";

    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, paramwar, "com.ibm.ws.jaxrs.fat.params",
                                      "com.ibm.ws.jaxrs.fat.params.form",
                                      "com.ibm.ws.jaxrs.fat.params.header",
                                      "com.ibm.ws.jaxrs.fat.params.newcookie",
                                      "com.ibm.ws.jaxrs.fat.params.query");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("SRVE0777E", "SRVE0315E");
        }
    }

    @Before
    public void getHttpClient() {
        client = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        client.getConnectionManager().shutdown();
    }

    private static String COOKIE_URL_PATTERN = "newcookies";

    private void setCookies() throws Exception {
        String uri = getBaseTestUri(paramwar, COOKIE_URL_PATTERN, "cookiestests");
        // call put to set the cookies
        HttpPut putHttpMethod = new HttpPut(uri);
        putHttpMethod.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.RFC_2965);

        HttpResponse resp = client.execute(putHttpMethod);
        System.out.println("Response body: ");
        String responseBody = asString(resp);
        System.out.println(responseBody);
        System.out.println("Response headers:");
        List<Header> headers = Arrays.asList(resp.getAllHeaders());
        System.out.println(headers);
        assertEquals(200, resp.getStatusLine().getStatusCode());
    }

    /**
     * Test that the HttpHeaders.getCookies() method returns correct cookies and
     * information
     *
     * @throws Exception
     */
    //@Test
    //Right now, this test isn't working. Need to figure out how to set
    // cookies on the GET request
    public void testHttpHeadersGetCookie() throws Exception {
        setCookies();

        String uri = getBaseTestUri(paramwar, COOKIE_URL_PATTERN, "cookiestests/getAll");
        // call get to exercise HttpHeaders.getCookies()
        HttpGet getHttpMethod = new HttpGet(uri);
        // This isn't resulting in cookies being set on the request
        getHttpMethod.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.RFC_2965);
        System.out.println("Request headers for GET:");
        System.out.println(Arrays.asList(getHttpMethod.getAllHeaders()));

        HttpResponse resp = client.execute(getHttpMethod);
        System.out.println("Response body from GET: ");
        String responseBody = asString(resp);
        System.out.println(responseBody);
        System.out.println("Response headers from GET:");
        List<Header> headers = Arrays.asList(resp.getAllHeaders());
        System.out.println(headers);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        StringTokenizer t = new StringTokenizer(responseBody, "\r\n\t\f");
        String next = null;
        boolean name3Found = false;
        boolean name2Found = false;
        boolean nameFound = false;
        String contextRoot = paramwar;
        String servletPath = "/newcookies";
        while (t.hasMoreTokens()) {
            next = t.nextToken();
            if (next.startsWith("name3")) {
                System.out.println("name3: " + next);
                assertEquals("name3,value3," + contextRoot
                             + servletPath
                             + "/cookiestests,"
                             + "localhost", next);
                name3Found = true;
            } else if (next.startsWith("name2")) {
                System.out.println("name2: " + next);
                assertEquals("name2,value2," + contextRoot
                             + servletPath
                             + "/cookiestests,"
                             + "localhost", next);
                name2Found = true;
            } else if (next.startsWith("name")) {
                System.out.println("name: " + next);
                assertEquals("name,value," + contextRoot
                             + servletPath
                             + "/cookiestests,"
                             + "localhost", next);
                nameFound = true;
            } else
                fail("Received an unexpected cookie: " + next);
        }
        if (!nameFound || !name2Found || !name3Found) {
            fail("Did not receive all the expected cookies." + nameFound
                 + name2Found
                 + name3Found);
        }
    }

    /**
     * Test the @CookieParameter annotation on a private class field
     *
     * @throws Exception
     */
    //@Test
    //Right now, this test isn't working. Need to figure out how to set
    // cookies on the GET request
    public void testCookieParamPrivateVar() throws Exception {

        setCookies();

        String uri = getBaseTestUri(paramwar, COOKIE_URL_PATTERN, "cookiestests/getValue2");
        HttpGet getHttpMethod = new HttpGet(uri);
        // This isn't resulting in cookies being set on the request
        getHttpMethod.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.RFC_2965);
        System.out.println("Request headers:");
        System.out.println(Arrays.asList(getHttpMethod.getAllHeaders()));

        HttpResponse resp = client.execute(getHttpMethod);
        System.out.println("Response status code: " + resp.getStatusLine().getStatusCode());
        System.out.println("Response body: ");
        String responseBody = asString(resp);
        System.out.println(responseBody);
        System.out.println("Response headers:");
        List<Header> headers = Arrays.asList(resp.getAllHeaders());
        System.out.println(headers);
        assertEquals(400, resp.getStatusLine().getStatusCode());
        assertEquals("value2", responseBody.trim());
    }

    // From CookieParamTest
    /**
     * Tests that a cookie parameter is retrieved.
     */
    @Test
    public void testCookieParam() throws Exception {

        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "cookiemonster");

        HttpPut httpMethod = new HttpPut(uri);
        httpMethod.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.RFC_2109);

        try {
            HttpResponse resp = client.execute(httpMethod);
            String responseBody = asString(resp);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            assertEquals("swiped:" + 0, responseBody);
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpPut(uri);
        httpMethod.getParams().setParameter("http.protocol.cookie-policy", CookiePolicy.RFC_2109);
        httpMethod.setHeader("Cookie", "jar=1");

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("swiped:" + 1, responseBody);
    }

    /**
     * Test that if no parameters are passed, the default values are used.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultValue() throws Exception {

        String uri = getBaseTestUri(paramwar, "ptest", "defaultvalue");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getRow:" + "offset="
                     + "0"
                     + ";version="
                     + "1.0"
                     + ";limit="
                     + "100"
                     + ";sort="
                     + "normal", responseBody);
    }

    /**
     * Test using some default values.
     *
     * @throws Exception
     */
    @Test
    public void testUseSomeDefaultValue() throws Exception {

        String uri = getBaseTestUri(paramwar, "ptest", "defaultvalue?sort=backward&offset=314");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getRow:" + "offset="
                     + "314"
                     + ";version="
                     + "1.0"
                     + ";limit="
                     + "100"
                     + ";sort="
                     + "backward", responseBody);
    }

    private static final String BASE_URI_ENCODE = "encodingparam";
    private static final String BASE_URI_DECODE = "decodedparams";
    private static final String PARAM_URL_PATTERN = "ptest";

    /**
     * testSingleDecodedQueryParam - testSingleEncodedFormParamMethod
     * tests that <code>@Encoded</code> annotated method and parameter level works.
     */
    @Test
    public void testSingleDecodedQueryParam() throws Exception {
        String ugly = "city;appversion=1.1?location=%21%20%2A%20%27%20%28%20%29%20%3B%20%3A%20%40%20%26%20%3D%20%2B%20%24%20%2C%20%2F%20%3F%20%25%20%23%20%5B%20%5D";
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_DECODE, ugly);
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInCityDecoded:location=! * ' ( ) ; : @ & = + $ , / ? % # [ ];appversion=1.1",
                     responseBody);
    }

    @Test
    public void testSingleEncodedQueryParam() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "city;appversion=1.1%2B?location=Austin%2B%20Texas");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInCity:location=Austin%2B%20Texas;appversion=1.1%2B",
                     responseBody);
    }

    @Test
    public void testSingleEncodedQueryParamMethod() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "method/city;appversion=1.1%2B?location=Austin%2B%20Texas");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInCityMethod:location=Austin%2B%20Texas;appversion=1.1%2B",
                     responseBody);
    }

    @Test
    public void testSingleDecodedPathParm() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_DECODE, "country/United%20States%20of%20America;appversion=1.1%2C2");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInCountryDecoded:location=United States of America;appversion=1.1,2",
                     responseBody);
    }

    @Test
    public void testSingleEncodedPathParam() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "country/United%20States%20of%20America;appversion=1.1%2B");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInCountry:location=United%20States%20of%20America;appversion=1.1%2B",
                     responseBody);
    }

    @Test
    public void testSingleEncodedPathParamMethod() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "method/country/United%20States%20of%20America;appversion=1.1%2B");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInCountryMethod:location=United%20States%20of%20America;appversion=1.1%2B",
                     responseBody);
    }

    @Test
    public void testSingleDecodedMatrixParam() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_DECODE, "street;location=Burnet%20Road;appversion=1.1%2B");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopOnStreetDecoded:location=Burnet Road;appversion=1.1+",
                     responseBody);
    }

    @Test
    public void testSingleEncodedMatrixParam() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "street;location=Burnet%20Road;appversion=1.1%2B");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopOnStreet:location=Burnet%20Road;appversion=1.1%2B",
                     responseBody);
    }

    @Test
    public void testSingleEncodedMatrixParamMethod() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "method/street;location=Burnet%2B%20Road;appversion=1.1%2B");
        HttpGet httpMethod = new HttpGet(uri);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopOnStreetMethod:location=Burnet%2B%20Road;appversion=1.1%2B",
                     responseBody);
    }

    @Test
    public void testSingleDecodedFormParam() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_DECODE, "region;appversion=");
        HttpPost httpMethod = new HttpPost(uri);
        StringEntity entity = new StringEntity("location=%21%20%2A%20%27%20%28%20%29%20%3B%20%3A%20%40%20%26%20%3D%20%2B%20%24%20%2C%20%2F%20%3F%20%25%20%23%20%5B%20%5D", "UTF-8");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInRegionDecoded:location=! * ' ( ) ; : @ & = + $ , / ? % # [ ];appversion=",
                     responseBody);
    }

    @Test
    public void testSingleEncodedFormParam() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "region;appversion=1.1%2B");
        HttpPost httpMethod = new HttpPost(uri);
        StringEntity entity = new StringEntity("location=The%20Southwest", "UTF-8");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInRegion:location=The%20Southwest;appversion=1.1%2B",
                     responseBody);
    }

    @Test
    public void testSingleEncodedFormParamMethod() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, BASE_URI_ENCODE, "method/region;appversion=1.1%2B");
        HttpPost httpMethod = new HttpPost(uri);
        StringEntity entity = new StringEntity("location=The%20Southwest", "UTF-8");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("getShopInRegionMethod:location=The%20Southwest;appversion=1.1%2B",
                     responseBody);
    }

    // The following two methods are from FormParamTest
    @Test
    public void testOnlyEntityFormParam() throws Exception {

        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "form", "withOnlyEntity");
        HttpPost httpMethod = new HttpPost(uri);
        StringEntity entity = new StringEntity("firstkey=somevalue&someothervalue=somethingelse");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);

        HttpResponse resp = client.execute(httpMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String str = asString(resp);
        assertTrue(str, str.contains("someothervalue=[somethingelse]"));
        assertTrue(str, str.contains("firstkey=[somevalue]"));
    }

    /**
     * In a weird instance, client posts a form encoded data but the resource is
     * expecting something else (say a String) as its entity. The engine should
     * not mangle the InputStream with ServletRequest.getParameter until
     * absolutely required.
     *
     * @throws Exception
     */
    @Test
    public void testPostFormEntityButResourceDoesNotExpect() throws Exception {

        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "form", "withStringEntity");
        HttpPost httpMethod = new HttpPost(uri);
        StringEntity entity = new StringEntity("firstkey=somevalue&someothervalue=somethingelse");
        entity.setContentType("application/x-www-form-urlencoded");

        httpMethod.setEntity(entity);
        HttpResponse resp = client.execute(httpMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String str = asString(resp);
        assertEquals(str, "str:firstkey=somevalue&someothervalue=somethingelse");
    }

    // testPostFormEntityButResourceDoesNotExpect - testHeaderPropertyValueOfException are from HeaderParamTest

    /**
     * Tests that a custom header is sent and received properly. Uses
     * constructor, property, field, and parameter parameters.
     */
    @Test
    public void testCustomHeaderParam() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "header");
        HttpGet httpMethod = new HttpGet(uri);

        httpMethod.setHeader("customHeaderParam", "somevalue");
        httpMethod.setHeader(new BasicHeader("User-Agent", "httpclient"));
        httpMethod.setHeader("Accept-Language", "en");

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("secret", resp.getFirstHeader("custResponseHeader").getValue());
        assertEquals("getHeaderParam:somevalue;User-Agent:httpclient;Accept-Language:en;language-method:en",
                     responseBody);
    }

    /**
     * Tests that headers are properly set with <code>@DefaultValue</code>s set.
     */
    @Test
    public void testHeaderDefaultValue() throws Exception {
        /*
         * the default values with no headers set.
         */
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "headerparam", "default");
        HttpGet getMethod = new HttpGet(uri);

        try {
            HttpResponse resp = client.execute(getMethod);
            String responseBody = asString(resp);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            assertEquals("", responseBody);
            assertEquals("MyCustomPropertyHeader", resp.getFirstHeader("RespCustomPropertyHeader").getValue());
            assertEquals("MyCustomConstructorHeader", resp.getFirstHeader("RespCustomConstructorHeader").getValue());
            // We are using http client version 4.3.1 for common feature, in future, we may update.
            assertTrue(resp.getFirstHeader("RespUserAgent").getValue().contains("Apache-HttpClient"));

            assertEquals("english", resp.getFirstHeader("RespAccept-Language").getValue());
            assertEquals("MyCustomMethodHeader", resp.getFirstHeader("RespCustomMethodHeader").getValue());
        } finally {
            client = new DefaultHttpClient();
        }

        /*
         * set values for custom headers
         */
        getMethod = new HttpGet(uri);
        getMethod.setHeader("CustomPropertyHeader", "setCustPropertyHeader");
        getMethod.setHeader("CustomConstructorHeader", "setCustConstructorHeader");
        getMethod.setHeader("Accept-Language", "da;en-gb;en");
        getMethod.setHeader("CustomMethodHeader", "12345678910");

        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("", responseBody);
        assertEquals("setCustPropertyHeader", resp.getFirstHeader("RespCustomPropertyHeader").getValue());
        assertEquals("setCustConstructorHeader", resp.getFirstHeader("RespCustomConstructorHeader").getValue());
        // We are using http client version 4.3.1 for common feature, in future, we may update.
        assertTrue(resp.getFirstHeader("RespUserAgent").getValue().contains("Apache-HttpClient"));
        assertEquals("da;en-gb;en", resp.getFirstHeader("RespAccept-Language").getValue());
        assertEquals("12345678910", resp.getFirstHeader("RespCustomMethodHeader").getValue());
    }

    /**
     * Tests that a custom header with a primitive type (int) can be used.
     */
    @Test
    public void testHeaderParamPrimitiveException() throws Exception {

        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "headerparam", "exception/primitive");
        HttpGet getMethod = new HttpGet(uri);
        getMethod.setHeader("CustomNumHeader", "314");

        try {
            HttpResponse resp = client.execute(getMethod);
            String responseBody = asString(resp);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            assertEquals("", responseBody);
            assertEquals("314", resp.getFirstHeader("RespCustomNumHeader").getValue());
        } finally {
            client = new DefaultHttpClient();
        }

        getMethod = new HttpGet(uri);
        getMethod.setHeader("CustomNumHeader", "abcd");

        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals("", responseBody);
        assertEquals(400, resp.getStatusLine().getStatusCode());
    }

    // testHeaderParamStringConstructorException - testHeaderPropertyValueOfException
    // result in FFDC logs being created, because either an NPE or IllegalArgumentException
    // is thrown. The NPE occurs because part of what executeStringConstructorHeaderTest
    // does is send a GET where there is NO header set.

    /**
     * Tests that a custom header with a custom constructor can be used.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header constructor throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testHeaderParamStringConstructorException() throws Exception {
        executeStringConstructorHeaderTest("headerparam/exception/constructor", "CustomStringHeader");
    }

    /**
     * Tests that a custom header with a custom static valueOf method can be
     * used.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testHeaderParamValueOfException() throws Exception {
        executeValueOfHeaderTest("headerparam/exception/valueof", "CustomValueOfHeader");
    }

    /**
     * Tests that a custom header is set correctly in a List of a type with a
     * custom static valueOf method.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.IllegalArgumentException")
    public void testHeaderParamListValueOfException() throws Exception {
        executeValueOfHeaderTest("headerparam/exception/listvalueof", "CustomListValueOfHeader");
    }

    /**
     * Tests that a custom header is set correctly in a Set of a type with a
     * custom static valueOf method.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.IllegalArgumentException")
    public void testHeaderParamSetValueOfException() throws Exception {
        executeValueOfHeaderTest("headerparam/exception/setvalueof", "CustomSetValueOfHeader");
    }

    /**
     * Tests that a custom header is set correctly in a Set of a type with a
     * custom static valueOf method.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.IllegalArgumentException")
    public void testHeaderParamSortedSetValueOfException() throws Exception {
        executeValueOfHeaderTest("headerparam/exception/sortedsetvalueof", "CustomSortedSetValueOfHeader");
    }

    /**
     * Tests that a custom header is set correctly in a field with a String
     * constructor type.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testHeaderFieldStringConstructorException() throws Exception {
        executeStringConstructorHeaderTest("headerparam/exception/fieldstrcstr", "CustomStringConstructorFieldHeader");
    }

    /**
     * Tests that a custom header is set correctly in a field with a static
     * valueOf method.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testHeaderFieldValueOfException() throws Exception {
        executeValueOfHeaderTest("headerparam/exception/fieldvalueof", "CustomValueOfFieldHeader");
    }

    /**
     * Tests that a custom header is set correctly in a field with a string
     * constructor.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testHeaderPropertyStringConstructorException() throws Exception {
        executeStringConstructorHeaderTest("headerparam/exception/propertystrcstr", "CustomStringConstructorPropertyHeader");
    }

    /**
     * Tests that a custom header is set correctly in a field with a type with a
     * static valueOf method.
     * <ul>
     * <li>If the header is not set, then the header parameter is set to null.</li>
     * <li>If the header valueOf throws an exception, then 400 Bad Request
     * status is returned.</li>
     * <li>If a <code>WebApplicationException</code> is thrown during parameter
     * valueOf construction, then use that.</li>
     * </ul>
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testHeaderPropertyValueOfException() throws Exception {
        executeValueOfHeaderTest("headerparam/exception/propertyvalueof", "CustomValueOfPropertyHeader");
    }

    /**
     * Tests a custom string constructor type.
     *
     * @param path
     * @param header
     * @throws IOException
     * @throws HTTPException
     */
    private void executeStringConstructorHeaderTest(String path, String header) throws Exception {

        // normal
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, path);
        HttpGet getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "MyCustomHeaderValue");

        try {
            HttpResponse resp = client.execute(getMethod);
            String responseBody = asString(resp);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            assertEquals("", responseBody);
            assertEquals("MyCustomHeaderValue", resp.getFirstHeader("Resp" + header).getValue());
        } finally {
            // We do all this because if not, there would be
            // connection issues because multiple method invocations
            // would be happening on the same client instance.
            // Perhaps a better way is to reset the client somehow.
            client = new DefaultHttpClient();
        }

        // no header set--this will cause an NPE in the HeaderParamExceptionResource
        // because the custom*Header parameter passed in is null
        getMethod = new HttpGet(uri);

        try {
            HttpResponse resp = client.execute(getMethod);
            assertEquals(500, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        // web app ex thrown
        getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "throwWeb");

        try {
            HttpResponse resp = client.execute(getMethod);
            String responseBody = asString(resp);
            assertEquals(499, resp.getStatusLine().getStatusCode());
            assertEquals("HeaderStringConstructorWebAppEx", responseBody);
        } finally {
            client = new DefaultHttpClient();
        }

        // runtime exception thrown
        getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "throwNull");
        try {
            HttpResponse resp = client.execute(getMethod);
            assertEquals(400, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        // exception thrown
        getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "throwEx");

        HttpResponse resp = client.execute(getMethod);
        assertEquals(400, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests a custom valueOf header.
     *
     * @param path the path to the resource
     * @param header the name of the header to test
     * @throws IOException
     * @throws HTTPException
     */
    private void executeValueOfHeaderTest(String path, String header) throws Exception {

        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, path);

        // normal
        HttpGet getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "MyCustomHeaderValue");

        try {
            HttpResponse resp = client.execute(getMethod);
            String responseBody = asString(resp);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            assertEquals("", responseBody);
            assertEquals("MyCustomHeaderValue", resp.getFirstHeader("Resp" + header).getValue());
        } finally {
            client = new DefaultHttpClient();
        }

        // no header set
        getMethod = new HttpGet(uri);

        try {
            HttpResponse resp = client.execute(getMethod);
            assertEquals(500, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        // web app ex thrown
        getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "throwWeb");

        try {
            HttpResponse resp = client.execute(getMethod);
            String responseBody = asString(resp);
            assertEquals(498, resp.getStatusLine().getStatusCode());
            assertEquals("HeaderValueOfWebAppEx", responseBody);
        } finally {
            client = new DefaultHttpClient();
        }

        // runtime exception thrown
        getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "throwNull");
        try {
            HttpResponse resp = client.execute(getMethod);
            assertEquals(400, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        // exception thrown
        getMethod = new HttpGet(uri);
        getMethod.setHeader(header, "throwEx");

        HttpResponse resp = client.execute(getMethod);
        assertEquals(400, resp.getStatusLine().getStatusCode());
    }

    // From MatrixParamTest
    // Common utility method shared by testNoParam - testParameterTypeWithValueOfMethod
    protected String sendGoodRequestAndGetResponse(String aPartialRequestURL, Class<? extends HttpRequestBase> aClass) throws Exception {
        // This should be something like http://localhost:8000/params/ptest/matrix;cstrparam=HelloWorld
        // for example
        String baseUri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, aPartialRequestURL);
        HttpRequestBase httpMethod = aClass.newInstance();
        httpMethod.setURI(new URI(baseUri));

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        return responseBody;
    }

    /**
     * Tests that no matrix parameters sent still calls proper resource.
     */
    @Test
    public void testNoParam() throws Exception {
        assertEquals("deleteConstructorMatrixParam:null",
                     sendGoodRequestAndGetResponse("matrix", HttpDelete.class));
        assertEquals("getConstructorMatrixParam:null",
                     sendGoodRequestAndGetResponse("matrix", HttpGet.class));
        assertEquals("putConstructorMatrixParam:null",
                     sendGoodRequestAndGetResponse("matrix", HttpPut.class));
        assertEquals("postConstructorMatrixParam:null",
                     sendGoodRequestAndGetResponse("matrix", HttpPost.class));
    }

    /**
     * Tests the constructor matrix parameter is processed.
     */
    @Test
    public void testConstructorParam() throws Exception {
        assertEquals("getConstructorMatrixParam:HelloWorld",
                     sendGoodRequestAndGetResponse("matrix;cstrparam=HelloWorld", HttpGet.class));
        assertEquals("deleteConstructorMatrixParam:HelloWorld",
                     sendGoodRequestAndGetResponse("matrix;cstrparam=HelloWorld",
                                                   HttpDelete.class));
        assertEquals("putConstructorMatrixParam:HelloWorld",
                     sendGoodRequestAndGetResponse("matrix;cstrparam=HelloWorld", HttpPut.class));
        assertEquals("postConstructorMatrixParam:HelloWorld",
                     sendGoodRequestAndGetResponse("matrix;cstrparam=HelloWorld", HttpPost.class));
    }

    /**
     * Tests both the simple constructor and method matrix parameter are
     * processed.
     */
    @Test
    public void testSimpleMatrixParam() throws Exception {
        assertEquals("getSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;cstrparam=Hello;life=good",
                                                   HttpGet.class));
        assertEquals("putSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;cstrparam=Hello;life=good",
                                                   HttpPut.class));
        assertEquals("postSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;cstrparam=Hello;life=good",
                                                   HttpPost.class));
        assertEquals("deleteSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;cstrparam=Hello;life=good",
                                                   HttpDelete.class));
    }

    /**
     * Tests that a no constructor matrix parameter is set.
     */
    @Test
    public void testNoConstructorMatrixParamAndSimpleMatrixParam() throws Exception {
        assertEquals("deleteSimpleMatrixParam:null;erase",
                     sendGoodRequestAndGetResponse("matrix/simple;life=erase", HttpDelete.class));
        assertEquals("getSimpleMatrixParam:null;good",
                     sendGoodRequestAndGetResponse("matrix/simple;life=good", HttpGet.class));
        assertEquals("postSimpleMatrixParam:null;new",
                     sendGoodRequestAndGetResponse("matrix/simple;life=new", HttpPost.class));
        assertEquals("putSimpleMatrixParam:null;progress",
                     sendGoodRequestAndGetResponse("matrix/simple;life=progress", HttpPut.class));
    }

    /**
     * Tests the constructor and simple matrix parameter can be out of order.
     */
    @Test
    public void testOutOfOrderMatrixParam() throws Exception {
        assertEquals("getSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;life=good;cstrparam=Hello;",
                                                   HttpGet.class));
        assertEquals("putSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;life=good;cstrparam=Hello;",
                                                   HttpPut.class));
        assertEquals("postSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;life=good;cstrparam=Hello",
                                                   HttpPost.class));
        assertEquals("deleteSimpleMatrixParam:Hello;good",
                     sendGoodRequestAndGetResponse("matrix/simple;life=good;cstrparam=Hello",
                                                   HttpDelete.class));
    }

    /**
     * Tests that matrix parameters are case sensitive.
     */
    @Test
    public void testLowercaseMatrixParam() throws Exception {
        assertEquals("getSimpleMatrixParam:null;null",
                     sendGoodRequestAndGetResponse("matrix/simple;LIFE=good;cstrParam=Hello",
                                                   HttpGet.class));
        assertEquals("postSimpleMatrixParam:null;null",
                     sendGoodRequestAndGetResponse("matrix/simple;LIFE=good;cstrParam=Hello",
                                                   HttpPost.class));
        assertEquals("putSimpleMatrixParam:null;null",
                     sendGoodRequestAndGetResponse("matrix/simple;LIFE=good;cstrParam=Hello",
                                                   HttpPut.class));
        assertEquals("deleteSimpleMatrixParam:null;null",
                     sendGoodRequestAndGetResponse("matrix/simple;LIFE=good;cstrParam=Hello",
                                                   HttpDelete.class));
    }

    /**
     * Tests multiple matrix parameters sent to same resource.
     */
    @Test
    public void testMultipleMatrixParam() throws Exception {
        assertEquals("getMultipleMatrixParam:first;capital;done",
                     sendGoodRequestAndGetResponse("matrix/multiple;1st=first;ONEMOREPARAM=capital;onemoreparam=done",
                                                   HttpGet.class));
        assertEquals("deleteMultipleMatrixParam:first;capital;done",
                     sendGoodRequestAndGetResponse("matrix/multiple;1st=first;ONEMOREPARAM=capital;onemoreparam=done",
                                                   HttpDelete.class));
        assertEquals("postMultipleMatrixParam:first;capital;done",
                     sendGoodRequestAndGetResponse("matrix/multiple;1st=first;ONEMOREPARAM=capital;onemoreparam=done",
                                                   HttpPost.class));
        assertEquals("putMultipleMatrixParam:first;capital;done",
                     sendGoodRequestAndGetResponse("matrix/multiple;1st=first;ONEMOREPARAM=capital;onemoreparam=done",
                                                   HttpPut.class));
    }

    /**
     * Tests that primitive types are accepted in matrix parameters.
     */
    @Test
    public void testPrimitiveTypedMatrixParameter() throws Exception {
        assertEquals("getMatrixParameterPrimitiveTypes:false;12;3.14;3;b;1234567890;32456;123.0",
                     sendGoodRequestAndGetResponse("matrix/types/primitive;bool=false;intNumber=12;dbl=3.14;bite=3;ch=b;lng=1234567890;float=32456;short=123",
                                                   HttpGet.class));
    }

    /**
     * Tests that primitive types are accepted in parameters.
     */
    @Test
    public void testParameterTypeWithStringConstructor() throws Exception {
        assertEquals("getMatrixParameterStringConstructor:1234",
                     sendGoodRequestAndGetResponse("matrix/types/stringcstr;paramStringConstructor=1234",
                                                   HttpGet.class));
    }

    /**
     * Tests that primitive types are accepted in parameters.
     */
    @Test
    public void testParameterTypeWithValueOfMethod() throws Exception {
        assertEquals("getMatrixParameterValueOf:456789",
                     sendGoodRequestAndGetResponse("matrix/types/valueof;staticValueOf=456",
                                                   HttpGet.class));
    }

    // testCharParamEmpty - testListParamEmpty from ParamQueryNotSet
    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testCharParamEmpty() throws Exception {
        assertEquals("\u0000", sendGoodRequestAndGetResponse("queryparamnotset/char", HttpGet.class));

        assertEquals("a", sendGoodRequestAndGetResponse("queryparamnotset/char?letter=a", HttpGet.class));

        // Don't send in the right query
        assertEquals("\u0000", sendGoodRequestAndGetResponse("queryparamnotset/char?lette=a", HttpGet.class));

        assertEquals("a", sendGoodRequestAndGetResponse("/matrixparamnotset/char;letter=a", HttpGet.class));

        assertEquals("\u0000" + "", sendGoodRequestAndGetResponse("matrixparamnotset/char", HttpGet.class));

        assertEquals("\u0000" + "", sendGoodRequestAndGetResponse("matrixparamnotset/char;lette=a", HttpGet.class));
    }

    private static Random r = new Random();

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testByteParamEmpty() throws Exception {
        // query parameters
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/byte", HttpGet.class));

        byte b = (byte) r.nextInt(Byte.MAX_VALUE);
        assertEquals("" + b, sendGoodRequestAndGetResponse("/queryparamnotset/byte?b=" + b, HttpGet.class));

        // don't send in the right query
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/byte?b1=a", HttpGet.class));

        // matrix parameters
        b = (byte) r.nextInt(Byte.MAX_VALUE);
        assertEquals("" + b, sendGoodRequestAndGetResponse("/matrixparamnotset/byte;b=" + b, HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/byte", HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/byte;b1=a", HttpGet.class));
    }

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testDoubleParamEmpty() throws Exception {

        // query parameters
        assertEquals("0.0", sendGoodRequestAndGetResponse("/queryparamnotset/double", HttpGet.class));

        double d = r.nextDouble();
        assertEquals("" + d, sendGoodRequestAndGetResponse("/queryparamnotset/double?d=" + d, HttpGet.class));

        // don't send in the right query
        assertEquals("0.0", sendGoodRequestAndGetResponse("/queryparamnotset/double?d1=a", HttpGet.class));

        // matrix parameters
        d = r.nextDouble();
        assertEquals("" + d, sendGoodRequestAndGetResponse("/matrixparamnotset/double;count=" + d, HttpGet.class));

        assertEquals("0.0", sendGoodRequestAndGetResponse("/matrixparamnotset/double", HttpGet.class));

        assertEquals("0.0", sendGoodRequestAndGetResponse("/matrixparamnotset/double;coun=a", HttpGet.class));
    }

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testFloatParamEmpty() throws Exception {
        // query parameters
        assertEquals("0.0", sendGoodRequestAndGetResponse("/queryparamnotset/float", HttpGet.class));

        float f = r.nextFloat();
        assertEquals("" + f, sendGoodRequestAndGetResponse("/queryparamnotset/float?floatCount=" + f, HttpGet.class));

        // don't send in the right query
        assertEquals("0.0", sendGoodRequestAndGetResponse("/queryparamnotset/float?floatCount1=a", HttpGet.class));

        // matrix parameters
        f = r.nextFloat();
        assertEquals("" + f, sendGoodRequestAndGetResponse("/matrixparamnotset/float;floatCount=" + f, HttpGet.class));

        assertEquals("0.0", sendGoodRequestAndGetResponse("/matrixparamnotset/float", HttpGet.class));

        assertEquals("0.0", sendGoodRequestAndGetResponse("/matrixparamnotset/float;floatCoun=a", HttpGet.class));
    }

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testIntParamEmpty() throws Exception {
        // query parameters
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/int", HttpGet.class));

        int i = r.nextInt();
        assertEquals("" + i, sendGoodRequestAndGetResponse("/queryparamnotset/int?count=" + i, HttpGet.class));

        // don't send in the right query
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/int?coun=a", HttpGet.class));

        // matrix parameters
        i = r.nextInt();
        assertEquals("" + i, sendGoodRequestAndGetResponse("/matrixparamnotset/int;count=" + i, HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/int", HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/int;coun=a", HttpGet.class));
    }

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testShortParamEmpty() throws Exception {

        // query parameters
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/short", HttpGet.class));

        short i = (short) r.nextInt(Short.MAX_VALUE);
        assertEquals("" + i, sendGoodRequestAndGetResponse("/queryparamnotset/short?smallCount=" + i, HttpGet.class));

        // don't send in the right query
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/short?smallcount=a", HttpGet.class));

        // matrix parameters
        i = (short) r.nextInt(Short.MAX_VALUE);
        assertEquals("" + i, sendGoodRequestAndGetResponse("/matrixparamnotset/short;smallCount=" + i, HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/short", HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/short;smallCoun=a", HttpGet.class));
    }

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testLongParamEmpty() throws Exception {
        // query parameters
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/long", HttpGet.class));

        long i = r.nextLong();
        assertEquals("" + i, sendGoodRequestAndGetResponse("/queryparamnotset/long?longCount=" + i, HttpGet.class));

        // don't send in the right query
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/long?longount=a", HttpGet.class));

        // matrix parameters
        i = r.nextLong();
        assertEquals("" + i, sendGoodRequestAndGetResponse("/matrixparamnotset/long;longCount=" + i, HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/long", HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/long;longCoun=a", HttpGet.class));
    }

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testSetParamEmpty() throws Exception {
        // query parameters
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/set", HttpGet.class));

        int i = r.nextInt();
        assertEquals("1", sendGoodRequestAndGetResponse("/queryparamnotset/set?bag=" + i, HttpGet.class));

        // don't send in the right query
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/set?bg=a", HttpGet.class));

        // matrix parameters
        i = r.nextInt();
        assertEquals("1", sendGoodRequestAndGetResponse("/matrixparamnotset/set;bag=" + i, HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/set", HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/set;bg=a", HttpGet.class));
    }

    /**
     * Tests that given a HttpMethod with a query or matrix parameter, if the
     * parameter is not sent, then the default value is given back for basic
     * Java types.
     */
    @Test
    public void testListParamEmpty() throws Exception {
        // query parameters
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/list", HttpGet.class));

        char c = 'b';
        assertEquals("1", sendGoodRequestAndGetResponse("/queryparamnotset/list?letter=" + c, HttpGet.class));

        // don't send in the right query
        assertEquals("0", sendGoodRequestAndGetResponse("/queryparamnotset/list?lette=a", HttpGet.class));

        // matrix parameters
        assertEquals("1", sendGoodRequestAndGetResponse("/matrixparamnotset/list;letter=" + c, HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/list", HttpGet.class));

        assertEquals("0", sendGoodRequestAndGetResponse("/matrixparamnotset/list;lette=a", HttpGet.class));
    }

    // Following tests are from PathSegmentTest
    @Test
    public void testPathSegmentNoMatrixParameters() throws Exception {
        assertEquals("somepath", sendGoodRequestAndGetResponse("pathsegment/somepath", HttpGet.class));

        assertEquals("123456", sendGoodRequestAndGetResponse("pathsegment/123456", HttpGet.class));

        assertEquals("123456", sendGoodRequestAndGetResponse("pathsegment/123456;mp=3145", HttpGet.class));
    }

    @Test
    public void testPathSegmentMatrixParameters() throws Exception {
        assertEquals("somepath-somepath-null-null", sendGoodRequestAndGetResponse("pathsegment/matrix/somepath", HttpGet.class));

        assertEquals("somepath-somepath-null-val", sendGoodRequestAndGetResponse("pathsegment/matrix/somepath;mp=val", HttpGet.class));

        assertEquals("somepath-somepath-[abc]-val", sendGoodRequestAndGetResponse("pathsegment/matrix/somepath;mp=val;val=abc", HttpGet.class));

        assertEquals("somepath-somepath-[abc, 123]-val", sendGoodRequestAndGetResponse("pathsegment/matrix/somepath;mp=val;val=abc;val=123", HttpGet.class));
    }

    @Test
    public void testMultiPathParamMethod() throws Exception {
        // test matches three templates
        assertEquals("250.067-somepath-987654321", sendGoodRequestAndGetResponse("pathsegment/250.067/somepath/987654321", HttpGet.class));
        // test matches two templates + one fixed path at the end
        assertEquals("250.067-somepath-hello", sendGoodRequestAndGetResponse("pathsegment/250.067/somepath/hello", HttpGet.class));
    }

    /**
     * Tests that no query parameters sent still calls proper resource.
     */
    @Test
    public void testNoQueryParam() throws Exception {
        assertEquals("deleteConstructorQueryID:null",
                     sendGoodRequestAndGetResponse("query", HttpDelete.class));
        assertEquals("getConstructorQueryID:null", sendGoodRequestAndGetResponse("query",
                                                                                 HttpGet.class));
        assertEquals("postConstructorQueryID:null", sendGoodRequestAndGetResponse("query",
                                                                                  HttpPost.class));
        assertEquals("putConstructorQueryID:null", sendGoodRequestAndGetResponse("query",
                                                                                 HttpPut.class));
    }

    /**
     * Tests the constructor query parameter is processed.
     */
    @Test
    public void testConstructorQueryParam() throws Exception {
        assertEquals("deleteConstructorQueryID:HelloWorld",
                     sendGoodRequestAndGetResponse("query?queryid=HelloWorld", HttpDelete.class));
        assertEquals("getConstructorQueryID:HelloWorld",
                     sendGoodRequestAndGetResponse("query?queryid=HelloWorld", HttpGet.class));
        assertEquals("postConstructorQueryID:HelloWorld",
                     sendGoodRequestAndGetResponse("query?queryid=HelloWorld", HttpPost.class));
        assertEquals("putConstructorQueryID:HelloWorld",
                     sendGoodRequestAndGetResponse("query?queryid=HelloWorld", HttpPut.class));
    }

    /**
     * Tests both the simple constructor and method parameter are processed.
     */
    @Test
    public void testSimpleQueryParam() throws Exception {
        assertEquals("deleteSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?queryid=somequeryid&simpleParam=hi",
                                                   HttpDelete.class));
        assertEquals("getSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?queryid=somequeryid&simpleParam=hi",
                                                   HttpGet.class));
        assertEquals("postSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?queryid=somequeryid&simpleParam=hi",
                                                   HttpPost.class));
        assertEquals("putSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?queryid=somequeryid&simpleParam=hi",
                                                   HttpPut.class));
    }

    /**
     * Tests that a no constructor query parameter is set.
     */
    @Test
    public void testNoConstructorQueryParamAndSimpleQueryParam() throws Exception {
        assertEquals("deleteSimpleQueryParameter:null;hi",
                     sendGoodRequestAndGetResponse("query/simple/?simpleParam=hi",
                                                   HttpDelete.class));
        assertEquals("getSimpleQueryParameter:null;hi",
                     sendGoodRequestAndGetResponse("query/simple/?simpleParam=hi", HttpGet.class));
        assertEquals("postSimpleQueryParameter:null;hi",
                     sendGoodRequestAndGetResponse("query/simple/?simpleParam=hi", HttpPost.class));
        assertEquals("putSimpleQueryParameter:null;hi",
                     sendGoodRequestAndGetResponse("query/simple/?simpleParam=hi", HttpPut.class));
    }

    /**
     * Tests the constructor and simple query parameter can be out of order.
     */
    @Test
    public void testOutOfOrderSimpleQueryParam() throws Exception {
        assertEquals("deleteSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?simpleParam=hi&queryid=somequeryid",
                                                   HttpDelete.class));
        assertEquals("getSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?simpleParam=hi&queryid=somequeryid",
                                                   HttpGet.class));
        assertEquals("postSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?simpleParam=hi&queryid=somequeryid",
                                                   HttpPost.class));
        assertEquals("putSimpleQueryParameter:somequeryid;hi",
                     sendGoodRequestAndGetResponse("query/simple?simpleParam=hi&queryid=somequeryid",
                                                   HttpPut.class));
    }

    /**
     * Tests that query parameters are case sensitive.
     */
    @Test
    public void testLowercaseQueryParam() throws Exception {
        assertEquals("getSimpleQueryParameter:null;null",
                     sendGoodRequestAndGetResponse("query/simple/?simpleparam=hi&QUERYID=abcd",
                                                   HttpGet.class));
        assertEquals("postSimpleQueryParameter:null;null",
                     sendGoodRequestAndGetResponse("query/simple/?simpleparam=hi&QUERYID=abcd",
                                                   HttpPost.class));
        assertEquals("putSimpleQueryParameter:null;null",
                     sendGoodRequestAndGetResponse("query/simple/?simpleparam=hi&QUERYID=abcd",
                                                   HttpPut.class));
        assertEquals("deleteSimpleQueryParameter:null;null",
                     sendGoodRequestAndGetResponse("query/simple/?simpleparam=hi&QUERYID=abcd",
                                                   HttpDelete.class));
    }

    /**
     * Tests multiple query parameters sent to same resource.
     */
    @Test
    public void testMultipleQueryParam() throws Exception {
        assertEquals("getMultiQueryParameter:somequeryid;hi;789;1moreparam2go",
                     sendGoodRequestAndGetResponse("query/multiple?queryid=somequeryid&multiParam1=hi&123Param=789&1MOREParam=1moreparam2go",
                                                   HttpGet.class));
        assertEquals("deleteMultiQueryParameter:somequeryid;hi;789;1moreparam2go",
                     sendGoodRequestAndGetResponse("query/multiple?queryid=somequeryid&multiParam1=hi&123Param=789&1MOREParam=1moreparam2go",
                                                   HttpDelete.class));
        assertEquals("putMultiQueryParameter:somequeryid;hi;789;1moreparam2go",
                     sendGoodRequestAndGetResponse("query/multiple?queryid=somequeryid&multiParam1=hi&123Param=789&1MOREParam=1moreparam2go",
                                                   HttpPut.class));
        assertEquals("postMultiQueryParameter:somequeryid;hi;789;1moreparam2go",
                     sendGoodRequestAndGetResponse("query/multiple?queryid=somequeryid&multiParam1=hi&123Param=789&1MOREParam=1moreparam2go",
                                                   HttpPost.class));
    }

    /**
     * Tests that primitive types are accepted in query parameters.
     */
    @Test
    public void testPrimitiveTypedQueryParameter() throws Exception {
        assertEquals("getQueryParameterPrimitiveTypes:false;12;3.14;3;b;1234567890;32456;123.0",
                     sendGoodRequestAndGetResponse("query/types/primitive?bool=false&intNumber=12&dbl=3.14&bite=3&ch=b&lng=1234567890&float=32456&short=123",
                                                   HttpGet.class));
    }

    /**
     * Tests that primitive types are accepted in query parameters.
     */
    @Test
    public void testQueryParameterTypeWithStringConstructor() throws Exception {
        assertEquals("getQueryParameterStringConstructor:1234",
                     sendGoodRequestAndGetResponse("query/types/stringcstr?paramStringConstructor=1234",
                                                   HttpGet.class));
    }

    /**
     * Tests that primitive types are accepted in query parameters.
     */
    @Test
    public void testQueryParameterTypeWithValueOfMethod() throws Exception {
        assertEquals("getQueryParameterValueOf:456789",
                     sendGoodRequestAndGetResponse("query/types/valueof?staticValueOf=456",
                                                   HttpGet.class));
    }

    // This test can't use sendGoodRequestAndGetResponse() because that method
    // expects a 200; these requests are different and are meant to result
    // in exceptions thrown
    @Test
    // @AllowedFFDC
    public void testQueryParamException() throws Exception {

        String qpExcBaseUri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "queryparam/exception");
        // query constructor field exceptions
        HttpGet httpMethod = new HttpGet(qpExcBaseUri + "/fieldstrcstr?CustomStringConstructorFieldQuery=throwWeb");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(499, resp.getStatusLine().getStatusCode());
            assertEquals("ParamStringConstructor", asString(resp));
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/fieldstrcstr?CustomStringConstructorFieldQuery=throwNull");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/fieldstrcstr?CustomStringConstructorFieldQuery=throwEx");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        /*
         * query value of field exceptions
         */
        httpMethod = new HttpGet(qpExcBaseUri + "/fieldvalueof?CustomValueOfFieldQuery=throwWeb");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(498, resp.getStatusLine().getStatusCode());
            assertEquals("ParamValueOfWebAppEx", asString(resp));
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/fieldvalueof?CustomValueOfFieldQuery=throwNull");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/fieldvalueof?CustomValueOfFieldQuery=throwEx");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        /*
         * query string constructor property exceptions
         */
        httpMethod = new HttpGet(qpExcBaseUri + "/propertystrcstr?CustomStringConstructorPropertyHeader=throwWeb");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(499, resp.getStatusLine().getStatusCode());
            assertEquals("ParamStringConstructor", asString(resp));
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/propertystrcstr?CustomStringConstructorPropertyHeader=throwNull");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/propertystrcstr?CustomStringConstructorPropertyHeader=throwEx");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        /*
         * query value of property exceptions
         */
        httpMethod = new HttpGet(qpExcBaseUri + "/propertyvalueof?CustomValueOfPropertyHeader=throwWeb");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(498, resp.getStatusLine().getStatusCode());
            assertEquals("ParamValueOfWebAppEx", asString(resp));
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/propertyvalueof?CustomValueOfPropertyHeader=throwNull");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/propertyvalueof?CustomValueOfPropertyHeader=throwEx");
        try {
            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        httpMethod = new HttpGet(qpExcBaseUri + "/primitive?CustomNumQuery=notANumber");
        HttpResponse resp = client.execute(httpMethod);
        assertEquals(404, resp.getStatusLine().getStatusCode());
    }

    /**
     * Resource methods echo the parameters given to it, specified in path
     * Each method should send back a non-null value.
     */
    @Test
    public void testDiffCaptureVariableNames() throws Exception {

        String baseuri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "diffvarnames");

        HttpGet get = new HttpGet(baseuri + "/1234");
        HttpResponse resp = client.execute(get);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("id1_1234", asString(resp));

        HttpPost post = new HttpPost(baseuri + "/5678/post");
        resp = client.execute(post);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("id2_5678", asString(resp));

        HttpDelete del = new HttpDelete(baseuri + "/0001");
        resp = client.execute(del);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("id3_0001", asString(resp));
    }

    @Test
    public void testHttpServletResponseGetParamsWithCharset() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "form/httpServletRequestGetParam");

        HttpPost httpMethod = new HttpPost(uri);
        StringEntity entity = new StringEntity("id=213456", "UTF-8");
        entity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
        httpMethod.setEntity(entity);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("id=213456", responseBody);
    }

    @Test
    public void testHttpServletResponseGetParamsNoCharset() throws Exception {
        String uri = getBaseTestUri(paramwar, PARAM_URL_PATTERN, "form/httpServletRequestGetParam");

        HttpPost httpMethod = new HttpPost(uri);
        StringEntity entity = new StringEntity("id=213456", "UTF-8");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);

        HttpResponse resp = client.execute(httpMethod);
        String responseBody = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("id=213456", responseBody);
    }
}