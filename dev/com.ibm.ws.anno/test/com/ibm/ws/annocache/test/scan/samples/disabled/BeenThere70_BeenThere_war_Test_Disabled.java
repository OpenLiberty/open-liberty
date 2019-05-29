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

package com.ibm.ws.annocache.test.scan.samples.disabled;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct;
import com.ibm.ws.annocache.test.scan.Test_Base;

public class BeenThere70_BeenThere_war_Test_Disabled extends Test_Base {
    public static final String TEST_DATA_ROOT = "c:\\annoSamples";

    public static final String LOG_NAME = "ammScan_BeenThere.log";
    public static final String LOG_PATH = "C:\\tmp\\" + LOG_NAME;

    public static final String EAR_NAME = "BeenThere.ear";
    public static final String EAR_SIMPLE_NAME = "BeenThere";
    public static final String EAR_PATH = TEST_DATA_ROOT + "\\" + EAR_NAME;

    public static final String WAR_NAME = "BeenThere.war";
    public static final String WAR_SIMPLE_NAME = "BeenThere";
    public static final String WAR_PATH = EAR_PATH + "\\" + WAR_NAME;

    //

    @Override
    public String getAppName() {
        return EAR_NAME;
    }
    
    @Override
    public String getAppSimpleName() {
        return EAR_SIMPLE_NAME;
    }
    
    @Override
    public String getModName() {
        return WAR_NAME;
    }
    
    @Override
    public String getModSimpleName() {
        return WAR_SIMPLE_NAME;
    }

    //    public void testBeenThere_nodetail() throws Exception {
    //        runScanTest(AnnotationTargets_Scanner.DETAIL_IS_NOT_ENABLED,
    //                    CONTAINER_IS_NOT_ENABLED,
    //                    new PrintWriter(System.out));
    //    }
    //
    //    public void testBeenThere_detail() throws Exception {
    //        runScanTest(AnnotationTargets_Scanner.DETAIL_IS_ENABLED,
    //                    CONTAINER_IS_NOT_ENABLED,
    //                    new PrintWriter(System.out));
    //    }

    //        AnnotationTargetsImpl_Context scannerContext = new AnnotationTargetsImpl_Context(classInternMap);
    //
    //        scannerContext.addDirectoryClassSource("WAR classes",
    //                                               WAR_PATH + "\\" + "WEB-INF\\classes",
    //                                               ClassSource_Aggregate.IS_SEED); // throws IOException
    //
    //        scannerContext.addDirectoryClassSource("EAR lib",
    //                                               EAR_PATH + "lib" + "\\" + "BeenThere.jar",
    //                                               ClassSource_Aggregate.IS_NOT_SEED); // throws IOException
    //
    //        scannerContext.addClassLoaderClassSource("system classloader",
    //                                                 BeenThere70_BeenThere_war_Test_Disabled.class.getClassLoader());
    //
    //        return scannerContext;   

    @Override
    public ClassSourceImpl_Specification_Direct createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        return null;
    }
}
