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
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ejbcontainer.remote.enventry.web.EnvEntryServlet;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class BadApplicationTests extends AbstractTest {
    public static String eeVersion;

    @Server("com.ibm.ws.ejbcontainer.remote.fat.BadAppServer")
    @TestServlets({ @TestServlet(servlet = EnvEntryServlet.class, contextRoot = "EnvEntryWeb") })
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.BadAppServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.fat.BadAppServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.BadAppServer"));

    private static Set<String> installedApps;

    @BeforeClass
    public static void beforeClass() throws Exception {
        eeVersion = JakartaEE9Action.isActive() ? "EE9" : RepeatTestFilter.isRepeatActionActive(EE8FeatureReplacementAction.ID) ? "EE8" : "";

        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        //#################### AppExcExtendsThrowableErrBean
        JavaArchive AppExcExtendsThrowableErrBeanJar = ShrinkHelper.buildJavaArchive("AppExcExtendsThrowableErrBean.jar", "com.ibm.ws.ejbcontainer.remote.jitdeploy.error1.ejb.");

        EnterpriseArchive AppExcExtendsThrowableErrBean = ShrinkWrap.create(EnterpriseArchive.class, "AppExcExtendsThrowableErrBean.ear");
        AppExcExtendsThrowableErrBean.addAsModule(AppExcExtendsThrowableErrBeanJar);

        ShrinkHelper.exportAppToServer(server, AppExcExtendsThrowableErrBean, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY);

        //#################### EnvEntryShared.jar
        JavaArchive EnvEntrySharedJar = ShrinkHelper.buildJavaArchive("EnvEntryShared.jar", "com.ibm.ws.ejbcontainer.remote.enventry.shared.");

        ShrinkHelper.exportToServer(server, "lib/global", EnvEntrySharedJar, DeployOptions.SERVER_ONLY);

        //#################### EnvEntryApp
        JavaArchive EnvEntryEJBJar = ShrinkHelper.buildJavaArchive("EnvEntryEJB.jar", "com.ibm.ws.ejbcontainer.remote.enventry.ejb.");
        WebArchive EnvEntryWeb = ShrinkHelper.buildDefaultApp("EnvEntryWeb.war", "com.ibm.ws.ejbcontainer.remote.enventry.web.");

        EnterpriseArchive EnvEntryApp = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryApp.ear");
        EnvEntryApp.addAsModule(EnvEntryEJBJar).addAsModule(EnvEntryWeb);
        EnvEntryApp = (EnterpriseArchive) ShrinkHelper.addDirectory(EnvEntryApp, "test-applications/EnvEntryApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EnvEntryApp, DeployOptions.SERVER_ONLY);

        //################### EnvEntryBad1App
        JavaArchive EnvEntryBad1EJBJar = ShrinkHelper.buildJavaArchive("EnvEntryBad1EJB.jar", "com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb.");

        EnterpriseArchive EnvEntryBad1App = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryBad1App.ear");
        EnvEntryBad1App.addAsModule(EnvEntryBad1EJBJar);
        ShrinkHelper.addDirectory(EnvEntryBad1App, "test-applications/EnvEntryBad1App.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EnvEntryBad1App, DeployOptions.SERVER_ONLY);

        //################### EnvEntryBad2App
        JavaArchive EnvEntryBad2EJBJar = ShrinkHelper.buildJavaArchive("EnvEntryBad2EJB.jar", "com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb2.");

        EnterpriseArchive EnvEntryBad2App = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryBad2App.ear");
        EnvEntryBad2App.addAsModule(EnvEntryBad2EJBJar);
        ShrinkHelper.addDirectory(EnvEntryBad2App, "test-applications/EnvEntryBad2App.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EnvEntryBad2App, DeployOptions.SERVER_ONLY);

        //################### EnvEntryBad3App
        JavaArchive EnvEntryBad3EJBJar = ShrinkHelper.buildJavaArchive("EnvEntryBad3EJB.jar", "com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb3.");

        EnterpriseArchive EnvEntryBad3App = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryBad3App.ear");
        EnvEntryBad3App.addAsModule(EnvEntryBad3EJBJar);
        ShrinkHelper.addDirectory(EnvEntryBad3App, "test-applications/EnvEntryBad3App.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EnvEntryBad3App, DeployOptions.SERVER_ONLY);

        //################### EnvEntryBad4App
        JavaArchive EnvEntryBad4EJBJar = ShrinkHelper.buildJavaArchive("EnvEntryBad4EJB.jar", "com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb4.");

        EnterpriseArchive EnvEntryBad4App = ShrinkWrap.create(EnterpriseArchive.class, "EnvEntryBad4App.ear");
        EnvEntryBad4App.addAsModule(EnvEntryBad4EJBJar);
        ShrinkHelper.addDirectory(EnvEntryBad4App, "test-applications/EnvEntryBad4App.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EnvEntryBad4App, DeployOptions.SERVER_ONLY);

        // Save list of dropins applications for server updates
        installedApps = server.listAllInstalledAppsForValidation();

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CNTR0075E - testApplicationExceptionExtendsThrowable, testApplicationExceptionExtendsThrowableEE8
        // CNTR0190E - testApplicationExceptionExtendsThrowable, testApplicationExceptionExtendsThrowableEE8
        // CNTR4002E - testApplicationExceptionExtendsThrowable, testApplicationExceptionExtendsThrowableEE8
        // CNTR4006E - testApplicationExceptionExtendsThrowable, testApplicationExceptionExtendsThrowableEE8
        // CNTR5107E - testApplicationExceptionExtendsThrowable, testApplicationExceptionExtendsThrowableEE8
        // CWNEN0009E - testE2xEnvEntryNonExistingEnumType, testE4xEnvEntryExistingNonEnumNonClass, testE2aEnvEntryNonExistingEnumType, testE4aEnvEntryExistingNonEnumNonClass
        // CWNEN0011E - testC2xEnvEntryNonExistingClass, testE3xEnvEntryNonExistingEnumValue, testC2aEnvEntryNonExistingClass, testE3aEnvEntryNonExistingEnumValue
        // CWNEN0021W - testE3xEnvEntryNonExistingEnumValue
        // CWNEN0063E - testE3xEnvEntryNonExistingEnumValue, testE3aEnvEntryNonExistingEnumValue
        // CWNEN0064E - testE4xEnvEntryExistingNonEnumNonClass, testE4aEnvEntryExistingNonEnumNonClass
        // CWWKZ0002E - testApplicationExceptionExtendsThrowable, testApplicationExceptionExtendsThrowableEE8
        // CWWKZ0106E - testApplicationExceptionExtendsThrowable, testApplicationExceptionExtendsThrowableEE8
        // CNTR0338W - Ambiguous binding warning
        server.stopServer("CNTR0075E", "CNTR0190E", "CNTR4002E", "CNTR4006E", "CNTR5107E", "CWNEN0009E", "CWNEN0011E", "CWNEN0021W", "CWNEN0063E", "CWNEN0064E", "CWWKZ0002E",
                          "CWWKZ0106E", "CNTR0338W");
    }

    // For C2, constant for the bogus class name, which should appear somewhere in the error message:
    protected static final String NOSUCHCLASS = "com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb.NoSuchClass";

    // For E2, constant for the bogus enum class name, which should appear somewhere in the error message:
    protected static final String NOSUCHENUMTYPE_ENUM_TYPE = "com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb2.NoSuchEnumType";

    // For E3, constants for the Enum class name and the bogus enum value, which should appear somewhere in the error message:
    protected static final String NOSUCHENUMVALUE_ENUM_TYPE = "com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver\\$EnvEntryEnum";
    protected static final String NOSUCHENUMVALUE_ENUM_VALUE = "NO_SUCH_ENUM_VALUE";

    // For E4, constants for the existing non-Enum, non-Class class name:
    protected static final String EXISTING_NON_ENUM_NON_CLASS_ENV_ENTRY_NAME = "EnvEntry_ExistingNonEnumNonClass_EntryName";
    // env-entry-type may be either ..Bad4XmlBean or ..Bad4AnnBean, so just look for Bad4, which is common to both variations
    protected static final String EXISTING_NON_ENUM_NON_CLASS_ENV_ENTRY_TYPE = "com.ibm.ws.ejbcontainer.remote.enventry.bad.ejb4.Bad4";

    /**
     * This test verifies that application exceptions declared on the throws
     * clause must extend Exception.
     *
     * <p>An application is installed with a module with a bean with an
     * exception that extends Throwable rather than Exception.
     *
     * <p>The expected result is that the following message is printed, and the
     * bean fails to start: "CNTR5107E: The {0} application exception defined on
     * method {1} of class {1} must be defined as a subclass of the
     * java.lang.Exception class."
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "javax.ejb.NoSuchEJBException", "com.ibm.wsspi.injectionengine.InjectionException",
                    "javax.ejb.EJBException", "com.ibm.ejs.container.ContainerException",
                    "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testApplicationExceptionExtendsThrowable() throws Exception {
        Log.info(this.getClass(), "testApplicationExceptionExtendsThrowable", "os.name : " + System.getProperty("os.name", "unknown").toLowerCase());
        server.setMarkToEndOfLog();
        server.saveServerConfiguration();
        server.setServerConfigurationFile("ExtendsThrowable" + eeVersion + ".xml");
        server.waitForStringInLogUsingMark("CWWKG0016I", 240 * 1000); // Starting server configuration update.
        server.waitForConfigUpdateInLogUsingMark(installedApps);
        assertNotNull(server.waitForStringInLogUsingMark("CNTR5107E")); // must subclass exception
        assertNotNull(server.waitForStringInLogUsingMark("CNTR0075E")); // class (wrapper) not loaded
        assertNotNull(server.waitForStringInLogUsingMark("CNTR4006E")); // bean failed to start
        assertNotNull(server.waitForStringInLogUsingMark("CNTR0190E")); // startup singleton failed to initialize
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0106E")); // could not start web application
        assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0002E")); // exception starting application
        // Generating this file on some windows systems can take awhile; wait for it before restoring configuration
        assertNotNull(server.waitForStringInLogUsingMark("SRVE9103I"), 240 * 1000); // config file for web server plugin generated
        server.setMarkToEndOfLog();
        server.restoreServerConfiguration();
        server.waitForStringInLogUsingMark("CWWKG0016I", 240 * 1000); // Starting server configuration update.
        server.waitForConfigUpdateInLogUsingMark(installedApps);
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad1XmlBean:
     * <env-entry>
     * <description>C2x - Non-existent class specified in XML only</description>
     * <env-entry-name>EnvEntry_Non-existentClass_EntryName</env-entry-name>
     * <env-entry-type>java.lang.Class</env-entry-type>
     * <env-entry-value>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchClass</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad1XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_NoSuchClass</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error CWNEN0011E is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException", "java.lang.ClassNotFoundException" })
    public void testC2xEnvEntryNonExistingClass() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHCLASS));
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad2XmlBean:
     * <env-entry>
     * <description>E2x - Non-existent enum type specified in XML only</description>
     * <env-entry-name>EnvEntry_Non-existentEnumType_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchEnumType</env-entry-type>
     * <env-entry-value>EV0</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad2XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_NoSuchEnumType</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException", "java.lang.ClassNotFoundException" })
    public void testE2xEnvEntryNonExistingEnumType() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHENUMTYPE_ENUM_TYPE));
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad2XmlBean:
     * <env-entry>
     * <description>E3x - Non-existent enum value specified in XML only</description>
     * <env-entry-name>EnvEntry_Non-existentEnumValue_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver$EnvEntryEnum</env-entry-type>
     * <env-entry-value>NO_SUCH_ENUM_VALUE</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad3XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_NoSuchEnumValue</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException" })
    public void testE3xEnvEntryNonExistingEnumValue() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHENUMVALUE_ENUM_TYPE));
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHENUMVALUE_ENUM_VALUE));
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad4XmlBean:
     * <env-entry>
     * <description>E4X - Existing class that is neither an Enum nor a Class; specified in XML only</description>
     * <env-entry-name>EnvEntry_ExistingNonEnumNonClass_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad4XmlBean</env-entry-type>
     * <env-entry-value>NOT_APPLICABLE</env-entry-value>
     * <injection-target>
     * <injection-target-class>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad4XmlBean</injection-target-class>
     * <injection-target-name>ivEnvEntry_ExistingNonEnumNonClass</injection-target-name>
     * </injection-target>
     * </env-entry>
     *
     * - Verify that an appropriate error is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException" })
    public void testE4xEnvEntryExistingNonEnumNonClass() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(EXISTING_NON_ENUM_NON_CLASS_ENV_ENTRY_NAME));
        assertNotNull(server.waitForStringInLogUsingMark(EXISTING_NON_ENUM_NON_CLASS_ENV_ENTRY_TYPE));
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad1Bean:
     * <env-entry>
     * <description>C2a - Non-existent class specified in XML, for @Resource annotation</description>
     * <env-entry-name>EnvEntry_Non-existentClass_EntryName</env-entry-name>
     * <env-entry-type>java.lang.Class</env-entry-type>
     * <env-entry-value>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchClass</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * \@Resource(name="EnvEntry_Non-existentClass_EntryName")
     * \Enum<?> ivEnvEntry_NoSuchClass
     *
     * - Verify that an appropriate error is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException", "java.lang.ClassNotFoundException" })
    public void testC2aEnvEntryNonExistingClass() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHCLASS));
    }

    /**
     * E2A.
     * Define an <env-entry> in ejb-jar.xml, under bean Bad2Bean:
     * <env-entry>
     * <description>E2a - Non-existent enum type specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_Non-existentEnumType_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.NoSuchEnumType</env-entry-type>
     * <env-entry-value>EV0</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * \@Resource(name="EnvEntry_Non-existentEnumType_EntryName")
     * \Enum<?> ivEnvEntry_NoSuchEnumType;
     *
     * - Verify that an appropriate error is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException", "java.lang.ClassNotFoundException" })
    public void testE2aEnvEntryNonExistingEnumType() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHENUMTYPE_ENUM_TYPE));
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad3Bean:
     * <env-entry>
     * <description>E3a - Non-existent enum value specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_Non-existentEnumValue_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver$EnvEntryEnum</env-entry-type>
     * <env-entry-value>NO_SUCH_ENUM_VALUE</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * \@Resource(name="EnvEntry_Non-existentEnumValue_EntryName")
     * \Enum<?> ivEnvEntry_NoSuchEnumValue;
     *
     * - Verify that an appropriate error is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException" })
    public void testE3aEnvEntryNonExistingEnumValue() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHENUMVALUE_ENUM_TYPE));
        assertNotNull(server.waitForStringInLogUsingMark(NOSUCHENUMVALUE_ENUM_VALUE));
    }

    /**
     * Define an <env-entry> in ejb-jar.xml, under bean Bad4Bean:
     * <env-entry>
     * <description>E4a - Existing class that is neither an Enum nor a Class; specified in XML and @Resource annotation</description>
     * <env-entry-name>EnvEntry_ExistingNonEnumNonClass_EntryName</env-entry-name>
     * <env-entry-type>com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad1Bean</env-entry-type>
     * <env-entry-value>NOT_APPLICABLE</env-entry-value>
     * </env-entry>
     * Annotate an Enum<?> instance var:
     *
     * \@Resource(name="EnvEntry_ExistingNonEnumNonClass_EntryName")
     * \Enum<?> ivEnvEntry_NotApplicableEnumValue;
     *
     * - Verify that an appropriate error is issued
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", "com.ibm.wsspi.injectionengine.InjectionConfigurationException" })
    public void testE4aEnvEntryExistingNonEnumNonClass() throws Exception {
        server.setMarkToEndOfLog();
        runTest("EnvEntryWeb/EnvEntryServlet");
        assertNotNull(server.waitForStringInLogUsingMark(EXISTING_NON_ENUM_NON_CLASS_ENV_ENTRY_NAME));
        assertNotNull(server.waitForStringInLogUsingMark(EXISTING_NON_ENUM_NON_CLASS_ENV_ENTRY_TYPE));
    }
}
