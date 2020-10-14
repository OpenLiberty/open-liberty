/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.checkFeature;

import static com.ibm.ws.jaxrs20.fat.checkFeature.CheckFeatureUtil.checkFeature;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test was created under defect 135992, in which a bug in the jaxrs feature manifest meant that
 * the server had to be restarted if the cdi-1.0 feature was added after the server had started.
 */
@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class CheckFeature12Test {

    @Server("com.ibm.ws.jaxrs.fat.checkFeature")
    public static LibertyServer server;

    private static final String appname = "checkFeature";

    @BeforeClass
    public static void setupClass() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs.fat.checkFeature.api",
                                      "com.ibm.ws.jaxrs.fat.checkFeature.api.impl",
                                      "com.ibm.ws.jaxrs.fat.checkFeature.web");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
            assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));
            assertNotNull("The Security Service should be ready", server.waitForStringInLog("CWWKS0008I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void checkFeature12() throws Exception {
        checkFeature(server, "server.with.cdi12.xml");
    }
}
