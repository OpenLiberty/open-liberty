/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.classpath.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import dummy.ra.DummyME;
import dummy.ra.DummyResourceAdapter;
import io.openliberty.classloading.classpath.test.client1.ClientLib1;
import io.openliberty.classloading.classpath.test.ejb1.InitBean1;
import io.openliberty.classloading.classpath.test.ejb2.InitBean2;
import io.openliberty.classloading.classpath.test.ejb3.InitBean3;
import io.openliberty.classloading.classpath.test.lib1.Lib1;
import io.openliberty.classloading.classpath.test.lib10.Lib10;
import io.openliberty.classloading.classpath.test.lib11.Lib11;
import io.openliberty.classloading.classpath.test.lib12.Lib12;
import io.openliberty.classloading.classpath.test.lib13.Lib13;
import io.openliberty.classloading.classpath.test.lib14.Lib14;
import io.openliberty.classloading.classpath.test.lib15.Lib15;
import io.openliberty.classloading.classpath.test.lib16.Lib16;
import io.openliberty.classloading.classpath.test.lib17.Lib17;
import io.openliberty.classloading.classpath.test.lib18.Lib18;
import io.openliberty.classloading.classpath.test.lib2.Lib2;
import io.openliberty.classloading.classpath.test.lib3.Lib3;
import io.openliberty.classloading.classpath.test.lib4.Lib4;
import io.openliberty.classloading.classpath.test.lib5.Lib5;
import io.openliberty.classloading.classpath.test.lib6.Lib6;
import io.openliberty.classloading.classpath.test.lib7.Lib7;
import io.openliberty.classloading.classpath.test.lib8.Lib8;
import io.openliberty.classloading.classpath.test.lib9.Lib9;
import io.openliberty.classloading.classpath.test.rar1.TestResourceAdapter;
import io.openliberty.classloading.classpath.test.war1.ClassPathDefaultLoaderServletTest1;
import io.openliberty.classloading.classpath.test.war2.ClassPathDefaultLoaderServletTest2;
import io.openliberty.classloading.classpath.test.war3.ClassPathDefaultLoaderServletTest3;
import io.openliberty.classloading.classpath.util.TestUtils;
import io.openliberty.classloading.dynamic.feature.lifecycle.test.app.DynamicFeatureLifecycleTestServlet;
import io.openliberty.classloading.dynamic.feature.unrelated.test.app.DynamicFeatureUnrelatedServlet;
import io.openliberty.classloading.feature.message.test.app.ParentLastFeatureMessageTestServlet;
import io.openliberty.classloading.lib.path.test.app.LibPathTestServlet;
import io.openliberty.classloading.library.precedence.test.app.LibPrecedenceBeforeAppTestServlet;
import io.openliberty.classloading.library.test.app.LibraryUserTestServlet;
import io.openliberty.classloading.libs.util.CodeSourceUtil;
import io.openliberty.classloading.parent.library.inconsistent.test.app.ParentLibraryInconsistentTestServlet;
import io.openliberty.classloading.platform.delegation.test.app.PlatformDelegationTestServlet;
import io.openliberty.classloading.shared.feature.lib.LibraryFeatureApiImplState1;
import io.openliberty.nativelib.test.app.NativeLibraryTestServlet;
import io.openlibery.classloading.override.library.test.app.OverrideLibraryTestServlet;
import io.openlibery.classloading.override.library.test.app.a.AOverride;
import io.openlibery.classloading.override.library.test.app.b.BOverride;
import io.openlibery.classloading.override.library.test.app.c.COverride;
import io.openlibery.classloading.override.library.test.app.d.DOverride;
import io.openlibery.classloading.override.library.test.app.e.EOverride;
import io.openlibery.classloading.override.library.test.app.f.FOverride;
import io.openlibery.classloading.override.library.test.app.g.GOverride;
import io.openlibery.classloading.override.library.test.app.h.HOverride;
import junit.framework.AssertionFailedError;
import test.bundle.api1.a.API_A1;
import test.bundle.api1.b.API_B1;
import test.bundle.api1.c.API_C1;
import test.bundle.api2.a.API_A2;
import test.bundle.api2.b.API_B2;
import test.bundle.api2.c.API_C2;
import test.bundle.api3.a.API_A3;
import test.bundle.api3.b.API_B3;
import test.bundle.api3.c.API_C3;
import test.bundle.api4.a.API_A4;
import test.bundle.api4.b.API_B4;
import test.bundle.api4.c.API_C4;

