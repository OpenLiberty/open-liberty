/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat.tck;

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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.utils.MvnUtils;

@RunWith(FATRunner.class)
public class MPConcurrencyTCKLauncher {

    @Server("MPConcurrencyTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @AllowedFFDC({ "java.lang.IllegalStateException", // transaction cannot be propagated to 2 threads at the same time
                   "java.lang.NegativeArraySizeException", // intentionally raised by test case to simulate failure during completion stage action
                   "org.jboss.weld.contexts.ContextNotActiveException" // expected when testing TransactionScoped bean cannot be accessed outside of transaction
    })
    @Test
    public void launchMPConcurrency10Tck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.concurrency.mp_fat_tck", this.getClass() + ":launchMPConcurrency10Tck");
        Map<String, String> resultInfo = new HashMap<>();
        try{
            JavaInfo javaInfo = JavaInfo.forCurrentVM();
            String productVersion = "";
            resultInfo.put("results_type", "MicroProfile");
            resultInfo.put("java_info", System.getProperty("java.runtime.name") + " (" + System.getProperty("java.runtime.version") +')');
            resultInfo.put("java_major_version", String.valueOf(javaInfo.majorVersion()));
            resultInfo.put("feature_name", "Concurrency");
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
