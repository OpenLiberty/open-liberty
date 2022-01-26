/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat_tck;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
@Mode(TestMode.LITE)
public class Config30TCKLauncher {

//    @ClassRule
//    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE8_FEATURES());

    @Server("Config30TCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.setDebuggingAllowed(true);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchConfig30Tck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "io.openliberty.microprofile.config.3.0.internal_fat_tck", this.getClass() + ":launchConfig30Tck");
        Map<String, String> resultInfo = new HashMap<>();
        try{
            JavaInfo javaInfo = JavaInfo.forCurrentVM();
            String productVersion = "";
            resultInfo.put("results_type", "MicroProfile");
            resultInfo.put("java_info", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") +')');
            resultInfo.put("java_major_version", String.valueOf(javaInfo.majorVersion()));
            resultInfo.put("feature_name", "Config");
            resultInfo.put("feature_version", "3.0");
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
