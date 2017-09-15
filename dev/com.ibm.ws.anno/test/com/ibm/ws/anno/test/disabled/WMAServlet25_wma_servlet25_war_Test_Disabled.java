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

package com.ibm.ws.anno.test.disabled;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct;
import com.ibm.ws.anno.test.cases.AnnotationTest_BaseClass;

public class WMAServlet25_wma_servlet25_war_Test_Disabled extends AnnotationTest_BaseClass {
    public static final String TEST_DATA_ROOT = "c:\\annoSamples";

    public static final String WAR_NAME = "wma_servlet25.war";
    public static final String WAR_PATH = TEST_DATA_ROOT + "\\" + WAR_NAME;

    public static final String EXTRA_LIB_NAME = "axis-1_4";
    public static final String EXTRA_LIB_PATH = WAR_PATH + "\\" + EXTRA_LIB_NAME;

    public static final String DEBUG_LOG_NAME = WAR_NAME + ".log";
    public static final String DEBUG_LOG_PATH = TEST_DATA_ROOT + "\\" + DEBUG_LOG_NAME;

    public static final String SUMMARY_LOG_NAME = WAR_NAME + ".log";
    public static final String SUMMARY_LOG_PATH = TEST_DATA_ROOT + "\\" + SUMMARY_LOG_NAME;

    //

    @Override
    public String getTargetName() {
        return "wma_servlet25";
    }

    @Override
    public int getIterations() {
        return 5;
    }

    //

    //        AnnotationTargetsImpl_Context scannerContext =
    //                        AnnotationTargetsImpl_Context.createWARContext(classInternMap,
    //                                                                       WAR_NAME,
    //                                                                       WAR_PATH,
    //                                                                       libPaths, (List<String>) null,
    //                                                                       WMAServlet25_wma_servlet25_war_Test_Disabled.class.getClassLoader());
    //        // 'createWARContext' throws AnnotationScannerException
    //
    //        return scannerContext;   

    /** {@inheritDoc} */
    @Override
    public ClassSource_Specification_Direct createClassSourceSpecification() {
        // TODO Auto-generated method stub
        return null;
    }
}
