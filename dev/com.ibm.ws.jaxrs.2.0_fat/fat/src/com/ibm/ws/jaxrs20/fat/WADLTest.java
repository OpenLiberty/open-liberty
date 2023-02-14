/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat({"EE9_FEATURES", "EE10_FEATURES"}) // WADL is not supported in EE9
public class WADLTest {

    @Server("com.ibm.ws.jaxrs.fat.wadl")
    public static LibertyServer server;

    private static final String TEST_PACKAGE_DIR = new StringBuilder().append("com").append(File.separator).append("ibm").append(File.separator).append("ws").append(File.separator).append("jaxrs").append(File.separator).append("wadl").toString();
    private static final String TEST_PACKAGE = "com.ibm.ws.jaxrs.wadl";

    private static Machine machine;
    private static String installRoot;

    private RemoteFile WADL2JAVASrcDir;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "wadl", "com.ibm.ws.jaxrs.fat.wadl");
        installRoot = server.getInstallRoot();
        machine = server.getMachine();
    }

    @Before
    public void start() throws Exception {
        WADL2JAVASrcDir = server.getFileFromLibertyServerRoot("temp" + File.separator + "WADL2JAVASrc");
    }

    @After
    public void tearDown() throws Exception {
        if (null != server && server.isStarted()) {
            server.stopServer();
        }
    }

    @Mode(TestMode.LITE)
    @Test
    public void testWADL2JAVATool() throws Exception {
        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wadl");

        String TEST_WADL_LOCATION = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/wadl/1/Order?_wadl").toString();

        String wadl2javaPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java").toString();
        String wadl2javaBatPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java.bat").toString();

        RemoteFile wadl2java = server.getFileFromLibertyInstallRoot(wadl2javaPath);
        RemoteFile wadl2javaBat = server.getFileFromLibertyInstallRoot(wadl2javaBatPath);
        String[] wadl2javaArgs = new String[] { "-d", WADL2JAVASrcDir.getAbsolutePath(), "-p", TEST_PACKAGE, TEST_WADL_LOCATION };

        assertTrue("The file bin/jaxrs/wadl2java does not exist.", wadl2java.exists());
        assertTrue("The file bin/jaxrs/wadl2java.bat does not exist.", wadl2javaBat.exists());

        ProgramOutput po = machine.execute(wadl2java.getAbsolutePath(), wadl2javaArgs, installRoot);

        Log.info(WADLTest.class, "print Output Msg for testWADL2JAVATool", po.getStdout());

        RemoteFile orderResource = server.getFileFromLibertyServerRoot("temp" + File.separator + "WADL2JAVASrc" + File.separator
                                                                       + TEST_PACKAGE_DIR
                                                                       + File.separator + "OrderResource.java");

        assertTrue("OrderResource.java does not exist.", orderResource.exists());
    }

    @Test
    public void testWADL2JAVAToolNoWADL() throws Exception {
        String wadl2javaPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java").toString();
        String wadl2javaBatPath = new StringBuilder().append("bin").append(File.separator).append("jaxrs").append(File.separator).append("wadl2java.bat").toString();

        RemoteFile wadl2java = server.getFileFromLibertyInstallRoot(wadl2javaPath);
        RemoteFile wadl2javaBat = server.getFileFromLibertyInstallRoot(wadl2javaBatPath);
        String[] wadl2javaArgs = new String[] { "-p", TEST_PACKAGE };

        assertTrue("The file bin/wsimport does not exist.", wadl2java.exists());
        assertTrue("The file bin/wsimport.bat does not exist.", wadl2javaBat.exists());

        ProgramOutput po = machine.execute(wadl2java.getAbsolutePath(), wadl2javaArgs, installRoot);

        Log.info(WADLTest.class, "print Output Msg for testWADL2JAVAToolNoWADL", po.getStdout());

        assertTrue("The output should contain the error message 'Missing argument: wadl', but the actual is " + po.getStdout() + "\n" + "commandBuilder="
                   + po.getCommand() + "\nwadl2javaArgs=" + Arrays.toString(wadl2javaArgs),
                   po.getStdout().indexOf("Missing argument: wadl") >= 0);
    }
}
