/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.ws.feature.tests.util.PlatformConstants;

// @formatter:off

@RunWith(Parameterized.class)
public class VersionlessJavaEEToMicroProfileTest extends VersionlessTest {
    public static final String SERVER_NAME_EE7 = "ee7toMP";
    public static final String SERVER_NAME_EE8 = "ee8toMP";
    public static final String SERVER_NAME_EE9 = "ee9toMP";
    public static final String SERVER_NAME_EE10 = "ee10toMP";
    public static final String SERVER_NAME_EE11 = "ee11toMP";

    public static final String[] ALLOWED_ERRORS = { "CWWKF0001E", "CWWKF0048E" };

    public static TestCase[] TEST_CASES = new TestCase[] {
        new TestCase("ee7toHealthAndMetricsMax",
                     SERVER_NAME_EE7,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-2.3", "mpHealth-2.2" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS),
        new TestCase("ee7toHealthAndMetricsMin",
                     SERVER_NAME_EE7,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-1.0", "mpHealth-1.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS),

        new TestCase("ee8toHealthAndMetricsMax",
                     SERVER_NAME_EE8,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-3.0", "mpHealth-3.1" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS),
        new TestCase("ee8toHealthAndMetricsMin",
                     SERVER_NAME_EE8,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-1.0", "mpHealth-1.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS),

        new TestCase("ee9toHealthAndMetricsMax",
                     SERVER_NAME_EE9,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-4.0", "mpHealth-4.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS),
        new TestCase("ee9toHealthAndMetricsMin",
                     SERVER_NAME_EE9,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-4.0", "mpHealth-4.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS),

        new TestCase("ee10toHealthAndMetricsMax",
                     SERVER_NAME_EE10,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-5.1", "mpHealth-4.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS,
                     TestCase.JAVA_11),
        new TestCase("ee10toHealthAndMetricsMin",
                     SERVER_NAME_EE10,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-5.0", "mpHealth-4.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS,
                     TestCase.JAVA_11),

        new TestCase("ee11toHealthAndMetricsMax",
                     SERVER_NAME_EE11,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-5.1", "mpHealth-4.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS,
                     TestCase.JAVA_17),
        new TestCase("ee11toHealthAndMetricsMin",
                     SERVER_NAME_EE11,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-5.1", "mpHealth-4.0" },
                     TestCase.NO_FAILURES,
                     ALLOWED_ERRORS,
                     TestCase.JAVA_17)
    };

    @Parameterized.Parameters
    public static Collection<Object[]> getData() throws Exception {
        return filterData(TEST_CASES);
    }

    public VersionlessJavaEEToMicroProfileTest(TestCase testCase) {
        super(testCase);
    }

    @Test
    public void versionless_javaEEToMicroProfileTest() throws Exception {
        test(getTestCase());
    }

    @Override
    protected boolean waitForSSL() {
        return true;
    }
}
// @formatter:on
