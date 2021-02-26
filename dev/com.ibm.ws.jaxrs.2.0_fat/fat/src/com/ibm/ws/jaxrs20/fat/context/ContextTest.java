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
package com.ibm.ws.jaxrs20.fat.context;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class ContextTest {

    @Server("com.ibm.ws.jaxrs.fat.context")
    public static LibertyServer server;

    private static final String contextwar = "context";
    private static final String uriInfo_username = "username";
    private static final String request_method = "GET";
    private static final String resourceContext_param = "bookid=123&bookname=jordan";


    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, contextwar, "com.ibm.ws.jaxrs.fat.context");

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

    @Test
    public void testContext_BeansAll_Classes() throws Exception {
        /**
         * All tests include: Bean, Field, Constructor, Param, NotBean testing in Resource, Application Filter
         *
         * Random test all 8 @Context in Providers (new JordanExceptionProviders) without Configuration
         */

        /**
         * Test (1) UriInfo in Resource and Application Filter
         */
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.URIINFONAME1, uriInfo_username);

        /**
         * Test (2) HttpHeaders, (5) Providers in Resource and (4) Application and (7) Providers in Application Filter
         *
         * Check if Application is null, if yes throws RuntimeException
         * Get Providers in Resource and write its name to HttpHeaders and double check them in Application Filter
         *
         */
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.HTTPHEADERSNAME1, ContextUtil.METHODNAME1);

        /**
         * Test (3) Request in Resource
         */
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.REQUESTNAME1, request_method);

        /**
         * Test (5) ResourceContext in Resource
         */
        assertContextCodeWithText(ContextUtil.CLASSESNAME2, ContextUtil.RESOURCECONTEXT1 + "/rc", resourceContext_param, null);

        /**
         * Test (6) Configuration has been fixed in CXF 3.0.2, add test!!!
         */
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.CONFIGNAME1, "SERVER");

        /**
         * Won't test (8) SecurityContext because it will be tested in another FAT
         */
    }

    @Test
    public void testContext_FieldsAll_Classes() throws Exception {
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.URIINFONAME2, uriInfo_username);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.URIINFONAME2, uriInfo_username); //ANDYMC
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.HTTPHEADERSNAME2, ContextUtil.METHODNAME2);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.REQUESTNAME2, request_method);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.CONFIGNAME2, "SERVER");
        assertContextCodeWithText(ContextUtil.CLASSESNAME2, ContextUtil.RESOURCECONTEXT2 + "/rc", resourceContext_param, null);
    }

    @Test
    public void testContext_ConstructorsAll_Classes() throws Exception {
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.URIINFONAME3, uriInfo_username);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.HTTPHEADERSNAME3, ContextUtil.METHODNAME3);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.REQUESTNAME3, request_method);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.CONFIGNAME3, "SERVER");
        assertContextCodeWithText(ContextUtil.CLASSESNAME2, ContextUtil.RESOURCECONTEXT3 + "/rc", resourceContext_param, null);
    }

    @Test
    public void testContext_ParamsAll_Classes() throws Exception {
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.URIINFONAME4, uriInfo_username);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.HTTPHEADERSNAME4, ContextUtil.METHODNAME4);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.REQUESTNAME4, request_method);
        assertContextString(ContextUtil.CLASSESNAME, ContextUtil.CONFIGNAME4, "SERVER");
        assertContextCodeWithText(ContextUtil.CLASSESNAME2, ContextUtil.RESOURCECONTEXT4 + "/rc", resourceContext_param, null);
    }

    @Test
    public void testContext_NotBeansAll_Classes() throws Exception {
        assertContextCodeWithText(ContextUtil.CLASSESNAME, ContextUtil.URIINFONAME5, uriInfo_username, "false");
        assertContextCodeWithText(ContextUtil.CLASSESNAME, ContextUtil.HTTPHEADERSNAME5, ContextUtil.METHODNAME5, "false");
        assertContextCodeWithText(ContextUtil.CLASSESNAME, ContextUtil.REQUESTNAME5, request_method, "false");
        assertContextCodeWithText(ContextUtil.CLASSESNAME, ContextUtil.CONFIGNAME5, "SERVER", "false");
        assertContextCodeWithText(ContextUtil.CLASSESNAME2, ContextUtil.RESOURCECONTEXT5, resourceContext_param, "false");
    }

    @Test
    public void testContextAll_InProviders_Classes() throws Exception {
        assertContextCode(ContextUtil.CLASSESNAME2, "jordanproviders/jordan", "", 454);
    }

    @Test
    public void testContext_BeansAll_Singletons() throws Exception {
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.URIINFONAME1, uriInfo_username);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.HTTPHEADERSNAME1, ContextUtil.METHODNAME1);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.REQUESTNAME1, request_method);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.CONFIGNAME1, "SERVER");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME2, ContextUtil.RESOURCECONTEXT1 + "/rc", resourceContext_param, null);
    }

    @Test
    public void testContext_FieldsAll_Singletons() throws Exception {
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.URIINFONAME2, uriInfo_username);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.HTTPHEADERSNAME2, ContextUtil.METHODNAME2);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.REQUESTNAME2, request_method);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.CONFIGNAME2, "SERVER");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME2, ContextUtil.RESOURCECONTEXT2 + "/rc", resourceContext_param, null);
    }

