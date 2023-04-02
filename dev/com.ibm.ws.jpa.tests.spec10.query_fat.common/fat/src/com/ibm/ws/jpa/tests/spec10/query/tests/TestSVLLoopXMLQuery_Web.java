/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_001_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_002_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_003_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_004_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_005_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_006_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_007_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_008_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_009_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_010_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_011_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_012_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_013_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_014_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_015_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_016_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_017_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_018_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_019_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_020_Servlet;
import com.ibm.ws.query.web.loopqueryxml.JULoopQueryXMLTest_021_Servlet;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.vehicle.web.JPAFATServletClient;

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
public class TestSVLLoopXMLQuery_Web extends JPAFATServletClient {

    @Rule
    public static SkipRule skipRule = new SkipRule();

    private final static String CONTEXT_ROOT = "svlquery";
    private final static String RESOURCE_ROOT = "test-applications/svlquery/";
    private final static String appFolder = "web";
    private final static String appName = "svlquery";
    private final static String appNameEar = appName + ".ear";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();
    private final static Set<String> populateSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA_SVLQUERY_${provider}_DROP_${dbvendor}.ddl");
        createSet.add("JPA_SVLQUERY_${provider}_CREATE_${dbvendor}.ddl");
        populateSet.add("JPA_SVLQUERY_${provider}_POPULATE_${dbvendor}.ddl");
    }

    @Server("JPA10SVLQueryLoopServer")
    @TestServlets({
                    // XML
                    @TestServlet(servlet = JULoopQueryXMLTest_001_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_001_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_002_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_002_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_003_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_003_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_004_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_004_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_005_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_005_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_006_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_006_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_007_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_007_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_008_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_008_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_009_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_009_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_010_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_010_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_011_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_011_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_012_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_012_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_013_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_013_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_014_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_014_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_015_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_015_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_016_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_016_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_017_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_017_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_018_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_018_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_019_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_019_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_020_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_020_Servlet"),
                    @TestServlet(servlet = JULoopQueryXMLTest_021_Servlet.class, path = CONTEXT_ROOT + "/" + "JULoopQueryXMLTest_021_Servlet")
    })
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = AbstractFATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, AbstractFATSuite.JAXB_PERMS);
        bannerStart(TestSVLLoopXMLQuery_Web.class);
        timestart = System.currentTimeMillis();

        int appStartTimeout = server.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server.setConfigUpdateTimeout(120 * 1000);
        }

        server.addEnvVar("repeat_phase", AbstractFATSuite.repeatPhase);

        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.startServer();

        setupDatabaseApplication(server, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        System.out.println(TestSVLLoopXMLQuery_Web.class.getName() + " Setting up database tables...");

        DatabaseVendor database = getDbVendor();
        JPAPersistenceProvider provider = AbstractFATSuite.provider;

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${provider}", provider.name()).replace("${dbvendor}", database.name()));
        }
        executeDDL(server, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${provider}", provider.name()).replace("${dbvendor}", database.name()));
        }
        executeDDL(server, ddlSet, false);

        ddlSet.clear();
        for (String ddlName : populateSet) {
            ddlSet.add(ddlName.replace("${provider}", provider.name()).replace("${dbvendor}", database.name()));
        }
        executeDDL(server, ddlSet, false);

        setupTestApplication();

        skipRule.setDatabase(database);
        skipRule.setProvider(provider);
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

        /*
         * Hibernate 5.2 (JPA 2.1) contains a bug that requires a dialect property to be set
         * for Oracle platform detection: https://hibernate.atlassian.net/browse/HHH-13184
         */
        if (AbstractFATSuite.repeatPhase != null && AbstractFATSuite.repeatPhase.contains("21")
            && DatabaseVendor.ORACLE.equals(getDbVendor())) {
            webApp.move("/WEB-INF/classes/META-INF/persistence-oracle-21.xml", "/WEB-INF/classes/META-INF/persistence.xml");
        }

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

        // setup the thirdparty classloader for Hibernate and OpenJPA
        if (AbstractFATSuite.repeatPhase != null && AbstractFATSuite.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        } else if (AbstractFATSuite.repeatPhase != null && AbstractFATSuite.repeatPhase.contains("openjpa")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("OpenJPALib");
            cel.add(loader);
        } else {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.setApiTypeVisibility("+third-party");
            cel.add(loader);
        }

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
                JPAPersistenceProvider provider = AbstractFATSuite.provider;
                final Set<String> ddlSet = new HashSet<String>();
                for (String ddlName : dropSet) {
                    ddlSet.add(ddlName.replace("${provider}", provider.name()).replace("${dbvendor}", getDbVendor().name()));
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
            bannerEnd(TestSVLLoopXMLQuery_Web.class, timestart);
        }
    }
}
