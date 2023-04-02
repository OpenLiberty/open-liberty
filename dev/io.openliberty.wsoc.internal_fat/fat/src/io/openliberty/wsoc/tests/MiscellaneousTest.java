/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedClientEP;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 *
 */
@RunWith(FATRunner.class)
public class MiscellaneousTest {
    public static final String SERVER_NAME = "miscellaneousTestServer";
    @Server(SERVER_NAME)

    public static LibertyServer LS;

    private static WebServerSetup bwst = null;

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private static WsocTest wt = null;

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
        ShrinkHelper.exportDropinAppToServer(LS, MiscellaneousApp);

        LS.startServer();
        LS.waitForStringInLog("CWWKZ0001I.* " + MISCELLANEOUS_WAR_NAME);

        bwst = new WebServerSetup(LS);
        bwst.setUp();
        wt = new WsocTest(LS, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        if (LS != null && LS.isStarted()) {
            LS.stopServer();
        }
        bwst.tearDown();
    }

    @Test
    public void testNOCDIInjection1() throws Exception {

        String[] textValues = { "EJB Injection withoutCDI worked" };

        String uri = "/miscellaneous/miscNoCDIInjectedEndpoint";
        wt.runEchoTest(new AnnotatedClientEP.TextTest(textValues), uri, textValues);

    }

}
