/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics11.tck.launcher;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class MetricsTCKLauncherNoAuth {
    private static Class<?> c = MetricsTCKLauncherNoAuth.class;

    @Server("MetricsTCKServerNoAuth")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Ignore CWWKZ0131W - In windows, some jars are being locked during the test. Issue #2768
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E", "CWWKZ0131W");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchTck() throws Exception {
        final String method = "launchTck";
        if (!MvnUtils.init) {
            MvnUtils.init(server);
        }

        // inject the test.url parameter into the command
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultPort());

        String[] customMvnCliTckRoot = MvnUtils.concatStringArray(MvnUtils.mvnCliTckRoot,
                                                                  new String[] {
                                                                                 "-Dtest.url=" + protocol + "://" + host + ":" + port
                                                                  });
        Log.info(c, method, "customMvnCliTckRoot=" + Arrays.toString(customMvnCliTckRoot));
        // Everything under autoFVT/results is collected from the child build machine
        File mvnOutput = new File(MvnUtils.resultsDir, MvnUtils.mvnOutputFilename);
        Log.info(c, method, "tckRunnerDir=" + MvnUtils.tckRunnerDir);
        Log.info(c, method, "mvnOutput=" + mvnOutput);
        int rc = MvnUtils.runCmd(customMvnCliTckRoot, MvnUtils.tckRunnerDir, mvnOutput);
        File src = new File(MvnUtils.resultsDir, "tck/surefire-reports");
        File tgt = new File(MvnUtils.resultsDir, "junit");

        try {
            Files.walkFileTree(src.toPath(), new MvnUtils.CopyFileVisitor(src.toPath(), tgt.toPath()));
        } catch (java.nio.file.NoSuchFileException nsfe) {
            Assert.assertNull(
                              "The TCK tests' results directory does not exist which suggests the TCK tests did not run - check build logs."
                              + src.getAbsolutePath(), nsfe);
        }

        Log.warning(getClass(), "Mvn command finished with return code: " + Integer.toString(rc));
        // mvn returns 0 if all surefire tests pass and -1 otherwise - this Assert is enough to mark the build as having failed
        // the TCK regression
        Assert.assertEquals("com.ibm.ws.microprofile.metrics11 TCK has returned non-zero return code of: " + rc +
                            " This indicates test failure, see: ...autoFVT/results/" + MvnUtils.mvnOutputFilename +
                            " and ...autoFVT/results/tck/surefire-reports/index.html", 0, rc);
    }

}
