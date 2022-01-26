/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.lra.tck;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs the whole LRA TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 * In normal (lite) mode, just one test from the TCK is run
 * To run the full TCK, the suite must be run in FULL mode
 * gradlew -Dfat.test.mode=FULL io.openliberty.microprofile.lra.1.0.internal_fat_tck:buildandrun
 */
@RunWith(FATRunner.class)
public class LraTckLauncher {

    private static final String SERVER_NAME = "LRATCKServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // microprofile config will allow this to be accessed by the application as
        // lra.tck.base.url, which is what the tck is looking for. LibertyServer won't allow
        // '.' to be used in an env var name, as it "isn't cross platform".
        String key = "lra_tck_base_url";
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultPort());
        String value = protocol + "://" + host + ":" + port;

        server.addEnvVar(key, value);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // server.stopServer("CWMFT5001E"); // Any expected message IDs in the logs could be added here.
        server.stopServer();
    }

    /**
     * Run one test from the TCK
     *
     * @throws Exception
     */
    @Test
    public void launchLRATCK() throws Exception {

        // This makes the property lra.tck.base.url available to maven, so that it can pass it on to the
        // arquillian launcher. Not entirely sure if it is needed or not.
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultPort());
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("lra.tck.base.url", protocol + "://" + host + ":" + port);
        additionalProps.put("lraTestsToRun", "**/TckTests.java");
        // This is the currently passing test methods from TckTests
        additionalProps.put("test", "TckTests#*LRA*+join*");

        MvnUtils.runTCKMvnCmd(server, "io.openliberty.microprofile.lra.1.0.internal_fat_tck", this.getClass() + ":launchLRATCK", additionalProps);
        Map<String, String> resultInfo = new HashMap<>();
        try{
            JavaInfo javaInfo = JavaInfo.forCurrentVM();
            String productVersion = "";
            resultInfo.put("results_type", "MicroProfile");
            resultInfo.put("java_info", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") +')');
            resultInfo.put("java_major_version", String.valueOf(javaInfo.majorVersion()));
            resultInfo.put("feature_name", "LRA");
            resultInfo.put("feature_version", "1.0");
            resultInfo.put("os_name",System.getProperty("os.name"));
            List<String> matches = server.findStringsInLogs("product =");
            if(!matches.isEmpty()){
                Pattern olVersionPattern = Pattern.compile("Liberty (.*?) \\(", Pattern.DOTALL);
                Matcher nameMatcher =olVersionPattern.matcher(matches.get(0));
                if (nameMatcher.find()) {
                    productVersion = nameMatcher.group(1);
                }
                resultInfo.put("product_version", productVersion);
            }
        }finally{
            MvnUtils.preparePublicationFile(resultInfo);
        };;

    }

    /**
     * Run the whole TCK
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    @Mode(TestMode.EXPERIMENTAL)
    public void launchLRATCKFull() throws Exception {

        // This makes the property lra.tck.base.url available to maven, so that it can pass it on to the
        // arquillian launcher. Not entirely sure if it is needed or not.
        String protocol = "http";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultPort());
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("lra.tck.base.url", protocol + "://" + host + ":" + port);
        additionalProps.put("lraTestsToRun", "**/*Test*.java");

        MvnUtils.runTCKMvnCmd(server, "io.openliberty.microprofile.lra.1.0.internal_fat_tck", this.getClass() + ":launchLRATCK", additionalProps);
        Map<String, String> resultInfo = new HashMap<>();
        try{
            JavaInfo javaInfo = JavaInfo.forCurrentVM();
            String productVersion = "";
            resultInfo.put("results_type", "MicroProfile");
            resultInfo.put("java_info", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") +')');
            resultInfo.put("java_major_version", String.valueOf(javaInfo.majorVersion()));
            resultInfo.put("feature_name", "LRA");
            resultInfo.put("feature_version", "1.0");
            resultInfo.put("os_name",System.getProperty("os.name"));
            List<String> matches = server.findStringsInLogs("product =");
            if(!matches.isEmpty()){
                Pattern olVersionPattern = Pattern.compile("Liberty (.*?) \\(", Pattern.DOTALL);
                Matcher nameMatcher =olVersionPattern.matcher(matches.get(0));
                if (nameMatcher.find()) {
                    productVersion = nameMatcher.group(1);
                }
                resultInfo.put("product_version", productVersion);
            }
        }finally{
            MvnUtils.preparePublicationFile(resultInfo);
        };

    }
}
