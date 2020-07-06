/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.util.logging.Logger;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;

import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.tests.all.AnnotatedTest;
import io.openliberty.wsoc.tests.all.BinaryEncodeDecodeTest;
import io.openliberty.wsoc.tests.all.ConfiguratorTest;
import io.openliberty.wsoc.tests.all.MultiClientTest;
import io.openliberty.wsoc.tests.all.OnErrorTest;
import io.openliberty.wsoc.tests.all.PathParamTest;
import io.openliberty.wsoc.tests.all.ProgrammaticTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

// comment #1 (referenced below): Some tests use the AllowedFFDC annotation because these Jetty client test will not run on Z, as desired
// due to the notOnZRule, but the test framework will still look for the "ExpectedFFDC" if that annotation is used, and not finding it, will
// fail the test on Z.

// comment #2 (referenced below): Some tests we found didn't run for "test" reasons, not product code reasons, test were commented out and
// not deleted so the same pattern would not be repeated.

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7)
public class BasicTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("basicTestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private final AnnotatedTest at = new AnnotatedTest(wt);

    private final ProgrammaticTest pt = new ProgrammaticTest(wt);

    private final MultiClientTest mct = new MultiClientTest(wt);

    private final OnErrorTest oet = new OnErrorTest(wt);

    private final PathParamTest ppt = new PathParamTest(wt);

    private final ConfiguratorTest ct = new ConfiguratorTest(wt);

    private final BinaryEncodeDecodeTest bedt = new BinaryEncodeDecodeTest(wt);

    private static final Logger LOG = Logger.getLogger(BasicTest.class.getName());

    private static final String BASIC_JAR_NAME = "basic";
    private static final String BASIC_WAR_NAME = "basic";

    protected WebResponse runAsSSCAndVerifyResponse(String className, String testName) throws Exception {
        int securePort = 0, port = 0;
        String host="";
        LibertyServer server = SS.getLibertyServer();
        if (WebServerControl.isWebserverInFront()) {
            try {
                host = WebServerControl.getHostname();
                securePort = WebServerControl.getSecurePort();
                port = Integer.valueOf(WebServerControl.getPort()).intValue();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get ports or host from webserver", e);
            }
        } else {
            securePort = server.getHttpDefaultSecurePort();
            host = server.getHostname();
            port = server.getHttpDefaultPort();
        }
        return SS.verifyResponse(createWebBrowserForTestCase(),
                                 "/basic/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
                                                                + "&secureport=" + securePort,
                                 "SuccessfulTest");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the basic jar to add to the war app as a lib
        JavaArchive BasicJar = ShrinkHelper.buildJavaArchive(BASIC_JAR_NAME + ".jar",
                                                                   "basic.jar");
        // Build the war app and add the dependencies
        WebArchive BasicApp = ShrinkHelper.buildDefaultApp(BASIC_WAR_NAME + ".war",
                                                                         "basic.war",
                                                                         "basic.war.coding",
                                                                         "basic.war.configurator",
                                                                         "basic.war.servlet",
                                                                         "basic.war.utils",
                                                                         "io.openliberty.wsoc.common",
                                                                         "io.openliberty.wsoc.util.wsoc",
                                                                         "io.openliberty.wsoc.tests.all",
                                                                         "io.openliberty.wsoc.endpoints.client.basic");
        BasicApp = (WebArchive) ShrinkHelper.addDirectory(BasicApp, "test-applications/"+BASIC_WAR_NAME+".war/resources");
        BasicApp = BasicApp.addAsLibraries(BasicJar);
        // Verify if the apps are in the server before trying to deploy them
        if (SS.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SS.getLibertyServer().getInstalledAppNames(BASIC_WAR_NAME);
            LOG.info("addAppToServer : " + BASIC_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
            ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), BasicApp);
        }
        SS.startIfNotStarted();
        SS.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + BASIC_WAR_NAME);
        bwst.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SS.getLibertyServer() != null && SS.getLibertyServer().isStarted()) {
            SS.getLibertyServer().stopServer("CWWKH0023E", "CWWKH0020E", "CWWKH0039E", "CWWKH0040E");
        }
        bwst.tearDown();
    }

    //
    //
    //  ANNOTATED TESTS
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedByteArraySuccess() throws Exception {
        at.testAnnotatedByteArraySuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCAnnotatedByteArraySuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedByteArraySuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMaxSessionIdleTimeout() throws Exception {
        at.testMaxSessionIdleTimeout();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testMaxSessionIdleTimeoutTCKStyle() throws Exception {
        at.testMaxSessionIdleTimeoutTCKStyle();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCMaxSessionIdleTimeoutTCKStyle() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testMaxSessionIdleTimeoutTCKStyle");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testAnnotatedReaderSuccess() throws Exception {
        at.testAnnotatedReaderSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCAnnotatedReaderSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedReaderSuccess");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedTextSuccess() throws Exception {
        at.testAnnotatedTextSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCAnnotatedTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedTextSuccess");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testAsyncAnnotatedTextSuccess() throws Exception {
        at.testAsyncAnnotatedTextSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCAsyncAnnotatedTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAsyncAnnotatedTextSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFutureAnnotatedTextSuccess() throws Exception {
        at.testFutureAnnotatedTextSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCFutureAnnotatedTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testFutureAnnotatedTextSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFutureAnnotatedWithReturnTextSuccess() throws Exception {
        at.testFutureAnnotatedWithReturnTextSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testFutureAnnotatedWithReturnByteSuccess() throws Exception {
        at.testFutureAnnotatedWithReturnByteSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCFutureAnnotatedWithReturnByteSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testFutureAnnotatedWithReturnByteSuccess");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedVoidOnMsgReturn() throws Exception {
        at.testAnnotatedVoidOnMsgReturn();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCAnnotatedVoidOnMsgReturn() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedVoidOnMsgReturn");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedPingSuccess() throws Exception {
        at.testAnnotatedPingSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCAnnotatedPingSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedPingSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testAnnotatedPingPongSuccess() throws Exception {
        at.testAnnotatedPingPongSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCAnnotatedPingPongSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedPingPongSuccess");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedTextMessageSuccess() throws Exception {
        at.testAnnotatedTextMessageSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCAnnotatedTextMessageSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedTextMessageSuccess");
    }

    /**
     * Tests custom ServerEndpoint Configurator's getEndpointInstance() method - 3.1.7 Customizing Endpoint Creation
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfiguratorGetEndpointInstance() throws Exception {
        at.testConfiguratorGetEndpointInstance();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCConfiguratorGetEndpointInstance() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testConfiguratorGetEndpointInstance");
    }

    /**
     * Tests custom ServerEndpoint Configurator's getEndpointInstance() method - 3.1.7 Customizing Endpoint Creation
     * this test shows how customer can share same server endpoint instance across 2 client calls.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testConfiguratorGetEndpointInstanceShared() throws Exception {
        at.testConfiguratorGetEndpointInstanceShared();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCConfiguratorGetEndpointInstanceShared() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testConfiguratorGetEndpointInstanceShared");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testDoubleTypeMessage() throws Exception {
        at.testDoubleTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCDoubleTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testDoubleTypeMessage");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFloatTypeMessage() throws Exception {
        at.testFloatTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCFloatTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testFloatTypeMessage");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testLongTypeMessage() throws Exception {
        at.testLongTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCLongTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testLongTypeMessage");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testIntegerTypeMessage() throws Exception {
        at.testIntegerTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCIntegerTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testIntegerTypeMessage");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testShortTypeMessage() throws Exception {
        at.testShortTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCShortTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testShortTypeMessage");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testPrimitiveShortTypeMessage() throws Exception {
        at.testPrimitiveShortTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCPrimitiveShortTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testPrimitiveShortTypeMessage");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testBooleanTypeMessage() throws Exception {
        at.testBooleanTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCBooleanTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testBooleanTypeMessage");
    }

    /**
     * Test for this section of the spec
     * "3.1.6 Custom State or Processing Across Server Endpoint Instances
     * The developer may also implement ServerEndpointConfig.Configurator in order to hold custom application
     * state or methods for other kinds of application specific processing that is accessible from all Endpoint
     * instances of the same logical endpoint via the EndpointConfig object."
     **/
    @Mode(TestMode.FULL)
    @Test
    public void testSSCCustomStateConfigurator() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testCustomStateConfigurator");
    }

    /**
     * This test show cases MessageHandler used in inheritance scenario
     */
    @Mode(TestMode.LITE)
    @Test
    public void testWillDecode() throws Exception {
        at.testWillDecode();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCWillDecode() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testWillDecode");
    }

    //    Jetty fails on this one -  cannot handle close reason of more than 107 bytes - at least I think it is jetty.
    @Mode(TestMode.FULL)
    // @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testMaxMessageSize() throws Exception {
        at.testMaxMessageSize();
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testSSCMaxMessageSize() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testMaxMessageSize");
    }

    //  Jetty fails on this one -  cannot handle close reason of more than 107 bytes - at least I think it is jetty.
    @Mode(TestMode.FULL)
    //    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testMaxTextMessageSize() throws Exception {
        at.testMaxTextMessageSize();
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testSSCProgrammaticMaxMessageSize() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testProgrammaticMaxMessageSize");
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
    public void testSSCMaxTextMessageSize() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testMaxTextMessageSize");
    }

    //Tests defaults in @ServerEndpoint
    @Mode(TestMode.LITE)
    @Test
    public void testDefaults() throws Exception {
        at.testDefaults();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCDefaults() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testDefaults");
    }

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.LITE)
    //public void testAnnotatedPrimitiveByte() throws Exception {
    //    at.testAnnotatedPrimitiveByte();
    //}

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testSSCAnnotatedPrimitiveByte() throws Exception {
    //    this.runAsSSCAndVerifyResponse("AnnotatedTest", "testAnnotatedPrimitiveByte");
    //}

    @Mode(TestMode.LITE)
    @Test
    public void testbyteReturnTypeMessage() throws Exception {
        at.testbyteReturnTypeMessage();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCbyteReturnTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testbyteReturnTypeMessage");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testListenerAddAnnotatedEndpointTextSuccess() throws Exception {
        at.testListenerAddAnnotatedEndpointTextSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCListenerAddAnnotatedEndpointTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testListenerAddAnnotatedEndpointTextSuccess");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCConnectToClass() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testConnectToClass");
    }

    //
    //
    // PROGRAMMATIC TESTS
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticTextSuccess() throws Exception {
        pt.testProgrammaticTextSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticTextSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticCloseSuccess() throws Exception {
        pt.testProgrammaticCloseSuccess();
    }

    // moved to trace bucket - january 2019
//    @Mode(TestMode.LITE)
//    @Test
//    public void testSSCProgrammaticCloseSuccess() throws Exception {
//        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticCloseSuccess");
//    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticReaderSuccess() throws Exception {
        pt.testProgrammaticReaderSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticReaderSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticReaderSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticInputStreamSuccess() throws Exception {
        pt.testProgrammaticInputStreamSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticInputStreamSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticInputStreamSuccess");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticByteBufferSuccess() throws Exception {
        pt.testProgrammaticByteBufferSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticByteBufferSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticByteBufferSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticCodinguccess() throws Exception {
        pt.testProgrammaticCodinguccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCProgrammaticCodinguccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticCodinguccess");
    }

    // defect 205750 - Test is in Trace Bucket so comment it out of here for now
    //@Mode(TestMode.FULL)
    //@Test
    //public void testProgrammaticCloseSuccessOnOpen() throws Exception {
    //    pt.testProgrammaticCloseSuccessOnOpen();
    //}

    /**
     * This test show cases MessageHandler used in inheritance scenario
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMsgHandlerInheritance() throws Exception {
        pt.testMsgHandlerInheritance();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCMsgHandlerInheritance() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testMsgHandlerInheritance");
    }

    /**
     * This tests session.getPathParameters() in programmatic endpoint
     */
    @Mode(TestMode.LITE)
    @Test
    public void testProgEndpointSessionGetPathParamaters() throws Exception {
        pt.testProgEndpointSessionGetPathParamaters();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgEndpointSessionGetPathParamaters() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgEndpointSessionGetPathParamaters");
    }

    //
    //
    // MULTI CLIENT TESTS
    //
    //

    @Mode(TestMode.FULL)
    @Test
    public void testMultipleClientsPublishingandReceivingToThemselvesTextSuccess() throws Exception {
        mct.testMultipleClientsPublishingandReceivingToThemselvesTextSuccess();
    }

    // Move to trace test bucket because of build break 217622
    //@Mode(TestMode.FULL)
    //@Test
    //public void testSSCMultipleClientsPublishingandReceivingToThemselvesTextSuccess() throws Exception {
    //    this.runAsSSCAndVerifyResponse("MultiClientTest", "testMultipleClientsPublishingandReceivingToThemselvesTextSuccess");
    //}

    // Move to trace test bucket because of build break 244260
    //@Mode(TestMode.FULL)
    //@Test
    //public void testSinglePublisherMultipleReciverTextSuccess() throws Exception {
    //    mct.testSinglePublisherMultipleReciverTextSuccess();
    //}

    @Mode(TestMode.FULL)
    @Test
    public void testSSCSinglePublisherMultipleReciverTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("MultiClientTest", "testSinglePublisherMultipleReciverTextSuccess");
    }

    //
    //
    //  ONERROR TESTS
    //
    //

    /*
     * Negative test for runtime exception during @PathParam value processing
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC({ "java.lang.reflect.InvocationTargetException" })
    // for why we are using AllowedFFDC - see comment #1 at the top of the file
    public void TestOnMessageError() throws Exception {
        oet.TestOnMessageError();
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void TestSSCOnMessageError() throws Exception {
        this.runAsSSCAndVerifyResponse("OnErrorTest", "TestOnMessageError");
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "java.lang.NullPointerException" })
    public void TestSSCProgramticEndpointError() throws Exception {
        this.runAsSSCAndVerifyResponse("OnErrorTest", "TestProgramticEndpointError");
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "javax.websocket.DecodeException" })
    public void TestSSCDecoderError() throws Exception {
        this.runAsSSCAndVerifyResponse("OnErrorTest", "TestDecoderError");
    }

    @Mode(TestMode.FULL)
    @Test
    public void TestSSCEncoderError() throws Exception {
        this.runAsSSCAndVerifyResponse("OnErrorTest", "TestEncoderError");
    }

    //
    //
    //  PATHPARAM TESTS
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testPathParamAnnotatedTextSuccess() throws Exception {
        ppt.testAnnotatedPathParamSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testPathParamShortTypeMessage() throws Exception {
        ppt.testShortTypePathParamMessage();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCPathParamShortTypeMessage() throws Exception {
        this.runAsSSCAndVerifyResponse("PathParamTest", "testShortTypePathParamMessage");
    }

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testPathParamAnotatedReaderSuccess() throws Exception {
    //    ppt.testAnotatedReaderPathParamSuccess();
    //}

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testSSCPathParamAnotatedReaderSuccess() throws Exception {
    //    this.runAsSSCAndVerifyResponse("PathParamTest", "testAnotatedReaderPathParamSuccess");
    //}

    @Mode(TestMode.FULL)
    @Test
    public void TestPathParamOnOpenAndTestOnClose() throws Exception {
        ppt.TestOnOpenAndTestOnClose();
    }

    @Mode(TestMode.FULL)
    @Test
    public void TestSSCPathParamOnOpenAndTestOnClose() throws Exception {
        this.runAsSSCAndVerifyResponse("PathParamTest", "TestOnOpenAndTestOnClose");
    }

    /*
     * Negative test for runtime exception during @PathParam value processing
     */
    @Mode(TestMode.FULL)
    @AllowedFFDC({ "java.lang.NumberFormatException", "javax.websocket.DecodeException" })
    // for why we are using AllowedFFDC - see comment #1 at the top of the file
    public void TestPathParamOnError() throws Exception {
        ppt.TestOnError();
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "java.lang.NumberFormatException", "javax.websocket.DecodeException" })
    public void TestSSCPathParamOnError() throws Exception {
        this.runAsSSCAndVerifyResponse("PathParamTest", "TestOnError");
    }

    /*
     * Negative test for runtime exception during @PathParam value processing
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC({ "javax.websocket.DecodeException" })
    // for why we are using AllowedFFDC - see comment #1 at the top of the file
    public void TestPathParamUnmatchedNonStringPathParamTest() throws Exception {
        ppt.TestUnmatchedNonStringPathParamTest();
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "javax.websocket.DecodeException" })
    public void TestSSCPathParamUnmatchedNonStringPathParamTest() throws Exception {
        this.runAsSSCAndVerifyResponse("PathParamTest", "TestUnmatchedNonStringPathParamTest");
    }

    /*
     * Negative test for runtime exception during @PathParam value processing
     */
    @Mode(TestMode.FULL)
    @Test
    public void TestPathParamUnmatchedStringPathParamTest() throws Exception {
        ppt.TestUnmatchedStringPathParamTest();
    }

    @Mode(TestMode.FULL)
    @Test
    public void TestSSCPathParamUnmatchedStringPathParamTest() throws Exception {
        this.runAsSSCAndVerifyResponse("PathParamTest", "TestUnmatchedStringPathParamTest");
    }

    /**
     * This tests session.getPathParameters() in annotated endpoint
     */
    @Mode(TestMode.LITE)
    @Test
    public void TestPathParamSessionPathParamTest() throws Exception {
        ppt.TestSessionPathParamTest();
    }

    @Mode(TestMode.FULL)
    @Test
    public void TestSSCPathParamSessionPathParamTest() throws Exception {
        this.runAsSSCAndVerifyResponse("PathParamTest", "TestSessionPathParamTest");
    }

    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void TestSSCRuntimeExceptionTCKTest() throws Exception {
        this.runAsSSCAndVerifyResponse("PathParamTest", "RuntimeExceptionTCKTest");
    }

    //
    //
    //  CONFIGURATOR TESTS
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testCheckOriginFailedSuccess() throws Exception {
        ct.testCheckOriginFailedSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCCheckOriginFailedSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testCheckOriginFailedSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testConfiguratorSuccess() throws Exception {

        ct.testConfiguratorSuccess();
    }

    // moved to trace bucket
    //@Mode(TestMode.LITE)
    //@Test
    //public void testSSCConfiguratorSuccess() throws Exception {
    //    this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testConfiguratorSuccess");
    //}

    @Mode(TestMode.FULL)
    @Test
    public void testNewConfiguratorSuccess() throws Exception {
        ct.testNewConfiguratorSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCNewConfiguratorSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testNewConfiguratorSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFailHandshakeSuccess() throws Exception {
        ct.testFailHandshakeSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCFailHandshakeSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testFailHandshakeSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBasicExtensionsSuccess() throws Exception {
        ct.testBasicExtensionsSuccess();
    }

    @Mode(TestMode.FULL)
    // Jetty fails on this... tyrus works..  @Test
    public void testNegotiatedExtensionsSuccess() throws Exception {
        ct.testNegotiatedExtensionsSuccess();;
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCNegotiatedExtensionsSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testNegotiatedExtensionsSuccess");
    }

    @Mode(TestMode.FULL)
    //Jetty fails on this - tyrus works.. @Test
    public void testNegotiatedExtensionsPart2Success() throws Exception {
        ct.testNegotiatedExtensionsPart2Success();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCNegotiatedExtensionsPart2Success() throws Exception {
        this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testNegotiatedExtensionsPart2Success");
    }

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testSSCBasicExtensionsSuccess() throws Exception {
    //    this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testBasicExtensionsSuccess");
    //}

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testDetailedExtensionsSuccess() throws Exception {
    //    ct.testDetailedExtensionsSuccess();
    //}

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testSSCDetailedExtensionsSuccess() throws Exception {
    //    this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testDetailedExtensionsSuccess");
    //}

    @Mode(TestMode.LITE)
    @Test
    public void testBasicSubprotocolsSuccess() throws Exception {
        ct.testBasicSubprotocolsSuccess();
    }

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testConfiguredSubprotocolsSuccess() throws Exception {
    //    ct.testConfiguredSubprotocolsSuccess();
    //}

    // for why this is commented - see comment #2 at the top of the file
    //@Mode(TestMode.FULL)
    //public void testSSCConfiguredSubprotocolsSuccess() throws Exception {
    //    this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testConfiguredSubprotocolsSuccess");
    //}

    @Mode(TestMode.LITE)
    @Test
    public void testClientProgWholeServerAnnotatedPartial() throws Exception {
        at.testClientProgWholeServerAnnotatedPartial();
    }

    // remove these, since server endpoint does this, which is not allowed according to the TCK:
    // request.getHeaders().put("CLIENTHEADER", new ArrayList<String>(
    //                Arrays.asList("SECOND")));
    //  These tests are not particularly useful, as we are really checking the function of treemap with insensitive comparator, but
    //  will include them in full in case anyone changes the maps away from insensitive treemap.
//    @Mode(TestMode.FULL)
//    @Test
//    public void testConfiguratorCase() throws Exception {
//        ct.testConfiguratorCaseSuccess();
//    }
//
//    @Mode(TestMode.FULL)
//    @Test
//    public void testSSCConfiguratorCase() throws Exception {
//        this.runAsSSCAndVerifyResponse("ConfiguratorTest", "testConfiguratorCaseSuccess");
//    }
//
    //
    //
    //  Partial Tests
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticPartialTextSuccess() throws Exception {
        pt.testProgrammaticPartialTextSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticPartialTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticPartialTextSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticPartialTextSuccess2() throws Exception {
        pt.testProgrammaticPartialTextSuccess2();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCProgrammaticPartialTextSuccess2() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticPartialTextSuccess2");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticPartialTextWithServerPing() throws Exception {
        pt.testProgrammaticPartialTextWithServerPing();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticPartialTextWithServerPing() throws Exception {
        this.runAsSSCAndVerifyResponse("ProgrammaticTest", "testProgrammaticPartialTextWithServerPing");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCClientProgWholeServerAnnotatedPartial() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testClientProgWholeServerAnnotatedPartial");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testClientAnnoWholeServerProgPartial() throws Exception {
        at.testClientAnnoWholeServerProgPartial();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCClientAnnoWholeServerProgPartial() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testClientAnnoWholeServerProgPartial");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticPartialBinarySuccess1() throws Exception {
        at.testProgrammaticPartialBinarySuccess1();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticPartialBinarySuccess1() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testProgrammaticPartialBinarySuccess1");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticPartialBinarySuccess2() throws Exception {
        at.testProgrammaticPartialBinarySuccess2();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticPartialBinarySuccess2() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testProgrammaticPartialBinarySuccess2");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testProgrammaticPartialTextSuccess3() throws Exception {
        at.testProgrammaticPartialTextSuccess3();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticPartialTextSuccess3() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testProgrammaticPartialTextSuccess3");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticPartialTextWithClientPing() throws Exception {
        at.testProgrammaticPartialTextWithClientPing();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticPartialTextWithClientPing() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testProgrammaticPartialTextWithClientPing");
    }

    //
    //
    //  BINARY ENCODE DECODE TESTS
    //
    //

    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedBinaryDecoderSuccess() throws Exception {
        bedt.testAnnotatedBinaryDecoderSuccess();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCAnnotatedBinaryDecoderSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("BinaryEncodeDecodeTest", "testAnnotatedBinaryDecoderSuccess");
    }

    //this tests binary decoder and encoder inheritance
    @Mode(TestMode.FULL)
    @Test
    public void testAnnotatedBinaryDecoderExtendSuccess() throws Exception {
        bedt.testAnnotatedBinaryDecoderExtendSuccess();
    }

    //this tests binary decoder and encoder inheritance
    @Mode(TestMode.LITE)
    @Test
    public void testSSCAnnotatedBinaryDecoderExtendSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("BinaryEncodeDecodeTest", "testAnnotatedBinaryDecoderExtendSuccess");
    }

    //this tests encoder inheritance with generics
    @Mode(TestMode.FULL)
    @Test
    public void testEncodeGeneric() throws Exception {
        at.testEncodeGeneric();
    }

    // Tests coding with generics/parameterized type no inheritance
    // Jetty does not handle this test, but tyrus client does - so so do we....
    @Mode(TestMode.FULL)
    @Test
    public void testSSCParamTypeCoding() throws Exception {
        this.runAsSSCAndVerifyResponse("AnnotatedTest", "testParamTypeCoding");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTCKConfiguratorSuccess() throws Exception {

        ct.testTCKConfiguratorSuccess();
    }

    @Mode(TestMode.LITE)
    @Test
    public void TCKTestEncoderRuntimeException() throws Exception {
        oet.TCKTestEncoderRuntimeException();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testURIOverlappingWithHtml() throws Exception {
        String[] expectedInResponse = {
                                        "Simple Vanilla HTML to test overlapping URL mapping"
        };
        this.verifyResponse("/basic/URIOverlapping.html", expectedInResponse);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testURIOverlappingWithServlet() throws Exception {
        String[] expectedInResponse = {
                                        "Hello World"
        };
        this.verifyResponse("/basic/URIOverlapping", expectedInResponse);
    }

    //  Tests for new WsWsocServerContainer upgrade API

    @Mode(TestMode.LITE)
    @Test
    public void testServletUpgradeProgrammaticTextSuccess() throws Exception {
        pt.testProgrammaticTextSuccess("/basic/upgradeServlet/EndpointConfig/basic.war.CodedServerEndpointConfig$TextEndpointConfig");
    }

    //  Tests for new WsWsocServerContainer upgrade API
    @Mode(TestMode.FULL)
    @Test
    public void testServletUpgradePartialTextWithPing() throws Exception {
        pt.testProgrammaticPartialTextWithServerPing("/basic/upgradeServlet/EndpointClass/basic.war.ProgrammaticServerEP$PartialTextWithSendingEmbeddedPingEndpoint");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testServletUpgradePathParamOnOpenSuccess() throws Exception {
        ppt.TestOnOpenThroughUpgrade("/basic/pathUpgradeServlet/testString/1");

        ppt.TestOnOpenThroughUpgrade("/basic/pathUpgradeFilter/testString/1");
    }


    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SS;
    }

}