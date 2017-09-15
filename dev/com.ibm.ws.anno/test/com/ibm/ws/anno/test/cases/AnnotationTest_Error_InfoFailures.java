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
package com.ibm.ws.anno.test.cases;

import java.io.PrintWriter;

import org.junit.Test;

import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.anno.test.data.AnnotationTest_Error_Data;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.PackageInfo;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Fault;

public class AnnotationTest_Error_InfoFailures extends AnnotationTest_BaseErrorClass {

    @Override
    public String getTargetName() {
        return AnnotationTest_Error_Data.WAR_NAME_INFO_FAILURES;
    }

    @Test
    public void testAnnotationTest_Error_InfoFailures() throws Exception {
        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }

    @Override
    public String[] getValidPackageNames() {
        return AnnotationTest_Error_Data.INFO_VALID_PACKAGE_NAMES;
    }

    @Override
    public String[] getNonValidPackageNames() {
        return AnnotationTest_Error_Data.INFO_NON_VALID_PACKAGE_NAMES;
    }

    @Override
    public String[] getValidClassNames() {
        return AnnotationTest_Error_Data.INFO_VALID_CLASS_NAMES;
    }

    @Override
    public String[] getNonValidClassNames() {
        return AnnotationTest_Error_Data.INFO_NON_VALID_CLASS_NAMES;
    }

    protected static final boolean IS_ARTIFICIAL = true;
    protected static final boolean IS_NOT_ARTIFICIAL = false;

    protected ClassInfo verifyClassInfo(PrintWriter writer,
                                        InfoStore infoStore,
                                        String className, boolean isArtificial,
                                        AnnotationTest_TestResult scanResult) {

        ClassInfo classInfo = infoStore.getDelayableClassInfo(className);

        if (classInfo == null) {
            AnnotationTargets_Fault mapFault = createFault("Class [ {0} ] could not be loaded!", new String[] { className });
            scanResult.addVerificationMessage(mapFault);
            writer.println(mapFault.getResolvedText());

            return null;

        } else {
            writer.println("Verified load of class [ " + className + " ]");
        }

        if (classInfo.isArtificial() != isArtificial) {
            AnnotationTargets_Fault mapFault = createFault("Class [ {0} ] artificial flag [ {1} ] does not match expected value [ {2} ]",
                                                           new String[] { className,
                                                                         Boolean.toString(classInfo.isArtificial()),
                                                                         Boolean.toString(isArtificial) });
            scanResult.addVerificationMessage(mapFault);
            writer.println(mapFault.getResolvedText());
        } else {
            writer.println("class [ " + className + " ] artificial flag [ " + Boolean.toString(isArtificial) + " ] is correct");
        }

        return classInfo;
    }

    protected static final boolean IS_NULL = true;
    protected static final boolean IS_NOT_NULL = false;

    protected static final boolean IS_FAILED = true;
    protected static final boolean IS_NOT_FAILED = false;

    protected void verifyPackageInfo(PrintWriter writer,
                                     InfoStore infoStore,
                                     String packageName, boolean isNull, boolean isFailed, boolean isArtificial,
                                     PackageInfo packageInfo,
                                     AnnotationTest_TestResult scanResult) {

        if (isNull) {
            if (packageInfo != null) {
                AnnotationTargets_Fault mapFault = createFault("Package [ {0} ] was found, but should be absent", new String[] { packageName });
                scanResult.addVerificationMessage(mapFault);
                writer.println(mapFault.getResolvedText());

            } else {
                writer.println("Verified absence of non-valid class [ " + packageName + " ]");
            }

        } else {
            if (packageInfo == null) {
                AnnotationTargets_Fault mapFault = createFault("Package [ {0} ] was not found, but should have been", new String[] { packageName });
                scanResult.addVerificationMessage(mapFault);
                writer.println(mapFault.getResolvedText());

            } else {
                writer.println("Verified presence of valid class [ " + packageName + " ]");

                if (packageInfo.getForFailedLoad() != isFailed) {
                    AnnotationTargets_Fault mapFault = createFault("Package [ {0} ] failure flag [ {1} ] does not match expected value [ {2} ]",
                                                                   new String[] { packageName,
                                                                                 Boolean.toString(packageInfo.getForFailedLoad()),
                                                                                 Boolean.toString(isFailed) });
                    scanResult.addVerificationMessage(mapFault);
                    writer.println(mapFault.getResolvedText());
                } else {
                    writer.println("Package [ " + packageName + " ] failure flag [ " + Boolean.toString(isFailed) + " ] is correct");
                }

                if (packageInfo.getIsArtificial() != isArtificial) {
                    AnnotationTargets_Fault mapFault = createFault("Package [ {0} ] artificial flag [ {1} ] does not match expected value [ {2} ]",
                                                                   new String[] { packageName,
                                                                                 Boolean.toString(packageInfo.getIsArtificial()),
                                                                                 Boolean.toString(isArtificial) });
                    scanResult.addVerificationMessage(mapFault);
                    writer.println(mapFault.getResolvedText());
                } else {
                    writer.println("Package [ " + packageName + " ] artificial flag [ " + Boolean.toString(isArtificial) + " ] is correct");
                }
            }
        }
    }

