/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedClientEP;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 *
 */
@RunWith(FATRunner.class)
public class MiscellaneousTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("miscellaneousTestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private static final Logger LOG = Logger.getLogger(MiscellaneousTest.class.getName());

    private static final String MISCELLANEOUS_WAR_NAME = "miscellaneous";

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the war app and add the dependencies
        WebArchive MiscellaneousApp = ShrinkHelper.buildDefaultApp(MISCELLANEOUS_WAR_NAME + ".war",
                                                                   "miscellaneous.war",
                                                                   "io.openliberty.wsoc.common",
                                                                   "io.openliberty.wsoc.util.wsoc",
                                                                   "io.openliberty.wsoc.tests.all",
                                                                   "io.openliberty.wsoc.endpoints.client.basic");
        MiscellaneousApp = (WebArchive) ShrinkHelper.addDirectory(MiscellaneousApp, "test-applications/" + MISCELLANEOUS_WAR_NAME + ".war/resources");
        // Verify if the apps are in the server before trying to deploy them
        if (SS.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SS.getLibertyServer().getInstalledAppNames(MISCELLANEOUS_WAR_NAME);
            LOG.info("addAppToServer : " + MISCELLANEOUS_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), MiscellaneousApp);
        }
        SS.startIfNotStarted();
        SS.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + MISCELLANEOUS_WAR_NAME);
        bwst.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        if (SS.getLibertyServer() != null && SS.getLibertyServer().isStarted()) {
            SS.getLibertyServer().stopServer(null);
        }
        bwst.tearDown();
    }

    @Test
    public void testNOCDIInjection1() throws Exception {

        String[] textValues = { "EJB Injection withoutCDI worked" };

        String uri = "/miscellaneous/miscNoCDIInjectedEndpoint";
        wt.runEchoTest(new AnnotatedClientEP.TextTest(textValues), uri, textValues);

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
