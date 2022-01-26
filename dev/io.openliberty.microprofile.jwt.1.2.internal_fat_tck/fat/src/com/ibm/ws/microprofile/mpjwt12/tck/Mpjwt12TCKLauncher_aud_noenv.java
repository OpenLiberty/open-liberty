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
package com.ibm.ws.microprofile.mpjwt12.tck;

import java.util.Collections;
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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 *
 */
//@Mode(TestMode.QUARANTINE)
@RunWith(FATRunner.class)
public class Mpjwt12TCKLauncher_aud_noenv {

    @Server("jwt12tckAudNoenv")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
        server.waitForStringInLog("CWWKS4105I", 30000); // wait for ltpa keys to be created and service ready, which can happen after startup.
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // CWWKZ0014W  - we need app listed in server.xml even when it might not there, so allow this "missing app" error.
        // CWWKE0921W, 12w - the harness generates a java2sec socketpermission error, there's no way to suppress it  by itself in server.xml, so suppress this way
        // CWWKG0014E - intermittently caused by server.xml being momentarily missing during server reconfig
        server.stopServer("CWWKG0014E", "CWWKS5524E", "CWWKS6023E", "CWWKS5523E", "CWWKS5522E", "CWWKS6031E", "CWWKS5524E", "CWWKZ0014W", "CWWKS5604E", "CWWKE0921W", "CWWKE0912W");
    }

    @Test
    @AllowedFFDC("org.jose4j.jwt.consumer.InvalidJwtSignatureException")
    public void launchMpjwt12TCKLauncher_aud_noenv() throws Exception {
        String port = String.valueOf(server.getBvtPort());
        String bucketAndTestName = this.getClass().getCanonicalName();
        Map<String, String> additionalProps = new HashMap<>();
        // need to pass the correct url for PublicKeyAsPEMLocationURLTest
        additionalProps.put("mp.jwt.tck.jwks.baseURL", "http://localhost:" + port + "/PublicKeyAsPEMLocationURLTest/");
        MvnUtils.runTCKMvnCmd(server, bucketAndTestName, bucketAndTestName, "tck_suite_aud_noenv.xml", additionalProps, Collections.emptySet());
        Map<String, String> resultInfo = new HashMap<>();
        try{
            JavaInfo javaInfo = JavaInfo.forCurrentVM();
            String productVersion = "";
            resultInfo.put("results_type", "MicroProfile");
            resultInfo.put("java_info", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") +')');
            resultInfo.put("java_major_version", String.valueOf(javaInfo.majorVersion()));
            resultInfo.put("feature_name", "JWT Auth");
            resultInfo.put("feature_version", "1.2");
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
