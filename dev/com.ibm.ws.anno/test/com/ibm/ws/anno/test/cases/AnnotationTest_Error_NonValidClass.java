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

import com.ibm.ws.anno.test.data.AnnotationTest_Error_Data;

public class AnnotationTest_Error_NonValidClass extends AnnotationTest_BaseErrorClass {

    @Override
    public String getTargetName() {
        return AnnotationTest_Error_Data.WAR_NAME_NONVALID_CLASS;
    }

    @Test
    public void testAnnotationTest_Error_NonValidClass() throws Exception {
        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(COMMON_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }
}