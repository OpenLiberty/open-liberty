/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.APP_PARENT_TEST_SERVER;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB6_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB7_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB8_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB9_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_PLATFORM_DELEGATION_WAR;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.JavaArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.app.JavaInfo;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.classloading.classpath.test.lib6.Lib6;
import io.openliberty.classloading.classpath.test.lib7.Lib7;
import io.openliberty.classloading.classpath.test.lib8.Lib8;
import io.openliberty.classloading.classpath.test.lib9.Lib9;

public abstract class AppParentDelegationAbstractTest extends FATServletClient {
    public static final String CONFIG_APP_PARENT_PROP = "io.openliberty.classloading.app.parent";
    public static final String CONFIG_APP_PARENT_PACKAGES_PROP = "io.openliberty.classloading.app.parent.packages";
    public static final String CONFIG_APP_PARENT_SYSTEM = "SYSTEM";
    public static final String CONFIG_APP_PARENT_PLATFORM = "PLATFORM";

    public static final String LOAD_CLASS_FILTERED_MSG = "loadClass: filtered class load from gateway parent: ";
    public static final String LOAD_CLASS = "loadClass: ";
    public static final String LOAD_CLASS_NOT_FILTERED_MSG = "loadClass: loading class from gateway parent: ";
    public static final String FIND_RESOURCE = "getResource: ";
    public static final String FIND_RESOURCE_FILTERED_MSG = "getResource: filtered get resource from gateway parent: ";
    public static final String FIND_RESOURCE_NOT_FILTERED_MSG = "getResource: getting resource from gateway parent: ";
    public static final String FIND_RESOURCES_FILTERED_MSG = "getResources: filtered get resources from gateway parent: ";
    public static final String FIND_RESOURCES_NOT_FILTERED_MSG ="getResources: getting resources from gateway parent: ";

    public static enum CheckTrace {
        testLoadLibrary6Class_NoFilter8_Filter9(
                                                Lib6.class.getName(),
                                                LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_FILTERED_MSG),

        testLoadLibrary7Class_NoFilter8_Filter9(
                                                Lib7.class.getName(),
                                                LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_FILTERED_MSG),

        testLoadLibrary7Class_NoFilter8_NoFilter9(
                                                Lib7.class.getName(),
                                                LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_NOT_FILTERED_MSG),

        testLoadLibrary8Class_NoFilter8_Filter9(
                                                Lib8.class.getName(),
                                                LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_FILTERED_MSG),

        testLoadLibrary9Class_NoFilter8_Filter9(
                                                Lib9.class.getName(),
                                                LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_FILTERED_MSG),

        testLoadLibrary9Class_NoFilter8_NoFilter9(
                                                Lib9.class.getName(),
                                                LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_NOT_FILTERED_MSG),

        testGetCommonResource_NoFilter8_Filter9(
                                                "io/openliberty/classloading/test/resources/common.properties",
                                                FIND_RESOURCE_NOT_FILTERED_MSG, FIND_RESOURCE_FILTERED_MSG),

        testGetCommonResource_NoFilter8_NoFilter9(
                                                "io/openliberty/classloading/test/resources/common.properties",
                                                FIND_RESOURCE_NOT_FILTERED_MSG, FIND_RESOURCE_NOT_FILTERED_MSG),

        testGetCommonResourcesOrder_NoFilter8_Filter9(
                                                "io/openliberty/classloading/test/resources/common.properties",
                                                FIND_RESOURCES_NOT_FILTERED_MSG, FIND_RESOURCES_FILTERED_MSG),

        testGetCommonResourcesOrder_NoFilter8_NoFilter9(
                                                "io/openliberty/classloading/test/resources/common.properties",
                                                FIND_RESOURCES_NOT_FILTERED_MSG, FIND_RESOURCES_NOT_FILTERED_MSG),

        testGetPlatformResourceDoesExist("java/lang/String.class", FIND_RESOURCE, FIND_RESOURCE, true),

