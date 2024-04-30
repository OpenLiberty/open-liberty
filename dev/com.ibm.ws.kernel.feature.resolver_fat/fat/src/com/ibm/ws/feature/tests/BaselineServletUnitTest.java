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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.kernel.feature.internal.util.VerifyData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;

/**
 * Baseline public singleton resolution.
 */
@RunWith(Parameterized.class)
public class BaselineServletUnitTest extends FeatureResolutionUnitTestBase {
    // Not currently used:
    //
    // BeforeClass is invoked after data() is invoked.
    //
    // But 'data' requires the locations and repository,
    // which were being setup in 'setupClass'.
    //
    // The setup steps have been moved to 'data()', and
    // have been set run at most once.

    @BeforeClass
    public static void setupClass() throws Exception {
        // doSetupClass(getServerName());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        doTearDownClass();
    }

    public static final String DATA_FILE_PATH_OL = "publish/verify/servlet_expected.xml";
    public static final String DATA_FILE_PATH_WL = "publish/verify/servlet_expected_WL.xml";

    public static File getDataFile_OL() {
        return new File(DATA_FILE_PATH_OL);
    }

    public static File getDataFile_WL() {
        return new File(DATA_FILE_PATH_WL);
    }

    // To use change the name of parameterized tests, you say:
    //
    // @Parameters(name="namestring")
    //
    // namestring is a string, which can have the following special placeholders:
    //
    //   {index} - the index of this set of arguments. The default namestring is {index}.
    //   {0} - the first parameter value from this invocation of the test.
    //   {1} - the second parameter value
    //   and so on
    // @Parameterized.Parameters(name = "{0}")

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        doSetupClass(); // 'data()' is invoked before the '@BeforeClass' method.

        VerifyData verifyData = readData(getDataFile_OL());

        // WAS liberty adds and modifies the Open liberty cases.
        if (RepositoryUtil.isWASLiberty()) {
            int initialCount = verifyData.getCases().size();
            VerifyData verifyData_WL = readData(getDataFile_WL());
            verifyData = verifyData.add(verifyData_WL);
            int finalCount = verifyData.getCases().size();
            System.out.println("Case adjustment [ " + (finalCount - initialCount) + " ]");
        }

        return asCases(verifyData);
    }

    public BaselineServletUnitTest(String name, VerifyCase testCase) throws Exception {
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
        return detectPairErrors(rootFeatures);
    }

    @Test
    public void testResolve_baseline_servlet() throws Exception {
        doTestResolve();
    }
}