@RunWith(Suite.class)
@SuiteClasses({
    ClassPathWarLoaderTests.class,
    ClassPathDefaultLoaderDropinsTests.class,
    ClassPathDefaultLoaderTests.class,
    ClassPathEarLoaderTests.class,
    ClassPathDefaultLoaderLibraryTests.class,
    ClassPathInvalidLoaderTests.class,
    LibraryPathTest.class,
    AppParentDelegationDefaultTest.class,
    AppParentDelegationPlatformNullPackagesTest.class,
    AppParentDelegationPlatformEmptyPackagesTest.class,
    AppParentDelegationPlatformConfigPackagesTest.class,
    AppParentDelegationSystemNullPackagesTest.class,
    AppParentDelegationSystemEmptyPackagesTest.class,
    AppParentDelegationSystemConfigPackagesTest.class,
    AppParentDelegationJVMLibTest.class,
    ParentLastLibraryFeatureTests.class,
    ParentLastInconsistentResourceAdaptorTests.class,
    ParentLastInconsistentLibraryTests.class,
    OverrideLibraryTests.class,
    LibraryPrecedenceBeforeAppTests.class,
    LibraryPrecedenceAfterAppTests.class,
    NativeLibraryTest.class,
    LibraryServiceTests.class,
    DynamicFeatureLifecycleTest.class,
    DynamicFeatureSharedLibTest.class,
    DynamicFeatureLibJarRemovedTest.class,
    DynamicFeatureGlobalSharedLibTest.class,
    DynamicFeatureEarTest.class,
    DynamicFeatureMultiGatewayTest.class,
})
public class FATSuite {
    static final String CLASSPATH_TEST_WAR_LOADER_SERVER = "classpathTestWarLoader";
    static final String CLASSPATH_TEST_DEFAULT_LOADER_SERVER = "classpathTestDefaultLoader";
    static final String CLASSPATH_TEST_DEFAULT_LOADER_DROPINS_SERVER = "classpathTestDefaultLoaderDropins";
    static final String CLASSPATH_TEST_INVALID_LOADER_SERVER = "classpathTestInvalidLoader";
    static final String CLASSPATH_TEST_EAR_LOADER_SERVER = "classpathTestEarLoader";
    static final String PRIVATE_LIBRARY_TEST_SERVER = "privateLibraryTest";
    static final String LIB_FILESET_TEST_SERVER = "libPathTest";
    static final String APP_PARENT_TEST_SERVER = "appParentTest";
    static final String JVM_LIB_TEST_SERVER = "jvmLibTest";
    static final String NATIVE_LIBRARY_TEST_SERVER = "nativeLibraryTest";
    static final String PARENT_LAST_LIBRARY_FEATURE_TEST_SERVER = "parentLastLibraryFeatureTest";
    static final String PARENT_LAST_LIBRARY_INCONSISTENT_SERVER = "parentLastLibraryInconsistentTest";
    static final String PARENT_LAST_RESOURCE_ADAPTOR_INCONSISTENT_SERVER = "parentLastResourceAdaptorInconsistentTest";
    static final String LIB_PRECEDENCE_BEFORE_APP_SERVER = "libPrecedenceBeforeAppTest";
    static final String LIB_PRECEDENCE_AFTER_APP_SERVER = "libPrecedenceAfterAppTest";
    static final String OVERRIDE_LIB_WAR_TEST_SERVER = "overrideLibWarTest";
    static final String LIBRARY_USER_TEST_SERVER = "libraryUserTest";
    static final String DYNAMIC_FEATURE_LIFECYCLE_TEST_SERVER = "dynamicFeatureLifecycleTest";
    static final String DYNAMIC_FEATURE_SHARED_LIB_TEST_SERVER = "dynamicFeatureSharedLibTest";
    static final String DYNAMIC_FEATURE_SHARED_LIB_TWO_APPS_TEST_SERVER = "dynamicFeatureSharedLibTwoAppsTest";
    static final String DYNAMIC_FEATURE_GLOBAL_SHARED_LIB_TEST_SERVER = "dynamicFeatureGlobalSharedLibTest";
    static final String DYNAMIC_FEATURE_EAR_TEST_SERVER = "dynamicFeatureEarTest";
    static final String DYNAMIC_FEATURE_MULTI_GATEWAY_TEST_SERVER = "dynamicFeatureMultiGatewayTest";
    static final String DYNAMIC_FEATURE_LIB_JAR_REMOVED_TEST_SERVER = "dynamicFeatureLibJarRemovedTest";

