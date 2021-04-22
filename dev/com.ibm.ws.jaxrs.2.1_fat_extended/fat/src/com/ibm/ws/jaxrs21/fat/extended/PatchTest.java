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
package com.ibm.ws.jaxrs21.fat.extended;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jaxrs21.fat.patch.PatchTestServlet;

@SkipForRepeat(JakartaEE9Action.ID) // requires SSL client enablement
@RunWith(FATRunner.class)
public class PatchTest extends FATServletClient {

    private static final Class<?> c = PatchTest.class;
    private static final String appName = "patchapp";

    @Server("jaxrs21.fat.patch")
    @TestServlet(servlet = PatchTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaInfo java = JavaInfo.forCurrentVM();
        int javaVersion = java.majorVersion();
        if (javaVersion > 8) {
            Log.info(c, "setup()", "Detected Java version (" + javaVersion + ") is 9 or higher, adding additional jvm.options file to compensate for strong encapsulation");
            server.copyFileToLibertyServerRoot("patch_java9_jvm/jvm.options");
        }
        ShrinkHelper.defaultDropinApp(server, appName, "jaxrs21.fat.patch");
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}