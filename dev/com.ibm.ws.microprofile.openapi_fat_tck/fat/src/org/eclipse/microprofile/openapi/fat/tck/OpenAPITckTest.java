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
package org.eclipse.microprofile.openapi.fat.tck;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpression;

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
import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.log.Log;
/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class OpenAPITckTest {

    @Server("FATServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E", "CWWKO1651W"); 
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testOpenAPITck() throws Exception {
        if (!MvnUtils.init) {
            MvnUtils.init(server);
        }
        
        // inject the test.url parameter into the command
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getPort(PortType.WC_defaulthost));
        
        String [] customMvnCliTckRoot = MvnUtils.concatStringArray(MvnUtils.mvnCliTckRoot, new String[] { "-Dtest.url=" + protocol + "://" + host + ":" + port });

        // Everything under autoFVT/results is collected from the child build machine
        File mvnOutput = new File(MvnUtils.resultsDir, "mvnOutput_TCK");
        int rc = MvnUtils.runCmd(customMvnCliTckRoot, MvnUtils.tckRunnerDir, mvnOutput);
        
        File src = new File(MvnUtils.resultsDir, "../publish/tckRunner/tck/target/surefire-reports/junitreports");
        File tgt = new File(MvnUtils.resultsDir, "junit");

        try {
            Files.walkFileTree(src.toPath(), new MvnUtils.CopyFileVisitor(src.toPath(), tgt.toPath()));
        } catch (java.nio.file.NoSuchFileException nsfe) {
        	Log.warning(getClass(), "SRC " + src.getAbsolutePath());
        	Log.warning(getClass(), "TGT " + tgt.getAbsolutePath());
        	
            Assert.assertNull(
                    "The TCK tests' results directory does not exist which suggests the TCK tests did not run - check build logs."
                            + src.getAbsolutePath(), nsfe);
        }
        
        Log.warning(getClass(), "Mvn command finished with return code: " + Integer.toString(rc));
        
        ResultsUtil result = new ResultsUtil();
        result.xPathProcessor();
        // mvn returns 0 if all surefire tests pass and -1 otherwise - this Assert is enough to mark the build as having failed
        // the TCK regression

//        Assert.assertTrue(server.getInstallRoot() + "  com.ibm.ws.microprofile.openapi_fat_tck:org.eclipse.microprofile.openapi.fat.tck.OpenAPITckTest:testTck:TCK has returned non-zero return code of: " + rc
//                          +
//                          " This indicates test failure, see:" +
//                          " autoFVT/publish/tckRunner/tck/target/surefire-reports/index.html", rc == 0);
    }

}