    // ##### ARCHIVE NAMES #####
    // WAR archive names
    public static final String TEST_CLASS_PATH1_APP = "testClassPath1";
    public static final String TEST_CLASS_PATH2_APP = "testClassPath2";
    public static final String TEST_CLASS_PATH3_APP = "testClassPath3";
    public static final String TEST_LIB_FILESET_APP = "testLibFileSet";
    public static final String TEST_PLATFORM_DELEGATION_APP = "testPlatformDelegation";
    public static final String TEST_NATIVE_LIBRARY_APP = "testNativeLibrary";
    public static final String TEST_PARENT_LAST_LIBRARY_FEATURE_APP = "testParentLastLibraryFeature";
    public static final String TEST_PARENT_LIBRARY_INCONSISTENT_APP = "testParentLibraryInconsistent";
    public static final String TEST_LIB_PRECEDENCE_APP = "testLibPrecedence";
    public static final String TEST_OVERRIDE_LIB_APP = "testOverrideLib";
    public static final String TEST_LIBRARY_USER_APP = "testLibraryUser";
    public static final String TEST_DYNAMIC_FEATURE_APP = "testDynamicFeatureApp";
    public static final String TEST_DYNAMIC_FEATURE_APP2 = "testDynamicFeatureApp2";
    // Second copy of the dynamic feature WAR deployed without a library reference; used to assert it is NOT restarted
    public static final String TEST_DYNAMIC_FEATURE_UNRELATED_APP = "testUnrelatedFeatureApp";
    public static final String TEST_DYNAMIC_FEATURE_SHARED_LIB = "testSharedFeatureLib";
    public static final String TEST_DYNAMIC_FEATURE_SHARED_LIB2 = "testSharedFeatureLib2";
    public static final String TEST_DYNAMIC_FEATURE_EAR_APP = "testDynamicFeatureEar";

    public static final String DYNAMIC_FEATURE_SERVLET_PATH =
        TEST_DYNAMIC_FEATURE_APP + "/DynamicFeatureLifecycleTestServlet";
    public static final String DYNAMIC_FEATURE_APP2_SERVLET_PATH =
        TEST_DYNAMIC_FEATURE_APP2 + "/DynamicFeatureLifecycleTestServlet";

    public static final List<String> BASE_FEATURES =
        Collections.unmodifiableList(Arrays.asList("servlet-4.0", "componenttest-1.0"));
    public static final List<String> BASE_PLUS_DYNAMIC_FEATURE =
        Collections.unmodifiableList(Arrays.asList("servlet-4.0", "componenttest-1.0", "testFeatureApi-1.0"));

    // ##### DYNAMIC FEATURE TEST HELPERS #####

    /**
     * Installs the dynamic feature and its supporting OSGi bundle into the Liberty installation root.
     * Must be called in {@code @BeforeClass} before {@code server.startServer()}.
     *
     * @param server the Liberty server instance
     * @throws Exception if installation fails or files are not found
     */
    public static void installTestFeatureAndBundle(LibertyServer server) throws Exception {
        server.installSystemFeature("testFeatureApi-1.0");
        assertTrue("testFeatureApi-1.0.mf should have been copied to lib/features",
                   server.fileExistsInLibertyInstallRoot("lib/features/testFeatureApi-1.0.mf"));

        server.installSystemBundle("test.feature.api");
        assertTrue("test.feature.api.jar should have been copied to lib",
                   server.fileExistsInLibertyInstallRoot("lib/test.feature.api.jar"));
    }

    /**
     * Uninstalls the dynamic feature and its supporting OSGi bundle from the Liberty installation root.
     * Safely checks for existence before uninstallation, making cleanup idempotent.
     *
     * @param server the Liberty server instance
     * @throws Exception if uninstallation fails
     */
    public static void uninstallTestFeatureAndBundle(LibertyServer server) throws Exception {
        if (server.fileExistsInLibertyInstallRoot("lib/features/testFeatureApi-1.0.mf")) {
            server.uninstallSystemFeature("testFeatureApi-1.0");
            assertFalse("testFeatureApi-1.0.mf was not cleaned up",
                        server.fileExistsInLibertyInstallRoot("lib/features/testFeatureApi-1.0.mf"));
        }
        if (server.fileExistsInLibertyInstallRoot("lib/test.feature.api.jar")) {
            server.uninstallSystemBundle("test.feature.api");
            assertFalse("test.feature.api.jar was not cleaned up",
                        server.fileExistsInLibertyInstallRoot("lib/test.feature.api.jar"));
        }
    }

    /**
     * Stops the specified Liberty server if running and cleanly uninstalls the dynamic test
     * feature and bundle from the Liberty installation root.
     *
     * @param server the Liberty server instance
     * @param ignoredLogMessages optional message prefixes to ignore during server shutdown
     * @throws Exception if server stop or uninstallation fails
     */
    public static void stopServerAndUninstall(LibertyServer server, String... ignoredLogMessages) throws Exception {
        try {
            if (server != null && server.isStarted()) {
                if (ignoredLogMessages != null && ignoredLogMessages.length > 0) {
                    server.stopServer(ignoredLogMessages);
                } else {
                    server.stopServer();
                }
            }
        } finally {
            if (server != null) {
                uninstallTestFeatureAndBundle(server);
            }
        }
    }

