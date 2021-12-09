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
import com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.web.jta.InjectionDPUEJBEarLibLevelJTATestServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.web.rl.InjectionDPUEJBEarLibLevelRLTestServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.jta.EarLibLevelJTADPUFieldInjectionServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.jta.EarLibLevelJTADPUMethodInjectionServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.rl.EarLibLevelRLDPUFieldInjectionServlet;
import com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.rl.EarLibLevelRLDPUMethodInjectionServlet;

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
public class JPA10InjectionDPU_Earlevel extends JPAFATServletClient {

    @Rule
    public org.junit.rules.Verifier skipDBRule = new org.junit.rules.Verifier() {

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

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DPU_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_DPU_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAServerEARLevel")
    @TestServlets({
                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.jta.InjectionDPUServlet.class,
                                 path = "injectiondpu_earlibjta" + "/" + "InjectionDPUServlet"),
                    @TestServlet(servlet = EarLibLevelJTADPUFieldInjectionServlet.class, path = "injectiondpu_earlibjta" + "/" + "EarLibLevelJTADPUFieldInjectionServlet"),
                    @TestServlet(servlet = EarLibLevelJTADPUMethodInjectionServlet.class, path = "injectiondpu_earlibjta" + "/" + "EarLibLevelJTADPUMethodInjectionServlet"),

                    @TestServlet(servlet = InjectionDPUEJBEarLibLevelJTATestServlet.class, path = "earliblvljtaejbexecutor" + "/" + "InjectionDPUEJBEarLibLevelJTATestServlet"),

                    @TestServlet(servlet = com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.rl.InjectionDPUServlet.class,
                                 path = "injectiondpu_earlibrl" + "/" + "InjectionDPUServlet"),
                    @TestServlet(servlet = EarLibLevelRLDPUFieldInjectionServlet.class, path = "injectiondpu_earlibrl" + "/" + "EarLibLevelRLDPUFieldInjectionServlet"),
                    @TestServlet(servlet = EarLibLevelRLDPUMethodInjectionServlet.class, path = "injectiondpu_earlibrl" + "/" + "EarLibLevelRLDPUMethodInjectionServlet"),

                    @TestServlet(servlet = InjectionDPUEJBEarLibLevelRLTestServlet.class, path = "earliblvlrlejbexecutor" + "/" + "InjectionDPUEJBEarLibLevelRLTestServlet"),

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
        bannerStart(JPA10InjectionDPU_Earlevel.class);
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

        setupTestApplicationEarLibJTA();
        setupTestApplicationEarLibRL();
    }

    /*
     * Construct InjectionDPUEarLibJTALevel.ear
     */
    private static void setupTestApplicationEarLibJTA() throws Exception {
        final JavaArchive testApiJar = buildTestAPIJar();

        // InjectionDPUEarLibLevelJTAEJB.jar
        final JavaArchive ejbjar1 = ShrinkWrap.create(JavaArchive.class, "InjectionDPUEarLibLevelJTAEJB.jar");
        ejbjar1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        ejbjar1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.jta");
        ShrinkHelper.addDirectory(ejbjar1, RESOURCE_ROOT + "/apps/InjectionDPUEarLibJTALevel.ear/InjectionDPUEarLibLevelJTAEJB.jar");

        // InjectionDPUEarLibLevelJTA.war
        final WebArchive webApp1 = ShrinkWrap.create(WebArchive.class, "InjectionDPUEarLibLevelJTA.war");
        webApp1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        webApp1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.jta");
        ShrinkHelper.addDirectory(webApp1, RESOURCE_ROOT + "/apps/InjectionDPUEarLibJTALevel.ear/InjectionDPUEarLibLevelJTA.war");

        // EJBExecutor.war
        final WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, "EJBExecutor.war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.web.jta");
        ShrinkHelper.addDirectory(webApp2, RESOURCE_ROOT + "/apps/InjectionDPUEarLibJTALevel.ear/EJBExecutor.war");

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injectiondpu.entities");
        ShrinkHelper.addDirectory(jpapulibJar, RESOURCE_ROOT + "/apps/InjectionDPUEarLibJTALevel.ear/lib/jpapulib.jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "InjectionDPUEarLibJTALevel.ear");
        app.addAsModule(ejbjar1);
        app.addAsModule(webApp1);
        app.addAsModule(webApp2);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "/apps/InjectionDPUEarLibJTALevel.ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
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
        appRecord.setLocation("InjectionDPUEarLibJTALevel.ear");
        appRecord.setName("InjectionDPUEarLibJTALevel");

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
        appNamesSet.add("InjectionDPUEarLibJTALevel");
        server1.waitForConfigUpdateInLogUsingMark(appNamesSet, "");
    }

    /*
     * Construct InjectionDPUEarLibRLLevel.ear
     */
    private static void setupTestApplicationEarLibRL() throws Exception {
        final JavaArchive testApiJar = buildTestAPIJar();

        // InjectionDPUEarLibLevelRLEJB.jar
        final JavaArchive ejbjar1 = ShrinkWrap.create(JavaArchive.class, "InjectionDPUEarLibLevelRLEJB.jar");
        ejbjar1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        ejbjar1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.rl");
        ShrinkHelper.addDirectory(ejbjar1, RESOURCE_ROOT + "/apps/InjectionDPUEarLibRLLevel.ear/InjectionDPUEarLibLevelRLEJB.jar");

        // InjectionDPUAppLevelRL.war
        final WebArchive webApp1 = ShrinkWrap.create(WebArchive.class, "InjectionDPUEarLibLevelRL.war");
        webApp1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.testlogic");
        webApp1.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.web.earliblevel.rl");
        ShrinkHelper.addDirectory(webApp1, RESOURCE_ROOT + "/apps/InjectionDPUEarLibRLLevel.ear/InjectionDPUEarLibLevelRL.war");

        // EJBExecutor.war
        final WebArchive webApp2 = ShrinkWrap.create(WebArchive.class, "EJBExecutor.war");
        webApp2.addPackages(true, "com.ibm.ws.jpa.fvt.injectiondpu.ejb.earliblevel.web.rl");
        ShrinkHelper.addDirectory(webApp2, RESOURCE_ROOT + "/apps/InjectionDPUEarLibRLLevel.ear/EJBExecutor.war");

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injectiondpu.entities");
        ShrinkHelper.addDirectory(jpapulibJar, RESOURCE_ROOT + "/apps/InjectionDPUEarLibRLLevel.ear/lib/jpapulib.jar");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "InjectionDPUEarLibRLLevel.ear");
        app.addAsModule(ejbjar1);
        app.addAsModule(webApp1);
        app.addAsModule(webApp2);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, RESOURCE_ROOT + "/apps/InjectionDPUEarLibRLLevel.ear", new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
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
        appRecord.setLocation("InjectionDPUEarLibRLLevel.ear");
        appRecord.setName("InjectionDPUEarLibRLLevel");

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
        appNamesSet.add("InjectionDPUEarLibRLLevel");
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

                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDPUEarLibJTALevel.ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionDPUEarLibRLLevel.ear");
                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(JPA10InjectionDPU_Earlevel.class, timestart);
        }
    }
}
