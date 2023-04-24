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
package com.ibm.ws.jpa.jpa31;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.FATSuite;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 11)
public class AsmServiceTest extends JPAFATServletClient {
    private final static String CONTEXT_ROOT = "eclAsmService";
    private final static String RESOURCE_ROOT = "test-applications/eclAsmService/";
    private final static String appFolder = "web";
    private final static String appName = "eclAsmService";
    private final static String appNameWar = appName + ".war";

    private final static String PKG_ROOT = "io.openliberty.jpa.tests.jpa31.eclipselink.asmservice";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();
    private final static Set<String> populateSet = new HashSet<String>();
    private static long timestart = 0;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    static {
        dropSet.add("ASMSERVICE_DROP_${dbvendor}.ddl");
        createSet.add("ASMSERVICE_CREATE_${dbvendor}.ddl");
    }

    @Server("JPA_ASM_Server_Default")
    public static LibertyServer serverWithDefaultAsm;

    @Server("JPA_ASM_Server_ECL")
    public static LibertyServer serverWithEclipselinkAsm;

    @Server("JPA_ASM_Server_OW2")
    public static LibertyServer serverWithOw2Asm;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(serverWithDefaultAsm, FATSuite.JAXB_PERMS);
        PrivHelper.generateCustomPolicy(serverWithEclipselinkAsm, FATSuite.JAXB_PERMS);
        PrivHelper.generateCustomPolicy(serverWithOw2Asm, FATSuite.JAXB_PERMS);

        //Get driver name
        serverWithDefaultAsm.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
        serverWithEclipselinkAsm.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
        serverWithOw2Asm.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(serverWithDefaultAsm, testContainer);
        DatabaseContainerUtil.setupDataSourceProperties(serverWithEclipselinkAsm, testContainer);
        DatabaseContainerUtil.setupDataSourceProperties(serverWithOw2Asm, testContainer);
    }

    private void setupDatabaseApp(LibertyServer server) throws Exception {
        setupDatabaseApplication(server, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        System.out.println(AsmServiceTest.class.getName() + " Setting up database tables...");

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server, ddlSet, false);

        ddlSet.clear();
        for (String ddlName : populateSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server, ddlSet, false);
    }

    private static void setupTestApplication(LibertyServer server) throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appNameWar);
        webApp.addPackages(true, PKG_ROOT + ".models");
        webApp.addPackages(true, PKG_ROOT + ".web");

        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder);
        ShrinkHelper.exportToServer(server, "apps", webApp);

        Application appRecord = new Application();
        appRecord.setLocation(appNameWar);
        appRecord.setName(appName);
        ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
        ClassloaderElement loader = new ClassloaderElement();
        loader.getCommonLibraryRefs().add("global");
        cel.add(loader);

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(appName);
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    protected void runTest(LibertyServer server) throws IOException {
        final int port = server.getHttpDefaultPort();
        final String url = "http://localhost:" + port + "/" + appName + "/ASMTestServlet";

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

    private void runAsmTest(LibertyServer server, String expectedAsmImpl) throws Exception {
        try {
            server.startServer();
            setupDatabaseApp(server);
            setupTestApplication(server);
            runTest(server);
            server.stopServer();
            if (server.defaultTraceFileExists()) {
                List<String> asmImplMsgList = server.findStringsInTrace("[eclipselink] " + expectedAsmImpl + " ASM implementation is used.");
                Assert.assertFalse(asmImplMsgList.isEmpty());
            }
        } finally {
            if (server.isStarted()) {
                try {
                    server.stopServer();
                } catch (Throwable t) {
                }
            }
        }
    }

    @Test
    public void testWithDefaultASM() throws Exception {
        // Default should be Eclipselink's ASM.
        runAsmTest(serverWithDefaultAsm, "EclipseLink");
    }

    @Test
    public void testWithEclipselinkASM() throws Exception {
        // Run with JVM option -Declipselink.asm.service=eclipselink
        runAsmTest(serverWithEclipselinkAsm, "EclipseLink");
    }

    @Test
    public void testWithOW2ASM() throws Exception {
        // Run with JVM option -Declipselink.asm.service=ow2
        runAsmTest(serverWithOw2Asm, "OW2");
    }
}