//    @Test
    public void testContext_ConstructorsAll_Singletons() throws Exception {
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.URIINFONAME3, uriInfo_username);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.HTTPHEADERSNAME3, ContextUtil.METHODNAME3);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.REQUESTNAME3, request_method);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.CONFIGNAME3, "SERVER");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME2, ContextUtil.RESOURCECONTEXT3 + "/rc", resourceContext_param, null);
    }

    @Test
    public void testContext_ParamsAll_Singletons() throws Exception {
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.URIINFONAME4, uriInfo_username);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.HTTPHEADERSNAME4, ContextUtil.METHODNAME4);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.REQUESTNAME4, request_method);
        assertContextString(ContextUtil.SINGLETONSNAME, ContextUtil.CONFIGNAME4, "SERVER");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME2, ContextUtil.RESOURCECONTEXT4 + "/rc", resourceContext_param, null);
    }

    @Test
    public void testContext_NotBeansAll_Singletons() throws Exception {
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME, ContextUtil.URIINFONAME5, uriInfo_username, "false");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME, ContextUtil.HTTPHEADERSNAME5, ContextUtil.METHODNAME5, "false");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME, ContextUtil.REQUESTNAME5, request_method, "false");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME, ContextUtil.CONFIGNAME5, "SERVER", "false");
        assertContextCodeWithText(ContextUtil.SINGLETONSNAME2, ContextUtil.RESOURCECONTEXT5, resourceContext_param, "false");
    }

    @Test
    public void testContextAll_InProviders_Singletons() throws Exception {
        assertContextCode(ContextUtil.SINGLETONSNAME2, "jordanproviders/jordan", "", 454);
    }

    @Test
    public void testContext_BeansAll_FromApplicationContext() throws Exception {
        assertContextString("context", ContextUtil.URIINFONAME1, uriInfo_username);
        assertContextString("context", ContextUtil.HTTPHEADERSNAME1, ContextUtil.METHODNAME1);
        assertContextString("context", ContextUtil.REQUESTNAME1, request_method);
        assertContextString("context", ContextUtil.CONFIGNAME1, "SERVER");
        assertContextCodeWithText("context", ContextUtil.RESOURCECONTEXT1 + "/rc", resourceContext_param, null);
    }

    public void assertContextString(String rootUri, String testUri, String param) throws Exception {
        HttpClient client = new DefaultHttpClient();
        String uri = getBaseTestUri(contextwar, contextwar, "/" + rootUri + "/" + testUri + "?" + param);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp).trim();
        assertEquals(testUri + ": " + param, responseBody);
        client.getConnectionManager().shutdown();
    }

    public void assertContextCodeWithText(String rootUri, String testUri, String param, String verifyText) throws Exception {
        HttpClient client = new DefaultHttpClient();
        String uri = getBaseTestUri(contextwar, contextwar, "/" + rootUri + "/" + testUri + "?" + param);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        if (verifyText != null) {
            assertEquals(asString(resp), verifyText);
        }
        client.getConnectionManager().shutdown();
    }

    public void assertContextCode(String rootUri, String testUri, String param, int code) throws Exception {
        HttpClient client = new DefaultHttpClient();
        String uri = getBaseTestUri(contextwar, contextwar, "/" + rootUri + "/" + testUri + "?" + param);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(code, resp.getStatusLine().getStatusCode());
        client.getConnectionManager().shutdown();
    }
}