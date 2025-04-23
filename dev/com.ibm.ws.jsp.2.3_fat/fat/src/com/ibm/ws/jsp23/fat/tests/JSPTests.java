/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsp23.fat.JSPUtils;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jspServer that use HttpUnit/HttpClient
 */

@SkipForRepeat("CDI-2.0")
@RunWith(FATRunner.class)
public class JSPTests {
    private static final Logger LOG = Logger.getLogger(JSPTests.class.getName());
    private static final String TestServlet_APP_NAME = "TestServlet";
    private static final String PI44611_APP_NAME = "PI44611";
    private static final String PI59436_APP_NAME = "PI59436";
    private static final String TestEDR_APP_NAME = "TestEDR";
    private static final String TestJDT_APP_NAME = "TestJDT";
    private static final String OLGH20509_APP_NAME1 = "OLGH20509jar";
    private static final String OLGH20509_APP_NAME2 = "OLGH20509TDfalse";
    private static final String OLGH27779_APP_NAME = "OLGH27779";
    private static final String PH62212_APP_NAME = "PH62212";
    private static final String PH62212_THREADPOOL_APP_NAME = "PH62212_ThreadPool";
    private static final String PH62212_PAGEPOOL_APP_NAME = "PH62212_PagePool";

    @Server("jspServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server,
                                      TestServlet_APP_NAME + ".war",
                                      "com.ibm.ws.jsp23.fat.testjsp23.beans",
                                      "com.ibm.ws.jsp23.fat.testjsp23.servlets");

        ShrinkHelper.defaultDropinApp(server, PI44611_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, PI59436_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, TestJDT_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, TestEDR_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, OLGH27779_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, PH62212_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, PH62212_THREADPOOL_APP_NAME + ".war");

        ShrinkHelper.defaultDropinApp(server, PH62212_PAGEPOOL_APP_NAME + ".war");

        JavaArchive jspJar = ShrinkWrap.create(JavaArchive.class, "OLGH20509Include.jar");
        jspJar = (JavaArchive) ShrinkHelper.addDirectory(jspJar, "test-applications/includejar/resources");
        WebArchive war = ShrinkHelper.buildDefaultApp(OLGH20509_APP_NAME1 + ".war");
        war.addAsLibraries(jspJar);
        ShrinkHelper.exportDropinAppToServer(server, war);

        ShrinkHelper.defaultDropinApp(server, OLGH20509_APP_NAME2 + ".war");

