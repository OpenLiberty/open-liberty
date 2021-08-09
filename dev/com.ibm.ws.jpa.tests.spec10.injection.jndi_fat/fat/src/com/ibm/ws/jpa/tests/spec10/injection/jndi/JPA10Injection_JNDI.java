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

package com.ibm.ws.jpa.tests.spec10.injection.jndi;

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
import com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi.AnnotatedJNDIEJBSFExServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi.AnnotatedJNDIEJBSFServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi.AnnotatedJNDIEJBSLServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi.DeploymentDescriptorJNDIEJBSFEXServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi.DeploymentDescriptorJNDIEJBSFServlet;
import com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi.DeploymentDescriptorJNDIEJBSLServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.jndi.AnnotatedJNDIServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.jndi.AnnotatedJNDIWebLibServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.jndi.DeploymentDescriptorJNDIServlet;
import com.ibm.ws.jpa.fvt.injection.tests.web.jndi.DeploymentDescriptorJNDIWebLibServlet;
import com.ibm.ws.jpa.tests.spec10.injection.common.JPAFATServletClient;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeaterInfo;

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
public class JPA10Injection_JNDI extends JPAFATServletClient {

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

    private final static Set<String> dropSet = new HashSet<String>();
    private final static Set<String> createSet = new HashSet<String>();

    private static long timestart = 0;

    static {
        dropSet.add("JPA10_INJECTION_DROP_${dbvendor}.ddl");
        createSet.add("JPA10_INJECTION_CREATE_${dbvendor}.ddl");
    }

