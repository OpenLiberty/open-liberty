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

import io.openliberty.classloading.classpath.test.ejb1.InitBean1;
import io.openliberty.classloading.classpath.test.lib1.Lib1;
import io.openliberty.classloading.classpath.test.lib12.Lib12;
import io.openliberty.classloading.classpath.test.lib17.Lib17;
import io.openliberty.classloading.classpath.test.lib2.Lib2;
import io.openliberty.classloading.classpath.test.rar1.TestResourceAdapter;
import io.openliberty.classloading.libs.util.CodeSourceUtil;
import io.openliberty.classloading.trace.lib.TestLibraryClass;
import io.openliberty.classloading.trace.war.TraceTestServlet;
import junit.framework.AssertionFailedError;
import test.bundle.api1.a.API_A1;
import test.bundle.api1.b.API_B1;
import test.bundle.api1.c.API_C1;
import test.bundle.api2.a.API_A2;
import test.bundle.api2.b.API_B2;
import test.bundle.api2.c.API_C2;

@RunWith(Suite.class)
@SuiteClasses({
    io.openliberty.classloading.trace.fat.LibraryClassLoadingTraceTest.class
})
public class FATSuite {

    // ##### ARCHIVE NAMES #####
    // WAR archive names
    public static final String TRACE_TEST_APP = "traceTest";
    public static final String TRACE_TEST_EAR_APP = "traceTestEar";
    public static final String TRACE_TEST_LIB = "traceTestLib";

    // EJB archive names
    public static final String TEST_EJB1 = "testEjb1";

    // Library archive names
    public static final String TEST_LIB1 = "testLib1";
    public static final String TEST_LIB2 = "testLib2";
    public static final String TEST_LIB12 = "testLib12";
    public static final String TEST_LIB17 = "testLib17";


    // RAR inner jar archive names
    public static final String TEST_RESOURCE_ADAPTOR = "testResourceAdaptor";

    // RAR archive names
    public static final String TEST_RAR1 = "testRar1";
    public static final String TEST_DUMMY_RA = "testDummyRA";

    // EAR archive names
    static final String TEST_CLASS_PATH_APP = "testClassPath";

    // ##### LIBRARY CLASS NAMES #####
    // EJB library class names
    public static final String EJB_LIB1_CLASS_NAME = "io.openliberty.classloading.classpath.test.ejb1.EjbLib1";

    // Library class names
    public static final String LIB1_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib1.Lib1";
    public static final String LIB2_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib2.Lib2";
    public static final String LIB12_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib12.Lib12";
    public static final String LIB17_CLASS_NAME = "io.openliberty.classloading.classpath.test.lib17.Lib17";

    // RAR library class names
    public static final String RAR_LIB1_CLASS_NAME = "io.openliberty.classloading.classpath.test.rar1.RarLib1";
    public static final String RAR_LIB2_CLASS_NAME = "io.openliberty.classloading.classpath.test.rar1.RarLib2";

    // ##### SHRINK WRAP ARCHIVES #####

    // EJB archives
    static final JavaArchive TEST_EJB1_JAR;

    // Library archives
    static final JavaArchive TEST_LIB1_JAR;
    static final JavaArchive TEST_LIB2_JAR;
    static final JavaArchive TEST_LIB12_JAR;
    static final JavaArchive TEST_LIB17_JAR;

    // RAR inner JAR archives
    static final JavaArchive TEST_RESOURCE_ADAPTOR_JAR;

    // RAR archives
    static final ResourceAdapterArchive TEST_RAR1_RAR;

    // Trace test archives
    static final JavaArchive TRACE_TEST_LIB_JAR;
    static final WebArchive TRACE_TEST_WAR;
    public static final EnterpriseArchive TRACE_TEST_EAR;

    static {
        try {

            TEST_LIB1_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB1 + ".jar", Lib1.class.getPackage().getName(), //
                                                          CodeSourceUtil.class.getPackage().getName(), //
                                                          API_A1.class.getPackage().getName(), //
                                                          API_B1.class.getPackage().getName(), //
                                                          API_C1.class.getPackage().getName());
            TEST_LIB2_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB2 + ".jar", Lib2.class.getPackage().getName(), //
                                                          API_A2.class.getPackage().getName(), //
                                                          API_B2.class.getPackage().getName(), //
                                                          API_C2.class.getPackage().getName());
            TEST_LIB12_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB12 + ".jar", Lib12.class.getPackage().getName());
            TEST_LIB17_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB17 + ".jar", Lib17.class.getPackage().getName());

            TEST_EJB1_JAR = ShrinkHelper.buildJavaArchive(TEST_EJB1 + ".jar", InitBean1.class.getPackage().getName());

            TEST_RESOURCE_ADAPTOR_JAR = ShrinkHelper.buildJavaArchive(TEST_RESOURCE_ADAPTOR + ".jar",
                                                                      TestResourceAdapter.class.getPackage().getName()).
                            add(TEST_LIB17_JAR, "/", ZipExporter.class);

            TEST_RAR1_RAR = ShrinkWrap.create(ResourceAdapterArchive.class, TEST_RAR1 + ".rar")
                            .addAsLibrary(TEST_RESOURCE_ADAPTOR_JAR)
                            .add(TEST_LIB12_JAR, "testlib/", ZipExporter.class);
            ShrinkHelper.addDirectory(TEST_RAR1_RAR, "test-applications/" + TEST_RAR1_RAR.getName() + "/resources/");


            // Build trace test archives
            TRACE_TEST_LIB_JAR = ShrinkHelper.buildJavaArchive(TRACE_TEST_LIB + ".jar",
                                                               TestLibraryClass.class.getPackage().getName());

            TRACE_TEST_WAR = ShrinkHelper.buildDefaultApp(TRACE_TEST_APP + ".war",
                                                          TraceTestServlet.class.getPackage().getName());

            // Create complex EAR with multiple modules for trace testing
            String traceTestEarFileName = TRACE_TEST_EAR_APP + ".ear";
            TRACE_TEST_EAR = ShrinkWrap.create(EnterpriseArchive.class, traceTestEarFileName)

                            .addAsModule(TRACE_TEST_WAR)                // Main WAR with servlet

                            .addAsLibrary(TRACE_TEST_LIB_JAR)           // Library JAR with TestLibraryClass in lib/
                                                                        // Accessible to all modules in EAR

                            .addAsLibrary(TEST_LIB1_JAR)                // Library in lib/ directory - accessible to WAR

                            .addAsModule(TEST_EJB1_JAR)                 // EJB module for enterprise testing

                            .addAsLibrary(TEST_LIB2_JAR)                // Library in lib/ directory

                            .addAsModule(TEST_RAR1_RAR);                // Resource adapter for comprehensive testing

        } catch (Exception e) {
            throw (AssertionFailedError) new AssertionFailedError().initCause(e);
        }

    }

}
