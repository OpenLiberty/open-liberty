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

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_EJB;
import com.ibm.ws.anno.test.data.AppDeployBench_GSEJB_jar_Data;

public class AppDeployBench_GSEJB_jar_Test extends AnnotationTest_BaseClass {

    @Override
    public ClassSource_Specification_Direct_EJB createClassSourceSpecification() {
        return AppDeployBench_GSEJB_jar_Data.createClassSourceSpecification(getClassSourceFactory(),
                                                                            getProjectPath(),
                                                                            getDataPath());
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(); // throws Exception

        setDataPath(AppDeployBench_GSEJB_jar_Data.EAR_NAME);
    }

    //

    @Override
    public String getTargetName() {
        return AppDeployBench_GSEJB_jar_Data.EJBJAR_NAME;
    }

    @Override
    public int getIterations() {
        return 5;
    }

    //  public boolean getSeedStorage() {
    //  return true;        
    //}

    //   

    @Test
    public void testAppDeployBench_GSEJB_JAR_nodetail_direct() throws Exception {
        runScanTest(DETAIL_IS_NOT_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_NO_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_NO_DETAIL,
                    new PrintWriter(System.out, true));
    }

    @Test
    public void testAppDeployBench_GSEJB_JAR_detail_direct() throws Exception {
        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }
}
