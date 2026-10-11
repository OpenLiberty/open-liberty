/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.fat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class BadFeatureFileTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.bad.kernel.feature");

    @BeforeClass
    public static void installSystemFeature() throws Exception {
        File installRoot = new File(server.getInstallRoot());
        File kernelCoreFeature = new File(installRoot, "lib/platform/kernelCore-1.0.mf");
        Path badKernelFeature = Files.copy(kernelCoreFeature.toPath(),
                                           new File(kernelCoreFeature.getParentFile(), "test.bad.kernel-1.0.mf").toPath(),
                                           StandardCopyOption.REPLACE_EXISTING);
        Files.write(badKernelFeature,
                    "BAD-HEADER-NO-COLON".getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);

        server.installSystemFeature("test.bad.feature-1.0");
        server.installSystemBundle("test.feature.api");
    }

    @Before
    public void startServer() throws Exception {
        server.startServer();
    }

    @Test
    @ExpectedFFDC("java.io.IOException")
    public void testBadKernel() throws Exception {
        server.waitForStringInLog("CWWKF0056E.*test.bad.kernel-1.0.mf", 100);
    }

    @Test
    @ExpectedFFDC("java.io.IOException")
    public void testBadFeature() throws Exception {
        server.waitForStringInLog("CWWKF0056E.*test.bad.feature-1.0.mf", 100);
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CWWKF0056E");
        }
    }

    @AfterClass
    public static void uninstallSystemFeature() throws Exception {
        server.deleteFileFromLibertyInstallRoot("lib/platform/test.bad.kernel-1.0.mf");
        server.uninstallSystemFeature("test.bad.feature-1.0");
        server.uninstallSystemBundle("test.feature.api");
    }
}