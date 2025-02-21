/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import io.openliberty.classloading.lib.path.test.app.LibPathTestServlet;
import junit.framework.AssertionFailedError;

@RunWith(Suite.class)
@SuiteClasses({
    ClassPathWarLoaderTests.class,
    ClassPathDefaultLoaderDropinsTests.class,
    ClassPathDefaultLoaderTests.class,
    ClassPathEarLoaderTests.class,
    ClassPathDefaultLoaderLibraryTests.class,
    ClassPathInvalidLoaderTests.class,
    LibraryPathTest.class
})
public class FATSuite {
    static final String CLASSPATH_TEST_WAR_LOADER_SERVER = "classpathTestWarLoader";
    static final String CLASSPATH_TEST_DEFAULT_LOADER_SERVER = "classpathTestDefaultLoader";
    static final String CLASSPATH_TEST_DEFAULT_LOADER_DROPINS_SERVER = "classpathTestDefaultLoaderDropins";
    static final String CLASSPATH_TEST_INVALID_LOADER_SERVER = "classpathTestInvalidLoader";
    static final String CLASSPATH_TEST_EAR_LOADER_SERVER = "classpathTestEarLoader";
    static final String PRIVATE_LIBRARY_TEST_SERVER = "privateLibraryTest";
    static final String LIB_FILESET_TEST_SERVER = "libPathTest";

    // ##### ARCHIVE NAMES #####
    // WAR archive names
    public static final String TEST_CLASS_PATH1_APP = "testClassPath1";
    public static final String TEST_CLASS_PATH2_APP = "testClassPath2";
    public static final String TEST_CLASS_PATH3_APP = "testClassPath3";
    public static final String TEST_LIB_FILESET_APP = "testLibFileSet";

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

    // RAR inner jar archive names
    public static final String TEST_RESOURCE_ADAPTOR = "testResourceAdaptor";

    // RAR archive names
    public static final String TEST_RAR1 = "testRar1";

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

    // RAR inner JAR archives
    static final JavaArchive TEST_RESOURCE_ADAPTOR_JAR;

    // RAR archives
    static final ResourceAdapterArchive TEST_RAR1_RAR;

    // Client archives
    static final JavaArchive TEST_CLIENT1_JAR;

    // EAR archives
    static final EnterpriseArchive TEST_CLASS_PATH_EAR;
    static {
        try {

            TEST_LIB1_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB1 + ".jar", Lib1.class.getPackage().getName());
            TEST_LIB2_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB2 + ".jar", Lib2.class.getPackage().getName());
            TEST_LIB3_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB3 + ".jar", Lib3.class.getPackage().getName());
            TEST_LIB4_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB4 + ".jar", Lib4.class.getPackage().getName());
            TEST_LIB5_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB5 + ".jar", Lib5.class.getPackage().getName());
            TEST_LIB6_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB6 + ".jar", Lib6.class.getPackage().getName());
            TEST_LIB7_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB7 + ".jar", Lib7.class.getPackage().getName());
            TEST_LIB8_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB8 + ".jar", Lib8.class.getPackage().getName());
            TEST_LIB9_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB9 + ".jar", Lib9.class.getPackage().getName());
            TEST_LIB10_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB10 + ".jar", Lib10.class.getPackage().getName());
            TEST_LIB11_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB11 + ".jar", Lib11.class.getPackage().getName());
            TEST_LIB12_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB12 + ".jar", Lib12.class.getPackage().getName());
            TEST_LIB13_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB13 + ".jar", Lib13.class.getPackage().getName());
            TEST_LIB14_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB14 + ".jar", Lib14.class.getPackage().getName());
            TEST_LIB15_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB15 + ".jar", Lib15.class.getPackage().getName());
            TEST_LIB16_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB16 + ".jar", Lib16.class.getPackage().getName());
            TEST_LIB17_JAR = ShrinkHelper.buildJavaArchive(TEST_LIB17 + ".jar", Lib17.class.getPackage().getName());

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

            TEST_RESOURCE_ADAPTOR_JAR = ShrinkHelper.buildJavaArchive(TEST_RESOURCE_ADAPTOR + ".jar",
                                                                      TestResourceAdapter.class.getPackage().getName()).
                            add(TEST_LIB17_JAR, "/", ZipExporter.class);

            TEST_RAR1_RAR = ShrinkWrap.create(ResourceAdapterArchive.class, TEST_RAR1 + ".rar")
                            .addAsLibrary(TEST_RESOURCE_ADAPTOR_JAR)
                            .add(TEST_LIB12_JAR, "testlib/", ZipExporter.class);
            ShrinkHelper.addDirectory(TEST_RAR1_RAR, "test-applications/" + TEST_RAR1_RAR.getName() + "/resources/");

            TEST_CLIENT1_JAR = ShrinkHelper.buildJavaArchive(TEST_CLIENT1 + ".jar", ClientLib1.class.getPackage().getName());

            String testClassPathEarFileName = TEST_CLASS_PATH_APP + ".ear";
            TEST_CLASS_PATH_EAR = ShrinkWrap.create(EnterpriseArchive.class, testClassPathEarFileName)

                            .addAsModule(TEST_CLASS_PATH1_WAR)          // Class-Path: testLib1.jar testLib2.jar doesNotExistFrom_testClassPath1.war

                            .addAsModule(TEST_CLASS_PATH2_WAR)          // Class-Path: testLib2.jar testLib1.jar
                                                                        //    - includes WEB-INF/lib/testEJB3.jar

                            .addAsModule(TEST_CLASS_PATH3_WAR)          // Class-Path: testLib2.jar testEjb2.jar testLib13.jar
                                                                        //    - includes WEB-INF/lib/testLib5.jar - Class-Path: ../testLib11.jar doesNotExistFrom_testLib5.jar
                                                                        //    - includes WEB-INF/testLib11.jar

                            .addAsModule(TEST_CLIENT1_JAR)              // Class-Path: testLib15.jar

                            .addAsModule(TEST_LIB1_JAR)                 // REFERENCED-BY testClassPath1.war testClassPath2.war

                            .addAsModule(TEST_LIB2_JAR)                 // REFERENCED-BY testClassPath1.war testClassPath2.war testClassPath3.war
                                                                        // Class-Path: testLib3.jar

                            .addAsModule(TEST_LIB3_JAR)                 // REFERENCED-BY testLib2.jar

                            .addAsLibrary(TEST_LIB4_JAR)                // REFERENCED-BY-NOTHING - implicitly included by lib/
                                                                        // Class-Path: ../testLib6.jar doesNotExistFrom_testLib4.jar

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
