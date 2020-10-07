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
package com.ibm.ws.jaxrs20.fat.subresource;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.xml.bind.JAXBContext;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxrs.fat.subresource.Comment;
import com.ibm.ws.jaxrs.fat.subresource.CommentError;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class ExceptionsSubresourcesTest {
    private static final Class<?> c = ExceptionsSubresourcesTest.class;

    // ExceptionMappersTest, ExceptionsSubresourcesTest, and StandardProviders rely
    // on the providers.war app deployed in com.ibm.ws.jaxrs.fat.providers
    @Server("com.ibm.ws.jaxrs.fat.providers")
    public static LibertyServer server;

    private static HttpClient client;
    private static final String providerswar = "providers";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, providerswar, "com.ibm.ws.jaxrs.fat.exceptionmappers.mapped",
                                      "com.ibm.ws.jaxrs.fat.exceptionmappers.nomapper",
                                      "com.ibm.ws.jaxrs.fat.exceptionmappers.nullconditions",
                                      "com.ibm.ws.jaxrs.fat.provider.readerwritermatch",
                                      "com.ibm.ws.jaxrs.fat.standard",
                                      "com.ibm.ws.jaxrs.fat.standard.jaxb",
                                      "com.ibm.ws.jaxrs.fat.standard.multipart",
                                      "com.ibm.ws.jaxrs.fat.subresource");

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

    // Should be the common URL for all test methods
    private String getSubresTestUri() {
        String uri = getBaseTestUri(providerswar, "subresourceexceptions", "guestbooksubresources");
        return uri;
    }

    /**
     * Test the positive workflow where a comment with a message and author is
     * successfully posted to the Guestbook.
     *
     * @throws Exception
     */
    @Test
    public void testRegularWorkflow() throws Exception {

        HttpPost postMethod = new HttpPost(getSubresTestUri() + "/commentdata");
        StringEntity entity = new StringEntity("<comment><id>10000</id><author>Anonymous</author><message>Hi there</message></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);
        HttpResponse resp;

        try {
            resp = client.execute(postMethod);
            assertEquals(201, resp.getStatusLine().getStatusCode());
        } finally {
            // Do this so that connection for GET below doesn't fail
            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();
        }

        String postURILocation = resp.getFirstHeader("Location").getValue();

        HttpGet getMethod = new HttpGet(postURILocation);

        client.execute(getMethod);
        assertEquals(201, resp.getStatusLine().getStatusCode());

        Comment c = (Comment) JAXBContext.newInstance(Comment.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Anonymous", c.getAuthor());
        assertEquals(10000, c.getId().intValue());
        assertEquals("Hi there", c.getMessage());
    }

    /**
     * Same test as above, but ensures that @OPTIONS methods can be invoked
     * on sub-resource locators.
     *
     * @throws Exception
     */
    @Test
    public void testRegularWorkflow_OPTIONS() throws Exception {
        HttpOptions optionsMethod = new HttpOptions(getSubresTestUri() + "/commentdata");
        HttpResponse resp;

        try {
            resp = client.execute(optionsMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            server.waitForStringInLog("Invoked CommentData.checkOptions");
        } finally {
            // Do this so that connection for GET below doesn't fail
            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();
        }
    }

    /**
     * We need to be able to invoke OPTIONS methods on sub-resource locators,
     * but we also need to ensure that when an OPTIONS method is not defined
     * that we get the correct response header (Allow) when invoking on
     * resources that contain not-quite-matching sub resource locator methods.
     * For example we want to see "Allow: GET,DELETE,HEAD,OPTIONS" for the
     * following sample resource (/root/sub/all):
     * <code>
     * @Path("/root")
     * public class Root {
     *
     * @GET
     *      @Path("/sub/all")
     *      public Response getSubAll() {...}
     *
     * @DELETE
     *         @Path("/sub/all")
     *         public Response deleteSubAll() {...}
     *
     *         @Path("/sub/{id}")
     *         public Sub sub(@PathParam("id") int id) {...}
     *         }
     *         </code>
     *
     *         Note the last method returns a sub resource locator object (Sub) but
     *         it uses an integer for the method - so "/root/sub/all" should not
     *         match. This test verifies this case.
     *
     */
    @Test
    public void testOPTIONSisNotMatchedByIntegerParams() throws Exception {
        HttpOptions optionsMethod = new HttpOptions(getSubresTestUri() + "/page/list");
        HttpResponse resp;

        try {
            resp = client.execute(optionsMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            Header[] allowHeaders = resp.getHeaders("Allow");
            boolean foundGET = false;
            for (Header h : allowHeaders) {
                Log.info(c, "testOPTIONSisNotMatchedByIntegerParams", "Allow header: " + h.getValue());
                String headerVal = h.getValue();
                if (headerVal.contains("DELETE")) {
                    fail("Reported DELETE (from sub resource locator) - only GET expected");
                }
                if (headerVal.contains("GET")) {
                    foundGET = true;
                }
            }
            assertTrue("Did not find expected GET method in Allowed header", foundGET);
        } finally {
            // Do this so that connection for GET below doesn't fail
            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();
        }
    }

    /**
     * Tests that we can get to a sub resource locator target when passing in an
     * integer to a method that expects a double.
     */
    @Test
    public void testPageWorkflow_Int() throws Exception {
        // we can pass in a 17, but since the method is expecting a double, we should expect 17.0
        testPageWorkflow("17", "17.0");
    }

    /**
     * Tests that we can get to a sub resource locator target when passing in a
     * normal floating point number to a method that expects a double.
     */
    @Test
    public void testPageWorkflow_Double() throws Exception {
        testPageWorkflow("-6.07", "-6.07");
    }

    /**
     * Tests that we can get to a sub resource locator target when passing in different
     * variations of floating point numbers using positives and negatives and exponents.
     */
    @Test
    public void testPageWorkflow_DoubleInScientificForm() throws Exception {
        testPageWorkflow("1.203E5", "120300.0");
        testPageWorkflow("1.203E7", "1.203E7");
        testPageWorkflow("2.321E-9", "2.321E-9");
        testPageWorkflow("5.4e8", "5.4E8");
        testPageWorkflow("+7.3e10", "7.3E10");
    }

    /**
     * Tests that if we pass a non-numeric value to a method that expects a double, that
     * we get a 404 response code.
     */
    @Test
    public void testPageWorkflow_InvalidFloatingPoints() throws Exception {
        testPageWorkflowNegative("one-point-seven");
        testPageWorkflowNegative("5oh7");
        testPageWorkflowNegative("pi");
        testPageWorkflowNegative("1.2E3E4"); // extra E4 after valid scientific notation
    }

    private void testPageWorkflow(final String pageNum, final String expectedValue) throws Exception {
        HttpGet getMethod = new HttpGet(getSubresTestUri() + "/page/list");
        HttpPut putMethod = new HttpPut(getSubresTestUri() + "/page/" + pageNum + "?data=Hello");
        HttpDelete deleteMethod = new HttpDelete(getSubresTestUri() + "/page/" + pageNum);
        HttpOptions optionsMethod = new HttpOptions(getSubresTestUri() + "/page/" + pageNum);
        HttpResponse resp;

        String responseStr = null;
        try {
            resp = client.execute(getMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            responseStr = asString(resp).trim();
            Log.info(c, "testPageWorkflow", responseStr);
            // no results expected
            assertEquals("Unexpected content prior to PUTting", "", responseStr);

            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();

            //put a page entry in the guest book:
            resp = client.execute(putMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());

            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();

            // check if it is there:
            resp = client.execute(getMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            responseStr = asString(resp).trim();
            Log.info(c, "testPageWorkflow", responseStr);
            // one results expected
            assertEquals("Unexpected content expected \"" + expectedValue + "\"", expectedValue, responseStr);

            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();

            // let's see what methods we can call on it:
            resp = client.execute(optionsMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            Header[] allowHeaders = resp.getHeaders("Allow");
            boolean foundGET = false;
            boolean foundPUT = false;
            boolean foundDELETE = false;
            for (Header h : allowHeaders) {
                String headerVal = h.getValue();
                if (headerVal.contains("GET")) {
                    foundGET = true;
                }
                if (headerVal.contains("PUT")) {
                    foundPUT = true;
                }
                if (headerVal.contains("DELETE")) {
                    foundDELETE = true;
                }
            }
            assertTrue("Did not find expected GET method in Allowed header", foundGET);
            assertTrue("Did not find expected PUT method in Allowed header", foundPUT);
            assertTrue("Did not find expected DELETE method in Allowed header", foundDELETE);

            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();

        } finally {

            try {
                // now delete it:
                resp = client.execute(deleteMethod);
                assertEquals(200, resp.getStatusLine().getStatusCode());

                client.getConnectionManager().shutdown();
                client = new DefaultHttpClient();

                // finally, confirm that it was actually deleted:
                resp = client.execute(getMethod);
                assertEquals(200, resp.getStatusLine().getStatusCode());
                responseStr = asString(resp).trim();
                Log.info(c, "testPageWorkflow", responseStr);
                // no results expected
                assertEquals("Unexpected content after DELETE", "", responseStr);
            } catch (Exception ex) {
                Log.error(c, "testPageWorkflow - exception caught cleaning up - this could be expected if the test failed", ex);
            } finally {
                // Do this so that connection for GET below doesn't fail
                client.getConnectionManager().shutdown();
                client = new DefaultHttpClient();
            }
        }
    }

    private void testPageWorkflowNegative(final String pageNum) throws Exception {
        // this tests that a 404 is returned when using a value that is definitely NOT considered a floating point number
        HttpGet getMethod = new HttpGet(getSubresTestUri() + "/page/list");
        HttpPut putMethod = new HttpPut(getSubresTestUri() + "/page/" + pageNum + "?data=Hello");

        HttpResponse resp;

        String responseStr = null;
        try {
            resp = client.execute(getMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            responseStr = asString(resp).trim();
            Log.info(c, "testPageWorkflow", responseStr);
            // no results expected
            assertEquals("Unexpected content prior to PUTting", "", responseStr);

            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();

            //put a page entry in the guest book:
            resp = client.execute(putMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());

            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();
        } finally {
            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();
        }
    }
    /**
     * Test that a <code>WebApplicationException</code> thrown from a
     * sub-resource is still processed properly.
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationException() throws Exception {

        HttpPost postMethod = new HttpPost(getSubresTestUri() + "/commentdata");
        StringEntity entity = new StringEntity("<comment></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        // Status.BAD_REQUEST
        assertEquals(400, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Please include a comment ID, a message, and your name.", c.getErrorMessage());
    }

    /**
     * Test that a checked exception is processed properly.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("org.apache.cxf.interceptor.Fault")
    public void testCheckedException() throws Exception {

        HttpPut putMethod = new HttpPut(getSubresTestUri() + "/commentdata");
        StringEntity entity = new StringEntity("<comment></comment>");
        entity.setContentType("text/xml");
        putMethod.setEntity(entity);

        HttpResponse resp = client.execute(putMethod);
        String str = asString(resp);
        // Status.INTERNAL_SERVER_ERROR
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertTrue(str.contains("com.ibm.ws.jaxrs.fat.subresource.GuestbookException: Unexpected ID"));
    }

    /**
     * Test the positive workflow where a comment with a message and author is
     * successfully posted to the Guestbook.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.lang.NumberFormatException")
    public void testRuntimeException() throws Exception {

        HttpDelete deleteMethod = new HttpDelete(getSubresTestUri() + "/commentdata/afdsfsdf");

        HttpResponse resp = client.execute(deleteMethod);
        String str = asString(resp);
        // Status.INTERNAL_SERVER_ERROR
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertTrue(str.contains("java.lang.NumberFormatException.forInputString"));
    }
}
