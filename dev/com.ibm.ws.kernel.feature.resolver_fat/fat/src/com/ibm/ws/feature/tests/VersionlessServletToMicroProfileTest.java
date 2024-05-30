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

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class VersionlessServletToMicroProfileTest extends VersionlessTestBase {

    public static final String SERVER_NAME_SERVLET3_HEALTH = "Servlet3toHealth";

    public static final String[] ALLOWED_ERRORS = { "CWWKF0001E", "CWWKF0048E" };

    @Test
    public void servlet3HealthMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpHealth-2.2" };

        test(SERVER_NAME_SERVLET3_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet3HealthMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpHealth-1.0" };

        test(SERVER_NAME_SERVLET3_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_SERVLET3_METRICS = "Servlet3toMetrics";

    @Test
    public void servlet3MetricsMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpMetrics-2.3" };

        test(SERVER_NAME_SERVLET3_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet3MetricsMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpMetrics-1.0" };

        test(SERVER_NAME_SERVLET3_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_SERVLET4_HEALTH = "Servlet4toHealth";

    @Test
    public void servlet4HealthMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpHealth-3.1" };

        test(SERVER_NAME_SERVLET4_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet4HealthMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpHealth-1.0" };

        test(SERVER_NAME_SERVLET4_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_SERVLET4_METRICS = "Servlet4toMetrics";

    @Test
    public void servlet4MetricsMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpMetrics-3.0" };

        test(SERVER_NAME_SERVLET4_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet4MetricsMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpMetrics-1.0" };

        test(SERVER_NAME_SERVLET4_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    //

    public static final String SERVER_NAME_SERVLET5_HEALTH = "Servlet5toHealth";

    @Test
    public void servlet5HealthMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpHealth-4.0" };

        test(SERVER_NAME_SERVLET5_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet5HealthMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpHealth-4.0" };

        test(SERVER_NAME_SERVLET5_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_SERVLET5_METRICS = "Servlet5toMetrics";

    @Test
    public void servlet5MetricsMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpMetrics-4.0" };

        test(SERVER_NAME_SERVLET5_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet5MetricsMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpMetrics-4.0" };

        test(SERVER_NAME_SERVLET5_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_SERVLET6_HEALTH = "Servlet6toHealth";

    @Test
    public void servlet6HealthMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpHealth-4.0" };

        test(SERVER_NAME_SERVLET6_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet6HealthMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpHealth-4.0" };

        test(SERVER_NAME_SERVLET6_HEALTH, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_SERVLET6_METRICS = "Servlet6toMetrics";

    @Test
    public void servlet6MetricsMaxTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_DESCENDING;
        String expectedResolved[] = { "mpMetrics-5.1" };

        test(SERVER_NAME_SERVLET6_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void servlet6MetricsMinTest() throws Exception {
        String preferredVersions = PlatformConstants.MICROPROFILE_ASCENDING;
        String expectedResolved[] = { "mpMetrics-5.0" };

        test(SERVER_NAME_SERVLET6_METRICS, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }
}
