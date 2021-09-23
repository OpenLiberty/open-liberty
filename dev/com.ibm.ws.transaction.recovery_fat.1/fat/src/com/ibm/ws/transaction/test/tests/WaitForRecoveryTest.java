/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.web.WaitForRecoveryServlet;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@Mode
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class WaitForRecoveryTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/WaitForRecoveryServlet";

    @Server("com.ibm.ws.transaction_waitForRecovery")
    @TestServlet(servlet = WaitForRecoveryServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");

        server1.setServerStartTimeout(TestUtils.LOG_SEARCH_TIMEOUT);
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        TestUtils.stopServer(server1);
    }

    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testWaitForRecovery() throws Exception {
        final String method = "testBaseRecovery";
        StringBuilder sb = null;

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(server1, SERVLET_NAME, "testRec001");
        } catch (Throwable e) {
        }
        Log.info(this.getClass(), method, "testRec001 returned: " + sb);

        server1.waitForStringInLog("Dump State:");

        // Now re-start cloud1
        ProgramOutput po = server1.startServerAndValidate(false, true, true);
        if (po.getReturnCode() != 0) {
            Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
            Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
            Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
            Exception ex = new Exception("Could not restart the server");
            Log.error(this.getClass(), "WaitForRecoveryTest", ex);
            throw ex;
        }

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        server1.waitForStringInTrace("Performed recovery for server");

        // Lastly stop server1
        server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected

        // Lastly, clean up XA resource file
        server1.deleteFileFromLibertyServerRoot("XAResourceData.dat");
    }

}