    @Server("JPAServer")
    @TestServlets({
                    // EJB
                    @TestServlet(servlet = AnnotatedJNDIEJBSLServlet.class, path = "JPA10InjectionJNDIEJB" + "/" + "AnnotatedJNDIEJBSLServlet"),
                    @TestServlet(servlet = AnnotatedJNDIEJBSFServlet.class, path = "JPA10InjectionJNDIEJB" + "/" + "AnnotatedJNDIEJBSFServlet"),
                    @TestServlet(servlet = AnnotatedJNDIEJBSFExServlet.class, path = "JPA10InjectionJNDIEJB" + "/" + "AnnotatedJNDIEJBSFExServlet"),

                    @TestServlet(servlet = DeploymentDescriptorJNDIEJBSLServlet.class, path = "JPA10InjectionJNDIEJB" + "/" + "DeploymentDescriptorJNDIEJBSLServlet"),
                    @TestServlet(servlet = DeploymentDescriptorJNDIEJBSFServlet.class, path = "JPA10InjectionJNDIEJB" + "/" + "DeploymentDescriptorJNDIEJBSFServlet"),
                    @TestServlet(servlet = DeploymentDescriptorJNDIEJBSFEXServlet.class, path = "JPA10InjectionJNDIEJB" + "/" + "DeploymentDescriptorJNDIEJBSFEXServlet"),

                    // Web
                    @TestServlet(servlet = AnnotatedJNDIServlet.class, path = "JPA10InjectionJNDIWeb" + "/" + "AnnotatedJNDIServlet"),
                    @TestServlet(servlet = DeploymentDescriptorJNDIServlet.class, path = "JPA10InjectionJNDIWeb" + "/" + "DeploymentDescriptorJNDIServlet"),

                    // Web-Lib
                    @TestServlet(servlet = AnnotatedJNDIWebLibServlet.class, path = "JPA10InjectionJNDIWebLib" + "/" + "AnnotatedJNDIWebLibServlet"),
                    @TestServlet(servlet = DeploymentDescriptorJNDIWebLibServlet.class, path = "JPA10InjectionJNDIWebLib" + "/" + "DeploymentDescriptorJNDIWebLibServlet"),

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
        bannerStart(JPA10Injection_JNDI.class);
        timestart = System.currentTimeMillis();

        server1.addEnvVar("repeat_phase", RepeaterInfo.repeatPhase);

        //Get driver name
        server1.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server1, testContainer);

        server1.startServer();

        setupDatabaseApplication(server1, "test-applications/injection/" + "ddl/");

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

        setupTestApplication_EJB();
        setupTestApplication_WEB();
        setupTestApplication_WEBLIB();
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

                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionJNDIEJB" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionJNDIWeb" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/" + "InjectionJNDIWebLib" + ".ear");
                server1.deleteFileFromLibertyServerRoot("apps/DatabaseManagement.war");
            } catch (Throwable t) {
                t.printStackTrace();
            }
            bannerEnd(JPA10Injection_JNDI.class, timestart);
        }
    }

    private static void setupTestApplication_EJB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "InjectionJNDIEJB"; // Name of EAR

        final String appModuleName = "injectionJNDI";
        final String appFileRootPath = RESOURCE_ROOT + "ejb/" + appModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejbA.jar
        final JavaArchive jpaejbAJar = ShrinkWrap.create(JavaArchive.class, "jpaejbA.jar");
        jpaejbAJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityA.class);
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        // jpaejbA.jar
        final JavaArchive jpaejbBJar = ShrinkWrap.create(JavaArchive.class, "jpaejbB.jar");
        jpaejbBJar.addClass(com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityB.class);
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        // jpawar.jar
        final JavaArchive jpawarJar = ShrinkWrap.create(JavaArchive.class, "jpawar.jar");
        jpawarJar.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        // EJB Jar Module
        JavaArchive ejbApp = ShrinkWrap.create(JavaArchive.class, appModuleName + ".jar");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        ejbApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.ejb.jndi");
        ShrinkHelper.addDirectory(ejbApp, appFileRootPath + appModuleName + ".jar");

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, appModuleName + "ejb.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.ejb.jndi");
        ShrinkHelper.addDirectory(webApp, appFileRootPath + appModuleName + "ejb.war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsModule(ejbApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbAJar);
        app.addAsLibrary(jpaejbBJar);
        app.addAsLibrary(jpawarJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, appFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
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

    private static void setupTestApplication_WEB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "InjectionJNDIWeb"; // Name of EAR

        final String webModuleName = "injectionJNDI";
        final String webFileRootPath = RESOURCE_ROOT + "web/" + webModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.entities.war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.jndi");
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, webFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
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

    private static void setupTestApplication_WEBLIB() throws Exception {
        final String RESOURCE_ROOT = "test-applications/injection/";
        final String applicationName = "InjectionJNDIWebLib"; // Name of EAR

        final String webModuleName = "injectionJNDIWebLib";
        final String webFileRootPath = RESOURCE_ROOT + "web/" + webModuleName + "/";
        final String libsPath = RESOURCE_ROOT + "libs/";

        // Library Jars

        // jpapulib.jar
        final JavaArchive jpapulibJar = ShrinkWrap.create(JavaArchive.class, "jpapulib.jar");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earlib");
        jpapulibJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.earroot");
        ShrinkHelper.addDirectory(jpapulibJar, libsPath + "jpapulib.jar");

        // jpacore.jar
        final JavaArchive jpacoreJar = ShrinkWrap.create(JavaArchive.class, "jpacore.jar");
        jpacoreJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.core");
//        ShrinkHelper.addDirectory(jpacoreJar, libsPath + "jpacore.jar");

        // jpaejb.jar
        final JavaArchive jpaejbJar = ShrinkWrap.create(JavaArchive.class, "jpaejb.jar");
        jpaejbJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.ejb");
//        ShrinkHelper.addDirectory(jpaejbJar, libsPath + "jpaejb.jar");

        // weblibjpajar.jar
        final JavaArchive weblibjpajarJar = ShrinkWrap.create(JavaArchive.class, "weblibjpajar.jar");
        weblibjpajarJar.addPackage("com.ibm.ws.jpa.fvt.injection.entities.war");

        final JavaArchive testApiJar = buildTestAPIJar();

        // Web Test WAR Module
        WebArchive webApp = ShrinkWrap.create(WebArchive.class, webModuleName + ".war");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.testlogic");
        webApp.addPackages(true, "com.ibm.ws.jpa.fvt.injection.tests.web.jndi");
        webApp.addAsLibrary(weblibjpajarJar);
        ShrinkHelper.addDirectory(webApp, webFileRootPath + webModuleName + ".war");

        final EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, applicationName + ".ear");
        app.addAsModule(webApp);
        app.addAsLibrary(jpapulibJar);
        app.addAsLibrary(jpacoreJar);
        app.addAsLibrary(jpaejbJar);
        app.addAsLibrary(testApiJar);
        ShrinkHelper.addDirectory(app, webFileRootPath, new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

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

        if (RepeaterInfo.repeatPhase != null && RepeaterInfo.repeatPhase.contains("hibernate")) {
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
}
