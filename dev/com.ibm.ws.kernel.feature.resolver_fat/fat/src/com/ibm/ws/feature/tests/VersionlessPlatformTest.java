/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;

/**
 * Baseline public singleton resolution.
 */
@RunWith(Parameterized.class)
public class VersionlessPlatformTest extends BaselineResolutionUnitTest {
    /** Control parameter: Used to disable this unit test. */
    public static final boolean IS_ENABLED = BaselineResolutionEnablement.enablePlatforms;

    // 'data()' is invoked before the '@BeforeClass' method.
    // @BeforeClass
    public static void setupClass() throws Exception {
        doSetupClass();
        setupExpected(); // Must be after 'doSetupClass'
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        doTearDownClass(VersionlessPlatformTest.class);
    }

    public static final String DATA_FILE_PATH_OL = "publish/verify/platform_expected.xml";
    public static final String DATA_FILE_PATH_WL = "publish/verify/platform_expected_WL.xml";

    public static File getDataFile_OL() {
        return new File(DATA_FILE_PATH_OL);
    }

    public static File getDataFile_WL() {
        return new File(DATA_FILE_PATH_WL);
    }

    public static VerifyData recordedData;

    public static VerifyData getRecordedData() {
        return recordedData;
    }

    public static void setupExpected() throws Exception {
        VerifyData verifyData = readData(getDataFile_OL());
        int olCount = verifyData.getCases().size();
        System.out.println("OL data [ " + DATA_FILE_PATH_OL + " ]: [ " + olCount + " ]");

        // WAS liberty adds and modifies the Open liberty cases.
        if (RepositoryUtil.isWASLiberty()) {
            VerifyData verifyData_WL = readData(getDataFile_WL());
            int wlCount = verifyData_WL.getCases().size();
            System.out.println("WL data [ " + DATA_FILE_PATH_WL + " ]: [ " + wlCount + " ]");

            verifyData = verifyData.add(verifyData_WL);
            int finalCount = verifyData.getCases().size();
            System.out.println("Merged data [ " + (finalCount - olCount) + " ]");
        }

        recordedData = verifyData;
    }

    public static VerifyData getBareInput() throws Exception {
        return new VerifyData(VersionlessPlatformTestData.getCases().values());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        setupClass();

        if (!IS_ENABLED) {
            return nullCases("platform resolution");
        } else {
            return asCases(getBareInput().splice(getRecordedData()));
        }
    }

    public VersionlessPlatformTest(String name, VerifyCase testCase) throws Exception {
        super(name, testCase);
    }

    @Before
    public void setupTest() throws Exception {
        doSetupResolver();
    }

    @After
    public void tearDownTest() throws Exception {
        doClearResolver();
    }

    @Override
    public List<String> detectFeatureErrors(List<String> rootFeatures) {
        return null;
    }

    @Test
    public void versionless_platformTest() throws Exception {
        if (!IS_ENABLED) {
            nullResult();
            return;
        }
        doTestResolve();
    }
}
