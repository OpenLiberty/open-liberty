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

package com.ibm.ws.jpa.tests.spec10.query.tests;

import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_001_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_002_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_003_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_004_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_005_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_006_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_007_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_008_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_009_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_010_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_011_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_012_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_013_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_014_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_015_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_016_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_017_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_018_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_019_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_020_Servlet;
import com.ibm.ws.query.web.loopqueryano.JULoopQueryAnoTest_021_Servlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TestSVLLoopAnoQuery_Web extends JPAFATServletClient {
    private final static String RESOURCE_ROOT = "test-applications/svlquery/";
    private final static String appFolder = "web";
    private final static String appName = "svlquery";
    private final static String appNameEar = appName + ".ear";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();
    private final static Set<String> populateSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA_SVLQUERY_DROP_${dbvendor}.ddl");
        createSet.add("JPA_SVLQUERY_CREATE_${dbvendor}.ddl");
        populateSet.add("JPA_SVLQUERY_POPULATE_${dbvendor}.ddl");
    }

    @Server("JPA10SVLQueryLoopServer")
    @TestServlets({
                    // Annotated
                    @TestServlet(servlet = JULoopQueryAnoTest_001_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_001_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_002_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_002_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_003_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_003_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_004_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_004_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_005_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_005_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_006_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_006_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_007_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_007_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_008_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_008_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_009_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_009_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_010_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_010_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_011_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_011_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_012_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_012_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_013_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_013_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_014_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_014_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_015_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_015_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_016_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_016_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_017_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_017_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_018_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_018_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_019_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_019_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_020_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_020_Servlet"),
                    @TestServlet(servlet = JULoopQueryAnoTest_021_Servlet.class, path = "svlquery" + "/" + "JULoopQueryAnoTest_021_Servlet"),
    })
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = AbstractFATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, AbstractFATSuite.JAXB_PERMS);
        bannerStart(TestSVLLoopAnoQuery_Web.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.startServer();

        setupDatabaseApplication(server, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        System.out.println(TestSVLLoopAnoQuery_Web.class.getName() + " Setting up database tables...");

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

        setupTestApplication();
    }

    private void populate(String servletName) {

    }

    private static void setupTestApplication() throws Exception {
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appName + ".war");
        webApp.addPackages(true, "com.ibm.ws.query.entities.ano");
        webApp.addPackages(true, "com.ibm.ws.query.entities.interfaces");
        webApp.addPackages(true, "com.ibm.ws.query.entities.xml");
        webApp.addPackages(true, "com.ibm.ws.query.testlogic");
        webApp.addPackages(true, "com.ibm.ws.query.utils");
        webApp.addPackages(true, "com.ibm.ws.query.web");
        webApp.addPackages(true, "com.ibm.ws.query.web.loopqueryano");
        webApp.addPackages(true, "com.ibm.ws.query.web.loopqueryxml");

        ShrinkHelper.addDirectory(webApp, RESOURCE_ROOT + appFolder + "/" + appName + ".war");

        final JavaArchive testApiJar = buildTestAPIJar();

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appNameEar);
        app.addAsModule(webApp);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + appFolder, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(appNameEar);
        appRecord.setName(appName);
        ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
        ClassloaderElement loader = new ClassloaderElement();
        loader.setApiTypeVisibility("+third-party");
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

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            // Clean up database
            try {
                final Set<String> ddlSet = new HashSet<String>();
                for (String ddlName : dropSet) {
                    ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
                }
                executeDDL(server, ddlSet, true);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            server.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                              "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server.getServerConfiguration();
                sc.getApplications().clear();
                server.updateServerConfiguration(sc);
                server.saveServerConfiguration();

                server.deleteFileFromLibertyServerRoot("apps/" + appNameEar);
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(TestSVLLoopAnoQuery_Web.class, timestart);
        }
    }
}
