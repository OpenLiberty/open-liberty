/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

public class BatchAppUtils {
    
    //////////
    // Methods to build application archives at runtime

    static JavaArchive testUtilJar = null;

    static JavaArchive getTestUtilJar() throws Exception {
        if (testUtilJar == null) {
            testUtilJar = ShrinkHelper.buildJavaArchiveNoResources("testutil.jar",
                                               "batch.fat.util");
        }
        return testUtilJar;
    }

    static JavaArchive commonUtilJar = null;

    static JavaArchive getCommonUtilJar() throws Exception {
        if (commonUtilJar == null) {
            commonUtilJar = ShrinkHelper.buildJavaArchiveNoResources("commonUtil.jar",
                                                 "batch.fat.common.util", 
                                                 "com.ibm.ws.jbatch.test",
                                                 "com.ibm.ws.jbatch.test.dbservlet");
        }
        return commonUtilJar;
    }

    static JavaArchive clientJar = null;

    static JavaArchive getClientJar() throws Exception {
        if (clientJar == null) {
            clientJar = ShrinkHelper.buildJavaArchiveNoResources("com.ibm.ws.jbatch.test.dbservlet.client.jar",
                                             "com.ibm.ws.jbatch.test",
                                             "com.ibm.ws.jbatch.test.dbservlet");
        }
        return clientJar;
    }

    static WebArchive dbServletApp = null;

    static WebArchive getDbServletAppWar() {
        if (dbServletApp == null) {
            dbServletApp = buildBatchWar("DbServletApp.war",
                                   true,
                                   "(.*)(DbServlet|ServerKillerServlet|StringUtils)(.*)",  // include regex
                                   "batch.fat.web");
        }
        return dbServletApp;
    }

    static WebArchive batchSecurityWar = null;

    static WebArchive getBatchSecurityWar() {
        if (batchSecurityWar == null) {
            batchSecurityWar = buildBatchWar("batchSecurity.war",
                                       false, 
                                       null, 
                                       "batch.fat.artifacts", "batch.security");
        }
        return batchSecurityWar;
    }

    static WebArchive batchFATWar = null;

    static WebArchive getBatchFATWar() throws Exception {
        if (batchFATWar == null) {
            batchFATWar = buildBatchWar("batchFAT.war",
                                  false,
                                  null, 
                                  "batch.fat.artifacts", "batch.fat.cdi", "batch.fat.common", "batch.fat.web", "batch.fat.web.customlogic",
                                  "chunktests.artifacts",
                                  "processitem.artifacts");
            batchFATWar.addAsLibrary(getTestUtilJar());
            batchFATWar.addAsLibrary(getCommonUtilJar());
        }
        return batchFATWar;
    }

    static WebArchive bonusPayoutWar = null;

    static WebArchive getBonusPayoutWar() throws Exception {
        if (bonusPayoutWar == null) {
            bonusPayoutWar = buildBatchWar("BonusPayout.war", 
                                     true,
                                     null,
                                     "com.ibm.websphere.samples.batch.artifacts",
                                     "com.ibm.websphere.samples.batch.beans",
                                     "com.ibm.websphere.samples.batch.fat",
                                     "com.ibm.websphere.samples.batch.util");
            bonusPayoutWar.addAsLibrary(getCommonUtilJar());
        }
        return bonusPayoutWar;
    }

    static EnterpriseArchive bonusPayoutEAREar = null;

    static EnterpriseArchive getBonusPayoutEAREar() throws Exception {
        if (bonusPayoutEAREar == null) {
            final String appName = "BonusPayoutEAR.ear";
            bonusPayoutEAREar = ShrinkWrap.create(EnterpriseArchive.class, appName);
            File appXml = new File("test-applications/fat.common/" + appName + "/resources/META-INF/application.xml");
            bonusPayoutEAREar.setApplicationXML(appXml);
            bonusPayoutEAREar.addAsModule(getBonusPayoutWar());
        }
        return bonusPayoutEAREar;
    }

