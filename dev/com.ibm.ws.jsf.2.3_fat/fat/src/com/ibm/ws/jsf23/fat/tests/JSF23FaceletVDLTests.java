/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

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
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class is to be used for the tests that test function specified in
 * the JSF 2.3 specification under the "Changes between 2.2 and 2.3" for Facelets/VDL.
 */
@RunWith(FATRunner.class)
public class JSF23FaceletVDLTests {

    protected static final Class<?> c = JSF23FaceletVDLTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23Server")
    public static LibertyServer jsf23Server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23Server, "ImportConstantsTag.war", "com.ibm.ws.jsf23.fat.constants");
        jsf23Server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23Server != null && jsf23Server.isStarted()) {
            jsf23Server.stopServer();
        }
    }

    /**
     * Test to ensure that the <f:importConstants> component actually works properly. The
     * test page references individual constants in multiple <h:outputText> components as well
     * as references the entire map of constants. This test ensure both sets of values are correct.
     *
     * This test tests constants defined in a Class,Interface, and an Enum.
     *
     * @throws Exception
     */
    @Test
    public void testImportConstantsTag() throws Exception {
        String contextRoot = "ImportConstantsTag";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "");

            HtmlPage testImportConstantsTagPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testImportConstantsTagPage.asText());
            Log.info(c, name.getMethodName(), testImportConstantsTagPage.asXml());

            // First ensure that the constants defined in a Class work properly
            String out1 = testImportConstantsTagPage.getElementById("form1:out1").getTextContent();
            String out2 = testImportConstantsTagPage.getElementById("form1:out2").getTextContent();
            String out3 = testImportConstantsTagPage.getElementById("form1:out3").getTextContent();

            String out4 = testImportConstantsTagPage.getElementById("form1:out4").getTextContent();

            // Ensure when referencing a specific constant the correct value is returned
            assertTrue("Look up of TestConstantsClass.TEST_CONSTANTS_1 returned an incorrect value:" + out1, out1.equals("Testing "));
            assertTrue("Look up of TestConstantsClass.TEST_CONSTANTS_2 returned an incorrect value:" + out2, out2.equals("a "));
            assertTrue("Look up of TestConstantsClass.TEST_CONSTANTS_3 returned an incorrect value:" + out3, out3.equals("class!"));

            // Ensure the correct values are returned when referencing the entire map
            assertTrue("The value returned by referencing the entire map of constants by the class name was not the correct length:" + out4.length(),
                       out4.length() == 73);
            assertTrue("Referencing the entire map of constants by the class name did not contain the correct values: "
                       + out4,
                       out4.contains("TEST_CONSTANTS_1=Testing ") && out4.contains("TEST_CONSTANTS_2=a ")
                               && out4.contains("TEST_CONSTANTS_3=class!"));

            // Ensure that the constants defined in an Interface work properly
            String out5 = testImportConstantsTagPage.getElementById("form1:out5").getTextContent();
            String out6 = testImportConstantsTagPage.getElementById("form1:out6").getTextContent();
            String out7 = testImportConstantsTagPage.getElementById("form1:out7").getTextContent();

            String out8 = testImportConstantsTagPage.getElementById("form1:out8").getTextContent();

            // Ensure when referencing a specific constant the correct value is returned
            assertTrue("Look up of TestConstantsInterface.TEST_CONSTANTS_1 returned an incorrect value:" + out5, out5.equals("Testing "));
            assertTrue("Look up of TestConstantsInterface.TEST_CONSTANTS_2 returned an incorrect value:" + out6, out6.equals("an "));
            assertTrue("Look up of TestConstantsInterface.TEST_CONSTANTS_3 returned an incorrect value:" + out7, out7.equals("interface!"));

            // Want to ensure the correct values are returned when referencing the entire map
            assertTrue("The value returned by referencing the entire map of constants by the interface name was not the correct length:" + out8.length(),
                       out8.length() == 78);
            assertTrue("Referencing the entire map of constants by the interface name did not contain the correct values: "
                       + out8,
                       out8.contains("TEST_CONSTANTS_1=Testing ") && out8.contains("TEST_CONSTANTS_2=an ")
                               && out8.contains("TEST_CONSTANTS_3=interface!"));

            // Ensure that the constants defined in an Enum work properly
            String out9 = testImportConstantsTagPage.getElementById("form1:out9").getTextContent();
            String out10 = testImportConstantsTagPage.getElementById("form1:out10").getTextContent();
            String out11 = testImportConstantsTagPage.getElementById("form1:out11").getTextContent();

            String out12 = testImportConstantsTagPage.getElementById("form1:out12").getTextContent();

            // Ensure when referencing a specific constant the correct value is returned
            assertTrue("Look up of TestConstantsEnum.TEST_CONSTANTS_1 returned an incorrect value:" + out9, out9.equals("Testing "));
            assertTrue("Look up of TestConstantsEnum.TEST_CONSTANTS_2 returned an incorrect value:" + out10, out10.equals("an "));
            assertTrue("Look up of TestConstantsEnum.TEST_CONSTANTS_3 returned an incorrect value:" + out11, out11.equals("enum!"));

            // Want to ensure the correct values are returned when referencing the entire map
            assertTrue("The value returned by referencing the entire map of constants by the enum name was not the correct length:" + out12.length(),
                       out12.length() == 105);
            assertTrue("Referencing the entire map of constants by the enum name did not contain the correct values: "
                       + out12,
                       out12.contains("TEST_CONSTANTS_1=TEST_CONSTANTS_1") && out12.contains("TEST_CONSTANTS_2=TEST_CONSTANTS_2")
                                && out12.contains("TEST_CONSTANTS_3=TEST_CONSTANTS_3"));
        }
    }

    /**
     * Test to ensure that if the default Project Stage of Production is active that
     * the FACELETS_REFRESH_PERIOD will default to -1, there is no refresh and the facelets
     * are cached indefinitely.
     *
     * In order to test this we are just looking at the values of the following context parameters
     * for a simple application:
     *
     * Then we try a hot update of the facelet and drive another request to ensure the facelet
     * is not updated.
     *
     * javax.faces.STATE_SAVING_METHOD should be set to server
     * javax.faces.FACELETS_REFRESH_PERIOD should be set to -1
     *
     * If we find the above values then we know the proper default values are being used.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testFaceletRefreshPeriodProduction() throws Exception {
        String contextRoot = "FaceletRefreshPeriodProductionProjectStage";
        String appName = contextRoot + ".war";

        jsf23Server.setMarkToEndOfLog();
        jsf23Server.saveServerConfiguration();
        ShrinkHelper.defaultApp(jsf23Server, appName);
        jsf23Server.setServerConfigurationFile(contextRoot + ".xml");

        // Ensure the application was installed successfully.
        assertNotNull("The application " + appName + " did not appear to have been installed.",
                      jsf23Server.waitForStringInLog("CWWKZ0001I.* " + appName.substring(0, appName.indexOf("."))));

        if(JakartaEE9Action.isActive()){
          String result = jsf23Server.waitForStringInLogUsingMark(".*No context init parameter 'jakarta\\.faces\\.FACELETS_REFRESH_PERIOD' found, using default value '-1'.*");
          String result2 = jsf23Server.waitForStringInLogUsingMark(".*No context init parameter 'jakarta\\.faces\\.STATE_SAVING_METHOD' found, using default value 'server'*");

          // Verify that the correct values of the context parameters were found.
          assertNotNull("The correct value of the jakarta.faces.FACELETS_REFRESH_PERIOD context parameter was not found", result);
          assertNotNull("The correct value of the jakarta.faces.STATE_SAVING_METHOD context parameter was not found", result2);
        } else {
          String result = jsf23Server.waitForStringInLogUsingMark(".*No context init parameter 'javax\\.faces\\.FACELETS_REFRESH_PERIOD' found, using default value '-1'.*");
          String result2 = jsf23Server.waitForStringInLogUsingMark(".*No context init parameter 'javax\\.faces\\.STATE_SAVING_METHOD' found, using default value 'server'*");

          // Verify that the correct values of the context parameters were found.
          assertNotNull("The correct value of the javax.faces.FACELETS_REFRESH_PERIOD context parameter was not found", result);
          assertNotNull("The correct value of the javax.faces.STATE_SAVING_METHOD context parameter was not found", result2);
        }

        // Drive a request to the context root and ensure it contains the correct text
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain the Original Facelet text.", page.asText().contains("Original Facelet"));

            // Perform a hot replace and ensure the facelet does not update on the next request;
            String appPath = jsf23Server.getServerRoot() + "/apps/expanded/" + appName;
            new File(appPath).mkdirs();
            jsf23Server.copyFileToLibertyInstallRoot("/usr/servers/jsf23Server/apps/expanded/" + appName, "index.xhtml");

            // Drive another request to ensure the facelet was not refreshed
            page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain the Original Facelet text.", page.asText().contains("Original Facelet"));

        }
        // Move the mark to the end of the log so we can ensure we wait for the correct server
        // configuration message to be output before uninstalling the application
        jsf23Server.setMarkToEndOfLog();

        // restore the original server configuration and uninstall the application
        jsf23Server.restoreServerConfiguration();

        // Ensure that the server configuration has completed before uninstalling the application
        jsf23Server.waitForConfigUpdateInLogUsingMark(null);

        jsf23Server.removeInstalledAppForValidation(contextRoot);
    }
}
