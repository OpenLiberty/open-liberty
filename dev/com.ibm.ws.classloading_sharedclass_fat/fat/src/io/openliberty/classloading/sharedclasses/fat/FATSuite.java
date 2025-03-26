/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.sharedclasses.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import junit.framework.AssertionFailedError;

@RunWith(Suite.class)
@SuiteClasses({
    AlwaysPassesTest.class,
    SharedClassesWarTest.class,
    SharedClassesServerLibTest.class,
    SharedClassesEarTest.class,
    SharedClassesServerLibExtractedTest.class,
    SharedClassesEarLooseTest.class
})
public class FATSuite {
    static final String SHARED_CLASSES_WAR_TEST_SERVER = "sharedClassesWarTest";
    static final String SHARED_CLASSES_EAR_TEST_SERVER = "sharedClassesEarTest";
    static final String SHARED_CLASSES_EAR_LOOSE_TEST_SERVER = "sharedClassesEarLooseTest";
    static final String SHARED_CLASSES_LIB_TEST_SERVER = "sharedClassesLibTest";

    static final String SHARED_CLASSES_LOOSE_WAR_TEST_SERVER = "sharedClassesLooseWarTest";
    static final String SHARED_CLASSES_LOOSE_EAR_TEST_SERVER = "sharedClassesLooseEarTest";
    static final String SHARED_CLASSES_LOOSE_LIB_TEST_SERVER = "sharedClassesLooseLibTest";

    public static final String SHARED_CLASSES_WAR_PATH = "/TestSharedClassesWar";
    public static final String SHARED_CLASSES_SERVER_LIB_PATH = "/TestSharedClassesServerLib";
    public static final String SHARED_CLASSES_EAR_PATH = "/TestSharedClassesEar";

    // ##### ARCHIVE NAMES #####
    // WAR archive names
    public static final String SHARED_CLASSES_WAR_NAME = SHARED_CLASSES_WAR_TEST_SERVER;

    // EJB archive names
    public static final String SHARED_CLASSES_EJB_NAME = "sharedClassesEJB";

    // Library archive names
    public static final String SHARED_CLASSES_WAR_LIB_NAME = "sharedClassesWarLib";
    public static final String SHARED_CLASSES_EAR_LIB_NAME = "sharedClassesEarLib";
    public static final String SHARED_CLASSES_EAR_LIB2_NAME = "sharedClassesEarLib2";
    public static final String SHARED_CLASSES_SERVER_LIB_NAME = "sharedClassesServerLib";

    // RAR inner jar archive names
    public static final String SHARED_CLASSES_RESOURCE_ADAPTOR_NAME = "sharedClassesResourceAdaptor";

    // RAR archive names
    public static final String SHARED_CLASSES_RAR_NAME = "sharedClassesRar";

    // Client archive names
    public static final String SHARED_CLASSES_CLIENT_NAME = "sharedClassesClient";

    // EAR archive names
    public static final String SHARED_CLASSES_EAR_NAME = SHARED_CLASSES_EAR_TEST_SERVER;

    // ##### SHRINK WRAP ARCHIVES #####
    // WAR archives
    static final WebArchive SHARED_CLASSES_WAR;

    // EJB archives
    static final JavaArchive SHARED_CLASSES_EJB;

    // Library archives
    static final JavaArchive SHARED_CLASSES_WAR_LIB;
    static final JavaArchive SHARED_CLASSES_EAR_LIB;
    static final JavaArchive SHARED_CLASSES_EAR_LIB2;
    static final JavaArchive SHARED_CLASSES_SERVER_LIB;

    // RAR inner JAR archives
    static final JavaArchive SHARED_CLASSES_RESOURCE_ADAPTOR;

    // RAR archives
    static final JavaArchive SHARED_CLASSES_RAR;

    // EAR archives
    static final EnterpriseArchive SHARED_CLASSES_EAR;

