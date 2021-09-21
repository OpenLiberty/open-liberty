/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * The RepsectBinding test is a Liberty port of the tWAS JAXWS-2.2 test for the enablement of
 * the RespectBinding feature on both the Client and Server side. A WSDL denotes that RespectBinding is
 * required when the <wsdl:binding> element contains the "required=true" property. This test uses two wsdls
 * one with a valid binding in when required is set to true, and one invalid where required is set to false.
 *
 * The test enables the Respect Binding feature via the @RespectBinding annotation
 * when set on the Web Service Implementation jaxws22.respectbinding.server.*.Echo, by swapping between three
 * different versions of the Web Service.
 *
 * 1 jaxws22.respectbinding.server.noanno.Echo - No @RespectBinding annotation set
 * 2 jaxws22.respectbinding.server.annotrue.Echo - @RespectBinding annotation set to true
 * 3 jaxws22.respectbinding.server.annofalse.Echo - @RespectBinding annotation set to false
 *
 * Dynamically adjusting which version of Echo the test uses requires alternating versions of web.xml with
 * with different settings for the Servlet class.
 *
 * The tests then use the webservice.xml to alternate between different configurations. One where no <respect-binding>
 * is set, one set to true, and the third set to false.
 *
 * Lastly, the test run through a set of three different WebServiceClient's that are injected into EchoServlet
 * each with @RespectBinding not set, set to true, or set to false.
 *
 * EchoSerlvet will determine which version of the injected client to use based on a parameter pasted in the HTTP
 * request url.
 *
 * Since the various test scenarios can be described through a truth table, each test method is described with a
 * config table to represent which components are set to what regarding Respect Binding, and then the expected
 * PASS or FAIl result.
 */
@RunWith(FATRunner.class)
public class RespectBindingTest {
    @Server("RespectBindingTestServer")
    public static LibertyServer server;

    private final Class<?> thisClass = RespectBindingTest.class;

    private static String appName = "respectBindingService";

    // The following strings are the path and file names for temporary directory path that contains all
    // of the config files used by the test, as well as all of the variations of the App's configuration
    // that can be used to test the RespectBinding feature.
    private static String tempConfigPath = "lib/LibertyFATTestFiles/respectBindingServiceTest/";

    private static String serverTempPath = "temp/";
    private static String ddFileName = "webservices.xml";
    private static String ddNoRBFileName = "webservices_none.xml";
    private static String ddDisabledRBFileName = "webservices_disabled.xml";
    private static String ddEnabledRBFileName = "webservices_enabled.xml";

    private static String noAnnoWebXml = "web_noanno.xml";
    private static String annoTrueWebXml = "web_annotrue.xml";
    private static String annoFalseWebXml = "web_annofalse.xml";

    private static String wsdlAppPath = "dropins/respectBindingService.war/WEB-INF/wsdl/";
    private static String wsdlFileName = "EchoService.wsdl";
    private static String validRBWsdlFileName = "EchoService_valid.wsdl";
    private static String invalidRBWsdlFileName = "EchoService_invalid.wsdl";

    // Parameters used by the tests. They're added to the HTTP Request and allow the
    // EchoServlet to determine with EchoService to use.
    private static String noAnnoParam = "noAnnotation";
    private static String annoTrueParam = "trueAnnotation";
    private static String annoFalseParam = "falseAnnotation";

    static WebArchive noAnnoApp;
    static WebArchive trueAnnoApp;
    static WebArchive falseAnnoApp;

    String expectedResponse = "Hello World";

