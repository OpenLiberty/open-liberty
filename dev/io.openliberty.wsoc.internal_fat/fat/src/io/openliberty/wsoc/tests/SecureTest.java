/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.util.Set;
import java.util.logging.Logger;

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

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.wsoc.util.DontRunWithWebServerRule;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.util.WebServerSetup;

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
@RunWith(FATRunner.class)
public class SecureTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("secureTestServer", true);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @ClassRule
    public static final TestRule WebServerRule = new DontRunWithWebServerRule();

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private static final Logger LOG = Logger.getLogger(SecureTest.class.getName());

    private static final String SECURE_WAR_NAME = "secure";

    protected WebResponse runAsSSCAndVerifyResponse(String className, String testName) throws Exception {
        int securePort = 0, port = 0;
        String host = "";
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
                                 "/secure/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
                                                                + "&secureport=" + securePort + "&secure=true",
                                 "SuccessfulTest");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the war app and add the dependencies
        WebArchive SecureApp = ShrinkHelper.buildDefaultApp(SECURE_WAR_NAME + ".war",
                                                            "secure.war",
                                                            "io.openliberty.wsoc.common",
                                                            "io.openliberty.wsoc.util.wsoc",
                                                            "io.openliberty.wsoc.tests.all",
                                                            "io.openliberty.wsoc.endpoints.client.secure");
        SecureApp = (WebArchive) ShrinkHelper.addDirectory(SecureApp, "test-applications/" + SECURE_WAR_NAME + ".war/resources");
        // Verify if the apps are in the server before trying to deploy them
        if (SS.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SS.getLibertyServer().getInstalledAppNames(SECURE_WAR_NAME);
            LOG.info("addAppToServer : " + SECURE_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), SecureApp);
        }

        //Replace config for the other server
        LibertyServer wlp = SS.getLibertyServer();
        wlp.saveServerConfiguration();
        wlp.setServerConfigurationFile("Secure/server.xml");

        SS.startIfNotStarted();
        wlp.waitForStringInLog("CWWKZ0001I.* " + SECURE_WAR_NAME);
        bwst.setUp();
        // tests cannot work until ssl is up
        wlp.waitForStringInLog("CWWKO0219I:.*ssl.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        // test cleanup
        SS.getLibertyServer().setMarkToEndOfLog();
        SS.getLibertyServer().restoreServerConfiguration();
        SS.getLibertyServer().waitForConfigUpdateInLogUsingMark(null);
        if (SS.getLibertyServer() != null && SS.getLibertyServer().isStarted()) {
            SS.getLibertyServer().stopServer("CWWKH0039E");
        }
        bwst.tearDown();
    }

    @Test
    public void testSSCAnnotatedSecureTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("SecurityTest", "testAnnotatedSecureSuccess");
    }

    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCWSSRequired() throws Exception {
        this.runAsSSCAndVerifyResponse("SecurityTest", "testWSSRequired");
    }

    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCAnnotatedSecureForbidden() throws Exception {
        this.runAsSSCAndVerifyResponse("SecurityTest", "testAnnotatedSecureForbidden");
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