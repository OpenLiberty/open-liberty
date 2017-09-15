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

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.ws.anno.test.data.AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data;

public class AcmeAnnuityWeb_AcmeAnnuityWeb_war_Test extends AnnotationTest_BaseClass {

    @Override
    public ClassSource_Specification_Direct_WAR createClassSourceSpecification() {
        return AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.createClassSourceSpecification(getClassSourceFactory(),
                                                                                     getProjectPath(),
                                                                                     getDataPath());
    }

    //

    public static final String LOG_NAME = AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.WAR_NAME + ".log";

    //

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(); // throws Exception

        setDataPath(AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.EAR_NAME);
    }

    //

    @Override
    public String getTargetName() {
        return AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.WAR_NAME;
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

    // Alternate cache location for Sun and Mac tests.
    //
    // See AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data for more details.
    //
    // Note that the common 'temp' location for the initial serialization tests
    // remains the same.

    public String getStorageFragment() {
        String storageFragment = COMMON_STORAGE_PATH;
        if (!AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.DETECTED_RESOLVER) {
            storageFragment = AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.RESOLVER_PREFIX + storageFragment;
        }

        return storageFragment;
    }

    @Test
    public void testAcumentAnnuityWeb_AcmeAnnuityWeb_war_nodetail_direct() throws Exception {
        // Disable for now: Keeping the test data in sync is not practical while
        // the API is being updated.

        if (!AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.DETECTED_RESOLVER) {
            return;
        }

        runScanTest(DETAIL_IS_NOT_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_NO_DETAIL,
                    getSeedStorage(), getStoragePath(getStorageFragment()), STORAGE_NAME_NO_DETAIL,
                    new PrintWriter(System.out, true));
    }

    @Test
    public void testAcumentAnnuityWeb_AcmeAnnuityWeb_war_detail_direct() throws Exception {
        // Disable for now: Keeping the test data in sync is not practical while
        // the API is being updated.

        if (!AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.DETECTED_RESOLVER) {
            return;
        }

        runScanTest(DETAIL_IS_ENABLED,
                    getStoragePath(COMMON_TEMP_STORAGE_PATH), STORAGE_NAME_DETAIL,
                    getSeedStorage(), getStoragePath(getStorageFragment()), STORAGE_NAME_DETAIL,
                    new PrintWriter(System.out, true));
    }
}
