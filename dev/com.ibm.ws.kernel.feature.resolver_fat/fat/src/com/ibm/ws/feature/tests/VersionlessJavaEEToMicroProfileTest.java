/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class VersionlessJavaEEToMicroProfileTest extends VersionlessTestBase {

    public static final String SERVER_NAME_EE10 = "ee10toMP";

    public static final String ALLOWED_ERRORS = "CWWKF0001E";

    @Test
    public void ee10toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = "mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpMetrics-3.0" +
                                   ",mpMetrics-2.3,mpMetrics-2.2,mpMetrics-2.0,mpMetrics-1.1,mpMetrics-1.0" +
                                   ",mpHealth-4.0,mpHealth-3.1,mpHealth-3.0,mpHealth-2.2,mpHealth-2.1" +
                                   ",mpHealth-2.0,mpHealth-1.0";

        String[] expectedResolved = { "mpMetrics-5.0", "mpHealth-4.0" };

        test(SERVER_NAME_EE10, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee10toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = "mpMetrics-1.0,mpMetrics-1.1,mpMetrics-2.0,mpMetrics-2.2,mpMetrics-2.3" +
                                   ",mpMetrics-3.0,mpMetrics-4.0,mpMetrics-5.0,mpMetrics-5.1" +
                                   ",mpHealth-1.0,mpHealth-2.0,mpHealth-2.1,mpHealth-2.2,mpHealth-3.0" +
                                   ",mpHealth-3.1,mpHealth-4.0";

        String[] expectedResolved = { "mpMetrics-5.0", "mpHealth-4.0" };

        test(SERVER_NAME_EE10, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_EE7 = "ee7toMP";

    @Test
    public void ee7toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = "mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpMetrics-3.0,mpMetrics-2.3" +
                                   ",mpMetrics-2.2,mpMetrics-2.0,mpMetrics-1.1,mpMetrics-1.0,mpHealth-4.0" +
                                   ",mpHealth-3.1,mpHealth-3.0,mpHealth-2.2,mpHealth-2.1,mpHealth-2.0,mpHealth-1.0";

        String[] expectedResolved = { "mpMetrics-2.3", "mpHealth-2.2" };

        test(SERVER_NAME_EE7, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee7toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = "mpMetrics-1.0,mpMetrics-1.1,mpMetrics-2.0,mpMetrics-2.2,mpMetrics-2.3" +
                                   ",mpMetrics-3.0,mpMetrics-4.0,mpMetrics-5.0,mpMetrics-5.1,mpHealth-1.0" +
                                   ",mpHealth-2.0,mpHealth-2.1,mpHealth-2.2,mpHealth-3.0,mpHealth-3.1,mpHealth-4.0";

        String[] expectedResolved = { "mpMetrics-1.0", "mpHealth-1.0" };

        test(SERVER_NAME_EE7, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    //

    public static final String SERVER_NAME_EE8 = "ee8toMP";

    @Test
    public void ee8toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = "mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpMetrics-3.0,mpMetrics-2.3,mpMetrics-2.2,mpMetrics-2.0,mpMetrics-1.1,mpMetrics-1.0,mpHealth-4.0,mpHealth-3.1,mpHealth-3.0,mpHealth-2.2,mpHealth-2.1,mpHealth-2.0,mpHealth-1.0";
        String[] expectedResolved = { "mpMetrics-3.0", "mpHealth-3.1" };

        test(SERVER_NAME_EE8, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee8toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = "mpMetrics-1.0,mpMetrics-1.1,mpMetrics-2.0,mpMetrics-2.2,mpMetrics-2.3,mpMetrics-3.0,mpMetrics-4.0,mpMetrics-5.0,mpMetrics-5.1,mpHealth-1.0,mpHealth-2.0,mpHealth-2.1,mpHealth-2.2,mpHealth-3.0,mpHealth-3.1,mpHealth-4.0";
        String[] expectedResolved = { "mpMetrics-1.0", "mpHealth-1.0" };

        test(SERVER_NAME_EE8, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    //

    public static final String SERVER_EE8 = "ee8toMP";

    public static final String EXPECTED_ERROR = "CWWKF0001E";

    @Test
    public void envVarEE8Test() throws Exception {
        String preferredVersions = "mpMetrics-2.3,mpHealth-2.2";
        String[] expectedResolved = { "mpMetrics-2.3", "mpHealth-2.2" };

        test(SERVER_EE8, EXPECTED_ERROR, preferredVersions, expectedResolved);
    }

    public static final String SERVER_SERV4_HEALTH = "Servlet4toHealth";

    @Test
    public void envVarServ4HealthTest() throws Exception {
        String preferredVersions = "mpMetrics-2.3,mpHealth-2.2";
        String[] expectedResolved = { "mpHealth-2.2" };

        test(SERVER_SERV4_HEALTH, EXPECTED_ERROR, preferredVersions, expectedResolved);
    }

    public static final String SERVER_SERV4_METRICS = "Servlet4toMetrics";

    @Test
    public void envVarServ4MetricsTest() throws Exception {
        String preferredVersions = "mpMetrics-3.0,mpHealth-2.2";
        String[] expectedResolved = { "mpMetrics-3.0" };

        test(SERVER_SERV4_HEALTH, EXPECTED_ERROR, preferredVersions, expectedResolved);
    }

    //

    public static final String SERVER_NAME_EE9 = "ee9toMP";

    @Test
    public void ee9toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = "mpMetrics-5.1,mpMetrics-5.0,mpMetrics-4.0,mpMetrics-3.0,mpMetrics-2.3,mpMetrics-2.2,mpMetrics-2.0,mpMetrics-1.1,mpMetrics-1.0,mpHealth-4.0,mpHealth-3.1,mpHealth-3.0,mpHealth-2.2,mpHealth-2.1,mpHealth-2.0,mpHealth-1.0";
        String[] expectedResolved = { "mpMetrics-4.0", "mpHealth-4.0" };

        test(SERVER_NAME_EE9, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee9toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = "mpMetrics-1.0,mpMetrics-1.1,mpMetrics-2.0,mpMetrics-2.2,mpMetrics-2.3,mpMetrics-3.0,mpMetrics-4.0,mpMetrics-5.0,mpMetrics-5.1,mpHealth-1.0,mpHealth-2.0,mpHealth-2.1,mpHealth-2.2,mpHealth-3.0,mpHealth-3.1,mpHealth-4.0";
        String[] expectedResolved = { "mpMetrics-4.0", "mpHealth-4.0" };

        test(SERVER_NAME_EE9, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }
}