        testGetPlatformResourceDoesNotExist_NoFilter8_Filter9(
                                                              "java/lang/platform-delegation-test.txt",
                                                              FIND_RESOURCE_NOT_FILTERED_MSG, FIND_RESOURCE_FILTERED_MSG),

        testGetPlatformResourcesDoesExist_NoFilter8_Filter9(
                                                            "java/lang/String.class",
                                                            FIND_RESOURCES_NOT_FILTERED_MSG, FIND_RESOURCES_FILTERED_MSG,
                                                            "testGetPlatformResourcesDoesExist:",
                                                            "count=2", "count=1"),

        testGetPlatformResourcesDoesNotExist_NoFilter8_Filter9(
                                                              "java/lang/platform-delegation-test.txt",
                                                              FIND_RESOURCES_NOT_FILTERED_MSG, FIND_RESOURCES_FILTERED_MSG),

        testLoadPlatformXAException("javax.transaction.xa.XAException",
                                    LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_NOT_FILTERED_MSG,
                                    "testLoadPlatformXAException:",
                                    "CLASS FOUND", "CLASS FOUND"),

        testLoadPlatformClassDoesExist(
                        "java.util.concurrent.atomic.AtomicReferenceArray", LOAD_CLASS, LOAD_CLASS, true),

        testLoadPlatformClassDoesNotExist_NoFilter8_Filter9(
                                                            "java.lang.PlatformDelegationTest",
                                                            LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_FILTERED_MSG),

        testLoadKernelClass_Found_NoFilter8_NoFilter9(
                                                            "com.ibm.wsspi.kernel.embeddable.ServerBuilder",
                                                            LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_NOT_FILTERED_MSG,
                                                            "testLoadKernelClass:",
                                                            "CLASS FOUND", "CLASS FOUND"),

        testLoadKernelClass_NotFound_NoFilter8_NoFilter9(
                                                      "com.ibm.wsspi.kernel.embeddable.ServerBuilder",
                                                      LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_NOT_FILTERED_MSG,
                                                      "testLoadKernelClass:",
                                                      "CLASS NOT FOUND", "CLASS NOT FOUND"),

        testLoadKernelClass_NotFound_NoFilter8_Filter9(
                                                         "com.ibm.wsspi.kernel.embeddable.ServerBuilder",
                                                         LOAD_CLASS_NOT_FILTERED_MSG, LOAD_CLASS_FILTERED_MSG,
                                                         "testLoadKernelClass:",
                                                         "CLASS NOT FOUND", "CLASS NOT FOUND"),

        testPlatformService;

        private final String traceTarget;
        private final String traceMsg9Plus;
        private final String traceMsg8;
        private final String secondaryTarget;
        private final String secondaryMsg9Plus;
        private final String secondaryMsg8;
        private final boolean negativeTest;

        private CheckTrace() {
            this(null, null, null, false);
        }
        private CheckTrace(
                           String traceTarget,
                           String traceMsg8, String traceMsg9Plus) {
            this(traceTarget, traceMsg8, traceMsg9Plus, false);
        }
        private CheckTrace(
                           String traceTarget,
                           String traceMsg8, String traceMsg9Plus,
                           boolean negativeTest) {
            this(
                 traceTarget, traceMsg8, traceMsg9Plus,
                 null, null, null, negativeTest);
        }

        private CheckTrace(
                           String traceTarget,
                           String traceMsg8, String traceMsg9Plus,
                           String secondaryTarget,
                           String secondaryMsg8, String secondaryMsg9Plus) {
            this(traceTarget, traceMsg8, traceMsg9Plus,
                 secondaryTarget, secondaryMsg8, secondaryMsg9Plus,
                 false);
        }

