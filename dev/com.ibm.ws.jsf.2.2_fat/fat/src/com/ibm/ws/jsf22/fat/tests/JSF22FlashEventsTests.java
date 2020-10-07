/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfTestServer1 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22FlashEventsTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22FlashEvents";

    protected static final Class<?> c = JSF22FlashEventsTests.class;

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    private static RemoteFile[] logFiles;

    private static List<String> eventMessages = Arrays.asList("PostPutFlashValueEvent processEvent", "PreClearFlashEvent processEvent", "PostKeepFlashValueEvent processEvent",
                                                              "PreRemoveFlashValueEvent processEvent");

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfTestServer1, "JSF22FlashEvents.war", "com.ibm.ws.jsf22.fat.flashevents.*");

        jsfTestServer1.startServer(JSF22FlashEventsTests.class.getSimpleName() + ".log");

        RemoteFile traceFile = new RemoteFile(jsfTestServer1.getMachine(), jsfTestServer1.getLogsRoot() + "trace.log");

        // Set up log files
        Log.info(c, "setup", "setupLogFiles - defaultLogFile: " + jsfTestServer1.getDefaultLogFile());
        Log.info(c, "setup", "setupLogFiles - traceFile: " + traceFile);
        logFiles = new RemoteFile[] { jsfTestServer1.getDefaultLogFile(), traceFile };
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
            jsfTestServer1.stopServer();
        }
    }

    /**
     * This series of tests contain a counter for the Events. Therefore they must be called in a certain order.
     * If they are separated in to differents tests then we cannot guarantee the order they are called in and could fail
     * because the counter was not at the expected total.
     * Since the version of junit we use does not contain the code to use the FixMethodOrder annotation, we need to have this
     * as one large test.
     * There are three different tests in here:
     * 1. Call indexNoFlash.jsf
     * Test a normal JSF request and ensure the events are not called.
     *
     * 2. Call indexFlash.jsf
     * Submit a value then check that the Events were called.
     * Check that the custom Factory implementation is used.
     * Check that the keep feature is working by verifying the third page still has the value.
     *
     * 3. Call indexFlashAndKeep.jsf
     * Submit a value then check that the Events were called and that the keep feature works.
     * Also check that the custom Factory implementation is used.
     *
     * @throws Exception
     */
    @Test
    public void testFlashEvents() throws Exception {
        try (WebClient webClient = new WebClient()) {
            jsfTestServer1.setMarkToEndOfLog(logFiles);

            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "indexNoFlash.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /JSF22FlashEvents/indexNoFlash.jsf");

            HtmlTextInput input = (HtmlTextInput) page.getElementById("testForm:firstName");
            input.type("John", false, false, false);

            HtmlElement button = (HtmlElement) page.getElementById("testForm:submitCommandButton1");
            page = button.click();

            List<String> areEventsCalled = jsfTestServer1.findStringsInLogsAndTraceUsingMarkMultiRegexp(eventMessages);
            // The events should not have been called in this test, fail if they are
            assertTrue("The events should not have been called.", areEventsCalled.isEmpty());

            //start of test with Flash but no Keep
            jsfTestServer1.setMarkToEndOfLog();
            url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "indexFlash.jsf");
            page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /JSF22FlashEvents/indexFlash.jsf");

            input = (HtmlTextInput) page.getElementById("testForm:firstName");
            input.type("John", false, false, false);

            button = (HtmlElement) page.getElementById("testForm:submitCommandButton1");
            page = button.click();

            HtmlElement output = (HtmlElement) page.getElementById("testFormPage2:firstName");
            Log.info(c, name.getMethodName(), "Check that the value was saved: " + output.asText());
            assertEquals("John", output.asText());

            HtmlElement outputFlash = (HtmlElement) page.getElementById("testFormPage2:flashImpl");
            Log.info(c, name.getMethodName(), "Check that the custom Flash implementation was used: " + outputFlash.asText());
            assertEquals("class com.ibm.ws.jsf22.fat.flashevents.factory.TestFlashImpl", outputFlash.asText());

            HtmlElement outputFactory = (HtmlElement) page.getElementById("testFormPage2:flashFactory");
            Log.info(c, name.getMethodName(), "Check that the custom FlashFactory implementation was used: " + outputFactory.asText());
            assertEquals("class com.ibm.ws.jsf22.fat.flashevents.factory.TestFlashFactory", outputFactory.asText());

            button = (HtmlElement) page.getElementById("testFormPage2:submitCommandButton2");
            page = button.click();

            List<String> postPutFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PostPutFlashValueEvent processEvent - counter: 3");
            List<String> preClearFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PreClearFlashEvent processEvent - counter: 1");
            List<String> postKeepFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PostKeepFlashValueEvent processEvent - counter:"); //should be not be in logs
            List<String> preRemoveFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PreRemoveFlashValueEvent processEvent - counter:"); //should be not be in logs

            Log.info(c, name.getMethodName(), "The PostPutFlashValueEvent should have been called 3 times in this test.  Actual value: " + postPutFlash);
            Log.info(c, name.getMethodName(), "The PreClearFlashEvent event should have been called 1 time in this test.  Actual value: " + preClearFlash);
            Log.info(c, name.getMethodName(), "The PostKeepFlashValueEvent event should not have been called in this test.  Actual value: " + postKeepFlash);
            Log.info(c, name.getMethodName(), "The PreRemoveFlashValueEvent event should not have been called in this test.  Actual value: " + preRemoveFlash);

            assertFalse("The expected number of PostPutFlashValueEvent calls did not occur", postPutFlash.isEmpty());
            assertFalse("The expected number of PreClearFlashEvent calls did not occur", preClearFlash.isEmpty());
            assertTrue("The expected number of PostKeepFlashValueEvent calls did not occur", postKeepFlash.isEmpty());
            assertTrue("The expected number of PreRemoveFlashValueEvent calls did not occur", preRemoveFlash.isEmpty());

            output = (HtmlElement) page.getElementById("testFormPage3:firstName");
            Log.info(c, name.getMethodName(), "Check that the value was not saved because the keep feature was not used: " + output.asText());
            assertEquals("", output.asText());

            //Start test with Flash and Keep
            jsfTestServer1.setMarkToEndOfLog();

            url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "indexFlashAndKeep.jsf");
            page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: /JSF22FlashEvents/indexFlashAndKeep.jsf");

            input = (HtmlTextInput) page.getElementById("testForm:firstName");
            input.type("Mary", false, false, false);

            button = (HtmlElement) page.getElementById("testForm:submitCommandButton1");
            page = button.click();

            output = (HtmlElement) page.getElementById("testFormPage2:firstName");
            Log.info(c, name.getMethodName(), "Check that the value was saved: " + output.asText());
            assertEquals("Mary", output.asText());

            outputFlash = (HtmlElement) page.getElementById("testFormPage2:flashImpl");
            Log.info(c, name.getMethodName(), "Check that the custom Flash implementation was used: " + outputFlash.asText());
            assertEquals("class com.ibm.ws.jsf22.fat.flashevents.factory.TestFlashImpl", outputFlash.asText());

            button = (HtmlElement) page.getElementById("testFormPage2:submitCommandButton2");
            page = button.click();

            postPutFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PostPutFlashValueEvent processEvent - counter: 2");
            preClearFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PreClearFlashEvent processEvent - counter: 2");
            postKeepFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PostKeepFlashValueEvent processEvent - counter: 1");
            preRemoveFlash = jsfTestServer1.findStringsInLogsAndTraceUsingMark("PreRemoveFlashValueEvent processEvent - counter:"); //should be not be in logs

            Log.info(c, name.getMethodName(), "The PostPutFlashValueEvent should have been called 2 times in this test.  Actual value: " + postPutFlash);
            Log.info(c, name.getMethodName(), "The PreClearFlashEvent event should have been called 2 times in this test.  Actual value: " + preClearFlash);
            Log.info(c, name.getMethodName(), "The PostKeepFlashValueEvent event should have been called 1 time in this test.  Actual value: " + postKeepFlash);
            Log.info(c, name.getMethodName(), "The PreRemoveFlashValueEvent event should not have been called in this test.  Actual value: " + preRemoveFlash);

            assertFalse("The expected number of PostPutFlashValueEvent calls did not occur", postPutFlash.isEmpty());
            assertFalse("The expected number of PreClearFlashEvent calls did not occur", preClearFlash.isEmpty());
            assertFalse("The expected number of PostKeepFlashValueEvent calls did not occur", postKeepFlash.isEmpty());
            assertTrue("The expected number of PreRemoveFlashValueEvent calls did not occur", preRemoveFlash.isEmpty());

            output = (HtmlElement) page.getElementById("testFormPage3:firstName");
            Log.info(c, name.getMethodName(), "Check that the value was saved by using the keep feature: " + output.asText());
            assertEquals("Mary", output.asText());
        }
    }
}