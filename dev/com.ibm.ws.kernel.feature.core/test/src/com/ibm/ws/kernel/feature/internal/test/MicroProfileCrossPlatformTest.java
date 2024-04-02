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
package com.ibm.ws.kernel.feature.internal.test;

import java.io.File;
import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;

/**
 * Micro profile cross platform tests.
 */
@RunWith(Parameterized.class)
public class MicroProfileCrossPlatformTest extends FeatureResolutionTest {
    @BeforeClass
    public static void setupClass() throws Exception {
        doSetupClass(getImageDir(), getBootstrapLibDir(), getServerName());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        doTearDownClass();
    }

    public static final String DATA_FILE_PATH = "data/microprofile.xml";

    public static File getDataFile() {
        return new File(DATA_FILE_PATH);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return readCases(getDataFile());
    }

    public MicroProfileCrossPlatformTest(VerifyCase testCase) throws Exception {
        super(testCase);
    }

    @Override
    public String getPreferredVersions() {
        return "mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpMetrics-3.0,mpMetrics-2.3,mpMetrics-2.2" +
               ",mpMetrics-2.0,mpMetrics-1.1,mpMetrics-1.0,mpHealth-4.0,mpHealth-3.1,mpHealth-3.0" +
               ",mpHealth-2.2,mpHealth-2.1,mpHealth-2.0,mpHealth-1.0";
    }

    @Before
    public void setupTest() throws Exception {
        doSetupResolver();
    }

    @After
    public void tearDownTest() throws Exception {
        doClearResolver();
    }

    @Test
    public void testResolve() throws Exception {
        doTestResolve();
    }
}