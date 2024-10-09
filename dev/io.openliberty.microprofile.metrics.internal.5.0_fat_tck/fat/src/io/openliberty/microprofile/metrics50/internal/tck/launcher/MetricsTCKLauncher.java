/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics50.internal.tck.launcher;

import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class MetricsTCKLauncher {

    @Server("MetricsTCKServer")
    public static LibertyServer server;

    /*
     * Java 11.0.14 known to cause issues.
     */
    private static boolean isBadJava() throws IOException {
        JavaInfo javaInfo = JavaInfo.forServer(server);

        Log.info(MetricsTCKLauncher.class, "isBadJava",
                 "Major.minor.micro : [" + JavaInfo.forServer(server).majorVersion()
                                                        + "."
                                                        + JavaInfo.forServer(server).minorVersion()
                                                        + "." + JavaInfo.forServer(server).microVersion()
                                                        + "]");
        if (javaInfo.majorVersion() == 11 && javaInfo.microVersion() == 14) {
            Log.info(MetricsTCKLauncher.class, "isBadJava", "JDK matches with 11.0.14. Will be skipping due to issue with JIT");
            return true;
        } else if (javaInfo.majorVersion() == 11 && javaInfo.minorVersion() == 0
                   && javaInfo.microVersion() <= 3) {
            //disable tests for Java versions 11.0.0 - 11.0.3 since there's a bug in TLS 1.3 implementation
            Log.info(MetricsTCKLauncher.class, "isBadJava", "JDK matches with 11.0.0-11.3. Will be skipping due to TLS 1.3 implementation issue");
            return true;
        }

        return false;
    }

    @Before
    public void checkJava() throws Exception {
        //Skip running FAT if (remote) server JVM detected to be 11.0.14
        assumeTrue(!isBadJava());

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Ignore CWWKZ0131W - In windows, some jars are being locked during the test. Issue #2768
        server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E", "CWWKZ0131W", "CWWKW1001W", "CWWKW1002W", "CWPMI2005W", "CWPMI2006W");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchMetrics50Tck() throws Exception {
        server.startServer();

        String protocol = "https";
        String host = server.getHostname();
        String port = Integer.toString(server.getHttpDefaultSecurePort());

        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("test.url", protocol + "://" + host + ":" + port);
        additionalProps.put("test.user", "theUser");
        additionalProps.put("test.pwd", "thePassword");

        TCKRunner.build()
                        .withServer(server)
                        .withType(Type.MICROPROFILE)
                        .withSpecName("Metrics")
                        .withDefaultSuiteFileName()
                        .withAdditionalMvnProps(additionalProps)
                        .runTCK();
    }

}
