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
package com.ibm.ws.jaxrs20.fat.validation;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.FormParam;

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
import com.ibm.ws.jaxrs20.fat.TestUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class ValidationTest {

    @Server("com.ibm.ws.jaxrs.fat.validation")
    public static LibertyServer server;

    private static final String valwar = "validation";

    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, valwar, "com.ibm.ws.jaxrs.fat.constructors",
                                      "com.ibm.ws.jaxrs.fat.param.entity",
                                      "com.ibm.ws.jaxrs.fat.param.formfield",
                                      "com.ibm.ws.jaxrs.fat.param.formparam",
                                      "com.ibm.ws.jaxrs.fat.param.formproperty",
                                      "com.ibm.ws.jaxrs.fat.pathmethods");

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
        client = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        client.getConnectionManager().shutdown();
    }

    private static String getBaseTestUri(String context, String urlpattern, String path) {
        return TestUtils.getBaseTestUri(context, urlpattern, path);
    }

    /**
     * Tests that the runtime will use the correct constructor with a resource
     * that has multiple constructors. The resource has multiple constructors
     * with different number of parameters in each constructor.
     */
    //TODO
    //@Test
    public void testConstructorWithMostParams() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "multi");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("matrixAndQueryAndContext1", asString(resp));
    }

    /**
     * Tests that the runtime will use the correct constructor with a resource
     * that has multiple constructors. The resource has multiple constructors
     * with different number of parameters in each constructor.
     */
    @Test
    public void testConstructorWithMostParams2() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "multi2/somepath");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("contextAndHeaderAndCookieAndPath1", asString(resp));
    }

    /**
     * Tests that the runtime will randomly choose a constructor between two
     * constructors with the same number of parameters. A warning should be
     * issued.
     */
    @Test
    public void testConstructorWithSameParamWarning() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "samenumparam");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String c = asString(resp);
        boolean foundConstructor = false;
        if ("context1".equals(c)) {
            foundConstructor = true;
        } else if ("query1".equals(c)) {
            foundConstructor = true;
        }
        assertTrue("Returned message body was: " + c, foundConstructor);
    }

    /**
     * Tests that the runtime will randomly choose a constructor between two
     * constructors with the same parameters except different types. A warning
     * should be issued.
     */
    @Test
    public void testConstructorWithSameParamWarning2() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "samenumparam2?q=15");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String c = asString(resp);
        boolean foundConstructor = false;
        if ("queryInt1".equals(c)) {
            foundConstructor = true;
        } else if ("queryString1".equals(c)) {
            foundConstructor = true;
        }
        assertTrue("Returned message body was: " + c, foundConstructor);
    }

    /**
     * Tests that the sub-resources can use a package default constructor.
     */
    @Test
    public void testSubResourceLocatorPackageEmptyConstructor() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/emptypackage");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("package", asString(resp));
    }

    /**
     * Tests that the sub-resources can use a package constructor with a String
     * parameter.
     */
    @Test
    public void testSubResourceLocatorPackageStringConstructor() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/stringpackage");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("packageString", asString(resp));
    }

    /**
     * Tests that the sub-resources can use a public constructor.
     */
    @Test
    public void testSubResourceLocatorPublicDefaultConstructor() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/emptypublic");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("public", asString(resp));
    }

    /**
     * Tests that the sub-resources can use a public constructor with a String
     * parameter.
     */
    @Test
    public void testSubResourceLocatorPublicStringConstructor() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/stringpublic?q=Hello");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("Hello", asString(resp));
    }

    /**
     * Tests that the sub-resources can use a private constructor.
     */
    @Test
    public void testSubResourceLocatorPrivateDefaultConstructor() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/emptyprivate");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("private", asString(resp));
    }

    /**
     * Tests that the sub-resources can use a private constructor with a String
     * parameter.
     */
    @Test
    public void testSubResourceLocatorPrivateStringConstructor() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/stringprivate?q=Hello");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("Hello", asString(resp));
    }

    /**
     * Tests that the sub-resources will eventually find the right resource.
     */
    @Test
    public void testSubResourceOtherSubPublicToPackage() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/stringpublic/other");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("subpackage", asString(resp));
    }

    /**
     * Tests that the sub-resources will eventually find the right resource.
     */
    @Test
    public void testSubResourceOtherSubPackageToPublic() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/stringpackage/other");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("public", asString(resp));
    }

    /**
     * Tests that the sub-resources will eventually return the right resource.
     */
    @Test
    public void testSubResourceDecideSubDynamic() throws Exception {

        String uri = getBaseTestUri(valwar, "constructors", "subresource/sub?which=public");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp;
        try {
            resp = client.execute(getMethod);
            assertEquals(200, resp.getStatusLine().getStatusCode());
            assertEquals("public", asString(resp));
        } finally {
            client = new DefaultHttpClient();
        }

        String uri2 = getBaseTestUri(valwar, "constructors", "subresource/sub?which=package");
        getMethod = new HttpGet(uri2);
        resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("package", asString(resp));
    }

    //TODO
    @Test
    public void testValidationMultipleEntities() throws Exception {

        String uri = getBaseTestUri(valwar, "entity", "/params/multientity");
        HttpGet httpMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(httpMethod);
        // Supposed to get back a 404 --This is wrong in WINK!!
        //according to jaxrs2.0 spec, if return type is void, we should return 204, so modify test case
        assertEquals(204, resp.getStatusLine().getStatusCode());

        // framework.defaults.test.FVTAssert
        // .assertInstallLogContainsException("ResourceValidationException");
        // if (Environment.getCurrentEnvironment() ==
        // Environment.GENERIC_WAS) {
        // framework.defaults.test.FVTAssert
        // .assertInstallLogContainsException("Uncaught exception created in one of the service methods "
        // + "of the servlet jaxrs.tests.validation.param.entity in "
        // + "application jaxrs.tests.validation.param.entity. "
        // + "Exception created : javax.servlet.ServletException: An error "
        // + "occurred validating JAX-RS artifacts in the application.");
        // }
    }

    /**
     * {@link FormParam} annotated fields are not supported.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.IOException")
    public void testFormFieldNoMultivaluedMapEntityValidation() throws Exception {

        HttpPost httpMethod = new HttpPost(getBaseTestUri(valwar, "formfield", "params/form/validate/fieldnotmultivaluedmapparam"));
        StringEntity entity = new StringEntity("firstkey=somevalue&someothervalue=somethingelse");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);
        HttpResponse resp = client.execute(httpMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("null:firstkey=somevalue&someothervalue=somethingelse", asString(resp));
    }

    /**
     * {@link FormParam} annotated parameters with entity parameters are not
     * supported.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.IOException")
    public void testFormParamNoMultivaluedMapEntityValidation() throws Exception {

        HttpPost httpMethod = new HttpPost(getBaseTestUri(valwar, "formparam", "params/form/validate/paramnotmultivaluedmaparam"));
        StringEntity entity = new StringEntity("firstkey=somevalue&someothervalue=somethingelse");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);
        HttpResponse resp = client.execute(httpMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("somevalue:", asString(resp));
    }

    /**
     * {@link FormParam} annotated JavaBean property methods are not supported.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.IOException")
    public void testFormPropertyNoMultivaluedMapEntityValidation() throws Exception {

        HttpPost httpMethod = new HttpPost(getBaseTestUri(valwar, "formproperty", "params/form/validate/propertynotmultivaluedmaparam"));
        StringEntity entity = new StringEntity("firstkey=somevalue&someothervalue=somethingelse");
        entity.setContentType("application/x-www-form-urlencoded");
        httpMethod.setEntity(entity);
        HttpResponse resp = client.execute(httpMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("null:firstkey=somevalue&someothervalue=somethingelse", asString(resp));
    }

    @Test
    public void testNonPublicMethodPathWarning() throws Exception {
        try {
            String uri1 = getBaseTestUri(valwar, "pathmethod", "pathwarnings/private");
            HttpGet httpMethod = new HttpGet(uri1);

            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        try {
            String uri2 = getBaseTestUri(valwar, "pathmethod", "pathwarnings/protected");
            HttpGet httpMethod = new HttpGet(uri2);

            HttpResponse resp = client.execute(httpMethod);
            assertEquals(404, resp.getStatusLine().getStatusCode());
        } finally {
            client = new DefaultHttpClient();
        }

        String uri3 = getBaseTestUri(valwar, "pathmethod", "pathwarnings/package");
        HttpGet httpMethod = new HttpGet(uri3);

        HttpResponse resp = client.execute(httpMethod);
        assertEquals(404, resp.getStatusLine().getStatusCode());
    }

}