    /**
     * Asserts that the specified application was stopped (CWWKZ0009I) and then restarted (CWWKZ0001I/CWWKZ0003I),
     * and that the stop occurred before the restart.
     *
     * @param appName the application name to check
     * @param state the lifecycle state description for failure messages
     * @param messages the log messages captured during configuration update
     */
    public static void assertAppStoppedThenStarted(String appName, String state, List<String> messages) {
        int stopped = -1;
        int started = -1;
        for (int i = 0; i < messages.size(); i++) {
            String line = messages.get(i);
            if (!line.matches(".*\\b" + appName + "\\b.*")) continue;
            if (line.contains("CWWKZ0009I") && stopped < 0) stopped = i;
            else if ((line.contains("CWWKZ0001I") || line.contains("CWWKZ0003I")) && started < 0) started = i;
        }
        assertTrue(state + ": " + appName + " was not stopped (CWWKZ0009I)", stopped >= 0);
        assertTrue(state + ": " + appName + " was not started again (CWWKZ0001I/0003I)", started >= 0);
        assertTrue(state + ": " + appName + " started before it stopped", stopped < started);
    }

    /**
     * Overload for tests where the default application name is {@link #TEST_DYNAMIC_FEATURE_APP}.
     *
     * @param state the lifecycle state description for failure messages
     * @param messages the log messages captured during configuration update
     */
    public static void assertAppStoppedThenStarted(String state, List<String> messages) {
        assertAppStoppedThenStarted(TEST_DYNAMIC_FEATURE_APP, state, messages);
    }

    /**
     * Counts the total number of start/stop messages (CWWKZ0001I, CWWKZ0003I, CWWKZ0009I)
     * for a given application in the server logs. Uses word boundaries to avoid partial matches.
     *
     * @param server the Liberty server instance
     * @param appName the application name
     * @return the number of matching log messages
     * @throws Exception if log search fails
     */
    public static int countAppMessages(LibertyServer server, String appName) throws Exception {
        return server.findStringsInLogs("CWWKZ000[139]I.*\\b" + appName + "\\b").size();
    }

    // EJB archive names
    public static final String TEST_EJB1 = "testEjb1";
    public static final String TEST_EJB2 = "testEjb2";
    public static final String TEST_EJB3 = "testEjb3";

    // Library archive names
    public static final String TEST_LIB1 = "testLib1";
    public static final String TEST_LIB2 = "testLib2";
    public static final String TEST_LIB3 = "testLib3";
    public static final String TEST_LIB4 = "testLib4";
    public static final String TEST_LIB5 = "testLib5";
    public static final String TEST_LIB6 = "testLib6";
    public static final String TEST_LIB7 = "testLib7";
    public static final String TEST_LIB8 = "testLib8";
    public static final String TEST_LIB9 = "testLib9";
    public static final String TEST_LIB10 = "testLib10";
    public static final String TEST_LIB11 = "testLib11";
    public static final String TEST_LIB12 = "testLib12";
    public static final String TEST_LIB13 = "testLib13";
    public static final String TEST_LIB14 = "testLib14";
    public static final String TEST_LIB15 = "testLib15";
    public static final String TEST_LIB16 = "testLib16";
    public static final String TEST_LIB17 = "testLib17";
    public static final String TEST_LIB18 = "testLib18";

    // RAR inner jar archive names
    public static final String TEST_RESOURCE_ADAPTOR = "testResourceAdaptor";

    // RAR archive names
    public static final String TEST_RAR1 = "testRar1";
    public static final String TEST_DUMMY_RA = "testDummyRA";

    // Client archive names
    public static final String TEST_CLIENT1 = "testClient1";
    // EAR archive names
    static final String TEST_CLASS_PATH_APP = "testClassPath";

    // ##### LIBRARY CLASS NAMES #####
    // EJB library class names
    public static final String EJB_LIB1_CLASS_NAME = "io.openliberty.classloading.classpath.test.ejb1.EjbLib1";
    public static final String EJB_LIB2_CLASS_NAME = "io.openliberty.classloading.classpath.test.ejb2.EjbLib2";
    public static final String EJB_LIB3_CLASS_NAME = "io.openliberty.classloading.classpath.test.ejb3.EjbLib3";

    // Library class names
    public static final String LIB1_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib1.Lib1";
    public static final String LIB2_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib2.Lib2";
    public static final String LIB3_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib3.Lib3";
    public static final String LIB4_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib4.Lib4";
    public static final String LIB5_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib5.Lib5";
    public static final String LIB6_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib6.Lib6";
    public static final String LIB7_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib7.Lib7";
    public static final String LIB8_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib8.Lib8";
    public static final String LIB9_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib9.Lib9";
    public static final String LIB10_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib10.Lib10";
    public static final String LIB11_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib11.Lib11";
    public static final String LIB12_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib12.Lib12";
    public static final String LIB13_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib13.Lib13";
    public static final String LIB14_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib14.Lib14";
    public static final String LIB15_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib15.Lib15";
    public static final String LIB16_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib16.Lib16";
    public static final String LIB17_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib17.Lib17";
    public static final String LIB18_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib18.Lib18";