        private CheckTrace(
                           String traceTarget,
                           String traceMsg8, String traceMsg9Plus,
                           String secondaryTarget,
                           String secondaryMsg8, String secondaryMsg9Plus,
                           boolean negativeTest) {

            this.traceTarget = traceTarget;
            this.traceMsg8 = traceMsg8;
            this.traceMsg9Plus = traceMsg9Plus;
            this.secondaryTarget = secondaryTarget;
            this.secondaryMsg8 = secondaryMsg8;
            this.secondaryMsg9Plus = secondaryMsg9Plus;
            this.negativeTest = negativeTest;
        }

        public void test(LibertyServer server) throws Exception {
            if (traceTarget != null) {
                if (negativeTest) {
                    checkNegativeTrace(server, this, JavaInfo.JAVA_VERSION >= 9 ? traceMsg9Plus : traceMsg8, traceTarget);
                } else {
                    checkTrace(server, this, JavaInfo.JAVA_VERSION >= 9 ? traceMsg9Plus : traceMsg8, traceTarget);
                }
            }
            if (secondaryTarget != null) {
                if (negativeTest) {
                    checkNegativeTrace(server, this, JavaInfo.JAVA_VERSION >= 9 ? secondaryMsg9Plus : secondaryMsg8, secondaryTarget);
                } else {
                    checkTrace(server, this, JavaInfo.JAVA_VERSION >= 9 ? secondaryMsg9Plus : secondaryMsg8, secondaryTarget);
                }
            }
        }
    }

    public static void setAppParent(LibertyServer server, String parent, String packages) throws Exception {
        Map<String, String> bootstrapProps = new HashMap<>();
        if (parent != null) {
            bootstrapProps.put(CONFIG_APP_PARENT_PROP, parent);
        }
        if (packages != null) {
            bootstrapProps.put(CONFIG_APP_PARENT_PACKAGES_PROP, packages);
        }
        if (parent != null || packages != null) {
            server.addBootstrapProperties(bootstrapProps);
            Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
            jvmOptions.put("-Dcom.ibm.ws.beta.edition", "true");
            server.setJvmOptions(jvmOptions);
        }
    }

    public static void setupTestServer(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, TEST_PLATFORM_DELEGATION_WAR, DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB6_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportToServer(server, "/libs", TEST_LIB7_JAR, DeployOptions.SERVER_ONLY);
        setupLibraryFolder(TEST_LIB8_JAR, server);
        setupLibraryFolder(TEST_LIB9_JAR, server);

        server.startServer();
    }

    private static void setupLibraryFolder(JavaArchive library, LibertyServer server) throws Exception {
        ShrinkHelper.exportArtifact(library, "publish/libs", true, false, true);
        String libJarName = library.getName();
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/usr/servers/" + APP_PARENT_TEST_SERVER + "/libs",
                                               libJarName.substring(0, libJarName.length() - 4),
                                               "publish/libs/" + libJarName, true, server.getServerRoot());
    }

    public static void checkTrace(LibertyServer server, CheckTrace checkTrace, String expectedTraceMsg, String expectedTarget) throws Exception {
        Iterator<String> traceLines = server.findStringsInLogsAndTrace(".*").iterator();
        while (traceLines.hasNext()) {
            String line = traceLines.next();
            if (line.contains(expectedTraceMsg) && line.contains(expectedTarget)) {
                return;
            }
        }
        fail(checkTrace + ": Did not find the expected trace message '" + expectedTraceMsg + "' for target: " + expectedTarget);
    }

    public static void checkNegativeTrace(LibertyServer server, CheckTrace checkTrace, String expectedTraceMsg, String expectedTarget) throws Exception {
        Iterator<String> traceLines = server.findStringsInLogsAndTrace(".*").iterator();
        while (traceLines.hasNext()) {
            String line = traceLines.next();
            if (line.contains(expectedTraceMsg) && line.contains(expectedTarget)) {
                fail(checkTrace + ": Did not expect to find trace message '" + expectedTraceMsg + "' for target: " + expectedTarget);
            }
        }
    }
}