    static {
        try {
            SHARED_CLASSES_WAR_LIB = ShrinkHelper.buildJavaArchive(SHARED_CLASSES_WAR_LIB_NAME + ".jar", //
                                                                   io.openliberty.classloading.sharedclasses.warlib.a.A.class.getPackage().getName(), //
                                                                   io.openliberty.classloading.sharedclasses.warlib.b.B.class.getPackage().getName());
            SHARED_CLASSES_EAR_LIB = ShrinkHelper.buildJavaArchive(SHARED_CLASSES_EAR_LIB_NAME + ".jar", //
                                                                   io.openliberty.classloading.sharedclasses.earlib.a.A.class.getPackage().getName(), //
                                                                   io.openliberty.classloading.sharedclasses.earlib.b.B.class.getPackage().getName());
            SHARED_CLASSES_EAR_LIB2 = ShrinkHelper.buildJavaArchive(SHARED_CLASSES_EAR_LIB2_NAME + ".jar", //
                                                                   io.openliberty.classloading.sharedclasses.earlib2.a.A.class.getPackage().getName(), //
                                                                   io.openliberty.classloading.sharedclasses.earlib2.b.B.class.getPackage().getName());
            SHARED_CLASSES_SERVER_LIB = ShrinkHelper.buildJavaArchive(SHARED_CLASSES_SERVER_LIB_NAME + ".jar", //
                                                                    io.openliberty.classloading.sharedclasses.serverlib.a.A.class.getPackage().getName(), //
                                                                    io.openliberty.classloading.sharedclasses.serverlib.b.B.class.getPackage().getName());

            SHARED_CLASSES_EJB = ShrinkHelper.buildJavaArchive(SHARED_CLASSES_EJB_NAME + ".jar", //
                                                               io.openliberty.classloading.sharedclasses.ejb.TestEJB.class.getPackage().getName(), //
                                                               io.openliberty.classloading.sharedclasses.ejb.a.A.class.getPackage().getName(), //
                                                               io.openliberty.classloading.sharedclasses.ejb.b.B.class.getPackage().getName());

            SHARED_CLASSES_WAR = ShrinkHelper.buildDefaultApp(SHARED_CLASSES_WAR_NAME + ".war",
                                                              io.openliberty.classloading.sharedclasses.war.TestSharedClassesWar.class.getPackage().getName(), //
                                                              io.openliberty.classloading.sharedclasses.war.a.A.class.getPackage().getName(), //
                                                              io.openliberty.classloading.sharedclasses.war.b.B.class.getPackage().getName(), //
                                                              io.openliberty.classloading.sharedclasses.war.c.C.class.getPackage().getName())
                            .addAsLibrary(SHARED_CLASSES_WAR_LIB);


            SHARED_CLASSES_RESOURCE_ADAPTOR = ShrinkHelper.buildJavaArchive(SHARED_CLASSES_RESOURCE_ADAPTOR_NAME + ".jar", //
                                                                            io.openliberty.classloading.sharedclasses.resourceadaptor.TestResourceAdapter.class.getPackage().getName(), //
                                                                            io.openliberty.classloading.sharedclasses.resourceadaptor.a.A.class.getPackage().getName(), //
                                                                            io.openliberty.classloading.sharedclasses.resourceadaptor.b.B.class.getPackage().getName());

            // Using JavaArchive to create a RAR here because the ResourceAdapterArchive does not allow packages to be added directly
            SHARED_CLASSES_RAR = ShrinkWrap.create(JavaArchive.class, SHARED_CLASSES_RAR_NAME + ".rar")
                            .addPackage(io.openliberty.classloading.sharedclasses.rar.a.A.class.getPackage())
                            .addPackage(io.openliberty.classloading.sharedclasses.rar.b.B.class.getPackage())
                            .add(SHARED_CLASSES_RESOURCE_ADAPTOR, "/", ZipExporter.class);
            ShrinkHelper.addDirectory(SHARED_CLASSES_RAR, "test-resourceadapters/" + SHARED_CLASSES_RAR_NAME + "/resources/");

            String sharedClassesEarFileName = SHARED_CLASSES_EAR_NAME + ".ear";
            SHARED_CLASSES_EAR = ShrinkWrap.create(EnterpriseArchive.class, sharedClassesEarFileName)
                            .addAsModule(SHARED_CLASSES_WAR)
                            .addAsModule(SHARED_CLASSES_EJB)
                            .addAsLibrary(SHARED_CLASSES_EAR_LIB)
                            .addAsLibrary(SHARED_CLASSES_EAR_LIB2)
                            .addAsModule(SHARED_CLASSES_RAR);
        } catch (Exception e) {
            throw (AssertionFailedError) new AssertionFailedError().initCause(e);
        }
    }

    enum TestMethod {
        testWarClassesA(io.openliberty.classloading.sharedclasses.war.a.A.class),
        testWarClassesB(io.openliberty.classloading.sharedclasses.war.b.B.class),
        testWarClassesC(io.openliberty.classloading.sharedclasses.war.c.C.class, false /* no share url */),
        testWarLibA(io.openliberty.classloading.sharedclasses.warlib.a.A.class),
        testWarLibB(io.openliberty.classloading.sharedclasses.warlib.b.B.class),
        testServerLibClassesA(io.openliberty.classloading.sharedclasses.serverlib.a.A.class),
        testServerLibClassesB(io.openliberty.classloading.sharedclasses.serverlib.b.B.class),
        testEjbClassesA(io.openliberty.classloading.sharedclasses.ejb.a.A.class),
        testEjbClassesB(io.openliberty.classloading.sharedclasses.ejb.b.B.class),
        testEarLibA(io.openliberty.classloading.sharedclasses.earlib.a.A.class),
        testEarLibB(io.openliberty.classloading.sharedclasses.earlib.b.B.class),
        testEarLib2A(io.openliberty.classloading.sharedclasses.earlib2.a.A.class, false /* no share url */),
        testEarLib2B(io.openliberty.classloading.sharedclasses.earlib2.b.B.class),
        testResoureAdaptorClassesA(io.openliberty.classloading.sharedclasses.resourceadaptor.a.A.class),
        testResoureAdaptorClassesB(io.openliberty.classloading.sharedclasses.resourceadaptor.b.B.class),
        testRarClassesA(io.openliberty.classloading.sharedclasses.rar.a.A.class),
        testRarClassesB(io.openliberty.classloading.sharedclasses.rar.b.B.class);


        private final String className;
        private final boolean hasShareURL;
        TestMethod(Class<?> c) {
            this(c, true);
        }
        /**
         *
         */
        private TestMethod(Class<?> c, boolean hasShareURL) {
            this.className = c.getName();
            this.hasShareURL = hasShareURL;
        }

        String className() {
            return className;
        }

        boolean hasShareURL() {
            return hasShareURL;
        }
    }
}
