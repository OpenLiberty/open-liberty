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

    public static final String[] ALLOWED_ERRORS = { "CWWKF0001E", "CWWKF0048E" };

    public static final String MIN_VERSIONS = "MicroProfile-1.2, MicroProfile-2.0, MicroProfile-3.0, MicroProfile-4.0, MicroProfile-5.0, MicroProfile-6.0, MicroProfile-6.1";
    public static final String MAX_VERSIONS = "MicroProfile-6.1, MicroProfile-6.0, MicroProfile-5.0, MicroProfile-4.0, MicroProfile-3.0, MicroProfile-2.0, MicroProfile-1.2";

    @Test
    public void ee10toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = MAX_VERSIONS;

        String[] expectedResolved = { "mpMetrics-5.1", "mpHealth-4.0" };

        test(SERVER_NAME_EE10, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee10toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = MIN_VERSIONS;

        String[] expectedResolved = { "mpMetrics-5.0", "mpHealth-4.0" };

        test(SERVER_NAME_EE10, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    public static final String SERVER_NAME_EE7 = "ee7toMP";

    @Test
    public void ee7toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = MAX_VERSIONS;

        String[] expectedResolved = { "mpMetrics-2.3", "mpHealth-2.2" };

        test(SERVER_NAME_EE7, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee7toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = MIN_VERSIONS;

        String[] expectedResolved = { "mpMetrics-1.0", "mpHealth-1.0" };

        test(SERVER_NAME_EE7, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    //

    public static final String SERVER_NAME_EE8 = "ee8toMP";

    @Test
    public void ee8toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = MAX_VERSIONS;
        String[] expectedResolved = { "mpMetrics-3.0", "mpHealth-3.1" };

        test(SERVER_NAME_EE8, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee8toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = MIN_VERSIONS;
        String[] expectedResolved = { "mpMetrics-1.0", "mpHealth-1.0" };

        test(SERVER_NAME_EE8, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    //

    public static final String SERVER_NAME_EE9 = "ee9toMP";

    @Test
    public void ee9toHealthAndMetricsMaxTest() throws Exception {
        String preferredVersions = MAX_VERSIONS;
        String[] expectedResolved = { "mpMetrics-4.0", "mpHealth-4.0" };

        test(SERVER_NAME_EE9, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }

    @Test
    public void ee9toHealthAndMetricsMinTest() throws Exception {
        String preferredVersions = MIN_VERSIONS;
        String[] expectedResolved = { "mpMetrics-4.0", "mpHealth-4.0" };

        test(SERVER_NAME_EE9, ALLOWED_ERRORS, preferredVersions, expectedResolved);
    }
}
