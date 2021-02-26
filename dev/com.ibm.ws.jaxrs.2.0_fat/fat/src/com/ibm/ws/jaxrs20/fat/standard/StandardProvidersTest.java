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
package com.ibm.ws.jaxrs20.fat.standard;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.wink.common.internal.providers.entity.DataSourceProvider;
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

/**
 * Centralized test for built-in standard providers required by the JAX-RS specification.
 */
@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class StandardProvidersTest {

    @Server("com.ibm.ws.jaxrs.fat.providers")
    public static LibertyServer server;

    private static HttpClient httpClient;
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
            server.stopServer();
        }
    }

    @Before
    public void getHttpClient() {
        httpClient = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        httpClient.getConnectionManager().shutdown();
    }

    private String getStandardTestURI() {
        return "http://localhost:" + getPort() + "/providers/stdtest";
    }

    /**
     * Tests sending in no request entity to a String entity parameter.
     */
    @Test
    public void testSendingNoRequestEntityString() throws Exception {
        // Since the context is "providers" as well, the resulting URL is goofy,
        // like http://localhost:8010/providers/stdtest/providers/standard/string/empty
        // but keep b/c removing the second "providers" would require
        // changing all the *Resource classes, too.
        String uri = getStandardTestURI() + "/providers/standard/string/empty";
        HttpPost post = new HttpPost(uri);
        HttpResponse resp = httpClient.execute(post);
        String respContent = asString(resp);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", respContent);

    }

    // URL for test methods from JAXRSByteArrayTest
    private final String byteArrayTestUri = getStandardTestURI() + "/providers/standard/bytesarray";

    /**
     * Tests posting a byte array.
     */
    @Test
    public void testPostByteArray() throws Exception {
        HttpPost post = new HttpPost(byteArrayTestUri);
        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        post.addHeader(new BasicHeader("Accept", "text/plain"));

        ByteArrayEntity postReq = new ByteArrayEntity(barr);
        postReq.setContentType("text/plain");
        post.setEntity(postReq);

        HttpResponse resp = httpClient.execute(post);

        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("text/plain", resp.getFirstHeader("Content-Type").getValue());
        assertEquals(barr.length, Integer.valueOf(resp.getFirstHeader("Content-Length").getValue()).intValue());
    }

    /**
     * Tests putting and then getting a byte array.
     */
    @Test
    public void testPutByteArray() throws Exception {

        HttpPut put = new HttpPut(byteArrayTestUri);
        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);

        ByteArrayEntity postReq = new ByteArrayEntity(barr);
        postReq.setContentType("bytes/array");
        put.setEntity(postReq);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(byteArrayTestUri);

        HttpResponse getResp = httpClient.execute(get);
        assertEquals(200, getResp.getStatusLine().getStatusCode());
        InputStream is = getResp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        String contentType =
                        (getResp.getFirstHeader("Content-Type") == null) ? null : getResp
                                        .getFirstHeader("Content-Type").getValue();
        int length = Integer.valueOf(getResp.getFirstHeader("Content-Length").getValue()).intValue();

        assertNotNull("text/xml", contentType);
        assertEquals(barr.length, length);
    }

    /**
     * Tests receiving an empty byte array.
     */
    @Test
    public void testByteArrayAcceptMatchesReturnContentType() throws Exception {

        HttpPut put = new HttpPut(byteArrayTestUri);
        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);

        ByteArrayEntity putReq = new ByteArrayEntity(barr);
        putReq.setContentType("any/type");
        put.setEntity(putReq);

        HttpResponse putResp = httpClient.execute(put);
        assertEquals(204, putResp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(byteArrayTestUri);
        get.addHeader("Accept", "mytype/subtype");

        HttpResponse getResp = httpClient.execute(get);
        assertEquals(200, getResp.getStatusLine().getStatusCode());
        InputStream is = getResp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        assertEquals("mytype/subtype", getResp.getFirstHeader("Content-Type").getValue());
        assertEquals(barr.length, Integer.valueOf(getResp.getFirstHeader("Content-Length")
                        .getValue()).intValue());
    }

    /**
     * Tests posting an request parameter to a byte[] entity parameter.
     */
    @Test
    public void testSendingNoRequestEntityByteArray() throws Exception {

        String postUri = byteArrayTestUri + "/empty";
        HttpPost post = new HttpPost(postUri);

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    // URL for test methods from JAXRSDataSourceTest
    private final String dsTestURI = getStandardTestURI() + "/providers/standard/datasource";

    /**
     * Tests posting to a DataSource entity parameter.
     */
    @Test
    public void testPostDataSource() throws Exception {

        HttpPost post = new HttpPost(dsTestURI);
        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);

        ByteArrayEntity postReq = new ByteArrayEntity(barr);
        postReq.setContentType("text/plain");
        post.setEntity(postReq);
        post.addHeader("Accept", "text/plain");

        HttpResponse resp = httpClient.execute(post);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        assertEquals("text/plain", resp.getFirstHeader("Content-Type").getValue());
        assertEquals("1000", resp.getFirstHeader("Content-Length").getValue());
    }

    /**
     * Tests putting and then getting a DataSource entity.
     */
    @Test
    public void testPutDataSource() throws Exception {

        HttpPut put = new HttpPut(dsTestURI);
        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity putReq = new ByteArrayEntity(barr);
        putReq.setContentType("bytes/array");
        put.setEntity(putReq);

        HttpResponse putResp = httpClient.execute(put);
        assertEquals(204, putResp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(dsTestURI);
        HttpResponse getResp = httpClient.execute(get);
        assertEquals(200, getResp.getStatusLine().getStatusCode());
        InputStream is = getResp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }

        String contentType = getResp.getFirstHeader("Content-Type").getValue();
        String contentLength = getResp.getFirstHeader("Content-Length").getValue();
        // ccording to spec,default content-type for response is application/octet-stream
        assertEquals("application/octet-stream", contentType);
        assertEquals("1000", contentLength);
    }

    /**
     * Tests receiving a DataSource with any media type.
     */
    @Test
    public void testDSAcceptMatchesReturnContentType() throws Exception {

        HttpPut put = new HttpPut(dsTestURI);

        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity putReq = new ByteArrayEntity(barr);
        putReq.setContentType("any/type");
        put.setEntity(putReq);

        HttpResponse putResp = httpClient.execute(put);
        assertEquals(204, putResp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(dsTestURI);
        get.addHeader("Accept", "mytype/subtype");

        HttpResponse getResp = httpClient.execute(get);
        assertEquals(200, getResp.getStatusLine().getStatusCode());
        InputStream is = getResp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        assertEquals("mytype/subtype", getResp.getFirstHeader("Content-Type").getValue());
        assertEquals("1000", getResp.getFirstHeader("Content-Length").getValue());
    }

    /**
     * Tests posting to a DataSource subclass. This should result in a 500
     * error.
     */
    // @Test
    //TODO
    public void testPostDataSourceSubclass() throws Exception {

        String postUri = dsTestURI + "/subclass/should/fail";
        HttpPost post = new HttpPost(postUri);

        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity postReq = new ByteArrayEntity(barr);
        postReq.setContentType("text/plain");
        post.setEntity(postReq);
        post.addHeader("Accept", "text/plain");

        HttpResponse resp = httpClient.execute(post);
        //This is supported via CXF-5835
        assertEquals(200, resp.getStatusLine().getStatusCode());
    }

    /**
     * Verify that we can send a DataSource and receive a DataSource. The 'POST'
     * method on the resource we are calling is a simple echo.
     */
    @Test
    public void testPOSTDataSource() throws Exception {

        String postUri = getStandardTestURI() + "/dstest";
        HttpPost post = new HttpPost(postUri);
        String input = "This is some test input";
        ByteArrayEntity postReq = new ByteArrayEntity(input.getBytes());
        postReq.setContentType("application/datasource");
        post.setEntity(postReq);

        HttpResponse resp = httpClient.execute(post);

        // just use our provider to read the response (original comment)
        // But why go to this much trouble???
        DataSourceProvider provider = new DataSourceProvider();
        DataSource returnedData =
                        provider.readFrom(DataSource.class,
                                          null,
                                          null,
                                          new MediaType("application", "datasource"),
                                          null,
                                          resp.getEntity().getContent());

        assertNotNull(returnedData);
        assertNotNull(returnedData.getInputStream());
        byte[] responseBytes = new byte[input.getBytes().length];
        returnedData.getInputStream().read(responseBytes);
        assertNotNull(responseBytes);
        String response = new String(responseBytes);
        assertEquals("This is some test input", response);
    }

    /**
     * Tests posting an empty request entity to a DataSource entity parameter.
     */
    @Test
    public void testSendingNoRequestEntityDataSource() throws Exception {

        String postUri = getStandardTestURI() + "/dstest/empty";
        HttpPost post = new HttpPost(postUri);

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    // URL for test methods from JAXRSFileTest
    private final String fileTestUri = getStandardTestURI() + "/providers/standard/file";

    /**
     * Tests posting to a File entity parameter.
     */
    @Test
    public void testPostFile() throws Exception {

        HttpPost post = new HttpPost(fileTestUri);

        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity postReq = new ByteArrayEntity(barr);
        postReq.setContentType("text/plain");
        post.setEntity(postReq);
        post.addHeader("Accept", "text/plain");

        HttpResponse resp = httpClient.execute(post);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        assertEquals("text/plain", resp.getFirstHeader("Content-Type").getValue());
        assertEquals(1000, Integer.valueOf(resp.getFirstHeader("Content-Length")
                        .getValue()).intValue());

        /* TODO : need to test that any temporary files created are deleted */
    }

    /**
     * Tests receiving an empty byte array.
     */
    @Test
    public void testFileAcceptMatchesReturnContentType() throws Exception {

        HttpPut put = new HttpPut(fileTestUri);

        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity putReq = new ByteArrayEntity(barr);
        putReq.setContentType("any/type");
        put.setEntity(putReq);

        HttpResponse putResp = httpClient.execute(put);
        assertEquals(204, putResp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(fileTestUri);
        get.addHeader("Accept", "mytype/subtype");
        HttpResponse resp = httpClient.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[1000];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        assertEquals("mytype/subtype", resp.getFirstHeader("Content-Type").getValue());
        assertEquals(barr.length, Integer.valueOf(resp.getFirstHeader("Content-Length")
                        .getValue()).intValue());
    }

    /**
     * Tests posting to a File entity parameter with no incoming request entity.
     */
    @Test
    public void testPostFileEmptyRequestEntity() throws Exception {

        HttpPost post = new HttpPost(fileTestUri + "/empty");
        HttpResponse resp = httpClient.execute(post);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    // URL for test methods from JAXRSInputStreamTest
    private final String isTestUri = getStandardTestURI() + "/providers/standard/inputstream";

    /**
     * Tests posting to an InputStream
     */
    @Test
    public void testPostInputStream() throws Exception {

        HttpPost post = new HttpPost(isTestUri);
        byte[] barr = new byte[50000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity postReq = new ByteArrayEntity(barr);
        postReq.setContentType("text/plain");
        post.setEntity(postReq);
        post.addHeader("Accept", "text/plain");

        HttpResponse resp = httpClient.execute(post);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[barr.length];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }

        assertEquals("text/plain", resp.getFirstHeader("Content-Type").getValue());
        // Original test expects Content-Length header to be null, too
        assertNull(resp.getFirstHeader("Content-Length"));

    }

    /**
     * Tests putting and then getting a byte array.
     *
     * @throws HttpException
     * @throws IOException
     */
    @Test
    public void testPutInputStream() throws HttpException, IOException {

        HttpPut put = new HttpPut(isTestUri);
        byte[] barr = new byte[50000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity putReq = new ByteArrayEntity(barr);
        putReq.setContentType("bytes/array");
        put.setEntity(putReq);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(isTestUri);
        resp = httpClient.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[barr.length];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }

        String contentType = (resp.getFirstHeader("Content-Type") == null) ? null : resp.getFirstHeader("Content-Type").getValue();
        assertNotNull(contentType);
        // Original test expects Content-Length header to be null, too
        assertNull(resp.getFirstHeader("Content-Length"));

    }

    /**
     * Tests receiving an empty byte array.
     *
     */
    @Test
    public void testISAcceptMatchesReturnContentType() throws Exception {

        HttpPut put = new HttpPut(isTestUri);
        byte[] barr = new byte[100000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity putReq = new ByteArrayEntity(barr);
        putReq.setContentType("any/type");
        put.setEntity(putReq);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(isTestUri);
        get.addHeader("Accept", "mytype/subtype");
        resp = httpClient.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[barr.length];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        assertEquals("mytype/subtype", resp.getFirstHeader("Content-Type").getValue());
        // Original test expects Content-Length header to be null, too
        assertNull(resp.getFirstHeader("Content-Length"));
    }

    /**
     * Tests a resource method invoked with a ByteArrayInputStream as a
     * parameter. This should fail with a 500 since the reader has no way to
     * necessarily wrap it to the type.
     */
    //TODO: this should be workable after we bring back CXF 5846
    // @Test
    public void testInputStreamImplementation() throws Exception {

        String uri = isTestUri + "/subclasses/shouldfail";
        HttpPost post = new HttpPost(uri);
        byte[] barr = new byte[100000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity postReq = new ByteArrayEntity(barr);
        postReq.setContentType("any/type");
        post.setEntity(postReq);

        HttpResponse resp = httpClient.execute(post);
        // according to spec, we can not put a subclass or implementation class, so we need return 500
        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests posting to an InputStream entity parameter with no incoming request entity.
     */
    @Test
    public void testPostInputStreamEmptyRequestEntity() throws Exception {

        String uri = isTestUri + "/empty";
        HttpPost post = new HttpPost(uri);

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    /**
     * Tests posting an empty request entity to a InputStream.
     */
    @Test
    public void testSendingNoRequestEntityInputStream() throws Exception {

        String uri = isTestUri + "/empty";
        HttpPost post = new HttpPost(uri);

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    /**
     * Tests sending in no request entity to a JAXB entity parameter.
     */
    @Test
    @AllowedFFDC("javax.ws.rs.core.NoContentException")
    public void testSendingNoRequestEntityJAXB() throws Exception {

        String jaxbUri = getStandardTestURI() + "/providers/standard/jaxb/empty";
        HttpPost post = new HttpPost(jaxbUri);
        post.addHeader("Content-Type", MediaType.TEXT_XML);

        HttpResponse resp = httpClient.execute(post);
        assertEquals(400, resp.getStatusLine().getStatusCode());
    }

    // URL for test methods from JAXRSMultivaluedMapTest
    private final String multivalTestUri = getStandardTestURI() + "/providers/standard/multivaluedmap";

    /**
     * Tests posting to a MultivaluedMap with application/x-www-form-urlencoded
     * request Content-Type.
     */
    @Test
    public void testPostMultivaluedMap() throws Exception {

        HttpPost post = new HttpPost(multivalTestUri);
        StringEntity entity = new StringEntity("tuv=wxyz&abcd=", "UTF-8");
        entity.setContentType("application/x-www-form-urlencoded");
        post.setEntity(entity);

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        String str = asString(resp);
        assertTrue(str, "abcd=&tuv=wxyz".equals(str) || "tuv=wxyz&abcd=".equals(str));
        assertEquals("application/x-www-form-urlencoded", resp
                        .getFirstHeader("Content-Type").getValue());
        Header contentLengthHeader = post.getFirstHeader("Content-Length");
        if (contentLengthHeader != null) {
            // some of the containers can be "smarter" and set the
            // content-length for us if the payload is small
            assertEquals("14", contentLengthHeader.getValue());
        }
    }

    /**
     *
     * Tests posting to a MultivaluedMap with a request Content-Type that is not
     * application/x-www-form-urlencoded.
     */
    @Test
    public void testPostMultivaluedMapNotFormURLEncoded() throws Exception {

        HttpPost post = new HttpPost(multivalTestUri);
        StringEntity entity = new StringEntity("tuv=wxyz&abcd=", "UTF-8");
        entity.setContentType("text/plain");
        post.setEntity(entity);

        HttpResponse resp = httpClient.execute(post);

        assertEquals(415, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests posting to a MultivaluedMap with a request Accept type that is not
     * application/x-www-form-urlencoded.
     */
    @Test
    public void testPostMultivaluedMapAcceptNotFormURLEncoded() throws Exception {

        String uri = multivalTestUri + "/noproduces";
        HttpPost post = new HttpPost(uri);
        StringEntity entity = new StringEntity("tuv=wxyz&abcd=", "UTF-8");
        entity.setContentType("application/x-www-form-urlencoded");
        post.setEntity(entity);
        post.addHeader("Accept", "not/expected");

        HttpResponse resp = httpClient.execute(post);

        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests putting and then getting a /multivaluedmap.
     */
    //@Test
    // TODO: This test isn't working: defect 60975 opened to investigate.
    public void testMulitValPutReader() throws Exception {

        HttpPut put = new HttpPut(multivalTestUri);
        StringEntity entity = new StringEntity("username=user1&password=user1password", "UTF-8");
        //entity.setContentType("application/x-www-form-urlencoded");

        System.out.println("testMulitValPutReader sending PUT and GET to " + multivalTestUri);
        try {
            HttpResponse resp = httpClient.execute(put);
            // TODO: The HTTP status returned right now is a 415. Investigate why.
            System.out.println("Status code from PUT = " + resp.getStatusLine().getStatusCode());
            //assertEquals(204, resp.getStatusLine().getStatusCode());
        } finally {
            httpClient.getConnectionManager().shutdown();
            httpClient = new DefaultHttpClient();
        }

        HttpGet get = new HttpGet(multivalTestUri);
        get.addHeader("Accept", "application/x-www-form-urlencoded");

        HttpResponse getResp = httpClient.execute(get);
        assertEquals(200, getResp.getStatusLine().getStatusCode());

        String str = asString(getResp);
        System.out.println("str = " + str);
        assertTrue(str,
                   "username=user1&password=user1password".equals(str) || "password=user1password&username=user1"
                                   .equals(str));
        assertEquals("application/x-www-form-urlencoded", getResp
                        .getFirstHeader("Content-Type").getValue());
        Header contentLengthHeader = getResp.getFirstHeader("Content-Length");
        if (contentLengthHeader != null) {
            // some of the containers can be "smarter" and set the
            // content-length for us if the payload is small
            assertEquals("37", contentLengthHeader.getValue());
        }
    }

    /**
     * Tests posting an empty request entity to a MultivaluedMap.
     */
    @Test
    public void testSendingNoRequestEntityMultivaluedMap() throws Exception {

        String uri = multivalTestUri + "/empty";
        HttpPost post = new HttpPost(uri);
        post.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    // URL for test methods from JAXRSReaderTest
    private final String readerTestUri = getStandardTestURI() + "/providers/standard/reader";

    /**
     * Tests posting to a Reader parameter.
     */
    @Test
    public void testPostReader() throws Exception {

        HttpPost post = new HttpPost(readerTestUri);
        StringEntity entity = new StringEntity("abcd", "UTF-8");
        entity.setContentType("text/plain");
        post.setEntity(entity);
        post.addHeader("Accept", "text/plain");

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        String str = asString(resp);
        assertEquals("abcd", str);
        assertEquals("text/plain", resp.getFirstHeader("Content-Type").getValue());
        Header contentLengthHeader = post.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }

    /**
     * Tests putting and then getting a Reader.
     */
    @Test
    public void testPutReader() throws Exception {

        HttpPut put = new HttpPut(readerTestUri);
        StringEntity entity = new StringEntity("wxyz", "UTF-8");
        entity.setContentType("char/array");
        put.setEntity(entity);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(readerTestUri);
        resp = httpClient.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        String str = asString(resp);
        assertEquals("wxyz", str);

        String contentType =
                        (resp.getFirstHeader("Content-Type") == null) ? null : resp
                                        .getFirstHeader("Content-Type").getValue();

        assertEquals("application/octet-stream", contentType);
        Header contentLengthHeader = resp.getFirstHeader("Content-Length");
        assertEquals("4", contentLengthHeader.getValue());
    }

    /**
     */
    @Test
    public void testReaderAcceptMatchesReturnedContentType() throws Exception {

        HttpPut put = new HttpPut(readerTestUri);
        StringEntity entity = new StringEntity("wxyz", "UTF-8");
        entity.setContentType("char/array");
        put.setEntity(entity);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(readerTestUri);
        get.addHeader("Accept", "mytype/subtype");

        resp = httpClient.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        String str = asString(resp);
        assertEquals("wxyz", str);
        assertEquals("mytype/subtype", resp.getFirstHeader("Content-Type").getValue());

        Header contentLengthHeader = get.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }

    /**
     * Tests a resource method invoked with a BufferedReader as a parameter.
     * This should fail with a 415 since the reader has no way to necessarily
     * wrap it to the type.
     */
    //TODO: this should be workable after we bring back CXF 5846
    //@Test
    public void testReaderInputStreamImplementation() throws Exception {

        HttpPost post = new HttpPost(readerTestUri + "/subclasses/shouldfail");
        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity entity = new ByteArrayEntity(barr);
        entity.setContentType("any/type");
        post.setEntity(entity);

        HttpResponse resp = httpClient.execute(post);
        // according to spec, we can not put a subclass or implementation class, so we need return 500
        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests sending in no request entity to a Reader entity parameter.
     */
    @Test
    public void testSendingNoRequestEntityReader() throws Exception {

        HttpPost post = new HttpPost(readerTestUri + "/empty");

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    // URL for test methods from JAXRSSourceTest
    private final String srcTestUri = getStandardTestURI() + "/providers/standard/source";

    /**
     * Tests posting to a Source entity parameter with text/xml
     */
    @Test
    public void testPostSourceWithTextXMLMediaType() throws Exception {

        HttpPost post = new HttpPost(srcTestUri);
        StringEntity entity = new StringEntity("<message><user>user1</user><password>user1pwd</password></message>", "UTF-8");
        entity.setContentType("text/xml");
        post.setEntity(entity);
        post.addHeader("Accept", "text/xml");

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String str = asString(resp);
        assertEquals("<message><user>user1</user><password>user1pwd</password></message>",
                     str);
        assertEquals("text/xml", resp.getFirstHeader("Content-Type").getValue());
        Header contentLengthHeader = resp.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }

    /**
     * Tests posting to a Source entity parameter with application/xml as the
     * media type.
     */
    @Test
    public void testPostSourceWithApplicationXMLMediaType() throws Exception {

        HttpPost post = new HttpPost(srcTestUri);
        StringEntity entity = new StringEntity("<message><user>user1</user><password>user1pwd</password></message>", "UTF-8");
        entity.setContentType("application/xml");
        post.setEntity(entity);
        post.addHeader("Accept", "application/xml");

        HttpResponse resp = httpClient.execute(post);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        String str = asString(resp);
        assertEquals("<message><user>user1</user><password>user1pwd</password></message>",
                     str);
        assertEquals("application/xml", resp.getFirstHeader("Content-Type").getValue());
        Header contentLengthHeader = resp.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }

    /**
     * Tests posting to a Source entity parameter and returning Source entity
     * response with an unacceptable response media type.
     */
    @Test
    public void testPostSourceWithNonExpectedAcceptType() throws Exception {

        HttpPost post = new HttpPost(srcTestUri);
        StringEntity entity = new StringEntity("<message><user>user1</user><password>user1pwd</password></message>", "UTF-8");
        entity.setContentType("application/xml");
        post.setEntity(entity);
        post.addHeader("Accept", "not/expected");

        HttpResponse resp = httpClient.execute(post);

        assertEquals(500, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests posting to a Source entity parameter and returning Source entity
     * response with an unacceptable request content-type.
     */
    @Test
    public void testPostSourceWithNonExpectedRequestContentType() throws Exception {

        HttpPost post = new HttpPost(srcTestUri);
        StringEntity entity = new StringEntity("<message><user>user1</user><password>user1pwd</password></message>", "UTF-8");
        entity.setContentType("text/plain");
        post.setEntity(entity);
        post.addHeader("Accept", "application/xml");

        HttpResponse resp = httpClient.execute(post);

        assertEquals(415, resp.getStatusLine().getStatusCode());
    }

    /**
     * Tests putting and then getting a source.
     */
    @Test
    public void testPutSource() throws Exception {

        HttpPut put = new HttpPut(srcTestUri);
        StringEntity entity = new StringEntity("<?xml version=\"1.0\" encoding=\"UTF-8\"?><message><user>user1</user><password>user1pwd</password></message>", "UTF-8");
        entity.setContentType("application/xml");
        put.setEntity(entity);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(srcTestUri);
        resp = httpClient.execute(get);

        assertEquals(200, resp.getStatusLine().getStatusCode());

        String str = asString(resp);
        assertTrue(str, str.contains("<message><user>user1</user><password>user1pwd</password></message>"));

        String contentType =
                        (resp.getFirstHeader("Content-Type") == null) ? null : resp
                                        .getFirstHeader("Content-Type").getValue();
        assertEquals("text/xml", contentType);
        Header contentLengthHeader = resp.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }

    /**
     * Tests a resource method invoked with a SAXSource as a parameter. This
     * should fail with a 500 since the reader has no way to necessarily wrap it
     * to the type.
     */
    //TODO: this should be workable after we bring back CXF 5846
    //@Test
    public void testSourceSubclassImplementation() throws Exception {
        String uri = srcTestUri + "/subclasses/shouldfail";
        HttpPost post = new HttpPost(uri);
        byte[] barr = new byte[1000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity entity = new ByteArrayEntity(barr);
        entity.setContentType("application/xml");
        post.setEntity(entity);

        HttpResponse resp = httpClient.execute(post);

        assertEquals(500, resp.getStatusLine().getStatusCode());

    }

    /**
     * Tests sending in no request entity to a Source entity parameter.
     */
    // in CXf the source has been converted to org.apache.cxf.staxutils.StaxSource
    @Test
    public void testSendingNoRequestEntitySource() throws Exception {

        String uri = srcTestUri + "/empty";
        HttpPost post = new HttpPost(uri);
        post.addHeader("Content-Type", "text/xml");

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("expected", asString(resp));
    }

    // URL for test methods from JAXRSStreamingOutputTest
    private final String strmTestUri = getStandardTestURI() + "/providers/standard/streamingoutput";

    /**
     * Tests posting to a StreamingOutput and then returning StreamingOutput.
     */
    @Test
    public void testPostStreamingOutput() throws Exception {

        HttpPost post = new HttpPost(strmTestUri);
        byte[] barr = new byte[50000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity entity = new ByteArrayEntity(barr);
        entity.setContentType("text/plain");
        post.setEntity(entity);
        post.addHeader("Accept", "text/plain");

        HttpResponse resp = httpClient.execute(post);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[barr.length];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }

        assertEquals("text/plain", resp.getFirstHeader("Content-Type").getValue());
        Header contentLengthHeader = resp.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }

    /**
     * Tests putting and then getting a StreamingOutput.
     */
    @Test
    public void testPutStreamngOutput() throws Exception {

        HttpPut put = new HttpPut(strmTestUri);
        byte[] barr = new byte[100000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity entity = new ByteArrayEntity(barr);
        entity.setContentType("bytes/array");
        put.setEntity(entity);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(strmTestUri);
        resp = httpClient.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());

        InputStream is = resp.getEntity().getContent();
        byte[] receivedBArr = new byte[barr.length];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }

        String contentType =
                        (resp.getFirstHeader("Content-Type") == null) ? null : resp
                                        .getFirstHeader("Content-Type").getValue();
        assertEquals("application/octet-stream", contentType);
        Header contentLengthHeader = resp.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }

    /**
     * Tests receiving a StreamingOutput with a non-standard content-type.
     */
    @Test
    public void testStreamingAcceptMatchesReturnedContentType() throws Exception {

        HttpPut put = new HttpPut(strmTestUri);
        byte[] barr = new byte[100000];
        Random r = new Random();
        r.nextBytes(barr);
        ByteArrayEntity entity = new ByteArrayEntity(barr);
        entity.setContentType("any/type");
        put.setEntity(entity);

        HttpResponse resp = httpClient.execute(put);
        assertEquals(204, resp.getStatusLine().getStatusCode());

        HttpGet get = new HttpGet(strmTestUri);
        get.addHeader("Accept", "mytype/subtype");
        resp = httpClient.execute(get);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        InputStream is = resp.getEntity().getContent();

        byte[] receivedBArr = new byte[barr.length];
        DataInputStream dis = new DataInputStream(is);
        dis.readFully(receivedBArr);

        int checkEOF = dis.read();
        assertEquals(-1, checkEOF);
        for (int c = 0; c < barr.length; ++c) {
            assertEquals(barr[c], receivedBArr[c]);
        }
        assertEquals("mytype/subtype", resp.getFirstHeader("Content-Type").getValue());
        Header contentLengthHeader = resp.getFirstHeader("Content-Length");
        assertNull(contentLengthHeader == null ? "null" : contentLengthHeader.getValue(),
                   contentLengthHeader);
    }
}