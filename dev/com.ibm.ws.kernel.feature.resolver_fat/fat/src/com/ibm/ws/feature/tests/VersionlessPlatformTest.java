/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;

/**
 * Test cases for variations on setting the platform.
 *
 * Test plan:
 *
 * <pre>
* Base configurations:
* 1: f servlet, f jsp
* 2: f mpMetrics, f mpHealth
* 3: f servlet, f jsp, f mpMetrics, f mpHealth
* Case 0: No platforms anywhere
*   Base 1, 2, 3
* Case 1: With platforms
*   Base 1, 2, 3
*   1: p javaee-7.0
*   2: p microProfile-3.2
*   3: p javaee-7.0, p microProfile-3.2
* Case 2: With environment variables
*   Base 1, 2, 3
*   1: e javaee-7.0
*   2: e microProfile-3.2
*   3: e javaee-7.0, e microProfile-3.2
* Case 3: With versioned features
*   1: v servlet-5.0 [Base 1, 3 only]
*   2: v mpMetrics-2.2 [Base 2, 3 only]
*   3: v servlet-5.0, v mpMetrics-2.2 Base 1, 2, 3
* Case 1-2: Mixing platforms and environment variables
*   Base 1, 2, 3
*   1: p javaee-7.0, e javaee-7.0
*   2: p microProfile-3.2, e microProfile-3.2
*   3: p javaee-7.0, e javaee-7.0, p microProfile-3.2, e microProfile-3.2
* Case 1-3: Mixing platforms and versioned features
*   Base 1, 2, 3
*   1: p javaee-7.0, v servlet-5.0
*   2: p microProfile-3.2, v mpMetrics-2.2
*   3: p javaee-7.0, v servlet-5.0, p microProfile-3.2, v mpMetrics-2.2
* Case 2-3: Mixing environment variables and versioned features
*   Base 1, 2, 3
*   1: e javaee-7.0, v servlet-5.0
*   2: e microProfile-3.2, v mpMetrics-2.2
*   3: e javaee-7.0, v servlet-5.0, e microProfile-3.2, v mpMetrics-2.2
* Case 1-2': Overriding environment variable with platform
*   1: e javaee-7.0, p javaee-8.0 [Base 1]
*   2: e microProfile-3.2, p microProfile-4.0 [Base 2]
*   3: e javaee-7.0, p javaee-8.0, e microProfile-3.2, p microProfile-4.0 [Base 3]
 * </pre>
 */
public class VersionlessPlatformTest {

    /*
     * 1: f servlet, f jsp
     * 2: f mpMetrics, f mpHealth
     * 3: f servlet, f jsp, f mpMetrics, f mpHealth
     */
    public void features_case1(VerifyCase testCase) {
        testCase.input.addRoot("servlet");
        testCase.input.addRoot("jsp");
    }

    public void features_case2(VerifyCase testCase) {
        testCase.input.addRoot("mpMetrics");
        testCase.input.addRoot("mpHealth");
    }

    public void features_case3(VerifyCase testCase) {
        features_case1(testCase);
        features_case2(testCase);
    }

    public void platforms_case0(VerifyCase testCase) {
        // None!
    }

    public void platforms_case1(VerifyCase testCase) {
        // None!
    }

    public void platforms_case2(VerifyCase testCase) {
        // None!
    }

    public void platforms_case3(VerifyCase testCase) {
        // None!
    }
}
