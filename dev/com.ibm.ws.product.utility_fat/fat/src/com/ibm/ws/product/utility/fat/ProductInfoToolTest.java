/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServerFactory;

public class ProductInfoToolTest extends ProductToolTestCommon {

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        setupEnv(LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.info.tool"));
        setupProductExtensions(SETUP_ALL_PROD_EXTS);

    }

    /**
     * Test that productInfo version will display information for all installed products.
     * This includes core, product extensions in default usr location and other installed product extensions.
     * 
     * @throws Exception
     */
    @Test
    public void testProdInfoToolPrintProductVersionForAllProductsInstalled() throws Exception {
        testPrintProductVersionForAllProductsInstalled(installRoot + "/bin/productInfo", new String[] { "version" }, installRoot);
    }

    /**
     * Test that productInfo featureInfo will display a list of all installed features.
     * This includes core, product extensions in default usr location and other installed product extensions.
     * 
     * @throws Exception
     */
    @Test
    public void testProdInfoToolPrintFeatureInfoForAllProductsInstalled() throws Exception {
        testPrintFeatureInfoForAllProductsInstalled(installRoot + "/bin/productInfo", new String[] { "featureInfo" }, installRoot);
    }

    /**
     * Test that productInfo version will display ifix information for all installed products.
     * This includes core, product extensions in default usr location and other installed product extensions.
     * 
     * @throws Exception
     */
    @Test
    public void testProdInfoToolPrintProductVersionIfixesForAllProductsInstalled() throws Exception {
        testPrintProductVersionIfixesForAllProductsInstalled(installRoot + "/bin/productInfo", new String[] { "version", "--ifixes" }, installRoot);
    }

    /**
     * 
     * Tests the command that lists the installed features for all products installed
     * does not have duplicates in the list.
     * 
     * @throws Exception
     */
    @Test
    public void testPrintFeatureInfoForAllProductsInstalledNoDuplicates() throws Exception {
        final String METHOD_NAME = "testPrintFeatureInfoForAllProductsInstalledNoDuplicates";
        Log.entering(c, METHOD_NAME);
        String cmd = installRoot + "/bin/productInfo";
        String[] parms = new String[] { "featureInfo" };

        ProgramOutput po = server.getMachine().execute(cmd, parms, installRoot);
        logInfo(po);
        String stdout = po.getStdout();
        assertTrue("The output should contain only one copy of the usr product feature: usertest-1.0 [1.0.0].",
                   stdout.indexOf("usertest [1.0.0]") == stdout.lastIndexOf("usertest [1.0.0]"));
        assertTrue("The output should contain only one copy of the product feature: prodtest-1.0 [1.0.0].",
                   stdout.indexOf("prodtest-1.0 [1.0.0]") == stdout.lastIndexOf("prodtest-1.0 [1.0.0]"));
        assertTrue("The output should contain only one copy of the core features: check servlet-3.0 [1.0.0].",
                   stdout.indexOf("servlet-3.0 [1.0.0]") == stdout.lastIndexOf("servlet-3.0 [1.0.0]"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    /**
     * This test validates that the productInfo script functions correctly when the CDPATH environment variable
     * is present.
     * 
     * @throws Exception
     */
    public void testProdInfoToolWithCDPATH() throws Exception {
        final String METHOD_NAME = "testProdInfoToolWithCDPATH";
        Log.entering(c, METHOD_NAME);

        // issuing the command from the Liberty install root while supplying the bin directory as
        // part of the command itself causes the productInfo script to cd to the bin directory, which
        // is where we noticed problems when CDPATH is set
        String executionDir = server.getInstallRoot();
        String command = "bin" + File.separator + "productInfo";

        String[] parms = new String[1];
        parms[0] = "version";

        Properties envVars = new Properties();
        envVars.put("CDPATH", ".");

        ProgramOutput po = server.getMachine().execute(command, parms, executionDir, envVars);
        Log.info(c, METHOD_NAME, "stdout = " + po.getStdout());
        Log.info(c, METHOD_NAME, "stderr = " + po.getStderr());

        assertTrue("productInfo does not contain the correct output", po.getStdout().contains("Product name"));

        Log.exiting(c, METHOD_NAME);
    }
}