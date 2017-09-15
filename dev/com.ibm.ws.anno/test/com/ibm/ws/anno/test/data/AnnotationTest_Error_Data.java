/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.data;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public class AnnotationTest_Error_Data {
    public static final String EAR_NAME = "ErrorTest.ear.unpacked";

    // Test plan (part 1):

    // Each test case uses a separate WAR, with the WAR name describing
    // the specific test.
    //
    // For each case, there is a mixture of valid and non-valid date.
    // The valid data must be read, while the non-valid data must
    // generate warnings, but must not cause a processing failure.
    //
    // Case 1: A WAR containing one valid jar and one non-valid jar.
    // Case 2: A WAR containing one valid jar, and one jar which contains one non-valid entry.
    // Case 3: A WAR contains a non-valid package.
    // Case 4: A WAR contains a mismatched package.
    // Case 5: A WAR contains a non-valid class.
    // Case 6: A WAR contains a mismatched class.
    //
    // Note: Case 2 is not currently implemented, due to problems defining the
    //       expected behavior.  (Defect 62551 was created to address case 2.)
    //
    // In all cases, the test data has four valid classes in four valid packages,
    // and with either one non-valid class or one non-valid package.
    //
    // The four valid classes are numbered 1,2,4,5; the four valid packages are
    // numbered 1,2,4,5.  The non-valid class is 3, and the non-valid package is 3.
    //
    // The class numbers were selected to put the non-valid data between valid data,
    // so to ensure that processing continued after encountering the non-valid data.
    //
    // For the non-valid jar case, the class and package data was partitioned into
    // subset1 (for numbers 1 and 2), subset2 (a non-valid jar), and subset3,
    // (for numbers 4 and 5).

    public static final String WAR_NAME_NONVALID_JAR = "nonValidJar.war.unpacked";
    public static final String WAR_NAME_NONVALID_JAR_ENTRY = "nonValidJarEntry.war.unpacked";

    public static final String WAR_NAME_NONVVALID_PACKAGE = "nonValidPackage.war.unpacked";
    public static final String WAR_NAME_PACKAGE_MISMATCH = "packageMismatch.war.unpacked";

    public static final String WAR_NAME_NONVALID_CLASS = "nonValidClass.war.unpacked";
    public static final String WAR_NAME_CLASS_MISMATCH = "classMismatch.war.unpacked";

    //

    public static final String NON_VALID_PACKAGE_NAME = "test.testpackage3";
    public static final String MISMATCHED_PACKAGE_NAME = "test.testpackage3";

    public static final String VALID_PACKAGE_NAME_1 = "test.testpackage1";
    public static final String VALID_PACKAGE_NAME_2 = "test.testpackage2";
    public static final String VALID_PACKAGE_NAME_4 = "test.testpackage4";
    public static final String VALID_PACKAGE_NAME_5 = "test.testpackage5";

    public static String[] VALID_PACKAGE_NAMES =
                { VALID_PACKAGE_NAME_1,
                  VALID_PACKAGE_NAME_2,
                  VALID_PACKAGE_NAME_4,
                  VALID_PACKAGE_NAME_5 };

    public static String[] NON_VALID_PACKAGE_NAMES =
                { NON_VALID_PACKAGE_NAME };

    public static final String NON_VALID_CLASS_NAME = "test.testpackage3.TestClass3";
    public static final String MISMATCHED_CLASS_NAME = "test.testpackage3.TestClass3";

    public static final String VALID_CLASS_NAME_1 = "test.testpackage1.TestClass1";
    public static final String VALID_CLASS_NAME_2 = "test.testpackage2.TestClass2";
    public static final String VALID_CLASS_NAME_4 = "test.testpackage4.TestClass4";
    public static final String VALID_CLASS_NAME_5 = "test.testpackage5.TestClass5";

    public static String[] VALID_CLASS_NAMES =
                { VALID_CLASS_NAME_1,
                  VALID_CLASS_NAME_2,
                  VALID_CLASS_NAME_4,
                  VALID_CLASS_NAME_5 };

    public static String[] NON_VALID_CLASS_NAMES =
                { NON_VALID_CLASS_NAME };

    //

    public static final String VALID_JAR_NAME = "validJar.jar";
    public static final String NONVALID_JAR_NAME = "nonValidJar.jar";

    // Test plan (part 2):

    // test.testpackage1.TestSuper_Broken
    // test.testpackage1.TestSub extends TestSuper
    //
    // 1) Artificial direct load of test.testpackage1.TestSuper_Broken
    // 2) Null direct load of test.testpackage1
    // 3) Successful load of test.testpackage1.TestSub
    // 4) Artificial for package test.testpackage1 of TestSub
    // 5) Artificial for superclass test.testpackage1.TestSuper_Broken of test.testpackage1.TestSub
    //
    // test.testpackage2.TestClass
    //
    // 6) Failed artificial load of test.testpackage2
    // 7) Successful load of test.testpackage2.TestClass
    // 8) Failed artificial for package test.testpackage2 of TestClass

    public static final String WAR_NAME_INFO_FAILURES = "infoFailures.war.unpacked";

    public static final String INFO_FAILURES_PACKAGE1 = "test.testpackage1";
    public static final String INFO_FAILURES_PACKAGE2 = "test.testpackage2";

    public static final String INFO_FAILURES_SUPERCLASS = "test.testpackage1.TestSuper_Broken";
    public static final String INFO_FAILURES_SUBCLASS = "test.testpackage1.TestSub";

    public static final String INFO_FAILURES_TESTCLASS = "test.testpackage2.TestClass";

    public static String[] INFO_VALID_PACKAGE_NAMES = {};
    public static String[] INFO_NON_VALID_PACKAGE_NAMES = { INFO_FAILURES_PACKAGE2 };

    public static String[] INFO_VALID_CLASS_NAMES = { INFO_FAILURES_SUBCLASS, INFO_FAILURES_TESTCLASS };
    public static String[] INFO_NON_VALID_CLASS_NAMES = { INFO_FAILURES_SUPERCLASS };

    //

    public static ClassSource_Specification_Direct_WAR createClassSourceSpecification(ClassSource_Factory classSourceFactory,
                                                                                      String projectPath,
                                                                                      String dataPath,
                                                                                      String warName) {

        ClassSource_Specification_Direct_WAR warSpecification = classSourceFactory.newWARSpecification();

        warSpecification.setImmediatePath(Common_Data.putIntoPath(projectPath, dataPath, warName));

        return warSpecification;
    }
}
