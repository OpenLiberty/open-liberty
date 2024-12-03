/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

//@formatter:off
@RunWith(Parameterized.class)
public class VersionlessServletToMicroProfileTest extends VersionlessTest {

    public static final String SERVER_NAME_SERVLET3_HEALTH = "Servlet3toHealth";
    public static final String SERVER_NAME_SERVLET3_METRICS = "Servlet3toMetrics";

    public static final String SERVER_NAME_SERVLET4_HEALTH = "Servlet4toHealth";
    public static final String SERVER_NAME_SERVLET4_METRICS = "Servlet4toMetrics";

    public static final String SERVER_NAME_SERVLET5_HEALTH = "Servlet5toHealth";
    public static final String SERVER_NAME_SERVLET5_METRICS = "Servlet5toMetrics";

    public static final String SERVER_NAME_SERVLET6_HEALTH = "Servlet6toHealth";
    public static final String SERVER_NAME_SERVLET6_METRICS = "Servlet6toMetrics";

    public static final String SERVER_JAKARTAEE8 = "jakartaee8";

    public static final String[] ALLOWED_ERRORS = { "CWWKF0001E", "CWWKF0048E" };

    public static final TestCase[] TEST_CASES = {
        new TestCase("servlet3HealthMax", SERVER_NAME_SERVLET3_HEALTH,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpHealth-2.2" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet3HealthMin", SERVER_NAME_SERVLET3_HEALTH,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpHealth-1.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet3MetricsMax", SERVER_NAME_SERVLET3_METRICS,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-2.3" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet3MetricsMin", SERVER_NAME_SERVLET3_METRICS,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-1.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),

        new TestCase("servlet4HealthMax", SERVER_NAME_SERVLET4_HEALTH,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpHealth-3.1" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet4HealthMin", SERVER_NAME_SERVLET4_HEALTH,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpHealth-1.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet4MetricsMax", SERVER_NAME_SERVLET4_METRICS,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-3.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet4MetricsMin", SERVER_NAME_SERVLET4_METRICS,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-1.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),

        new TestCase("servlet5HealthMax", SERVER_NAME_SERVLET5_HEALTH,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpHealth-4.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet5HealthMin", SERVER_NAME_SERVLET5_HEALTH,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpHealth-4.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet5MetricsMax", SERVER_NAME_SERVLET5_METRICS,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-4.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet5MetricsMin", SERVER_NAME_SERVLET5_METRICS,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-4.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),

        new TestCase("servlet6HealthMax", SERVER_NAME_SERVLET6_HEALTH,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpHealth-4.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet6HealthMin", SERVER_NAME_SERVLET6_HEALTH,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpHealth-4.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS),
        new TestCase("servlet6MetricsMax", SERVER_NAME_SERVLET6_METRICS,
                     PlatformConstants.MICROPROFILE_DESCENDING,
                     new String[] { "mpMetrics-5.1" }, TestCase.NO_FAILURES, ALLOWED_ERRORS,
                     TestCase.JAVA_11),
        new TestCase("servlet6MetricsMin", SERVER_NAME_SERVLET6_METRICS,
                     PlatformConstants.MICROPROFILE_ASCENDING,
                     new String[] { "mpMetrics-5.0" }, TestCase.NO_FAILURES, ALLOWED_ERRORS,
                     TestCase.JAVA_11),

        //JAKARTA8 TEST
        new TestCase("jakartaee8", SERVER_JAKARTAEE8, "",
                     new String[] { "servlet-4.0", "jpa-2.2"}, TestCase.NO_FAILURES, ALLOWED_ERRORS,
                     TestCase.JAVA_11)
    };

    @Parameterized.Parameters
    public static Collection<Object[]> getData() throws Exception {
        return filterData(TEST_CASES);
    }

    public VersionlessServletToMicroProfileTest(TestCase testCase) {
        super(testCase);
    }

    @Test
    public void versionless_servletToMicroProfileTest() throws Exception {
        test(getTestCase());
    }

}
//@formatter:on