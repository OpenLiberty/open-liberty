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
package com.ibm.ws.jaxrs20.fat.exceptionmappers;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXBContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
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
import com.ibm.ws.jaxrs.fat.exceptionmappers.mapped.Comment;
import com.ibm.ws.jaxrs.fat.exceptionmappers.mapped.CommentError;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class ExceptionMappersTest {

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

    private String getBaseTestUri() {
        return "http://localhost:" + getPort() + "/providers";
    }

    private final String mappedUri = getBaseTestUri() + "/exceptionsmapped/guestbookmapped";

    /**
     * Test the positive workflow where a comment with a message and author is
     * successfully posted to the Guestbook.
     *
     * @throws Exception
     */
    @Test
    public void testRegularWorkflow() throws Exception {

        // Clear messages first
        HttpPost postMethod = new HttpPost(mappedUri + "/clear");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        postMethod = new HttpPost(mappedUri);
        StringEntity entity = new StringEntity("<comment><message>Hello World!</message><author>Anonymous</author></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        String newPostURILocation;
        try {
            resp = client.execute(postMethod);
            assertEquals(201, resp.getStatusLine().getStatusCode());
            newPostURILocation = resp.getFirstHeader("Location").getValue();
        } finally {
            // Do this so that connection for GET below doesn't fail
            client.getConnectionManager().shutdown();
            client = new DefaultHttpClient();
        }

        HttpGet getMethod = new HttpGet(newPostURILocation);
        resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        Comment c = (Comment) JAXBContext.newInstance(Comment.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Anonymous", c.getAuthor());
        assertEquals(1, c.getId().intValue());
        assertEquals("Hello World!", c.getMessage());
    }

    /**
     * Tests a method that throws an emptily constructed
     * <code>WebApplicationException</code>.
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationExceptionDefaultMappedProvider() throws Exception {

        HttpPost postMethod = new HttpPost(mappedUri);
        StringEntity entity = new StringEntity("<comment></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertEquals(mappedUri, resp.getFirstHeader("ExceptionPage").getValue());
    }

    /**
     * Tests a method that throws a <code>WebApplicationException</code> with an
     * integer status code.
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationExceptionStatusCodeSetMappedProvider() throws Exception {

        HttpPost postMethod = new HttpPost(mappedUri);
        StringEntity entity = new StringEntity("<comment><message>Suppose to fail with missing author.</message></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(497, resp.getStatusLine().getStatusCode());
        assertEquals(mappedUri, resp.getFirstHeader("ExceptionPage").getValue());
    }

    /**
     * Tests a method that throws a <code>WebApplicationException</code> with a
     * Response.Status set.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("javax.ws.rs.core.NoContentException")
    public void testWebApplicationExceptionResponseStatusSetMappedProvider() throws Exception {

        HttpPost postMethod = new HttpPost(mappedUri);
        StringEntity entity = new StringEntity("");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(496, resp.getStatusLine().getStatusCode());
        assertEquals(mappedUri, resp.getFirstHeader("ExceptionPage").getValue());
    }

    /**
     * Tests a method that throws a <code>WebApplicationException</code> with a
     * Response with an entity (which will not get mapped via an exception
     * mapper).
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationExceptionResponseWithEntitySetMappedProvider() throws Exception {

        HttpPost postMethod = new HttpPost(mappedUri);
        StringEntity entity = new StringEntity("<comment><author>Anonymous</author></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(400, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Missing the message in the comment.", c.getErrorMessage());
    }

    /**
     * Tests a method that throws a <code>WebApplicationException</code> with a
     * Response with no entity (which will not get mapped via an exception
     * mapper).
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationExceptionResponseWithNoEntitySetMappedProvider() throws Exception {

        HttpPost postMethod = new HttpPost(mappedUri);
        StringEntity entity = new StringEntity("<comment><message>throwemptywebappexception</message><author>Anonymous</author></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(491, resp.getStatusLine().getStatusCode());
        assertEquals("Some message", resp.getFirstHeader("throwemptyentitywebappexception").getValue());
        assertEquals(mappedUri, resp.getFirstHeader("ExceptionPage").getValue());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("WebApplicationExceptionMapProvider set message", c.getErrorMessage());
    }

    /**
     * Tests a method that throws a subclass of
     * <code>WebApplicationException</code> with a Response.
     *
     * @throws Exception
     */
    @Test
    public void testCustomWebApplicationExceptionMappedProvider() throws Exception {

        HttpPost postMethod = new HttpPost(mappedUri);
        StringEntity entity = new StringEntity("<comment><message></message><author></author></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(498, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Cannot post an invalid message.", c.getErrorMessage());
    }

    /**
     * Tests a method that throws a runtime exception.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testRuntimeExceptionMappedProvider() throws Exception {

        /*
         * abcd is an invalid ID so a NumberFormatException will be thrown in
         * the resource
         */
        HttpDelete postMethod = new HttpDelete(mappedUri + "/abcd");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(450, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("For input string: \"abcd\"", c.getErrorMessage());
    }

    /**
     * Tests a method that throws a NullPointerException inside a called method.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testNullPointerExceptionMappedProvider() throws Exception {

        HttpDelete postMethod = new HttpDelete(mappedUri + "/10000");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(451, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("The comment did not previously exist.", c.getErrorMessage());
    }

    /**
     * Tests a method that throws an error.
     *
     * @throws Exception
     */
    @Test
    public void testErrorMappedProvider() throws Exception {

        HttpDelete postMethod = new HttpDelete(mappedUri + "/-99999");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(453, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Simulated error", c.getErrorMessage());
    }

    /**
     * Tests a method that throws a checked exception.
     *
     * @throws Exception
     */
    @Test
    public void testCheckExceptionMappedProvider() throws Exception {

        HttpPut putMethod = new HttpPut(mappedUri + "/-99999");
        StringEntity entity = new StringEntity("<comment><id></id><message></message><author></author></comment>");
        entity.setContentType("text/xml");
        putMethod.setEntity(entity);

        HttpResponse resp = client.execute(putMethod);
        assertEquals(454, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Unexpected ID.", c.getErrorMessage());
    }

    // Head's up: There will be some exceptions logged to trace or console log;
    // Do not fear, because exceptions are being thrown on purpose when several
    // of these test run.
    String nomappedUri = getBaseTestUri() + "/exceptionsnomapper/guestbooknomap";

    /**
     * Test the positive workflow where a comment with a message and author is
     * successfully posted to the Guestbook.
     *
     * @throws Exception
     */
    public void testRegularWorkflow_noMapped() throws Exception {
        HttpPost postMethod = new HttpPost(nomappedUri + "/clear");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        postMethod = new HttpPost(nomappedUri);
        StringEntity entity = new StringEntity("<comment><message>Hello World!</message><author>Anonymous</author></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        resp = client.execute(postMethod);
        assertEquals(201, resp.getStatusLine().getStatusCode());
        String newPostURILocation = resp.getFirstHeader("Location").getValue();

        HttpGet getMethod = new HttpGet(newPostURILocation);
        resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        // There are two sets of Comment* classes, in *mapped and *nomapper packages.
        // Unless there's an error, just use the classes in mapped, even for the
        // "nomapper" tests, because they're pretty much identical.
        Comment c = (Comment) JAXBContext.newInstance(Comment.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Anonymous", c.getAuthor());
        assertEquals(1, c.getId().intValue());
        assertEquals("Hello World!", c.getMessage());
    }

    /**
     * Tests a method that throws an emptily constructed
     * <code>WebApplicationException</code>.
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationExceptionDefaultNoMappingProvider() throws Exception {

        HttpPost postMethod = new HttpPost(nomappedUri);
        StringEntity entity = new StringEntity("<comment></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertEquals("", asString(resp));
    }

    /**
     * Tests a method that throws a <code>WebApplicationException</code> with an
     * integer status code.
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationExceptionStatusCodeSetNoMappingProvider() throws Exception {

        HttpPost postMethod = new HttpPost(nomappedUri);
        StringEntity entity = new StringEntity("<comment><message>Suppose to fail with missing author.</message></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(499, resp.getStatusLine().getStatusCode());
        assertEquals("", asString(resp));
    }

    /**
     * Tests a method that throws a <code>WebApplicationException</code> with a
     * Response.Status set.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("javax.ws.rs.core.NoContentException")
    public void testWebApplicationExceptionResponseStatusSetNoMappingProvider() throws Exception {

        HttpPost postMethod = new HttpPost(nomappedUri);
        StringEntity entity = new StringEntity("");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(400, resp.getStatusLine().getStatusCode());
        assertEquals("", asString(resp));
    }

    /**
     * Tests a method that throws a <code>WebApplicationException</code> with a
     * Response.
     *
     * @throws Exception
     */
    @Test
    public void testWebApplicationExceptionResponseSetNoMappingProvider() throws Exception {

        HttpPost postMethod = new HttpPost(nomappedUri);
        StringEntity entity = new StringEntity("<comment><author>Anonymous</author></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(400, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Missing the message in the comment.", c.getErrorMessage());
    }

    /**
     * Tests a method that throws a subclass of
     * <code>WebApplicationException</code> with a Response.
     *
     * @throws Exception
     */
    @Test
    public void testCustomWebApplicationExceptionNoMappingProvider() throws Exception {

        HttpPost postMethod = new HttpPost(nomappedUri);
        StringEntity entity = new StringEntity("<comment><message></message><author></author></comment>");
        entity.setContentType("text/xml");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);
        assertEquals(498, resp.getStatusLine().getStatusCode());

        CommentError c = (CommentError) JAXBContext.newInstance(CommentError.class.getPackage().getName()).createUnmarshaller().unmarshal(resp.getEntity().getContent());
        assertEquals("Cannot post an invalid message.", c.getErrorMessage());
    }

    // Depending on the build type (or JDK used?), test F/W can fail because FFDCs are logged.
    // I can't see those failures in my own local build or personal build, and it seems like it
    // varies depending on the platform or build type. So, use AllowedFFDC versus ExpectedFFDC;
    // using the latter causes *some* build types to fail because test doesn't seem to mind or
    // even detect the FFDC log files. AllowedFFDC is more lenient.
    /**
     * Tests a method that throws a runtime exception.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.lang.NumberFormatException")
    public void testRuntimeExceptionNoMappingProvider() throws Exception {

        // nomapped.Guestbook.deleteMessage() takes in a String param and
        // tries to convert that an Integer; but that results in a
        // NumberFormatException because "abcd" is invalid argument to
        // Integer.valueOf()--this is on purpose, so don't be alarmed to see
        // that in log files.
        HttpDelete postMethod = new HttpDelete(nomappedUri + "/abcd");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests a method that throws a NullPointerException inside a called method.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.lang.NullPointerException")
    public void testNullPointerExceptionNoMappingProvider() throws Exception {

        HttpDelete postMethod = new HttpDelete(nomappedUri + "/10000");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests a method that throws an error.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("org.apache.cxf.interceptor.Fault")
    public void testErrorNoMappingProvider() throws Exception {

        HttpDelete postMethod = new HttpDelete(nomappedUri + "/-99999");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests a method that throws a checked exception.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("org.apache.cxf.interceptor.Fault")
    public void testCheckExceptionNoMappingProvider() throws Exception {

        HttpPut putMethod = new HttpPut(nomappedUri + "/-99999");
        StringEntity entity = new StringEntity("<comment><id></id><message></message><author></author></comment>");
        entity.setContentType("text/xml");
        putMethod.setEntity(entity);

        HttpResponse resp = client.execute(putMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    private final String nullconditionsUri = getBaseTestUri() + "/exceptionsnull/guestbooknullconditions";

    /**
     * Tests that an empty constructor constructed
     * <code>WebApplicationException</code> will return status 500 and no
     * response body by default.
     *
     * @throws Exception
     */

    @Test
    public void testEmptyWebException() throws Exception {

        HttpGet getMethod = new HttpGet(nullconditionsUri + "/emptywebappexception");
        HttpResponse resp = client.execute(getMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertEquals("RuntimeExceptionMapper was used", asString(resp));
    }

    /**
     * Tests that a <code>WebApplicationException</code> constructed with a
     * cause will return status 500 and no response body by default.
     *
     * @throws Exception
     */
    @Test
    public void testWebExceptionWithCause() throws Exception {

        HttpGet getMethod = new HttpGet(nullconditionsUri + "/webappexceptionwithcause");
        HttpResponse resp = client.execute(getMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertEquals("RuntimeExceptionMapper was used", asString(resp));
    }

    /**
     * Tests that a <code>WebApplicationException</code> constructed with a
     * cause and status will return status and no response body by default.
     *
     * @throws Exception
     */

    @Test
    public void testWebExceptionWithCauseAndStatus() throws Exception {

        HttpPost postMethod = new HttpPost(nullconditionsUri + "/webappexceptionwithcauseandstatus");
        HttpResponse resp = client.execute(postMethod);
//        assertEquals(499, resp.getStatusLine().getStatusCode());
        assertEquals("RuntimeExceptionMapper was used", asString(resp));
    }

    /**
     * Tests that a <code>WebApplicationException</code> constructed with a
     * cause and response will return the Response entity by default.
     *
     * @throws Exception
     */

    @Test
    public void testWebExceptionWithCauseAndResponse() throws Exception {

        HttpPut putMethod = new HttpPut(nullconditionsUri + "/webappexceptionwithcauseandresponse");
        HttpResponse resp = client.execute(putMethod);
        // Response.Status.NOT_ACCEPTABLE
//        assertEquals(406, resp.getStatusLine().getStatusCode());
        assertEquals("RuntimeExceptionMapper was used", asString(resp));
    }

    /**
     * Tests that a <code>WebApplicationException</code> constructed with a
     * cause and response status will return the response status and empty
     * response body by default.
     *
     * @throws Exception
     */

    @Test
    public void testWebExceptionWithCauseAndResponseStatus() throws Exception {

        HttpDelete deleteMethod = new HttpDelete(nullconditionsUri + "/webappexceptionwithcauseandresponsestatus");
        HttpResponse resp = client.execute(deleteMethod);
        // Response.Status.BAD_REQUEST
//        assertEquals(400, resp.getStatusLine().getStatusCode());
        assertEquals("RuntimeExceptionMapper was used", asString(resp));
    }

    /**
     * Tests that a <code>ExceptionMapper</code> that returns null should see a
     * HTTP 204 status.
     *
     * @throws Exception
     */

    //comment out this case out as this is not supported by CXF
    // @Test
    public void testExceptionMapperReturnNull() throws Exception {

        HttpGet getMethod = new HttpGet(nullconditionsUri + "/exceptionmappernull");
        HttpResponse resp = client.execute(getMethod);
        // Response.Status.NO_CONTENT
        assertEquals(204, resp.getStatusLine().getStatusCode());
        // Original test expected to be null?
        assertEquals("", asString(resp));
    }

    /**
     * Tests that a <code>ExceptionMapper</code> that throws an exception or
     * error should see a HTTP 500 status error and empty response.
     *
     * @throws Exception
     */
    @Test
    public void testExceptionMapperThrowsException() throws Exception {

        HttpPost postMethod = new HttpPost(nullconditionsUri + "/exceptionmapperthrowsexception");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertEquals("", asString(resp));
    }

    /**
     * Tests that a <code>ExceptionMapper</code> that throws an error should see
     * a HTTP 500 status error and unknown response.
     *
     * @throws Exception
     */
    //TODO
    //open an issue to CXF:
    //http://cxf.547215.n5.nabble.com/Throw-Error-in-ExceptionMapper-td5747662.html
    //@Test
    public void testExceptionMapperThrowsError() throws Exception {

        HttpPost postMethod = new HttpPost(nullconditionsUri + "/exceptionmapperthrowserror");
        HttpResponse resp = client.execute(postMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertEquals("", asString(resp));
    }

    /**
     * Tests that a <code>ExceptionMapper</code> can catch a generic Throwable.
     *
     * @throws Exception
     */
    @Test
    public void testExceptionMapperForSpecificThrowable() throws Exception {

        HttpPut putMethod = new HttpPut(nullconditionsUri + "/throwableexceptionmapper");
        HttpResponse resp = client.execute(putMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        assertEquals("Throwable mapper used", asString(resp));
    }

    /**
     * Tests that a Throwable can propagate throughout the code.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("org.apache.cxf.interceptor.Fault")
    public void testThrowableCanBeThrown() throws Exception {

        HttpDelete deleteMethod = new HttpDelete(nullconditionsUri + "/throwsthrowable");
        HttpResponse resp = client.execute(deleteMethod);
        assertEquals(500, resp.getStatusLine().getStatusCode());
        String strResp = asString(resp);
        assertTrue(strResp.contains("nullconditions.GuestbookResource$1: Throwable was thrown"));
    }
}
