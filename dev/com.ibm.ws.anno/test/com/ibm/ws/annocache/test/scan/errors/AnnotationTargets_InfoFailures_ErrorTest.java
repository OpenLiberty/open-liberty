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
package com.ibm.ws.annocache.test.scan.errors;

import org.junit.Test;

import com.ibm.ws.annocache.test.scan.Test_Base_Result;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.InfoStoreException;
import com.ibm.wsspi.annocache.info.PackageInfo;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Fault;

public class AnnotationTargets_InfoFailures_ErrorTest extends AnnotationTargets_TestBase_ScanError {

    @Override
    public String getModName() {
        return AnnotationTargets_ErrorData.WAR_NAME_INFO_FAILURES;
    }

    @Override
    public String getModSimpleName() {
        return AnnotationTargets_ErrorData.WAR_SIMPLE_NAME_INFO_FAILURES;
    }

    @Test
    public void testAnnotationTest_Error_InfoFailures() throws Exception {
        runSuiteTest( getBaseCase() ); // 'runSuiteTest' throws Exception
    }

    @Override
    public String[] getValidPackageNames() {
        return AnnotationTargets_ErrorData.INFO_VALID_PACKAGE_NAMES;
    }

    @Override
    public String[] getNonValidPackageNames() {
        return AnnotationTargets_ErrorData.INFO_NON_VALID_PACKAGE_NAMES;
    }

    @Override
    public String[] getValidClassNames() {
        return AnnotationTargets_ErrorData.INFO_VALID_CLASS_NAMES;
    }

    @Override
    public String[] getNonValidClassNames() {
        return AnnotationTargets_ErrorData.INFO_NON_VALID_CLASS_NAMES;
    }

    public static final boolean IS_ARTIFICIAL = true;
    public static final boolean IS_NOT_ARTIFICIAL = false;

    public ClassInfo validateClassInfo(String className, boolean isArtificial) {
    	Test_Base_Result useResults = getBaseResults();

        ClassInfo classInfo = getClassInfo(className);
        if ( classInfo == null ) {
            AnnotationTargets_Fault fault = createFault("Class [ {0} ] could not be loaded!", className);
            useResults.addFault(fault);

            println(fault.getResolvedText());

            return null;
        }

        println("Loaded class [ " + className + " ]");

        if ( classInfo.isArtificial() != isArtificial ) {
            AnnotationTargets_Fault fault =
                createFault("Class [ {0} ] isArtificial actual [ {1} ] does not match expected [ {2} ]",
                            new String[] { className,
                                           Boolean.toString(classInfo.isArtificial()),
                                           Boolean.toString(isArtificial) });
            useResults.addFault(fault);
            println(fault.getResolvedText());

        } else {
            println("Class [ " + className + " ] isArtificial [ " + Boolean.toString(isArtificial) + " ] is correct");
        }

        return classInfo;
    }

    public static final boolean IS_NULL = true;
    public static final boolean IS_NOT_NULL = false;

    public static final boolean IS_FAILED = true;
    public static final boolean IS_NOT_FAILED = false;

    public PackageInfo validatePackageInfo(
        String packageName,
        boolean isNull, boolean isFailed, boolean isArtificial) {

        PackageInfo packageInfo = getPackageInfo(packageName);

        validatePackageInfo(
            packageName, isNull, isFailed, isArtificial,
            packageInfo);

        return packageInfo;
    }

    public void validatePackageInfo(
        String packageName,
        boolean isNull, boolean isFailed, boolean isArtificial,
        PackageInfo packageInfo) {

    	Test_Base_Result useResults = getBaseResults();
    	
        if ( isNull ) {
            if ( packageInfo != null ) {
                AnnotationTargets_Fault fault =
                    createFault("Loaded non-valid package [ {0} ]", packageName);
                useResults.addFault(fault);
                println(fault.getResolvedText());

            } else {
                println("Failed to load non-valid package [ " + packageName + " ]");
            }

        } else {
            if ( packageInfo == null ) {
                AnnotationTargets_Fault fault =
                    createFault("Failed to load valid package [ {0} ]", packageName);
                useResults.addFault(fault);
                println(fault.getResolvedText());

            } else {
                println("Loaded valid package [ " + packageName + " ]");

                if ( packageInfo.getForFailedLoad() != isFailed ) {
                    AnnotationTargets_Fault fault =
                        createFault("Package [ {0} ] isFailed actual [ {1} ] expected [ {2} ]",
                                    new String[] { packageName,
                                                   Boolean.toString(packageInfo.getForFailedLoad()),
                                                   Boolean.toString(isFailed) });
                    useResults.addFault(fault);
                    println(fault.getResolvedText());
                } else {
                    println("Package [ " + packageName + " ] isFailed [ " + Boolean.toString(isFailed) + " ]");
                }

                if ( packageInfo.getIsArtificial() != isArtificial ) {
                    AnnotationTargets_Fault fault =
                        createFault("Package [ {0} ] isArtificial actual [ {1} ] expected [ {2} ]",
                                    new String[] { packageName,
                                                   Boolean.toString(packageInfo.getIsArtificial()),
                                                   Boolean.toString(isArtificial) });
                    useResults.addFault(fault);
                    println(fault.getResolvedText());
                } else {
                    println("Package [ " + packageName + " ] isArtificial [ " + Boolean.toString(isArtificial) + " ]");
                }
            }
        }
    }