        server.startServer(JSPTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server, below are the expected exception:
        // SRVE8094W and SRVE8115W...Response already committed...
        // JSPG0077E: End of file reached while processing scripting element xxxxxxx
        //      Caused by test0077()
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE8094W", "SRVE8115W", "JSPG0077E");
        }
    }

    /**
     * Test Servlet 3.1 request/response API
     *
     * @throws Exception
     *                       if something goes wrong
     */
    @Test
    public void testServlet31RequestResponse() throws Exception {
        String[] expectedInResponse = { "JSP to test Servlet 3.1 Request and Response",
                                        "Testing BASIC_AUTH static field from HttpServletRequest (Expected: BASIC): BASIC",
                                        "Testing request.getParameterNames method (Expected: [firstName, lastName]): [lastName, firstName]",
                                        "Testing request.getParameter method (Expected: John): John",
                                        "Testing request.getParameter method (Expected: Smith): Smith",
                                        "Testing request.getQueryString method (Expected: firstName=John&lastName=Smith): firstName=John&lastName=Smith",
                                        "Testing request.getContextPath method (Expected: /TestServlet): /TestServlet",
                                        "Testing request.getRequestURI method (Expected: /TestServlet/Servlet31RequestResponseTest.jsp): /TestServlet/Servlet31RequestResponseTest.jsp",
                                        "Testing request.getMethod method (Expected: GET): GET",
                                        "Testing request.getContentLengthLong method (Expected: -1): -1",
                                        "Testing request.getProtocol method (Expected: HTTP/1.1): HTTP/1.1",
                                        "Testing SC_NOT_FOUND static field from HttpServletResponse (Expected: 404): 404",
                                        "Testing response.getStatus method (Expected: 200): 200",
                                        "Testing response.getBufferSize method (Expected: 4096): 4096",
                                        "Testing response.getCharacterEncoding method (Expected: ISO-8859-1): ISO-8859-1",
                                        "Testing response.getContentType method (Expected: text/html; charset=ISO-8859-1): text/html; charset=ISO-8859-1",
                                        "Testing response.containsHeader method (Expected: true): true",
                                        "Testing response.isCommitted method (Expected: false): false" };

        this.verifyStringsInResponse(TestServlet_APP_NAME, "Servlet31RequestResponseTest.jsp?firstName=John&lastName=Smith", expectedInResponse);
    }

    /**
     * This test makes a request to a jsp page and expects no exceptions,
     * and the text "Test passed!" to be output after a session invalidation.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test

    public void testPI44611() throws Exception {
        this.verifyStringInResponse(PI44611_APP_NAME, "PI44611.jsp", "Test passed!");
    }

    /**
     * This test makes a request to a jsp page and expects no NullPointerExceptions,
     * and the text "Test passed." to be output.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test

    public void testPI59436() throws Exception {
        this.verifyStringInResponse(PI59436_APP_NAME, "PI59436.jsp", "Test passed.");
    }

    /**
     * Verify TLD file check per issue 18411.
     * Run with applicationManager autoExpand="false" (default)
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTLD() throws Exception {
        // Use TestEDR app but just call index.jsp twice.
        // 2nd call should not have SRVE0253I message if issue 18411 is fixed
        // and no other files included in the JSP are updated.
        String orgEdrFile = "headerEDR1.jsp";
        String relEdrPath = "../../shared/config/ExtendedDocumentRoot/";
        server.copyFileToLibertyServerRoot(relEdrPath, orgEdrFile);
        // Hit the TestEDR app again so its index.jsp has a newer
        // last modified timestamp than headerEDR1.jsp.
        ShrinkHelper.defaultDropinApp(server, TestEDR_APP_NAME + ".war");
        Thread.sleep(5000L); // sleep to insure sufficient time for app restart
        String url = JSPUtils.createHttpUrlString(server, TestEDR_APP_NAME, "index.jsp");
        LOG.info("url: " + url);
        WebConversation wc1 = new WebConversation();
        WebRequest request1 = new GetMethodWebRequest(url);
        wc1.getResponse(request1);

        server.setMarkToEndOfLog(); // mark after 1st call to index.jsp since it might have compiled and caused a SRVE0253I
        Thread.sleep(5000L);
        WebConversation wc2 = new WebConversation();
        WebRequest request2 = new GetMethodWebRequest(url);
        wc2.getResponse(request2);
        assertNull("Log should not contain SRVE0253I: Destroy successful.",
                   server.verifyStringNotInLogUsingMark("SRVE0253I", 1200));
        server.deleteFileFromLibertyServerRoot(relEdrPath + orgEdrFile); // cleanup testTLD's edr file
        Thread.sleep(500L); // ensure file is deleted
    }

    /**
     * This test verifies that a included JSP in the extended document root when
     * updated will cause the parent JSP to recompile.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testEDR() throws Exception {
        // Tests on index page
        String url = JSPUtils.createHttpUrlString(server, TestEDR_APP_NAME, "index.jsp");
        LOG.info("url: " + url);

        runEDR(url, false);
    }

    /**
     * This test verifies compile works without ClassCastException,
     * per issue 19197.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void TestJDT() throws Exception {
        this.verifyStringInResponse(TestJDT_APP_NAME, "index.jsp", "Test passed.");
    }

    /**
     * Same test as above, but this test verifies that the dependentsList
     * is populated when processing multiple requests concurrently.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testConcurrentRequestsForTrackDependencies() throws Exception {
        // Tests on trackDependencies page
        String url = JSPUtils.createHttpUrlString(server, TestEDR_APP_NAME, "trackDependencies.jsp");
        LOG.info("url: " + url);

        runEDR(url, true);
    }

    /**
     * This test verifies no destroy/init cycles, i.e.,
     * JSP recompiles, occur after 2nd attempt,
     * while using jsp within a jar under WEB-INF,
     * trackDependencies=true, per issue 20509.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testTrackDependenciesTrue() throws Exception {
        server.setMarkToEndOfLog();
        this.verifyStringInResponse(OLGH20509_APP_NAME1, "index.jsp", "Test Passed!");
        Thread.sleep(5100L);
        this.verifyStringInResponse(OLGH20509_APP_NAME1, "index.jsp", "Test Passed!");
        assertNull("Log should not contain SRVE0253I: Destroy successful.",
                   server.verifyStringNotInLogUsingMark("SRVE0253I.*OLGH20509jar.*index.jsp.*Destroy successful", 1200));
    }

    /**
     * This test verifies no NPE occurs after 2nd attempt,
     * trackDependencies=false, per issue 20509.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testTrackDependenciesFalse() throws Exception {
        this.verifyStringInResponse(OLGH20509_APP_NAME2, "index.jsp", "Test Passed!");
        Thread.sleep(5100L);
        this.verifyStringInResponse(OLGH20509_APP_NAME2, "index.jsp", "Test Passed!");
    }

    /**
     * Test for JSPG0077E
     *
     * @throws Exception
     *                       if something goes wrong
     */
    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC("java.security.PrivilegedActionException")
    @AllowedFFDC("com.ibm.ws.jsp.JspCoreException")
    public void test0077() throws Exception {
        String e77 = "JSPG0077E";
        server.setMarkToEndOfLog();

        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        String url = JSPUtils.createHttpUrlString(server, TestServlet_APP_NAME, "error0077.jsp");
        LOG.info("url: " + url);

        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response: " + response.getText());
        //error0077.jsp contains a JSP syntax error, therefore verify the following:
        //   response code is 500
        //   response text includes the JSPG0077E message
        //   messages.log has the JSPG0077E message
        assertEquals("Expected " + 500 + " status code was not returned!",
                     500, response.getResponseCode());
        assertTrue("Response should contain " + e77 + ".",
                   response.getText().contains(e77));
        assertTrue("Log should contain " + e77 + ".",
                   null != server.waitForStringInLogUsingMark(e77));
    }

    private void runEDR(String url, boolean makeConcurrentRequests) throws Exception {
        String expect1 = "initial EDR header";
        String expect2 = "updated EDR header";
        String orgEdrFile = "headerEDR1.jsp";
        String updEdrFile = "headerEDR2.jsp";
        String relEdrPath = "../../shared/config/ExtendedDocumentRoot/";
        String fullEdrPath = server.getServerRoot() + "/" + relEdrPath;
        LOG.info("fullEdrPath: " + fullEdrPath);

        server.copyFileToLibertyServerRoot(relEdrPath, orgEdrFile);
        WebConversation wc1 = new WebConversation();
        WebRequest request1 = new GetMethodWebRequest(url);

        if (makeConcurrentRequests) {
            // Make 2 requests.
            makeConcurrentRequests(wc1, request1, 2);
        }

        WebResponse response1 = wc1.getResponse(request1);
        LOG.info("Servlet response : " + response1.getText());
        assertTrue("The response did not contain: " + expect1, response1.getText().contains(expect1));

        Thread.sleep(5000L); // delay a bit to ensure a noticeable time diff on updated EDR file
        server.copyFileToLibertyServerRoot(relEdrPath, updEdrFile);
        server.deleteFileFromLibertyServerRoot(relEdrPath + orgEdrFile);
        server.renameLibertyServerRootFile(relEdrPath + updEdrFile, relEdrPath + orgEdrFile);
        File updFile = new File(fullEdrPath + orgEdrFile);
        updFile.setReadable(true);
        updFile.setLastModified(System.currentTimeMillis());

        WebConversation wc2 = new WebConversation();
        WebRequest request2 = new GetMethodWebRequest(url);
        WebResponse response2 = wc2.getResponse(request2);
        LOG.info("Servlet response : " + response2.getText());
        assertTrue("The response did not contain: " + expect2, response2.getText().contains(expect2));
        server.deleteFileFromLibertyServerRoot(relEdrPath + orgEdrFile); // cleanup
        Thread.sleep(500L); // ensure file is deleted
    }

    /*
     * Verify a stackover flow error does not occur via the include(String relativeUrlPath, boolean flush)
     *
     * See https://github.com/OpenLiberty/open-liberty/issues/27779 for more details
     */
    @Test
    @Mode(TestMode.FULL)
    public void testOLGH27779() throws Exception {
        this.verifyStringInResponse(OLGH27779_APP_NAME, "index.jsp", "Test Passed!");
    }

    /*
     * Test to verify the following error does not occur:
     * "The code of method _jspService(HttpServletRequest, HttpServletResponse) is exceeding the 65535 bytes limit"
     *
     * Method should be around 65518 bytes long. (Checked via javap -v _test.class)
     * 65514: aload_3
     * 65515: invokevirtual #551 // Method _jsp_performFinalCleanUp:(Ljava/util/ArrayList;Ljakarta/servlet/jsp/PageContext;)V
     * 65518: return
     */
    @Test
    @Mode(TestMode.FULL)
    public void testPH62212() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, PH62212_APP_NAME, "large-jsp.jsp"));
        WebResponse response = wc.getResponse(request);

        int status = response.getResponseCode();

        if (status != 200) {
            LOG.info("Response : " + response.getText());
        }
        assertEquals("Expected " + 200 + " status code was not returned!", 200, status);
    }

    /*
     * This is with usePageTagPool enabled. Size should be around 65514 bytes long:
     * 65509: aload 7
     * 65511: invokespecial #659 // Method cleanupTaglibLookup:(Ljava/util/HashMap;)V
     * 65514: return
     */
    @Test
    @Mode(TestMode.FULL)
    public void testPH62212_ThreadPool() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, PH62212_THREADPOOL_APP_NAME, "large-jsp.jsp"));
        WebResponse response = wc.getResponse(request);

        int status = response.getResponseCode();

        if (status != 200) {
            LOG.info("Response : " + response.getText());
        }
        assertEquals("Expected " + 200 + " status code was not returned!", 200, status);
    }

    /*
     * This is with useThreadTagPool enabled. Size should be around 65522 bytes long.
     * 65517: aload 7
     * 65519: invokespecial #649 // Method cleanupTaglibLookup:(Ljakarta/servlet/http/HttpServletRequest;Ljava/util/HashMap;)V
     * 65522: return
     */
    @Test
    @Mode(TestMode.FULL)
    public void testPH62212_PagePool() throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, PH62212_PAGEPOOL_APP_NAME, "large-jsp.jsp"));
        WebResponse response = wc.getResponse(request);

        int status = response.getResponseCode();

        if (status != 200) {
            LOG.info("Response : " + response.getText());
        }
        assertEquals("Expected " + 200 + " status code was not returned!", 200, status);
    }
    // Helper Methods

    public void makeConcurrentRequests(WebConversation wc1, WebRequest request1, int numberOfCalls) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfCalls);
        final Collection<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();

        // run the test multiple times concurrently
        for (int i = 0; i < numberOfCalls; i++) {
            tasks.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    LOG.info("Thread Started: Making Request!");
                    wc1.getResponse(request1);
                    return true;
                }
            }));
        }

        // check runs completed successfully
        for (Future<Boolean> task : tasks) {
            try {
                if (!task.get())
                    throw new Exception("0");
            } catch (Exception e) {
                throw new Exception("1", e);
            }
        }
    }

    private void verifyStringsInResponse(String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, contextRoot, path));
        WebResponse response = wc.getResponse(request);
        LOG.info("Response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        String responseText = response.getText();

        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseText.contains(expectedResponse));
        }
    }

    private void verifyStringInResponse(String contextRoot, String path, String expectedResponseString) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(JSPUtils.createHttpUrlString(server, contextRoot, path));
        WebResponse response = wc.getResponse(request);
        LOG.info("Response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        String responseText = response.getText();

        assertTrue("The response did not contain: " + expectedResponseString, responseText.contains(expectedResponseString));
    }

}
