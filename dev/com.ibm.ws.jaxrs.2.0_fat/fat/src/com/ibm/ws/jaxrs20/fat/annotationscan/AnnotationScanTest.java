/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.annotationscan;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static com.ibm.ws.jaxrs20.fat.TestUtils.readEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs.fat.annotation.multipleapp.MyResource;
import com.ibm.ws.jaxrs.fat.duplicateuris.MyOtherRootResource;
import com.ibm.ws.jaxrs.fat.duplicateuris.MyRegularResource;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyServer;

@AllowedFFDC({"java.lang.ClassNotFoundException", "java.lang.ClassCastException"})
@RunWith(FATRunner.class)
public class AnnotationScanTest {

    @Server("com.ibm.ws.jaxrs.fat.annotationscan")
    public static LibertyServer server;
    private static final String annwar = "annotationscan";
    private static final String respkgwar = "resourcepkg"; // used by testResourceInWAR and testResourceInJAR

    private Client client;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, annwar, "com.ibm.ws.jaxrs.fat.annotation.multipleapp",
                                      "com.ibm.ws.jaxrs.fat.duplicateuris");
        WebArchive app = ShrinkHelper.buildDefaultApp(respkgwar, "com.ibm.ws.jaxrs.fat.resourceinwar");
        JavaArchive jar = ShrinkHelper.buildJavaArchive("resourcejar.jar", "com.ibm.ws.jaxrs.fat.resourceinjar");
        app.addAsLibraries(jar);
        ShrinkHelper.exportDropinAppToServer(server, app);

        // Conditionally apply for non-Hotspot JVMs, since Hotspot does not support -Xdump
        if (JavaInfo.forServer(server).vendor() != Vendor.SUN_ORACLE) {
            server.setJvmOptions(Collections.singletonList("-Xdump:java+system+snap:events=throw+systhrow,filter=java/lang/reflect/GenericSignatureFormatError"));
        }

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
        server.stopServer("CWWKW0101W", "CWWKW0102W", "SRVE8052E", "SRVE0276E", "SRVE0190E", "SRVE0271E");
    }

    @Before
    public void setupClient() throws Exception {
        client = ClientBuilder.newClient();
    }

    // Notes:
    //
    // 1) Most of these tests are somewhat malicious; i.e., there should be a 404 Not Found
    // on purpose. The server will throw a java.io.FileNotFoundException because the
    // tests are using invalid paths in the first place. e.g., calls to resource with
    // "/app3" won't work because the class annotated with@ApplicationPath("app3") is
    // purposefully invalid.
    //
    // 2) *.annotations.multipleapp.MyResource and *.duplicateuris.MyRegularResource
    // have "/" and "" as their paths, respectively. So, matching requests can be served by
    // one or the other resource class. This is on purpose; the duplicates serve the needs
    // of testRootResource - testRootResourceOther.

    /**
     * Tests that a resource with a {@link Path} value of "/" is ok.
     *
     * @throws Exception
     */
    @Test
    public void testApplication1Resource1() throws Exception {
        String resp = client.target(getBaseTestUri(annwar, "/app1/")).request().get(String.class);
        assertTrue(resp, resp.equals("Hello world!") || resp.equals(MyRegularResource.class.getName()));
    }

    /**
     * Tests that a resource with a {@link Path} value of "/resource2" is ok.
     *
     * @throws Exception
     */
    @Test
    public void testApplication1Resource2() throws Exception {
        String helloWorld = client.target(getBaseTestUri(annwar, "/app1/resource2")).request().get(String.class);
        assertEquals("Hello world 2!", helloWorld);
        helloWorld = client.target(getBaseTestUri(annwar, "/app1/resource2/")).request().get(String.class);
        assertEquals("Hello world 2!", helloWorld);
    }

    /**
     * Tests that a resource with a {@link Path} value of "/resource2" which has
     * a subresource method is ok.
     *
     * @throws Exception
     */
    @Test
    public void testApplication1Resource2SubresourceMethod() throws Exception {
        String helloWorld = client.target(getBaseTestUri(annwar, "/app1/resource2/subresource")).request().get(String.class);
        assertEquals("Hello world 2 subresource!", helloWorld);

        helloWorld = client.target(getBaseTestUri(annwar, "/app1/resource2/subresource/")).request().get(String.class);
        assertEquals("Hello world 2 subresource!", helloWorld);
    }

    /**
     * Tests that a resource with a {@link Path} value of "/" is ok.
     *
     * @throws Exception
     */
    @Test
    public void testApplication2Resource1() throws Exception {
        String resp = client.target(getBaseTestUri(annwar, "/app2/")).request().get(String.class);
        assertTrue(resp, resp.equals("Hello world!") || resp.equals(MyRegularResource.class.getName()));
    }

    /**
     * Tests that a resource with a {@link Path} value of "/resource2" is ok.
     *
     * @throws Exception
     */
    @Test
    public void testApplication2Resource2() throws Exception {
        String helloWorld = client.target(getBaseTestUri(annwar, "/app2/resource2")).request().get(String.class);
        assertEquals("Hello world 2!", helloWorld);

        helloWorld = client.target(getBaseTestUri(annwar, "/app2/resource2/")).request().get(String.class);
        assertEquals("Hello world 2!", helloWorld);
    }

    /**
     * Tests that a resource with a {@link Path} value of "/resource2" which has
     * a subresource method is ok.
     *
     * @throws Exception
     */
    @Test
    public void testApplication2Resource2SubresourceMethod() throws Exception {
        String helloWorld = client.target(getBaseTestUri(annwar, "/app2/resource2/subresource")).request().get(String.class);
        assertEquals("Hello world 2 subresource!", helloWorld);

        helloWorld = client.target(getBaseTestUri(annwar, "/app2/resource2/subresource/")).request().get(String.class);
        assertEquals("Hello world 2 subresource!", helloWorld);
    }

    /**
     * Tests that app 3 is not valid so 404.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication3Resource1() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app3")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app3/")).request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests that app 3 is not valid so 404.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication3Resource2() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app3/resource2")).request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests that app 3 is not valid so 404.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication3Resource2SubresourceMethod() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app3/resource2/subresource")).request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests that app 4 is not valid at the {@link ApplicationPath} URL but is
     * valid at the web.xml one.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication4Resource1() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app4")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app4/")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app4withwebxml")).request().get();
        String strRsp = readEntity(String.class, response);
        assertTrue(strRsp.equals("Hello world!") || strRsp.equals(MyRegularResource.class.getName()));

        response = client.target(getBaseTestUri(annwar, "/app4withwebxml/")).request().get();
        assertTrue(strRsp.equals("Hello world!") || strRsp.equals(MyRegularResource.class.getName()));
    }

    /**
     * Tests that app 4 is not valid at the {@link ApplicationPath} URL but is
     * valid at the web.xml one.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication4Resource2() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app4/resource2")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app4withwebxml/resource2")).request().get();
        assertEquals("Hello world 2!", readEntity(String.class, response));

        response = client.target(getBaseTestUri(annwar, "/app4withwebxml/resource2/")).request().get();
        assertEquals("Hello world 2!", readEntity(String.class, response));
    }

    /**
     * Tests that app 4 is not valid at the {@link ApplicationPath} URL but is
     * valid at the web.xml one.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication4Resource2SubresourceMethod() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app4/resource2/subresource")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app4withwebxml/resource2/subresource")).request().get();
        assertEquals("Hello world 2 subresource!", readEntity(String.class, response));

        response = client.target(getBaseTestUri(annwar, "/app4withwebxml/resource2/subresource/")).request().get();
        assertEquals("Hello world 2 subresource!", readEntity(String.class, response));
    }

    /**
     * Tests that app 5 is valid for MyResource.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication5Resource1() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app5")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app5withwebxml/")).request().get();
        assertEquals("Hello world!", readEntity(String.class, response));
    }

    /**
     * Tests that app 5 is not valid for resource 2 since app 5 returns only the
     * MyResource class.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication5Resource2() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app5/resource2")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app5withwebxml/resource2/")).request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests that app 5 is not valid for resource 2's subresource since app 5
     * returns only the MyResource class.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication5Resource2SubresourceMethod() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app5/resource2/subresource")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app5withwebxml/resource2/subresource/")).request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests that app 6 is valid for MyResource.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication6Resource1() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app6")).request().get();
        assertEquals("Hello world!", readEntity(String.class, response));

        response = client.target(getBaseTestUri(annwar, "/app6/")).request().get();
        assertEquals("Hello world!", readEntity(String.class, response));
    }

    /**
     * Tests that app 6 is not valid for resource 2 since app 6 returns only the
     * MyResource class.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication6Resource2() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app6/resource2")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app6/resource2/")).request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests that app 6 is not valid for resource 2's subresource since app 6
     * returns only the MyResource class.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.FileNotFoundException")
    public void testApplication6Resource2SubresourceMethod() throws Exception {
        Response response = client.target(getBaseTestUri(annwar, "/app4/resource2/subresource")).request().get();
        assertEquals(404, response.getStatus());

        response = client.target(getBaseTestUri(annwar, "/app4/resource2/subresource/")).request().get();
        assertEquals(404, response.getStatus());
    }

    /**
     * Tests that the resource in WEB-INF/classes can be reached.
     */
    @Test
    public void testResourceInWAR() {
        Response response = client.target(getBaseTestUri(respkgwar, "/resourceinwar")).request().get();
        assertEquals(200, response.getStatus());
        assertEquals("com.ibm.ws.jaxrs.fat.resourceinwar.JAXRSResourceInWAR", readEntity(String.class, response));
    }

    /**
     * Tests that the resource in a WEB-INF/lib JAR can be reached.
     */
    @Test
    public void testResourceInJAR() {
        Response response = client.target(getBaseTestUri(respkgwar, "/resourceinjar")).request().get();
        assertEquals(200, response.getStatus());
        assertEquals("com.ibm.ws.jaxrs.fat.resourceinjar.JAXRSResourceInJAR", readEntity(String.class, response));
    }

    /**
     * Tests that either one of the "/" root resources can still be found. This
     * is for the extreme case. The annotation scanning will not care about
     * which one is picked first (so therefore at the end of the day either one
     * can be used by JAX-RS rules) but one of them should be picked.
     *
     * @throws Exception
     */
    @Test
    public void testRootResource() throws Exception {
        String uri = getBaseTestUri(annwar, "duplicateuris", "");
        System.out.println("testRootResource uri = " + uri);
        String ret = client.target(uri).request().get(String.class);
        assertTrue(ret, ret.equals(MyResource.class.getName()) || ret.equals(MyRegularResource.class.getName()));
    }

    /**
     * Tests that the root resource other is still available.
     *
     * @throws Exception
     */
    @Test
    public void testRootResourceOther() throws Exception {
        String uri = getBaseTestUri(annwar, "duplicateuris", "other");
        System.out.println("testRootResourceOther uri = " + uri);
        String ret = client.target(uri).request().get(String.class);
        assertTrue(ret, ret.equals(MyResource.class.getName()) || ret.equals(MyOtherRootResource.class.getName()));
    }

    /**
     * Tests we see the expected warning message when a user specifies the
     * IBMRestServlet servlet definition in the web.xml but does not specify
     * the Application init-param element.
     */
    @Test
    @SkipForRepeat(JakartaEE9Action.ID) // this actually should be fine under the EE8/EE9 spec - but it would ignore any Application subclasses
    public void testServletSpecifiedWithoutApplicationInitParam() throws Exception {
        assertEquals("Did not find expected warning indicating servlet is missing Application init-param", 1,
                     server.findStringsInLogs("CWWKW0101W.*annotationscan.*App7IBMRestServlet.*com.ibm.websphere.jaxrs.server.IBMRestServlet").size());
    }

    /**
     * Tests that we see the expected warning message when a user specifies an
     * Application class that does not actually extend javax.ws.rs.Application.
     */
    @Test
    public void testServletSpecifiedWithInvalidApplicationClass() throws Exception {
        int messageCount = 0;
        if (JakartaEE9Action.isActive()) {
            messageCount = server.findStringsInLogs("SRVE0271E.*NotAnAppIBMRestServlet.*annotationscan.*com.ibm.ws.jaxrs.fat.annotation.multipleapp.MyResource3.*jakarta.ws.rs.core.Application").size();
        } else {
            messageCount = server.findStringsInLogs("CWWKW0102W.*annotationscan.*NotAnAppIBMRestServlet.*com.ibm.ws.jaxrs.fat.annotation.multipleapp.MyResource3").size();
        }
        assertEquals("Did not find expected warning indicating servlet contains invalid Application class", 1,
                     messageCount);
    }
}