    @Override
    protected void verifyInfoStore(PrintWriter writer,
                                   ClassSource_Aggregate classSource,
                                   AnnotationTargetsImpl_Targets annotationTargets,
                                   InfoStore infoStore,
                                   AnnotationTest_TestResult scanResult) throws InfoStoreException {
        super.verifyInfoStore(writer, classSource, annotationTargets, infoStore, scanResult);

        String TEST_PACKAGE1_NAME = AnnotationTest_Error_Data.INFO_FAILURES_PACKAGE1;
        String TEST_PACKAGE2_NAME = AnnotationTest_Error_Data.INFO_FAILURES_PACKAGE2;

        String SUPERCLASS_NAME = AnnotationTest_Error_Data.INFO_FAILURES_SUPERCLASS;
        String SUBCLASS_NAME = AnnotationTest_Error_Data.INFO_FAILURES_SUBCLASS;
        String TESTCLASS_NAME = AnnotationTest_Error_Data.INFO_FAILURES_TESTCLASS;

        // No 'package-info' for package1: An initial load returns null.

        PackageInfo packageInfo1_direct_try1 = infoStore.getPackageInfo(TEST_PACKAGE1_NAME);

        verifyPackageInfo(writer, infoStore,
                          TEST_PACKAGE1_NAME, IS_NULL, IS_NOT_FAILED, IS_NOT_ARTIFICIAL,
                          packageInfo1_direct_try1,
                          scanResult);

        // The superclass is not valid: A direct load returns an artificial.

        @SuppressWarnings("unused")
        ClassInfo superclassInfo_direct = verifyClassInfo(writer, infoStore,
                                                          SUPERCLASS_NAME, IS_ARTIFICIAL,
                                                          scanResult);

        // The subclass is valid: A direct load is successful.

        ClassInfo subclassInfo_direct = verifyClassInfo(writer, infoStore,
                                                        SUBCLASS_NAME, IS_NOT_ARTIFICIAL,
                                                        scanResult);

        if (subclassInfo_direct != null) {
            // While there is no "package-info" for the package of the test subclass,
            // an associated artificial package object is still generated.

            PackageInfo packageInfo1_indirect = subclassInfo_direct.getPackage();

            verifyPackageInfo(writer, infoStore,
                              TEST_PACKAGE1_NAME, IS_NOT_NULL, IS_NOT_FAILED, IS_ARTIFICIAL,
                              packageInfo1_indirect,
                              scanResult);

            // The superclass is not valid: Retrieve generates an artificial class.

            ClassInfo superclassInfo_indirect = subclassInfo_direct.getSuperclass();

            if (superclassInfo_indirect == null) {
                AnnotationTargets_Fault mapFault = createFault("Superclass [ {0} ] of [ {1} ] could not be generated!",
                                                               new String[] { SUPERCLASS_NAME, SUBCLASS_NAME });
                scanResult.addVerificationMessage(mapFault);
                writer.println(mapFault.getResolvedText());

            } else {
                writer.println("Generated super class [ " + SUPERCLASS_NAME + " ] of [ " + SUBCLASS_NAME + " ]");

                if (!superclassInfo_indirect.isArtificial()) {
                    AnnotationTargets_Fault mapFault = createFault("Superclass [ {0} ] of [ {1} ] is not marked as artificial",
                                                                   new String[] { SUPERCLASS_NAME, SUBCLASS_NAME });
                    scanResult.addVerificationMessage(mapFault);
                    writer.println(mapFault.getResolvedText());
                } else {
                    writer.println("Generated super class [ " + SUPERCLASS_NAME + " ] of [ " + SUBCLASS_NAME + " ] is marked as artificial");
                }
            }
        }

        // After the indirect access to the test package, despite there being no "package-info",
        // the artificial package info is retained.

        PackageInfo packageInfo1_direct_try2 = infoStore.getPackageInfo(TEST_PACKAGE1_NAME);

        verifyPackageInfo(writer, infoStore,
                          TEST_PACKAGE1_NAME, IS_NOT_NULL, IS_NOT_FAILED, IS_ARTIFICIAL,
                          packageInfo1_direct_try2,
                          scanResult);

        // There *is* "package-info" for package 2.  However, it does not load.
        //
        // An artificial, "failed" package info is generated.

        PackageInfo packageInfo2_direct_try1 = infoStore.getPackageInfo(TEST_PACKAGE2_NAME);

        verifyPackageInfo(writer, infoStore,
                          TEST_PACKAGE2_NAME, IS_NOT_NULL, IS_FAILED, IS_ARTIFICIAL,
                          packageInfo2_direct_try1,
                          scanResult);

        ClassInfo testclassInfo_direct = verifyClassInfo(writer, infoStore,
                                                         TESTCLASS_NAME, IS_NOT_ARTIFICIAL,
                                                         scanResult);

        if (testclassInfo_direct != null) {
            // The loaded class obtains the same artificial, failed package info.

            PackageInfo packageInfo2_indirect = testclassInfo_direct.getPackage();

            verifyPackageInfo(writer, infoStore,
                              TEST_PACKAGE2_NAME, IS_NOT_NULL, IS_FAILED, IS_ARTIFICIAL,
                              packageInfo2_indirect,
                              scanResult);
        }

        // The artificial "failed" package info is retained.

        PackageInfo packageInfo2_direct_try2 = infoStore.getPackageInfo(TEST_PACKAGE2_NAME);

        verifyPackageInfo(writer, infoStore,
                          TEST_PACKAGE2_NAME, IS_NOT_NULL, IS_FAILED, IS_ARTIFICIAL,
                          packageInfo2_direct_try2,
                          scanResult);
    }
}