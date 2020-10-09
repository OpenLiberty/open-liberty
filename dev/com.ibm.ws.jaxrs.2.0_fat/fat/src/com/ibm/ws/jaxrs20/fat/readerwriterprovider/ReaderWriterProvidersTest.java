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
package com.ibm.ws.jaxrs20.fat.readerwriterprovider;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getPort;
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
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

public class ReaderWriterProvidersTest {

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

    private String getTestURI() {
        return "http://localhost:" + getPort() + "/providers/readerwritermatch";
    }

    /**
     * RTC defect 161757
     *
     * @test_Strategy: An implementation MUST support application-provided
     *                 entity providers and MUST use those in preference
     *                 to its own pre-packaged providers when either could
     *                 handle the same request. More precisely, step 4 in
     *                 Section 4.2.1 and step 5 in Section 4.2.2 MUST prefer
     *                 application-provided over pre-packaged entity providers.
     *                 i.e. When have the same mediaType
     */
    @Test
    public void testPostBoolean() throws Exception {

        String uri = getTestURI() + "/resource/boolean";
        HttpPost post = new HttpPost(uri);
        post.setEntity(new StringEntity("false"));
        HttpResponse resp = httpClient.execute(post);
        String respContent = asString(resp);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("false", respContent);

    }

    /*
     * RTC defect 162143
     *
     * @test_Strategy: Character, text/plain media type
     *
     * The pre-packaged JAXB and the prepackaged primitive type
     * MessageBodyReaders MUST throw a BadRequestException
     * (400 status) for zero-length request entities.
     */
    @Test
    @AllowedFFDC("javax.ws.rs.core.NoContentException")
    public void testPostEmptyEntiry() throws Exception {

        String uri = getTestURI() + "/resource/character";
        HttpPost post = new HttpPost(uri);
        post.setHeader("Content-Type", "text/plain");
        post.setHeader("Accept", "*/*");
        HttpResponse resp = httpClient.execute(post);

        assertEquals(400, resp.getStatusLine().getStatusCode());

    }

    /*
     * RTC defect 161035 cxf-6256
     *
     * @test_Strategy: Generate a WebApplicationException with a not acceptable
     * response (HTTP 406 status) and no entity. The exception
     * MUST be processed as described in section 3.3.4. Finish.
     */
    //org.apache.cxf.jaxrs.interceptor.JAXRSOutInterceptor.checkFinalContentType has issue on spec 3.8|2
    @Test
    @AllowedFFDC("javax.ws.rs.NotAcceptableException")
    public void testGetAcceptTextStar() throws Exception {

        String uri = getTestURI() + "/resource/text";
        HttpGet get = new HttpGet(uri);

        get.setHeader("Accept", "text/*");
        HttpResponse resp = httpClient.execute(get);

        assertEquals(406, resp.getStatusLine().getStatusCode());

    }

    /*
     * RTC defect 174015
     * Sort the selected MessageBodyWriter providers with a
     * primary key of generic type where providers whose
     * generic type is the nearest superclass of the object
     * class are sorted first and a secondary key of media
     * type (see Section 4.2.3).
     */
    @Test
    public void testPostCharacter() throws Exception {

        String uri = getTestURI() + "/resource/character";
        HttpPost post = new HttpPost(uri);

        post.setEntity(new StringEntity("a"));
        HttpResponse resp = httpClient.execute(post);

        String respContent = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals(true, respContent.endsWith("N"));

    }

    /*
     * RTC defect 174461 nested generic type
     *
     * @test_Strategy: The implementation-supplied entity provider(s) for
     * javax.xml.bind.JAXBElement and application supplied
     * JAXB classes MUST use JAXBContext instances provided
     * by application-supplied context resolvers, see
     * Section 4.3.
     *
     * com.ibm.ws.jaxrs.fat.provider.readerwritermatch.ApplicationJaxbProvider should not be chosen , it's reader will return null
     */
    @Test
    public void testPostJAXBElement() throws Exception {

        String uri = getTestURI() + "/resource/jaxb";
        HttpPost post = new HttpPost(uri);
        post.setHeader("Content-Type", "application/xml");
        post.setEntity(new StringEntity("anything"));
        HttpResponse resp = httpClient.execute(post);

        assertEquals(200, resp.getStatusLine().getStatusCode());

    }
}