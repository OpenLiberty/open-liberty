/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.microprofile.config.tck;

import java.io.File;
import java.nio.file.Files;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ConfigTckPackageTest {

    @Server("FATServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testConfigTck() throws Exception {
        if (!MvnUtils.init) {
            MvnUtils.init(server);
        }
        // Everything under autoFVT/results is collected from the child build machine
        File mvnOutput = new File(MvnUtils.home, "results/mvnOutput_TCK");
        int rc = MvnUtils.runCmd(MvnUtils.mvnCliTckRoot, MvnUtils.tckRunnerDir, mvnOutput);
        File src = new File(MvnUtils.home, "results/tck/surefire-reports/junitreports");
        File tgt = new File(MvnUtils.home, "results/junit");
        try {
            Files.walkFileTree(src.toPath(), new MvnUtils.CopyFileVisitor(src.toPath(), tgt.toPath()));
        } catch (java.nio.file.NoSuchFileException nsfe) {
            Assert.assertNull(
                    "The TCK tests' results directory does not exist which suggests the TCK tests did not run - check build logs."
                            + src.getAbsolutePath(), nsfe);
        }
        
        // mvn returns 0 if all surefire tests pass and -1 otherwise - this Assert is enough to mark the build as having failed
        // the TCK regression

        Assert.assertTrue("com.ibm.ws.microprofile.config_fat_tck:org.eclipse.microprofile.config.tck.ConfigTckPackageTest:testTck:TCK has returned non-zero return code of: " + rc
                          +
                          " This indicates test failure, see: ...autoFVT/results/mvn* " +
                          "and ...autoFVT/results/tck/surefire-reports/index.html", rc == 0);
    }

}