    // RAR library class names
    public static final String RAR_LIB1_CLASS_NAME = "io.openliberty.classloading.classpath.test.rar1.RarLib1";
    public static final String RAR_LIB2_CLASS_NAME = "io.openliberty.classloading.classpath.test.rar1.RarLib2";

    // Client library class names
    public static final String CLIENT_LIB1_CLASS_NAME = "io.openliberty.classloading.classpath.test.client1.ClientLib1";

    // ##### SHRINK WRAP ARCHIVES #####
    // WAR archives
    static final WebArchive TEST_CLASS_PATH1_WAR;
    static final WebArchive TEST_CLASS_PATH2_WAR;
    static final WebArchive TEST_CLASS_PATH3_WAR;
    static final WebArchive TEST_LIB_FILESET_WAR;
    static final WebArchive TEST_PLATFORM_DELEGATION_WAR;
    static final WebArchive TEST_NATIVE_LIBRARY_WAR;
    static final WebArchive TEST_PARENT_LAST_LIBRARY_FEATURE_WAR;
    static final WebArchive TEST_LIB_DELEGATION_PARENT_INCONSISTENT_WAR;
    static final WebArchive TEST_OVERRIDE_LIB_WAR;
    static final WebArchive TEST_LIB_PRECEDENCE_WAR;
    static final WebArchive TEST_LIBRARY_USER_WAR;
    static final WebArchive TEST_DYNAMIC_FEATURE_WAR;
    static final WebArchive TEST_DYNAMIC_FEATURE_APP2_WAR;
    static final WebArchive TEST_DYNAMIC_FEATURE_UNRELATED_WAR;
    static final JavaArchive TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR;
    static final JavaArchive TEST_DYNAMIC_FEATURE_SHARED_LIB2_JAR;

    // EJB archives
    static final JavaArchive TEST_EJB1_JAR;
    static final JavaArchive TEST_EJB2_JAR;
    static final JavaArchive TEST_EJB3_JAR;

    // Library archives
    static final JavaArchive TEST_LIB1_JAR;
    static final JavaArchive TEST_LIB2_JAR;
    static final JavaArchive TEST_LIB3_JAR;
    static final JavaArchive TEST_LIB4_JAR;
    static final JavaArchive TEST_LIB5_JAR;
    static final JavaArchive TEST_LIB6_JAR;
    static final JavaArchive TEST_LIB7_JAR;
    static final JavaArchive TEST_LIB8_JAR;
    static final JavaArchive TEST_LIB9_JAR;
    static final JavaArchive TEST_LIB10_JAR;
    static final JavaArchive TEST_LIB11_JAR;
    static final JavaArchive TEST_LIB12_JAR;
    static final JavaArchive TEST_LIB13_JAR;
    static final JavaArchive TEST_LIB14_JAR;
    static final JavaArchive TEST_LIB15_JAR;
    static final JavaArchive TEST_LIB16_JAR;
    static final JavaArchive TEST_LIB17_JAR;
    static final JavaArchive TEST_LIB18_JAR;

    // RAR inner JAR archives
    static final JavaArchive TEST_RESOURCE_ADAPTOR_JAR;

    // RAR archives
    static final ResourceAdapterArchive TEST_RAR1_RAR;
    static final ResourceAdapterArchive TEST_DUMMY_RAR;

    // Client archives
    static final JavaArchive TEST_CLIENT1_JAR;