    protected String getBaseUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/respectBindingService/EchoServlet?serviceType=";
    }

    @BeforeClass
    public static void setUp() throws Exception {

        // Build different apps for each scenario
        noAnnoApp = ShrinkHelper.buildDefaultApp(appName, "jaxws22.respectbinding.server", "jaxws22.respectbinding.server.noanno");

        trueAnnoApp = ShrinkHelper.buildDefaultApp(appName, "jaxws22.respectbinding.server", "jaxws22.respectbinding.server.annotrue");
        falseAnnoApp = ShrinkHelper.buildDefaultApp(appName, "jaxws22.respectbinding.server", "jaxws22.respectbinding.server.annofalse");

    }

    @Before
    public void preTest() throws Exception {
        // make sure the server is stopped and applications are removed before starting each test
        if (!server.isStarted()) {
            server.startServer();
        }
    }

    @After
    public void postTest() throws Exception {
        if (server.isStarted()) {
            server.removeAndStopDropinsApplications(appName + ".war");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {

            server.stopServer("SRVE0315E", "SRVE0777E", "SRVE0271E", "SRVE0207E", "CWWKZ0014W");
        }

    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None None None
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoNoneServiceAnnoNoneDDNone() throws Exception {

        // Copy web.xml with ...noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None None True
     *
     * Expected Result: * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoNoneServiceAnnoNoneDDTrue() throws Exception {

        // Copy web.xml with ...noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect Fail
        runTest(noAnnoParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None None False
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoNoneServiceAnnoNoneDDFalse() throws Exception {

        // Copy web.xml with ...noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None True None
     *
     * Expected Result: * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoNoneServiceAnnoTrueDDNone() throws Exception {

        // Copy web.xml with …trueanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect Fail
        runTest(noAnnoParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None True True
     *
     * Expected Result: * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoNoneServiceAnnoTrueDDTrue() throws Exception {

        // Copy web.xml with …trueanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect Fail
        runTest(noAnnoParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None True False
     *
     * Expected Result: * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoNoneServiceAnnoTrueDDFalse() throws Exception {

        // Copy web.xml with …trueanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect Fail
        runTest(noAnnoParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None False None
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoNoneServiceAnnoFalseDDNone() throws Exception {

        // Copy web.xml with …falseanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None False True
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoNoneServiceAnnoFalseDDTrue() throws Exception {

        // Copy web.xml with …falseanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid None False False
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoNoneServiceAnnoFalseDDFalse() throws Exception {

        // Copy web.xml with …falseanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True None None
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoTrueServiceAnnoNoneDDNone() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True None True
     *
     * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoTrueServiceAnnoNoneDDTrue() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Fail
        runTest(annoTrueParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True None False
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoTrueServiceAnnoNoneDDFalse() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding disabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True True None
     *
     * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoTrueServiceAnnoTrueDDNone() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Fail
        runTest(annoTrueParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True True True
     *
     * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoTrueServiceAnnoTrueDDTrue() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Fail
        runTest(annoTrueParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True True False
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoTrueServiceAnnoTrueDDFalse() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding disabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect PASS
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True False None
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoTrueServiceAnnoFalseDDNone() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect PASS
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True False True
     *
     * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoTrueServiceAnnoFalseDDTrue() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Fail
        runTest(annoTrueParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid True False False
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoTrueServiceAnnoFalseDDFalse() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False None None
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoFalseServiceAnnoNoneDDNone() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False None True
     *
     * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoFalseServiceAnnoNoneDDTrue() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False None False
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoFalseServiceAnnoNoneDDFalse() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False True None
     *
     * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoFalseServiceAnnoTrueDDNone() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False True True
     *
     * Expected Result: Fail
     */
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    @Test
    public void testValidWsdlClientAnnoFalseServiceAnnoTrueDDTrue() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect FAIL
        runTest(annoFalseParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False True False
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoFalseServiceAnnoTrueDDFalse() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding disabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False False None
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoFalseServiceAnnoFalseDDNone() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False False True
     *
     * Expected Result: Fail
     */
    @Test
    @ExpectedFFDC({ "javax.xml.ws.WebServiceException", "javax.servlet.ServletException" })
    public void testValidWsdlClientAnnoFalseServiceAnnoFalseDDTrue() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect Fail
        runTest(annoFalseParam, false);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * Valid False False True
     *
     * Expected Result: Pass
     */
    @Test
    public void testValidWsdlClientAnnoFalseServiceAnnoFalseDDFalse() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a Valid Binding to a temp directory that can be published to the app
        setWsdl(validRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect Pass
        runTest(annoFalseParam, true);
    }

/*
 * ***** Test Configuration Table ****
 * WSDL | Client | Service | DD
 * invalid None None None
 *
 * Expected Result: Pass
 */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoNoneDDNone() throws Exception {

        // Copy web.xml with ...noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None None True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoNoneDDTrue() throws Exception {

        // Copy web.xml with ...noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None None False
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoNoneDDFalse() throws Exception {

        // Copy web.xml with ...noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None True None
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoTrueDDNone() throws Exception {

        // Copy web.xml with …trueanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None True True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoTrueDDTrue() throws Exception {

        // Copy web.xml with …trueanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None True False
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoTrueDDFalse() throws Exception {

        // Copy web.xml with …trueanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None False None
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoFalseDDNone() throws Exception {

        // Copy web.xml with …falseanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None False True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoFalseDDTrue() throws Exception {

        // Copy web.xml with …falseanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid None False False
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoNoneServiceAnnoFalseDDFalse() throws Exception {

        // Copy web.xml with …falseanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with noAnnoParam - Expect PASS
        runTest(noAnnoParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True None None
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoNoneDDNone() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True None True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoNoneDDTrue() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True None False
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoNoneDDFalse() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding disabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True True None
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoTrueDDNone() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True True True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoTrueDDTrue() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True True False
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoTrueDDFalse() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding disabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect PASS
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True False None
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoFalseDDNone() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect PASS
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True False True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoFalseDDTrue() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid True False False
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoTrueServiceAnnoFalseDDFalse() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoTrueParam - Expect Pass
        runTest(annoTrueParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False None None
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoNoneDDNone() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect Pass
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False None True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoNoneDDTrue() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False None False
     *
     * Expected Result: Fail
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoNoneDDFalse() throws Exception {

        // Copy web.xml with …noanno.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(noAnnoWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(noAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False True None
     *
     * Expected Result: Fail
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoTrueDDNone() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False True True
     *
     * Expected Result: Fail
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoTrueDDTrue() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect Pass
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False True False
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoTrueDDFalse() throws Exception {

        // Copy web.xml with ...annotrue.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoTrueWebXml);

        // Copy WebServices.xml DD with RespectBinding disabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(trueAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False False None
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoFalseDDNone() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding not defined to a temp directory that can be published to the app
        setWebservicesXml(ddNoRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect PASS
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False False True
     *
     * Expected Result: Pass
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoFalseDDTrue() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddEnabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect Pass
        runTest(annoFalseParam, true);
    }

    /*
     * ***** Test Configuration Table ****
     * WSDL | Client | Service | DD
     * invalid False False True
     *
     * Expected Result: True
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidWsdlClientAnnoFalseServiceAnnoFalseDDFalse() throws Exception {

        // Copy web.xml with ...annofalse.EchoPort endpoint to a temp directory that can be published to the app
        setWebXml(annoFalseWebXml);

        // Copy WebServices.xml DD with RespectBinding enabled to a temp directory that can be published to the app
        setWebservicesXml(ddDisabledRBFileName);

        // Copy WSDL with a invalid Binding to a temp directory that can be published to the app
        setWsdl(invalidRBWsdlFileName);

        // Publish temp directory with updated config to app
        buidTestAppFromTempDirectory(falseAnnoApp);

        // Invoke EchoServlet with annoFalseParam - Expect Pass
        runTest(annoFalseParam, true);
    }

    // The tests can use three different web.xmls that expose The Echo web service as a Servlet.
    // The webXml string indicates what version of the web.xml to be copied to the app.
    private void setWebXml(String webXml) {
        // Copy web.xml with ...noanno.EchoPort endpoint
        try {
            server.copyFileToLibertyServerRoot(tempConfigPath, serverTempPath, webXml);
            server.renameLibertyServerRootFile(serverTempPath + webXml, serverTempPath + "web.xml");
            server.copyFileToTempDir(serverTempPath + "web.xml", "/WEB-INF/" + "web.xml");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
    }

    // The tests can use three different webservices.xmls that all contain different RespectBinding overrides
    // The webServicesXml string indicates what version of the webservices.xml to be copied to the app.
    private void setWebservicesXml(String webServicesXml) {
        // Copy web.xml with ...noanno.EchoPort endpoint
        try {
            server.copyFileToLibertyServerRoot(tempConfigPath, serverTempPath, webServicesXml);
            server.renameLibertyServerRootFile(serverTempPath + webServicesXml, serverTempPath + ddFileName);
            server.copyFileToTempDir(serverTempPath + ddFileName, "/WEB-INF/" + ddFileName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
    }

    // The tests can use two different wsdls that all contain wsdl:binding values
    // The wsdl string indicates what version of the EchoService.wsdl to be copied to the app.
    private void setWsdl(String wsdl) {
        try {
            server.copyFileToLibertyServerRoot(tempConfigPath, serverTempPath, wsdl);
            server.renameLibertyServerRootFile(serverTempPath + wsdl, serverTempPath + wsdlFileName);
            server.copyFileToTempDir(serverTempPath + wsdlFileName, "WEB-INF/wsdl/" + wsdlFileName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
    }

    // The tests build and export the app from the temp directory
    private void buidTestAppFromTempDirectory(WebArchive webApp) {

        try {
            ShrinkHelper.addDirectory(webApp, "lib/LibertyFATTestFiles/tmp");
            ShrinkHelper.addDirectory(webApp, "lib/LibertyFATTestFiles/tmp/WEB-INF/wsdl");
            ShrinkHelper.exportDropinAppToServer(server, webApp, DeployOptions.OVERWRITE);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
    }

    /**
     * Individual tests use this modularized method to make an HTTP Request to the
     * EchoServlet. If the response contains the expected Hello World the app deployed
     * successfully. If not then we know that the app didn't deploy.
     *
     * @param string
     * @param b
     * @throws Exception
     */
    private void runTest(String annoParam, boolean expectedToDeploy) throws Exception {

        String thisMethod = "testInvalidWsdlRequiredNoFeature()";

        String response = null;

        response = TestUtils.getServletResponse(getBaseUrl() + annoParam);

        if (expectedToDeploy) {
            Log.info(thisClass, thisMethod, "Endpoint " + (getBaseUrl() + annoParam) + " is Expected to Deploy");
            assertTrue("Can not access Echo when it should have been accessible, the return result is: " + response,
                       response.contains(expectedResponse));
        } else {
            Log.info(thisClass, thisMethod, "Endpoint " + (getBaseUrl() + annoParam) + " is not Expected to Deploy");
            assertTrue("Can not access Echo when it should have been accessible, the return result is: " + response,
                       !response.contains(expectedResponse));
        }

    }

}
