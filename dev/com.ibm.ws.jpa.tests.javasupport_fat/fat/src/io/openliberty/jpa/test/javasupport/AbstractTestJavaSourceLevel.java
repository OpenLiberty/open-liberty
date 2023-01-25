/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package io.openliberty.jpa.test.javasupport;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;

import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

/**
 *
 */
public class AbstractTestJavaSourceLevel {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";",
                                                "permission java.lang.RuntimePermission \"accessDeclaredMembers\";" };

    public final static int MIN_JAVA_LEVEL = 7;
    public final static int MAX_JAVA_LEVEL = 20; // Update this when supporting a new level of JDK
    public final static int JVM_JAVA_LEVEL = JavaInfo.JAVA_VERSION;

    public static boolean isJakartaLevel = false;

    protected static void setupFATForJPA20(LibertyServer server, boolean isJakarta) throws Exception {
        PrivHelper.generateCustomPolicy(server, JAXB_PERMS);

        isJakartaLevel = isJakarta;

        server.startServer();

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        HashSet<String> appNamesSet = new HashSet<String>();

        int max = 7;

        for (int index = MIN_JAVA_LEVEL; index <= max; index++) {
            String appName = isJakartaLevel ? "JakartaSupportTest_" + index : "JavaSupportTest_" + index;
            Application appRecord = new Application();
            appRecord.setLocation(appName + ".war");
            appRecord.setName(appName);
            sc.getApplications().add(appRecord);

            appNamesSet.add(appName);
        }

        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    protected static void setupFAT(LibertyServer server, boolean isJakarta) throws Exception {
        PrivHelper.generateCustomPolicy(server, JAXB_PERMS);

        isJakartaLevel = isJakarta;

        server.startServer();

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        HashSet<String> appNamesSet = new HashSet<String>();

        int max = JVM_JAVA_LEVEL > MAX_JAVA_LEVEL ? MAX_JAVA_LEVEL : JVM_JAVA_LEVEL;

        for (int index = MIN_JAVA_LEVEL; index <= max; index++) {
            String appName = isJakartaLevel ? "JakartaSupportTest_" + index : "JavaSupportTest_" + index;
            Application appRecord = new Application();
            appRecord.setLocation(appName + ".war");
            appRecord.setName(appName);
            sc.getApplications().add(appRecord);

            appNamesSet.add(appName);
        }

        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    protected static void shutdownFAT(LibertyServer server) throws Exception {
        server.stopServer("CWWJP9991W" // From Eclipselink drop-and-create tables option
        );
    }

    protected void runTest(LibertyServer server, int javaLevel) throws IOException {
        final int port = server.getHttpDefaultPort();
        final String url = "http://localhost:" + port + (isJakartaLevel ? "/JakartaSupportTest_" : "/JavaSupportTest_") + javaLevel + "/JSTestServlet";

        runGetMethod(200, "[TEST PASSED]", new URL(url));
    }

    private StringBuilder runGetMethod(int exprc, String testOut, URL url) throws IOException {

        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br
                            .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(testOut) < 0)
                fail("Missing success message in output. " + lines);

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                     + retcode);

            return lines;
        } finally {
            con.disconnect();
        }
    }
}
