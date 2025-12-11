/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;

/**
 * Tests to ensure new Annotation Literals can be used in an expected way.
 */
@RunWith(FATRunner.class)
public class AnnotationLiteralsTest {

    protected static final Class<?> clazz = AnnotationLiteralsTest.class;

    private static final String APP_NAME = "AnnotationLiteralsTest";

    @Server("faces40_AnnotationLiteralsTest")
    public static LibertyServer server;

    Map<String, String> testData = new HashMap<>();

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME + ".war", "io.openliberty.org.apache.faces40.fat.literals");

//        //Test differences between myfaces and mojarra if needed
//        app.addAsLibraries(new File("publish/files/mojarra40/").listFiles());
//        app.addAsDirectories("publish/files/permissions");

        ShrinkHelper.exportDropinAppToServer(server, app);
        server.startServer(AnnotationLiteralsTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    public String getOriginalMethodName(){
        String currentName = name.getMethodName();
        /*
         * Repeats are appended with _<repeat-name>, so we look for the underscore. 
         */
        int index = currentName.indexOf("_");
        if(index == -1){
            return currentName;
        }
        return currentName.substring(0, index);

    }

    private URL getURL(String path, String... queries) throws Exception {
        // Original name needed as the test app checks for specifc query parameters. Otherwise, this test class breaks for repeats.
        String finalQuery = "?" + getOriginalMethodName() + "=true";
        for (String query : queries) {
            finalQuery += "&" + query;
        }
        return HttpUtils.createURL(server, "/" + APP_NAME + "/" + path + finalQuery);
    }

    private HtmlPage getAndLogPage(WebClient webClient, String... queries) throws Exception {
        return getAndLogPage("annotationLiterals.xhtml", webClient, queries);
    }

    private HtmlPage getAndLogPage(String path, WebClient webClient, String... queries) throws Exception {
        URL url = getURL(path, queries);
        Log.info(clazz, "getAndLogPage", "URL: " +  url);
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".html");
        return page;
    }

    /**
     * Test data barAttribute set in TestAttributeFilter class of application
     */
    @Test
    public void testApplicationMap() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("applicationMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the ClientBehavior class that uses the @FacesBehavior annotation
     */
    @Test
    public void testFacesBehavior() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("facesBehavior did not produce the expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the ConfigBean class that uses the @FacesConfig annotation
     * FIXME - this doesn't currently work due to a NPE in the MyFaces impl
     * Issue - https://github.com/jakartaee/faces/issues/1784
     */
//    @Test
    public void testFacesConfig() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("facesConfig did not produce the expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the IntegerConverter class that uses the @FacesConverter annotation
     */
    @Test
    public void testFacesConverter() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("facesConverter did not produce the expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the IntegerDataModel class that uses the @FacesDataModel annotation
     */
    @Test
    public void testFacesDataModel() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("facesDataModel did not produce the expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the IntegerValidator class that uses the @FacesValidator annotation
     */
    @Test
    public void testFacesValidator() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("facesValidator did not produce the expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data foo set in header of this test
     */
    @Test
    public void testHeaderMap() throws Exception {
        String expected = "bar";
        try (WebClient webClient = new WebClient()) {
            webClient.addRequestHeader("foo", "bar");
            assertEquals("headerMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data foo set in header of this test
     *
     * FIXME Multiple values on header should use the common notation key: value1, value2
     * No way using HTMLUtil to create a request header with duplicate keys
     * Reported against faces spec https://github.com/jakartaee/faces/issues/1778
     *
     * For now just use a single header value.
     */
    @Test
    public void testHeaderValuesMap() throws Exception {
        String expected = "bar";
        try (WebClient webClient = new WebClient()) {
            webClient.addRequestHeader("foo", "bar");
            assertEquals("headerValuesMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data TESTCOOKIE set in web client cookie
     */
    @Test
    public void testRequestCookieMap() throws Exception {
        String expected = "testCookieValue";
        try (WebClient webClient = new WebClient()) {
            webClient.addCookie("TESTCOOKIE=testCookieValue", getURL("annotationLiterals.xhtml"), null);
            assertEquals("requestCookieMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data fooAttribute set in TestAttributeFilter class of application
     */
    @Test
    public void testRequestMap() throws Exception {
        String expected = "bar";
        try (WebClient webClient = new WebClient()) {
            assertEquals("requestMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data foo set in URL query
     */
    @Test
    public void testRequestParameterMap() throws Exception {
        String expected = "bar";
        try (WebClient webClient = new WebClient()) {
            assertEquals("requestParameterMap did not produce an expected result", expected, getAndLogPage(webClient, "foo=bar").getBody().asText());
        }
    }

    /**
     * Test data foo set in URL query
     */
    @Test
    public void testRequestParameterValuesMap() throws Exception {
        String expected = "bar:tar";
        try (WebClient webClient = new WebClient()) {
            assertEquals("requestParameterValuesMap did not produce an expected result", expected, getAndLogPage(webClient, "foo=bar", "foo=tar").getBody().asText());
        }
    }

    /**
     * Test data is the ViewScopedBean class which uses the @ViewScoped annotation and is added to the view map
     */
    @Test
    public void testViewMap() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("viewMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the TestFaceletViewBean class with uses the @View annotation
     */
    @Test
    public void testView() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("view did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data MY_TEST_PARAMETER set in web.xml of application
     */
    @Test
    public void testInitParameterMap() throws Exception {
        String expected = "IS_THERE";
        try (WebClient webClient = new WebClient()) {
            assertEquals("headerValuesMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the SessionMap object itself, ensure we can look it up and that is contains at least 1 session.
     */
    @Test
    public void testSessionMap() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("sessionMap did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * Test data is the ManagedPropertyBean which has methods that return an integer and map
     */
    @Test
    public void testManagedProperty() throws Exception {
        String expected = "true";
        try (WebClient webClient = new WebClient()) {
            assertEquals("managedProperty did not produce an expected result", expected, getAndLogPage(webClient).getBody().asText());
        }
    }

    /**
     * NOTE - the following annotation literals were added but cannot be tested from a user application:
     *
     * @Push - this is a produced bean that has no scope and as such can only be injected and isn't generally available to the bean manager.
     * @WebsocketEvent.Opened - this is an event bean, that is fired, but never cached in the bean manager.
     * @WebsocketEvent.Closed - this is an event bean, that is fired, but never cached in the bean manager.
     */

    /**
     * Test the configured flows testFlow and testFlowNext work
     * and that we are able to lookup the Flow, FlowBuilder, and FlowMap
     */
    @Test
    public void testFlowMap() throws Exception {
        try (WebClient webClient = new WebClient()) {
            // Start on initial (non-flow) view
            HtmlPage page = getAndLogPage(webClient);

            // Enter main flow
            page = page.getHtmlElementById("form:enter").click();
            page = page.getHtmlElementById("form:init").click();
            page = page.getHtmlElementById("form:next").click();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".main.html");
            assertEquals(page.getElementById("flowReturn").asText(), "foo:bar");

            // Enter nested flow
            page = page.getHtmlElementById("form:nested").click();
            page = page.getHtmlElementById("form:init").click();
            page = page.getHtmlElementById("form:next").click();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".nested.html");
            assertEquals(page.getElementById("flowReturn").asText(), "foo:barnested");

            // Exit nested flow
            page = page.getHtmlElementById("form:exit").click();
            FATSuite.logOutputForDebugging(server, page.asXml(), name.getMethodName() + ".exit.html");
            assertEquals(page.getElementById("flowReturn").asText(), "foo:bar");
        }
    }
}
