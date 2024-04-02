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
 * Baseline public singleton resolution.
 */
@RunWith(Parameterized.class)
public class BaselineSingletonTest extends FeatureResolutionTest {
    @BeforeClass
    public static void setupClass() throws Exception {
        doSetupClass(getImageDir(), getBootstrapLibDir(), getServerName());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        doTearDownClass();
    }

    public static final String DATA_FILE_PATH = "data/singleton.xml";

    public static File getDataFile() {
        return new File(DATA_FILE_PATH);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return readCases(getDataFile());
    }

    public BaselineSingletonTest(VerifyCase testCase) throws Exception {
        super(testCase);
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