    public static final String PACKAGE1_NAME = AnnotationTargets_ErrorData.INFO_FAILURES_PACKAGE1;
    public static final String PACKAGE2_NAME = AnnotationTargets_ErrorData.INFO_FAILURES_PACKAGE2;

    public static final String SUPERCLASS_BROKEN_NAME = AnnotationTargets_ErrorData.INFO_FAILURES_SUPERCLASS_BROKEN;
    public static final String SUBCLASS_NAME = AnnotationTargets_ErrorData.INFO_FAILURES_SUBCLASS;
    public static final String TESTCLASS_NAME = AnnotationTargets_ErrorData.INFO_FAILURES_TESTCLASS;

    @SuppressWarnings("unused")
	@Test
    public void validateInfoStore()
        throws InfoStoreException {

    	Test_Base_Result useResults = getBaseResults();

        // No 'package-info' for package1: An initial load returns null.

        PackageInfo packageInfo1_direct_try1 = validatePackageInfo(
            PACKAGE1_NAME, IS_NULL, IS_NOT_FAILED, IS_NOT_ARTIFICIAL);

        // The superclass is not valid: A direct load returns an artificial class.

        ClassInfo superclassInfo_direct =
            validateClassInfo(SUPERCLASS_BROKEN_NAME, IS_ARTIFICIAL);

        // The subclass is valid: A direct load is successful.

        ClassInfo subclassInfo_direct =
            validateClassInfo(SUBCLASS_NAME, IS_NOT_ARTIFICIAL);

        if ( subclassInfo_direct != null ) {
            // While there is no "package-info" for the package of the test subclass,
            // an associated artificial package object is still generated.

            PackageInfo packageInfo1_indirect = subclassInfo_direct.getPackage();
            validatePackageInfo(
                PACKAGE1_NAME, IS_NOT_NULL, IS_NOT_FAILED, IS_ARTIFICIAL,
                packageInfo1_indirect);

            // The superclass is not valid: Retrieve generates an artificial class.

            ClassInfo superclassInfo_indirect = subclassInfo_direct.getSuperclass();

            if ( superclassInfo_indirect == null ) {
                AnnotationTargets_Fault fault =
                    createFault("Failed to generate superclass [ {0} ] of [ {1} ]",
                                new String[] { SUPERCLASS_BROKEN_NAME, SUBCLASS_NAME });
                useResults.addFault(fault);
                println(fault.getResolvedText());

            } else {
                println("Generated superclass [ " + SUPERCLASS_BROKEN_NAME + " ] of [ " + SUBCLASS_NAME + " ]");

                if ( !superclassInfo_indirect.isArtificial() ) {
                    AnnotationTargets_Fault fault =
                        createFault("Generated superclass [ {0} ] of [ {1} ] not marked as artificial",
                                    new String[] { SUPERCLASS_BROKEN_NAME, SUBCLASS_NAME });
                    useResults.addFault(fault);
                    println(fault.getResolvedText());

                } else {
                    println("Generated superclass [ " + SUPERCLASS_BROKEN_NAME + " ] of [ " + SUBCLASS_NAME + " ] marked as artificial");
                }
            }
        }

        // After the indirect access to the test package, despite there being no "package-info",
        // the artificial package info is retained.

        PackageInfo packageInfo1_direct_try2 = validatePackageInfo(
            PACKAGE1_NAME, IS_NOT_NULL, IS_NOT_FAILED, IS_ARTIFICIAL);

        // There *is* "package-info" for package 2.  However, it does not load.
        //
        // An artificial, "failed" package info is generated.

        PackageInfo packageInfo2_direct_try1 = validatePackageInfo(
            PACKAGE2_NAME, IS_NOT_NULL, IS_FAILED, IS_ARTIFICIAL);

        ClassInfo testclassInfo_direct =
            validateClassInfo(TESTCLASS_NAME, IS_NOT_ARTIFICIAL);

        if ( testclassInfo_direct != null ) {
            // The loaded class obtains the same artificial, failed package info.

            PackageInfo packageInfo2_indirect = testclassInfo_direct.getPackage();
            validatePackageInfo(PACKAGE2_NAME, IS_NOT_NULL, IS_FAILED, IS_ARTIFICIAL,
                                packageInfo2_indirect);
        }

        // The artificial "failed" package info is retained.

        PackageInfo packageInfo2_direct_try2 = validatePackageInfo(
            PACKAGE2_NAME, IS_NOT_NULL, IS_FAILED, IS_ARTIFICIAL);
    }
}