    // EAR archives
    static final EnterpriseArchive TEST_CLASS_PATH_EAR;
    static final EnterpriseArchive TEST_DYNAMIC_FEATURE_EAR;
    static {
        try {

            TEST_LIB1_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB1 + ".jar", Lib1.class.getPackage().getName(), //
                                                          CodeSourceUtil.class.getPackage().getName(), //
                                                          AOverride.class.getPackage().getName(), //
                                                          API_A1.class.getPackage().getName(), //
                                                          API_B1.class.getPackage().getName(), //
                                                          API_C1.class.getPackage().getName());
            TEST_LIB2_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB2 + ".jar", Lib2.class.getPackage().getName(), //
                                                          AOverride.class.getPackage().getName(), //
                                                          BOverride.class.getPackage().getName(), //
                                                          API_A2.class.getPackage().getName(), //
                                                          API_B2.class.getPackage().getName(), //
                                                          API_C2.class.getPackage().getName());
            TEST_LIB3_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB3 + ".jar", Lib3.class.getPackage().getName(), //
                                                          AOverride.class.getPackage().getName(), //
                                                          BOverride.class.getPackage().getName(), //
                                                          COverride.class.getPackage().getName(), //
                                                          API_A3.class.getPackage().getName(), //
                                                          API_B3.class.getPackage().getName(), //
                                                          API_C3.class.getPackage().getName() );
            TEST_LIB4_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB4 + ".jar", Lib4.class.getPackage().getName(), //
                                                          AOverride.class.getPackage().getName(), //
                                                          BOverride.class.getPackage().getName(), //
                                                          COverride.class.getPackage().getName(), //
                                                          DOverride.class.getPackage().getName(), //
                                                          API_A4.class.getPackage().getName(), //
                                                          API_B4.class.getPackage().getName(), //
                                                          API_C4.class.getPackage().getName() );
            TEST_LIB5_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB5 + ".jar", Lib5.class.getPackage().getName(), //
                                                          EOverride.class.getPackage().getName());
            TEST_LIB6_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB6 + ".jar", Lib6.class.getPackage().getName(), //
                                                          EOverride.class.getPackage().getName(), //
                                                          FOverride.class.getPackage().getName());
            TEST_LIB7_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB7 + ".jar", Lib7.class.getPackage().getName(), //
                                                          EOverride.class.getPackage().getName(), //
                                                          FOverride.class.getPackage().getName(), //
                                                          GOverride.class.getPackage().getName());
            TEST_LIB8_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB8 + ".jar", Lib8.class.getPackage().getName(), //
                                                          EOverride.class.getPackage().getName(), //
                                                          FOverride.class.getPackage().getName(), //
                                                          GOverride.class.getPackage().getName(), //
                                                          HOverride.class.getPackage().getName());
            TEST_LIB9_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB9 + ".jar", Lib9.class.getPackage().getName(), //
                                                          DummyME.class.getPackage().getName());
            TEST_LIB10_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB10 + ".jar", Lib10.class.getPackage().getName());
            TEST_LIB11_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB11 + ".jar", Lib11.class.getPackage().getName());
            TEST_LIB12_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB12 + ".jar", Lib12.class.getPackage().getName());
            TEST_LIB13_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB13 + ".jar", Lib13.class.getPackage().getName());
            TEST_LIB14_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB14 + ".jar", Lib14.class.getPackage().getName());
            TEST_LIB15_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB15 + ".jar", Lib15.class.getPackage().getName());
            TEST_LIB16_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB16 + ".jar", Lib16.class.getPackage().getName());
            TEST_LIB17_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB17 + ".jar", Lib17.class.getPackage().getName());
            TEST_LIB18_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB18 + ".jar", Lib18.class.getPackage().getName());

            TEST_EJB1_JAR = ShrinkHelper.buildJavaArchive(TEST_EJB1 + ".jar", InitBean1.class.getPackage().getName());
            TEST_EJB2_JAR = ShrinkHelper.buildJavaArchive(TEST_EJB2 + ".jar", InitBean2.class.getPackage().getName());
            TEST_EJB3_JAR = ShrinkHelper.buildJavaArchive(TEST_EJB3 + ".jar", InitBean3.class.getPackage().getName());

            TEST_CLASS_PATH1_WAR = ShrinkHelper.buildDefaultApp(TEST_CLASS_PATH1_APP + ".war",
                                                                       ClassPathDefaultLoaderServletTest1.class.getPackage().getName(),
                                                                       TestUtils.class.getPackage().getName());
            TEST_CLASS_PATH2_WAR = ShrinkHelper.buildDefaultApp(TEST_CLASS_PATH2_APP + ".war",
                                                                       ClassPathDefaultLoaderServletTest2.class.getPackage().getName(),
                                                                       TestUtils.class.getPackage().getName())
                            .addAsLibrary(TEST_EJB3_JAR);
            TEST_CLASS_PATH3_WAR = ShrinkHelper.buildDefaultApp(TEST_CLASS_PATH3_APP + ".war",
                                                                       ClassPathDefaultLoaderServletTest3.class.getPackage().getName(),
                                                                       TestUtils.class.getPackage().getName())
                            .addAsLibrary(TEST_LIB5_JAR)
                            .add(TEST_LIB11_JAR, "WEB-INF/", ZipExporter.class);

            TEST_LIB_FILESET_WAR = ShrinkHelper.buildDefaultApp(TEST_LIB_FILESET_APP + ".war",
                                                                        LibPathTestServlet.class.getPackage().getName(),
                                                                        TestUtils.class.getPackage().getName());

            TEST_PLATFORM_DELEGATION_WAR = ShrinkHelper.buildDefaultApp(TEST_PLATFORM_DELEGATION_APP + ".war",
                                                                PlatformDelegationTestServlet.class.getPackage().getName(),
                                                                TestUtils.class.getPackage().getName());

            TEST_NATIVE_LIBRARY_WAR = ShrinkHelper.buildDefaultApp(TEST_NATIVE_LIBRARY_APP + ".war",
                                                                NativeLibraryTestServlet.class.getPackage().getName(),
                                                                TestUtils.class.getPackage().getName());

            TEST_PARENT_LAST_LIBRARY_FEATURE_WAR = ShrinkHelper.buildDefaultApp(TEST_PARENT_LAST_LIBRARY_FEATURE_APP + ".war",
                                                                ParentLastFeatureMessageTestServlet.class.getPackage().getName());

            TEST_LIB_DELEGATION_PARENT_INCONSISTENT_WAR = ShrinkHelper.buildDefaultApp(TEST_PARENT_LIBRARY_INCONSISTENT_APP + ".war",
                                                                ParentLibraryInconsistentTestServlet.class.getPackage().getName());

            TEST_OVERRIDE_LIB_WAR = ShrinkHelper.buildDefaultApp(TEST_OVERRIDE_LIB_APP + ".war",
                                                                   OverrideLibraryTestServlet.class.getPackage().getName(), //
                                                                   TestUtils.class.getPackage().getName(), //
                                                                   AOverride.class.getPackage().getName(), //
                                                                   BOverride.class.getPackage().getName(), //
                                                                   COverride.class.getPackage().getName(), //
                                                                   DOverride.class.getPackage().getName(), //
                                                                   EOverride.class.getPackage().getName(), //
                                                                   FOverride.class.getPackage().getName(), //
                                                                   GOverride.class.getPackage().getName(), //
                                                                   HOverride.class.getPackage().getName() );

            TEST_LIB_PRECEDENCE_WAR = ShrinkHelper.buildDefaultApp(TEST_LIB_PRECEDENCE_APP + ".war",
                                                                   LibPrecedenceBeforeAppTestServlet.class.getPackage().getName(), //
                                                                   TestUtils.class.getPackage().getName());

            TEST_LIBRARY_USER_WAR = ShrinkHelper.buildDefaultApp(TEST_LIBRARY_USER_APP + ".war",
                                                                   LibraryUserTestServlet.class.getPackage().getName());

            // Single consolidated WAR: contains the servlet and the in-WAR impl classes.
            // Used across all dynamic feature lifecycle tests.
            TEST_DYNAMIC_FEATURE_WAR = ShrinkHelper.buildDefaultApp(TEST_DYNAMIC_FEATURE_APP + ".war",
                                                                     DynamicFeatureLifecycleTestServlet.class.getPackage().getName(),
                                                                     "io.openliberty.classloading.dynamic.feature.test.app");

            // Independent application with no dependency on the dynamic feature or its shared library.
            // Used to assert the unrelated app is NOT restarted when only the feature or library changes.
            TEST_DYNAMIC_FEATURE_UNRELATED_WAR = ShrinkHelper.buildDefaultApp(TEST_DYNAMIC_FEATURE_UNRELATED_APP + ".war",
                                                                              DynamicFeatureUnrelatedServlet.class.getPackage().getName());

            TEST_DYNAMIC_FEATURE_APP2_WAR = ShrinkHelper.buildDefaultApp(TEST_DYNAMIC_FEATURE_APP2 + ".war",
                                                                     DynamicFeatureLifecycleTestServlet.class.getPackage().getName(),
                                                                     "io.openliberty.classloading.dynamic.feature.test.app");

            // Shared library JAR: contains the four implementation classes for Tests 2 and 3.
            TEST_DYNAMIC_FEATURE_SHARED_LIB_JAR = ShrinkHelper.buildJavaArchive(TEST_DYNAMIC_FEATURE_SHARED_LIB + ".jar",
                                                                     LibraryFeatureApiImplState1.class.getPackage().getName());

            // Second shared library JAR: contains distinct implementation classes for multi-gateway test (S4).
            TEST_DYNAMIC_FEATURE_SHARED_LIB2_JAR = ShrinkHelper.buildJavaArchive(TEST_DYNAMIC_FEATURE_SHARED_LIB2 + ".jar",
                                                                     "io.openliberty.classloading.shared.feature.lib2");

            TEST_RESOURCE_ADAPTOR_JAR = ShrinkHelper.buildJavaArchive(TEST_RESOURCE_ADAPTOR + ".jar",
                                                                      TestResourceAdapter.class.getPackage().getName()).
                            add(TEST_LIB17_JAR, "/", ZipExporter.class);

            TEST_RAR1_RAR = ShrinkWrap.create(ResourceAdapterArchive.class, TEST_RAR1 + ".rar")
                            .addAsLibrary(TEST_RESOURCE_ADAPTOR_JAR)
                            .add(TEST_LIB12_JAR, "testlib/", ZipExporter.class);
            ShrinkHelper.addDirectory(TEST_RAR1_RAR, "test-applications/" + TEST_RAR1_RAR.getName() + "/resources/");

            TEST_DUMMY_RAR = ShrinkHelper.buildDefaultRar(TEST_DUMMY_RA, DummyResourceAdapter.class.getPackage().getName());

            TEST_CLIENT1_JAR = ShrinkHelper.buildJavaArchive(TEST_CLIENT1 + ".jar", ClientLib1.class.getPackage().getName());

            String testDynamicFeatureEarFileName = TEST_DYNAMIC_FEATURE_EAR_APP + ".ear";
            TEST_DYNAMIC_FEATURE_EAR = ShrinkWrap.create(EnterpriseArchive.class, testDynamicFeatureEarFileName)
                            .addAsModule(TEST_DYNAMIC_FEATURE_WAR)
                            .addAsModule(TEST_DYNAMIC_FEATURE_APP2_WAR);

            String testClassPathEarFileName = TEST_CLASS_PATH_APP + ".ear";
            TEST_CLASS_PATH_EAR = ShrinkWrap.create(EnterpriseArchive.class, testClassPathEarFileName)

                            .addAsModule(TEST_CLASS_PATH1_WAR)          // Class-Path: testLib1.jar testLib2.jar doesNotExistFrom_testClassPath1.war

                            .addAsModule(TEST_CLASS_PATH2_WAR)          // Class-Path: testLib2.jar testLib1.jar
                                                                        //    - includes WEB-INF/lib/testEJB3.jar

                            .addAsModule(TEST_CLASS_PATH3_WAR)          // Class-Path: testLib2.jar testEjb2.jar testLib13.jar lib/testLib18.jar
                                                                        //    - includes WEB-INF/lib/testLib5.jar - Class-Path: ../testLib11.jar doesNotExistFrom_testLib5.jar
                                                                        //    - includes WEB-INF/testLib11.jar

                            .addAsModule(TEST_CLIENT1_JAR)              // Class-Path: testLib15.jar

                            .addAsModule(TEST_LIB1_JAR)                 // REFERENCED-BY testClassPath1.war testClassPath2.war

                            .addAsModule(TEST_LIB2_JAR)                 // REFERENCED-BY testClassPath1.war testClassPath2.war testClassPath3.war
                                                                        // Class-Path: testLib3.jar

                            .addAsModule(TEST_LIB3_JAR)                 // REFERENCED-BY testLib2.jar

                            .addAsLibrary(TEST_LIB4_JAR)                // REFERENCED-BY-NOTHING - implicitly included by lib/
                                                                        // Class-Path: ../testLib6.jar doesNotExistFrom_testLib4.jar
                            .addAsLibrary(TEST_LIB18_JAR)               // REFERENCED-BY testClassPath3.war

                            .addAsModule(TEST_LIB6_JAR)                 // REFERENCED-BY  testLib4.jar
                                                                        // Class-Path: doesNotExistFrom_testLib6.jar

                            .addAsModule(TEST_LIB9_JAR)                 // REFERENCED-BY-NOTHING

                            .addAsModule(TEST_LIB10_JAR)                // REFERENCED-BY testRar1.rar

                            .addAsModule(TEST_LIB13_JAR)                // REFERENCED-BY testClassPath3.war

                            .addAsModule(TEST_LIB14_JAR)                // REFERENCED-BY testEjb1.jar

                            .addAsModule(TEST_LIB15_JAR)                // REFERENCED-BY testClient1.jar

                            .addAsModule(TEST_LIB16_JAR)                // REFERENCED-BY testResourceAdaptor.jar

                            .addAsModule(TEST_EJB1_JAR)                 // REFERENCED-BY-NOTHING - implicitly included because of EJBs
                                                                        // Class-Path: testLib14.jar

                            .addAsModule(TEST_EJB2_JAR)                 // REFERENCED-BY testClassPath3.war

                            .addAsModule(TEST_RAR1_RAR);                // REFERENCED-BY-NOTHING - implicitly included because of RARs
                                                                        // Class-Path: testLib10.jar doesNotExistFrom_testRar1.rar
                                                                        //    - includes:
                                                                        //         testlib/testList12.jar
                                                                        //         testResourceAdaptor.jar - Class-Path: testlib/testLib12.jar ../testLib16.jar doesNotExistFrom_testResourceAdaptor.jar
                                                                        //            - includes:
                                                                        //                 testLib17.jar
        } catch (Exception e) {
            throw (AssertionFailedError) new AssertionFailedError().initCause(e);
        }

    }

}
