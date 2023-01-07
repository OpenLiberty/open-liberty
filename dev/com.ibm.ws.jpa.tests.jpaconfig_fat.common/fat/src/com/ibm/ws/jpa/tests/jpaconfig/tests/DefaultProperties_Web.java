/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jpa.tests.jpaconfig.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.ibm.ws.jpa.defaultproperties.web.TestDefaultPropertiesServlet;
import com.ibm.ws.jpa.overridepersistencecontext.web.TestOverridePersistenceContextServlet;
import com.ibm.ws.jpa.overridepersistencexml.web.TestOverridePersistenceXmlServlet;
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
@Mode(TestMode.LITE)
public class DefaultProperties_Web extends JPAFATServletClient {

    @Rule
    public static SkipDatabaseRule skipDBRule = new SkipDatabaseRule();

    enum ApplicationConfig {
        DefaultApp("test-applications/defaultproperties/", "web", "defaultpropertiesWeb", "defaultpropertiesWeb.ear", new String[] { "com.ibm.ws.jpa.defaultproperties.model",
                                                                                                                                     "com.ibm.ws.jpa.defaultproperties.testlogic",
                                                                                                                                     "com.ibm.ws.jpa.defaultproperties.web" }),
        OverridePersCont("test-applications/overridepersistencecontext/", "web", "overridepersistencecontextWeb", "overridepersistencecontextWeb.ear", new String[] { "com.ibm.ws.jpa.overridepersistencecontext.model",
                                                                                                                                                                      "com.ibm.ws.jpa.overridepersistencecontext.testlogic",
                                                                                                                                                                      "com.ibm.ws.jpa.overridepersistencecontext.web" }),
        OverridePersXML("test-applications/overridepersistencexml/", "web", "overridepersistencexmlWeb", "overridepersistencexmlWeb.ear", new String[] { "com.ibm.ws.jpa.overridepersistencexml.model",
                                                                                                                                                         "com.ibm.ws.jpa.overridepersistencexml.testlogic",
                                                                                                                                                         "com.ibm.ws.jpa.overridepersistencexml.web" });

        private String resourceRoot;
        private String appFolder;
        private String appName;
        private String appNameEar;
        private String[] webPackages;

        ApplicationConfig(String resourceRoot, String appFolder, String appName, String appNameEar, String[] webPackages) {
            this.resourceRoot = resourceRoot;
            this.appFolder = appFolder;
            this.appName = appName;
            this.appNameEar = appNameEar;
            this.webPackages = webPackages;
        }
    }

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA_DEFAULTPROPERTIES_${provider}_DROP_${dbvendor}.ddl");
        dropSet.add("JPA_OVERRIDEPERSISTENCECONTEXT_${provider}_DROP_${dbvendor}.ddl");
        dropSet.add("JPA_OVERRIDEPERSISTENCEXML_${provider}_DROP_${dbvendor}.ddl");
        createSet.add("JPA_DEFAULTPROPERTIES_${provider}_CREATE_${dbvendor}.ddl");
        createSet.add("JPA_OVERRIDEPERSISTENCECONTEXT_${provider}_CREATE_${dbvendor}.ddl");
        createSet.add("JPA_OVERRIDEPERSISTENCEXML_${provider}_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAConfigServer")
    @TestServlets({
                    @TestServlet(servlet = TestDefaultPropertiesServlet.class, path = "defaultpropertiesWeb" + "/" + "TestDefaultPropertiesServlet"),
                    @TestServlet(servlet = TestOverridePersistenceContextServlet.class, path = "overridepersistencecontextWeb" + "/" + "TestOverridePersistenceContextServlet"),
                    @TestServlet(servlet = TestOverridePersistenceXmlServlet.class, path = "overridepersistencexmlWeb" + "/" + "TestOverridePersistenceXmlServlet")
    })
    public static LibertyServer server;

    public static final JdbcDatabaseContainer<?> testContainer = AbstractFATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, AbstractFATSuite.JAXB_PERMS);
        bannerStart(DefaultProperties_Web.class);
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

        List<String> ddlPaths = new ArrayList<String>();
        for (ApplicationConfig config : ApplicationConfig.values()) {
            ddlPaths.add(config.resourceRoot + "ddl/");
        }
        setupDatabaseApplication(server, ddlPaths.toArray(new String[ddlPaths.size()]));

        JPAPersistenceProvider provider = AbstractFATSuite.provider;
        DatabaseVendor dbv = getDbVendor();
        System.out.println(DefaultProperties_Web.class.getName() + " Setting up database[" + dbv + "] tables for provider[" + provider + "]...");

        final Set<String> ddlSet = new HashSet<String>();
        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${provider}", provider.name()).replace("${dbvendor}", dbv.name()));
        }
        executeDDL(server, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${provider}", provider.name()).replace("${dbvendor}", dbv.name()));
        }
        executeDDL(server, ddlSet, false);

        setupTestApplication();
    }

    private static void setupTestApplication() throws Exception {
        for (ApplicationConfig appConfig : ApplicationConfig.values()) {
            WebArchive webApp = ShrinkWrap.create(WebArchive.class, appConfig.appName + ".war");
            for (String pkg : appConfig.webPackages) {
                webApp.addPackages(true, pkg);
            }
            ShrinkHelper.addDirectory(webApp, appConfig.resourceRoot + appConfig.appFolder + "/" + appConfig.appName + ".war");

            final JavaArchive testApiJar = buildTestAPIJar();

            final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, appConfig.appNameEar);
            app.addAsModule(webApp);
            app.addAsLibrary(testApiJar);
            ShrinkHelper.addDirectory(app, appConfig.resourceRoot + appConfig.appFolder, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
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
            appRecord.setLocation(appConfig.appNameEar);
            appRecord.setName(appConfig.appName);

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
            }

            server.setMarkToEndOfLog();
            ServerConfiguration sc = server.getServerConfiguration();
            sc.getApplications().add(appRecord);
            server.updateServerConfiguration(sc);
        }

        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        for (ApplicationConfig appConfig : ApplicationConfig.values()) {
            appNamesSet.add(appConfig.appName);
        }
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
                    ddlSet.add(ddlName.replace("${provider}", provider.name().toUpperCase()).replace("${dbvendor}", getDbVendor().name()));
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

                for (ApplicationConfig appConfig : ApplicationConfig.values()) {
                    server.deleteFileFromLibertyServerRoot("apps/" + appConfig.appNameEar);
                }
                server.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(DefaultProperties_Web.class, timestart);
        }
    }
}
