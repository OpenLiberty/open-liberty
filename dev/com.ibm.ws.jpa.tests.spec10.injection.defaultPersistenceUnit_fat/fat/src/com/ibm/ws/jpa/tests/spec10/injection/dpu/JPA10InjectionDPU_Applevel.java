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

package com.ibm.ws.jpa.tests.spec10.injection.dpu;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.web.InjectionDPUEJBAppLevelTestServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.jta.AppLevelJTADPUFieldInjectionServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.jta.AppLevelJTADPUMethodInjectionServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.jta.InjectionDPUServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.rl.AppLevelRLDPUFieldInjectionServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.rl.AppLevelRLDPUMethodInjectionServlet;

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
public class JPA10InjectionDPU_Applevel extends JPAFATServletClient {

    @Rule
    public org.junit.rules.TestRule skipDBRule = new org.junit.rules.Verifier() {

        @Override
        public Statement apply(Statement arg0, Description arg1) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    String database = getDbVendor().name();
                    boolean shouldSkip = (database != null
                                          && !Pattern.compile("derby", Pattern.CASE_INSENSITIVE).matcher(database).find());
                    System.out.println("Checking if skip test");
                    if (shouldSkip) {
                        throw new AssumptionViolatedException("Database is not Derby. Skipping test!");
                    } else {
                        System.out.println("Not Skipping");
                        arg0.evaluate();
                    }
                }
            };
        }
    };

    private final static String RESOURCE_ROOT = "test-applications/injection_dpu/";
    private final static String applicationName = "InjectionDPUAppLevel";

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DPU_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_DPU_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAServerAppLevel")
    @TestServlets({
                    @TestServlet(servlet = InjectionDPUServlet.class, path = "injectiondpu_appjta" + "/" + "InjectionDPUServlet"),
                    @TestServlet(servlet = AppLevelJTADPUFieldInjectionServlet.class, path = "injectiondpu_appjta" + "/" + "AppLevelJTADPUFieldInjectionServlet"),
                    @TestServlet(servlet = AppLevelJTADPUMethodInjectionServlet.class, path = "injectiondpu_appjta" + "/" + "AppLevelJTADPUMethodInjectionServlet"),

                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.rl.InjectionDPUServlet.class, path = "injectiondpu_apprl" + "/" + "InjectionDPUServlet"),
                    @TestServlet(servlet = AppLevelRLDPUFieldInjectionServlet.class, path = "injectiondpu_apprl" + "/" + "AppLevelRLDPUFieldInjectionServlet"),
                    @TestServlet(servlet = AppLevelRLDPUMethodInjectionServlet.class, path = "injectiondpu_apprl" + "/" + "AppLevelRLDPUMethodInjectionServlet"),

                    @TestServlet(servlet = InjectionDPUEJBAppLevelTestServlet.class, path = "applvlejbexecutor" + "/" + "InjectionDPUEJBAppLevelTestServlet"),

    })
    public static LibertyServer server1;

    public static final JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;

    @BeforeClass
    public static void setUp() throws Exception {
        int appStartTimeout = server1.getAppStartTimeout();
        if (appStartTimeout < (120 * 1000)) {
            server1.setAppStartTimeout(120 * 1000);
        }

        int configUpdateTimeout = server1.getConfigUpdateTimeout();
        if (configUpdateTimeout < (120 * 1000)) {
            server1.setConfigUpdateTimeout(120 * 1000);
        }

        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);
        bannerStart(JPA10InjectionDPU_Applevel.class);
        timestart = System.currentTimeMillis();

        server1.addEnvVar("repeat_phase", FATSuite.repeatPhase);

        //Get driver name
        server1.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server1, testContainer);

        server1.startServer();

        setupDatabaseApplication(server1, RESOURCE_ROOT + "ddl/");

        final Set<String> ddlSet = new HashSet<String>();

        ddlSet.clear();
        for (String ddlName : dropSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, true);

        ddlSet.clear();
        for (String ddlName : createSet) {
            ddlSet.add(ddlName.replace("${dbvendor}", getDbVendor().name()));
        }
        executeDDL(server1, ddlSet, false);

        setupTestApplication();
    }

    /*
     * Construct InjectionDPUAppLevel.ear
     */
    private static void setupTestApplication() throws Exception {
        final JavaArchive testApiJar = buildTestAPIJar();

        // InjectionDPUAppLevelJTAEJB.jar
        final JavaArchive ejbjar1 = ShrinkWrap.create(JavaArchive.class, "InjectionDPUAppLevelJTAEJB.jar");
        ejbjar1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.entities");
        ejbjar1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        ejbjar1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.jta");
        ShrinkHelper.addDirectory(ejbjar1, RESOURCE_ROOT + "/apps/InjectionDPUAppLevel.ear/InjectionDPUAppLevelJTAEJB.jar");

        // InjectionDPUAppLevelRLEJB.jar
        final JavaArchive ejbjar2 = ShrinkWrap.create(JavaArchive.class, "InjectionDPUAppLevelRLEJB.jar");
        ejbjar2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.entities");
        ejbjar2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        ejbjar2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.rl");
        ShrinkHelper.addDirectory(ejbjar2, RESOURCE_ROOT + "/apps/InjectionDPUAppLevel.ear/InjectionDPUAppLevelRLEJB.jar");

        // InjectionDPUAppLevelJTA.war
        final WebArchive webApp1 = ShrinkWrap.create(WebArchive.class, "InjectionDPUAppLevelJTA.war");
        webApp1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.entities");
        webApp1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        webApp1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.jta");
        ShrinkHelper.addDirectory(webApp1, RESOURCE_ROOT + "/apps/InjectionDPUAppLevel.ear/InjectionDPUAppLevelJTA.war");

        // InjectionDPUAppLevelRL.war
        final WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, "InjectionDPUAppLevelRL.war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.entities");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.web.applevel.rl");
        ShrinkHelper.addDirectory(webApp2, RESOURCE_ROOT + "/apps/InjectionDPUAppLevel.ear/InjectionDPUAppLevelRL.war");

        // EJBExecutor.war
        final WebArchive webApp3 = ShrinkWrap.create(WebArchive.class, "EJBExecutor.war");
        webApp3.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.ejb.applevel.web");
        ShrinkHelper.addDirectory(webApp3, RESOURCE_ROOT + "/apps/InjectionDPUAppLevel.ear/EJBExecutor.war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "InjectionDPUAppLevel.ear");
        app.addAsModule(ejbjar1);
        app.addAsModule(ejbjar2);
        app.addAsModule(webApp1);
        app.addAsModule(webApp2);
        app.addAsModule(webApp3);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "/apps/InjectionDPUAppLevel.ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
            @Override
            public boolean include(ArchivePath arg0) {
                if (arg0.get().startsWith("/META-INF/")) {
                    return true;
                }
                return false;
            }

        });

        ShrinkHelper.exportToServer(server1, "apps", app);

        Application appRecord = new Application();
        appRecord.setLocation(applicationName + ".ear");
        appRecord.setName(applicationName);

        if (FATSuite.repeatPhase != null && FATSuite.repeatPhase.contains("hibernate")) {
            ConfigElementList<ClassloaderElement> cel = appRecord.getClassloaders();
            ClassloaderElement loader = new ClassloaderElement();
            loader.getCommonLibraryRefs().add("HibernateLib");
            cel.add(loader);
        }

        server1.setMarkToEndOfLog();
        ServerConfiguration sc = server1.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server1.updateServerConfiguration(sc);
        server1.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add(applicationName);
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server1.stopServer("CWWJP9991W", // From Eclipselink drop-and-create tables option
                               "WTRN0074E: Exception caught from before_completion synchronization operation" // RuntimeException test, expected
            );
        } finally {
            try {
                ServerConfiguration sc = server1.getServerConfiguration();
                sc.getApplications().clear();
                server1.updateServerConfiguration(sc);
                server1.saveServerConfiguration();

                server1.deleteFileFromLibertyServerRoot("apps/" + applicationName + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(JPA10InjectionDPU_Applevel.class, timestart);
        }
    }
}
