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

package com.ibm.ws.security.client.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ProgrammaticJaasLoginConfigFileTest extends CommonTest {
    private static final Class<?> c = ProgrammaticJaasLoginConfigFileTest.class;

    @BeforeClass
    public static void theBeforeClass() throws Exception {
        String thisMethod = "before";
        Log.info(c, thisMethod, "Performing server setup");
        try {
            commonServerSetUp("BasicAuthTest", true);
        } catch (Exception e) {
            Log.info(c, thisMethod, "Server setup failed, tests will not run: " + e.getMessage());
            throw (new Exception("Server setup failed, tests will not run: " + e.getMessage(), e));
        }

        Log.info(c, thisMethod, "Server setup is complete");
    };

    @AfterClass
    public static void theAfterClass() {
        try {
            Log.info(c, "after", "Stopping server process");
            testServer.stopServer("CWWKZ0124E: Application testmarker does not contain any modules.");
        } catch (Exception e) {
            Log.info(c, "after", "Exception thrown in after " + e.getMessage());
            e.printStackTrace();
        }
    };

    /**
     * Test description:
     * - Perform a programmatic login with a custom login module using a wsjaas_client.conf file.
     * 
     * Expected results:
     * - Login should be successful and a subject for the specified user should be returned.
     * - Attributes specific to the custom login module should be present in the subject.
     * 
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaasProgrammaticLoginConfigFile_CustomLoginModule() {
        try {
            Log.info(c, name.getMethodName(), "Performing programmatic login with a custom login module");
            commonClientSetUpforConfFile("ProgrammaticJaasLoginConfigFileTestClient", "wsjaas_client.conf.orig");
            String user = Constants.USER_1;
            List<String> args = new ArrayList<String>();
            args.add(user);
            args.add(Constants.USER_1_PWD);
            args.add(Constants.CLIENT_CONTAINER_JAAS_LOGIN_CONTEXT);
            ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticJaasLoginConfigFileTestClient", "client_customJaasLoginModule.xml", args, "CWWKS9702W");
            String output = programOutput.getStdout();

            assertTrue("Client output did not contain the message expected to be output by the custom login module.",
                       output.contains(Constants.CUSTOM_LOGIN_MODULE_MESSAGE));

            assertTrue("Client output did not contain the expected WSTestPrincipal name " + user,
                       output.contains("WSTestPrincipal:" + user));

            assertNoErrMessages(output);

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Perform a programmatic login with a custom login module, checking to see that we use client.xml and not wsjaas_client.conf
     * 
     * Expected results:
     * - Login should be successful and a subject for the specified user should be returned.
     * - Attributes specific to the custom login module should be present in the subject.
     * - the WSTestPrincipal should not be present if using client.xml
     */
    @Test
    public void testClientXMLhasSameCustomLoginEntryAsJaasConfigFile() {
        try {
            Log.info(c, name.getMethodName(), "Performing programmatic login with a custom login module");
            commonClientSetUpforConfFile("ProgrammaticJaasLoginConfigFileTestClient", "wsjaas_client.conf.orig");
            String user = Constants.USER_1;
            List<String> args = new ArrayList<String>();
            args.add(user);
            args.add(Constants.USER_1_PWD);
            args.add(Constants.CLIENT_CONTAINER_JAAS_LOGIN_CONTEXT);
            ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticJaasLoginConfigFileTestClient", "client_customLoginModule.xml", args, "CWWKS1142W", "CWWKS9702W");
            String output = programOutput.getStdout();

            assertTrue("Client output did not contain the message expected to be output by the custom login module.",
                       output.contains(Constants.CUSTOM_LOGIN_MODULE_MESSAGE));

            assertFalse("Client output should not contain the WSTestPrincipal name " + user,
                        output.contains("WSTestPrincipal:" + user));

            assertNoErrMessages(output);

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Perform a programmatic login with a custom login module with conf file missing delegate for proxy login
     * 
     * Expected results:
     * - Login should fail with error indicating missing delegate.
     * 
     */
    @Test
    public void testJaasConfigFileWithMissingDelegate() {
        try {
            Log.info(c, name.getMethodName(), "Performing programmatic login with a custom login module");
            commonClientSetUpforConfFile("ProgrammaticJaasLoginConfigFileTestClient", "wsjaas_client.conf.nodelegate");
            String user = Constants.USER_1;
            List<String> args = new ArrayList<String>();
            args.add(user);
            args.add(Constants.USER_1_PWD);
            args.add(Constants.CLIENT_CONTAINER_JAAS_LOGIN_CONTEXT);
            ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticJaasLoginConfigFileTestClient", "client_customJaasLoginModule.xml", args, "CWWKS9702W",
                                                                     "CWWKS1108E");
            String output = programOutput.getStdout();

            assertTrue("Client output did not contain the expected failure message of no delegate.",
                       output.contains("CWWKS1108E: WSLoginModuleProxy delegate option is not set."));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Perform a programmatic login with a custom login module with conf file missing classmodule definition
     * 
     * Expected results:
     * - Login should fail with error indicating login modules ignored
     * 
     */
    @Test
    public void testJaasConfigFileWithMissingClassModules() {
        try {
            Log.info(c, name.getMethodName(), "Performing programmatic login with a custom login module");
            commonClientSetUpforConfFile("ProgrammaticJaasLoginConfigFileTestClient", "wsjaas_client.conf.noclasses");

            String user = Constants.USER_1;
            List<String> args = new ArrayList<String>();
            args.add(user);
            args.add(Constants.USER_1_PWD);
            args.add(Constants.CLIENT_CONTAINER_JAAS_LOGIN_CONTEXT);
            ProgramOutput programOutput = commonClientSetUpWithParms("ProgrammaticJaasLoginConfigFileTestClient", "client_customJaasLoginModule.xml", args, "CWWKS9702W");
            String output = programOutput.getStdout();

            assertTrue("Client output did not contain the expected failure message.",
                       output.contains("Failed to log in: Login Failure: all modules ignored"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

}