    /**
     * Build a Batch Web Application Archive.
     * @param appName The name of a web application archive file, including the .war extension
     * @param regex A regex pattern to precisely match paths names in packages to include in the web application.
     * @param packageNames A comma-delimited list of package paths to include in the web application.  
     *  All path names in each packages will be included unless further filtered by the regex pattern.
     */
    static WebArchive buildBatchWar(String appName,
                                     boolean isCommonFat, 
                                     String regex,
                                     String... packageNames) {
        
        String srcRoot = isCommonFat ? "test-applications/fat.common/" : "test-applications/";

        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName);
        final boolean INCLUDE_SUBPKGS = false;  // Exclude subpackages
        if (regex == null) {
            webApp.addPackages(INCLUDE_SUBPKGS, packageNames); // Include all pkg paths
        } else {
            webApp.addPackages(INCLUDE_SUBPKGS, Filters.include(regex), packageNames);  // Include only pkg paths matching regex 
        }
        // Web-inf resources
        File webInf = new File(srcRoot + appName + "/resources/WEB-INF");
        if (webInf.exists()) {
            for (File webInfElement : webInf.listFiles()) {
                if (!!!webInfElement.isDirectory()) { // Ignore classes subdir
                    webApp.addAsWebInfResource(webInfElement);
                }
            }
        }
        // Batch job definition files
        File webInfBatchJobs = new File(srcRoot + appName + "/resources/WEB-INF/classes/META-INF/batch-jobs");
        if (webInfBatchJobs.exists()) {
            for (File batchJob : webInfBatchJobs.listFiles()) {
                String target = "classes/META-INF/batch-jobs/" + batchJob.getName();
                webApp.addAsWebInfResource(batchJob, target);
            }
        }
        // Package properties
        File pkgProps = new File(srcRoot + appName + "/package.properties");
        if (pkgProps.exists()) {
            webApp.addAsWebResource(pkgProps);
        }
        // Readme
        File readme = new File(srcRoot + appName + "/README.txt");
        if (readme.exists()) {
            webApp.addAsWebResource(readme);
        }

        return webApp;
    }

    //////////
    // Methods to deploy applications

    final static String PrebuiltAppArtifactPath = "build/lib/test-application/";  // Relative to autoFVT dir

    public static final String USE_PREBUILT = "USE_PREBUILT";

    static boolean usePrebuilt(String[] options) {
        return Arrays.asList(options).contains(USE_PREBUILT);
    }

    static void addDropinPrebuilt(LibertyServer targetServer, String appName) throws Exception{
        targetServer.copyFileToLibertyServerRoot(PrebuiltAppArtifactPath, "dropins", appName);
        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(Paths.get(targetServer.getServerRoot(), "dropins", appName));
        }
    }

    public static void addDropinsDbServletAppWar(LibertyServer targetServer, String... options) throws Exception {
        if (usePrebuilt(options)) {
            addDropinPrebuilt(targetServer, "DbServletApp.war");
        } else {
            ShrinkHelper.exportToServer(targetServer, "dropins", getDbServletAppWar());
        }
    }

    public static void addDropinsBonusPayoutWar(LibertyServer targetServer, String... options) throws Exception {
        if (usePrebuilt(options)) {
            addDropinPrebuilt(targetServer, "bonusPayout.war");
        } else {
            ShrinkHelper.exportToServer(targetServer, "dropins", getBonusPayoutWar());
        }
    }

    public static void addDropinsBonusPayoutEAREar(LibertyServer targetServer, String... options) throws Exception {
        if (usePrebuilt(options)) {
            addDropinPrebuilt(targetServer, "bonusPayoutEAR.ear");
        } else {
            ShrinkHelper.exportToServer(targetServer, "dropins", getBonusPayoutEAREar());
        }
    }

    public static void addDropinsBatchSecurityWar(LibertyServer targetServer) throws Exception {
        ShrinkHelper.exportToServer(targetServer, "dropins", getBatchSecurityWar());
    }

    public static void addDropinsBatchFATWar(LibertyServer targetServer) throws Exception {
        ShrinkHelper.exportToServer(targetServer, "dropins", getBatchFATWar());
    }

}