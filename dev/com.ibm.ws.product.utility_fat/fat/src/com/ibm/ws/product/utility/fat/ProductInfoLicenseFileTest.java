/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This class tests that license information and license agreement gets generated properly
 */
public class ProductInfoLicenseFileTest {

    public static final Class<?> c = ProductInfoLicenseFileTest.class;
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.info.tool");
    public static String javaExc;
    public static String installRoot;
    public static final Collection<String> filesToTidy = new HashSet<String>();

    @Rule
    public final TestName method = new TestName();

    @BeforeClass
    public static void before() throws Exception {
        setupEnv(LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.info.tool"));
    }

    @AfterClass
    public static void after() throws Exception {
        server.deleteDirectoryFromLibertyInstallRoot("lafiles");
    }

    /**
     * Setup the environment.
     * 
     * @param svr The server instance.
     * 
     * @throws Exception
     */
    public static void setupEnv(LibertyServer svr) throws Exception {
        final String METHOD_NAME = "setup";
        server = svr;
        installRoot = server.getInstallRoot();
        javaExc = System.getProperty("java.home") + "/bin/java";
        Log.entering(c, METHOD_NAME);
        Log.info(c, METHOD_NAME, "java: " + javaExc);
        Log.info(c, METHOD_NAME, "installRoot: " + installRoot);

        // Create a directory to store the output files.
        File toolsOutputDir = new File(installRoot + "/tool.output.dir");
        toolsOutputDir.mkdir();
    }

    /**
     * Tests that the license information gets generated properly
     * 
     * @throws Exception
     */
    @Test
    public void testLicenseInformation() throws Exception {
        Log.entering(c, method.getMethodName());

        if (!verifyLicenseFilesExists()) {
            Log.info(c, method.getMethodName(), "The lafiles directory does not exist in installdir.  Skipping.");
            return;
        }

        testLicenseInfoContents(installRoot + "/bin/productInfo", new String[] { "viewLicenseInfo" }, installRoot);
        Log.exiting(c, method.getMethodName());
    }

    /**
     * Tests that the agreement information gets generated properly
     * 
     * @throws Exception
     */
    @Test
    public void testLicenseAgreement() throws Exception {
        Log.entering(c, method.getMethodName());

        if (!verifyLicenseFilesExists()) {
            Log.info(c, method.getMethodName(), "The lafiles directory does not exist in installdir.  Skipping.");
            return;
        }
        testLicenseAgreementContents(installRoot + "/bin/productInfo", new String[] { "viewLicenseAgreement" }, installRoot);
        Log.exiting(c, method.getMethodName());
    }

    /**
     * Tests the command productInfo produces license information correctly
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testLicenseInfoContents(String cmd, String[] parms, String workDir) throws Exception {
        Log.entering(c, method.getMethodName());

        if (!verifyLicenseFilesExists()) {
            Log.info(c, method.getMethodName(), "The lafiles directory does not exist in installdir.  Skipping.");
            return;
        }

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po);
        assertEquals("License information should be generated", po.getReturnCode(), 0);

        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        Log.info(ProductInfoLicenseFileTest.class, "localelanguage", lang);

        assertTrue("FAIL: License information doesnt exist:", new File(installRoot + "/lafiles/" + "LI_" + lang).exists());
        assertEquals("License locale is not en or not LI:", "LI_" + lang, new File(installRoot + "/lafiles/" + "LI_" + lang).getName());
        Log.exiting(c, method.getMethodName());
    }

    /**
     * Tests the command productInfo produces license agreement correctly
     * 
     * @param cmd The command to execute.
     * @param parms The parameters for the command.
     * @param workDir The working directory where the command is to be issued.
     * 
     * @throws Exception
     */
    public void testLicenseAgreementContents(String cmd, String[] parms, String workDir) throws Exception {
        Log.entering(c, method.getMethodName());

        if (!verifyLicenseFilesExists()) {
            Log.info(c, method.getMethodName(), "The lafiles directory does not exist in installdir.  Skipping.");
            return;
        }

        ProgramOutput po = server.getMachine().execute(cmd, parms, workDir);
        logInfo(po);
        assertEquals("License agreement should be generated", po.getReturnCode(), 0);

        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        Log.info(ProductInfoLicenseFileTest.class, "localelanguage", lang);

        assertTrue("FAIL: License agreement doesnt exist:", new File(installRoot + "/lafiles/" + "LA_" + lang).exists());
        assertEquals("License locale is not en or not LA:", "LA_" + lang, new File(installRoot + "/lafiles/" + "LA_" + lang).getName());
        Log.exiting(c, method.getMethodName());
    }

    /**
     * Prints an extended debug output.
     * 
     * @param po The programOutput
     * @param fileName
     * @throws Exception
     */
    public void logInfo(ProgramOutput po) throws Exception {
        String methodName = "logInfo";
        Log.info(c, methodName, "Return Code: " + po.getReturnCode() + ". STDOUT: " + po.getStdout());

        if (po.getReturnCode() != 0) {
            Log.info(c, methodName, "STDERR: " + po.getStderr());
        }
    }

    private boolean verifyLicenseFilesExists() {
        boolean licenseFileExists = false;
        File folder = new File(installRoot + "/lafiles/");

        if (folder.exists()) {
            for (File fileEntry : folder.listFiles()) {
                if (fileEntry.exists() && !(fileEntry.getName().startsWith("com.ibm"))) {
                    licenseFileExists = true;
                } else {
                    licenseFileExists = false;
                }
                Log.info(ProductInfoLicenseFileTest.class, method.getMethodName(), fileEntry.getName() + "status:" + licenseFileExists);
            }
        }
        return licenseFileExists;
    }
}
