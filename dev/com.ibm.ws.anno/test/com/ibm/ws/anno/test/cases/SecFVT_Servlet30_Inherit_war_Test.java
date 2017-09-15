/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import java.util.Set;

import javax.servlet.annotation.ServletSecurity;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Targets;
import com.ibm.ws.anno.test.data.SecFVT_Servlet30_Common_war_Data;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Fault;

public class SecFVT_Servlet30_Inherit_war_Test extends AnnotationTest_BaseClass {

    @Override
    public ClassSource_Specification_Direct_WAR createClassSourceSpecification() {
        return SecFVT_Servlet30_Common_war_Data.createClassSourceSpecification(getClassSourceFactory(),
                                                                               getProjectPath(),
                                                                               getDataPath(),
                                                                               SecFVT_Servlet30_Common_war_Data.WAR_INHERIT_NAME);
    }

    //

    public static final String LOG_NAME = SecFVT_Servlet30_Common_war_Data.WAR_INHERIT_NAME + ".log";

    //

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(); // throws Exception

        setDataPath(SecFVT_Servlet30_Common_war_Data.EAR_NAME);
    }

    //

    @Override
    public String getTargetName() {
        return SecFVT_Servlet30_Common_war_Data.WAR_INHERIT_NAME;
    }

    @Override
    public int getIterations() {
        return 5;
    }

    //

    //    @Override
    //    public boolean getSeedStorage() {
    //        return true;
    //    }

    //

    @Test
    public void testSecFVT_Servlet30_Common_war_nodetail_direct() throws Exception {
        runScanTest(DETAIL_IS_NOT_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_NO_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_NO_DETAIL,
                    new PrintWriter(System.out, true));
    }

    @Test
    public void testSecFVT_Servlet30_Common_war_detail_direct() throws Exception {
        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }

    public static final String SERVLET_SECURITY_CLASS_NAME = ServletSecurity.class.getName();

    @Override
    protected void verifyInfoStore(PrintWriter writer,
                                   ClassSource_Aggregate classSource,
                                   AnnotationTargetsImpl_Targets annotationTargets,
                                   InfoStore infoStore,
                                   AnnotationTest_TestResult scanResult) throws InfoStoreException {

        super.verifyInfoStore(writer, classSource, annotationTargets, infoStore, scanResult);
        // throws InfoStoreException

        writer.println("Running child defailt verification ...");

        writer.println("Verifying inheritance of [ " + SERVLET_SECURITY_CLASS_NAME + " ]");

        Set<String> matchingClassNames = annotationTargets.getAllInheritedAnnotatedClasses(SERVLET_SECURITY_CLASS_NAME);

        for (String className : matchingClassNames) {
            ClassInfo classInfo = infoStore.getDelayableClassInfo(className);
            AnnotationInfo servletSecurityAnnotationInfo = classInfo.getAnnotation(SERVLET_SECURITY_CLASS_NAME);

            if (servletSecurityAnnotationInfo == null) {
                AnnotationTargets_Fault infoFault =
                                createFault("Annotation [ {0} ] not found on subclass [ {1} ]",
                                            new String[] { SERVLET_SECURITY_CLASS_NAME, classInfo.getHashText() });
                writer.println("Info fault: [ " + infoFault.getResolvedText() + " ]");
                scanResult.addVerificationMessage(infoFault);
            }
        }

        writer.println("Running child defailt verification ... done");
    }

}
