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
package com.ibm.ws.microprofile.reactive.streams.operators.tck;

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
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class ReactiveStreamsTCKLauncher {

    @Server("ReactiveStreamsTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Map<String, String> props = new HashMap<String, String>();
        //Set timeout for the tests.
        props.put("DEFAULT_TIMEOUT_MILLIS", "10000");//Increase timeout before tests fail.
        props.put("DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS", "100"); //By default NO_SIGNALS_TIMEOUT == DEFAULT_TIMEOUT_MILLIS. Every test will sleep for NO_SIGNALS_TIMEOUT so set this back to the original default to prevent the tests taking hours.
        server.setAdditionalSystemProperties(props);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchReactiveStreamsTck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.reactive.streams.operators_fat_tck", this.getClass() + ":launchReactiveStreamsTck");
        Map<String, String> resultInfo = new HashMap<>();
        try{
            JavaInfo javaInfo = JavaInfo.forCurrentVM();
            String productVersion = "";
            resultInfo.put("results_type", "MicroProfile");
            resultInfo.put("java_info", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") +')');
            resultInfo.put("java_major_version", String.valueOf(javaInfo.majorVersion()));
            resultInfo.put("feature_name", "Reactive Streams");
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

}
