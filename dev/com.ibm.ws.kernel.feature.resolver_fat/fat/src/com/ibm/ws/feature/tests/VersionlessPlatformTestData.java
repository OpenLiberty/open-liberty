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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

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
*
* Case 0: No platforms anywhere
*
* Case 1: With platforms
*   1: p javaee-7.0
*   2: p microProfile-3.2
*   3: p javaee-7.0, p microProfile-3.2
*
* Case 2: With environment variables
*   1: e javaee-7.0
*   2: e microProfile-3.2
*   3: e javaee-7.0, e microProfile-3.2
*
* Case 3: With versioned features
*   1: v servlet-5.0
*   2: v mpMetrics-2.2
*   3: v servlet-5.0, v mpMetrics-2.2
*
* Case 1-2: Mixing platforms and environment variables
*   1: p javaee-7.0, e javaee-7.0
*   2: p microProfile-3.2, e microProfile-3.2
*   3: p javaee-7.0, e javaee-7.0, p microProfile-3.2, e microProfile-3.2
*
* Case 1-3: Mixing platforms and versioned features
*   1: p javaee-7.0, v servlet-5.0
*   2: p microProfile-3.2, v mpMetrics-2.2
*   3: p javaee-7.0, v servlet-5.0, p microProfile-3.2, v mpMetrics-2.2
*
* Case 2-3: Mixing environment variables and versioned features
*   1: e javaee-7.0, v servlet-5.0
*   2: e microProfile-3.2, v mpMetrics-2.2
*   3: e javaee-7.0, v servlet-5.0, e microProfile-3.2, v mpMetrics-2.2
*
* Case 1-2': Overriding environment variable with platform
*   1: e javaee-7.0, p javaee-8.0 [Base 1]
*   2: e microProfile-3.2, p microProfile-4.0 [Base 2]
*   3: e javaee-7.0, p javaee-8.0, e microProfile-3.2, p microProfile-4.0 [Base 3]
 * </pre>
 */
@SuppressWarnings("unchecked")
public class VersionlessPlatformTestData {

    public static class PlatformCase {
        public final String name;

        public final List<Consumer<VerifyCase>> inits;

        public PlatformCase(String name, Consumer<VerifyCase>... inits) {
            this.name = name;

            List<Consumer<VerifyCase>> useInits = new ArrayList<Consumer<VerifyCase>>(inits.length);
            for (Consumer<VerifyCase> init : inits) {
                useInits.add(init);
            }
            this.inits = useInits;
        }

        public void init(VerifyCase verifyCase) {
            for (Consumer<VerifyCase> init : inits) {
                init.accept(verifyCase);
            }
        }

        public VerifyCase init() {
            VerifyCase verifyCase = new VerifyCase(name, "Platform case [ " + name + " ]", false);
            init(verifyCase);
            return verifyCase;
        }
    }

    public static Map<String, PlatformCase> platformCases;

    public static void mapCase(Map<String, PlatformCase> map, String name, Consumer<VerifyCase>... inits) {
        map.put(name, new PlatformCase(name, inits));
    }

    public static Map<String, VerifyCase> getCases() {
        Map<String, VerifyCase> useCases = new HashMap<>(platformCases.size());
        for (PlatformCase useCase : platformCases.values()) {
            useCases.put(useCase.name, useCase.init());
        }
        return useCases;
    }

    // @formatter:off
    static {
        Map<String, PlatformCase> useCases = new HashMap<>();

        // null cases

        mapCase(useCases, "features_none_platforms_none",
                VersionlessPlatformTestData::features_none,
                VersionlessPlatformTestData::platforms_none);
        mapCase(useCases, "features_ee_platforms_none",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_none);
        mapCase(useCases, "features_mp_platforms_none",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_none);
        mapCase(useCases, "features_mixed_platforms_none",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_none);

        // simple cases

        mapCase(useCases, "features_ee_platforms_p_ee",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_p_ee);
        mapCase(useCases, "features_mp_platforms_p_mp",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_p_mp);
        mapCase(useCases, "features_mixed_platforms_p_mixed",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_p_mixed);

        mapCase(useCases, "features_ee_platforms_e_ee",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_e_ee);
        mapCase(useCases, "features_mp_platforms_e_mp",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_e_mp);
        mapCase(useCases, "features_mixed_platforms_e_mixed",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_e_mixed);

        mapCase(useCases, "features_ee_platforms_v_servlet",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_v_servlet);
        mapCase(useCases, "features_mp_platforms_v_mpMetrics",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_v_mpMetrics);
        mapCase(useCases, "features_mixed_platforms_v_mixed",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_v_mixed);

        // mixed cases ...

        mapCase(useCases, "features_ee_platforms_ep_ee",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_ep_ee);
        mapCase(useCases, "features_mp_platforms_ep_mp",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_ep_mp);
        mapCase(useCases, "features_mixed_platforms_ep_mixed",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_ep_mixed);

        mapCase(useCases, "features_ee_platforms_ev_ee",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_ev_ee);
        mapCase(useCases, "features_mp_platforms_ev_mp",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_ev_mp);
        mapCase(useCases, "features_mixed_platforms_ev_mixed",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_ev_mixed);

        mapCase(useCases, "features_ee_platforms_pv_ee",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_pv_ee);
        mapCase(useCases, "features_mp_platforms_pv_mpMetrics",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_pv_mp);
        mapCase(useCases, "features_mixed_platforms_pv_mixed",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_pv_mixed);

        // override cases ...

        /*
         * Case 1-2': Overriding environment variable with platform
         * 1: e javaee-7.0, p javaee-8.0 [Base 1]
         * 2: e microProfile-3.2, p microProfile-4.0 [Base 2]
         * 3: e javaee-7.0, p javaee-8.0, e microProfile-3.2, p microProfile-4.0 [Base 3]
         */

        mapCase(useCases, "features_ee_platforms_ep_override",
                VersionlessPlatformTestData::features_ee,
                VersionlessPlatformTestData::platforms_ep_ee_override);

        mapCase(useCases, "features_mp_platforms_ep_override",
                VersionlessPlatformTestData::features_mp,
                VersionlessPlatformTestData::platforms_ep_mp_override);

        mapCase(useCases, "features_mixed_platforms_ep_override",
                VersionlessPlatformTestData::features_mixed,
                VersionlessPlatformTestData::platforms_ep_mixed_override);

        platformCases = useCases;
    }
    // @formatter:on

    /*
     * 1: f servlet, f jsp
     * 2: f mpMetrics, f mpHealth
     * 3: f servlet, f jsp, f mpMetrics, f mpHealth
     */

    public static final String SERVLET = "servlet";
    public static final String SERVLET_50 = "servlet-5.0";

    public static final String JSP = "jsp";

    public static final String MPMETRICS = "mpMetrics";
    public static final String MPMETRICS_22 = "mpMetrics-2.2";

    public static final String MPHEALTH = "mpHealth";

    public static void features_none(VerifyCase testCase) {
        // Empty!
    }

    public static void features_ee(VerifyCase testCase) {
        testCase.input.addRoot(SERVLET);
        testCase.input.addRoot(JSP);
    }

    public static void features_mp(VerifyCase testCase) {
        testCase.input.addRoot(MPMETRICS);
        testCase.input.addRoot(MPHEALTH);
    }

    public static void features_mixed(VerifyCase testCase) {
        features_ee(testCase);
        features_mp(testCase);
    }

    /*
     * Case 0: No platforms anywhere
     * Case 1: With platforms
     * 1: p javaee-7.0
     * 2: p microProfile-3.2
     * 3: p javaee-7.0, p microProfile-3.2
     * Case 2: With environment variables
     * 1: e javaee-7.0
     * 2: e microProfile-3.2
     * 3: e javaee-7.0, e microProfile-3.2
     * Case 3: With versioned features
     * 1: v servlet-5.0
     * 2: v mpMetrics-2.2
     * 3: v servlet-5.0, v mpMetrics-2.2
     */

    public static final String JAVAEE_70 = "javaee-7.0";
    public static final String JAVAEE_80 = "javaee-8.0";

    public static final String MICROPROFILE_32 = "microProfile-3.2";
    public static final String MICROPROFILE_40 = "microProfile-4.0";

    public static void platforms_none(VerifyCase testCase) {
        // None!
    }

    public static void platforms_p_ee(VerifyCase testCase) {
        testCase.input.addPlatform(JAVAEE_70);
    }

    public static void platforms_p_mp(VerifyCase testCase) {
        testCase.input.addPlatform(MICROPROFILE_32);
    }

    public static void platforms_p_mixed(VerifyCase testCase) {
        platforms_p_ee(testCase);
        platforms_p_mp(testCase);
    }

    public static final String PLATFORM_ENV_VAR = FeatureResolver.PREFERRED_PLATFORM_VERSIONS_PROPERTY_NAME;

    public static final String delimit(char delimiter, String... values) {
        if (values.length == 0) {
            return "";
        }

        int len = values.length - 1;
        for (String value : values) {
            len += value.length();
        }

        StringBuilder builder = new StringBuilder(len);
        for (int vNo = 0; vNo < values.length; vNo++) {
            if (vNo > 0) {
                builder.append(delimiter);
            }
            builder.append(values[vNo]);
        }
        return builder.toString();
    }

    public static void platforms_e_ee(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, JAVAEE_70);
    }

    public static void platforms_e_mp(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, MICROPROFILE_32);
    }

    public static void platforms_e_mixed(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, delimit(',', JAVAEE_70, MICROPROFILE_32));
    }

    /*
     * Case 3: With versioned features
     * 1: v servlet-5.0
     * 2: v mpMetrics-2.2
     * 3: v servlet-5.0, v mpMetrics-2.2
     */

    public static void platforms_v_servlet(VerifyCase testCase) {
        testCase.input.addRoot(SERVLET_50);
    }

    public static void platforms_v_mpMetrics(VerifyCase testCase) {
        testCase.input.addRoot(MPMETRICS_22);
    }

    public static void platforms_v_mixed(VerifyCase testCase) {
        platforms_v_servlet(testCase);
        platforms_v_mpMetrics(testCase);
    }

    /*
     * Case 1-2: Mixing platforms and environment variables
     * 1: p javaee-7.0, e javaee-7.0
     * 2: p microProfile-3.2, e microProfile-3.2
     * 3: p javaee-7.0, e javaee-7.0, p microProfile-3.2, e microProfile-3.2
     */

    public static void platforms_ep_ee(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, JAVAEE_70);
        testCase.input.addPlatform(JAVAEE_70);
    }

    public static void platforms_ep_mp(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, MICROPROFILE_32);
        testCase.input.addPlatform(MICROPROFILE_32);
    }

    public static void platforms_ep_mixed(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, delimit(',', JAVAEE_70, MICROPROFILE_32));
        testCase.input.addPlatform(JAVAEE_70);
        testCase.input.addPlatform(MICROPROFILE_32);
    }

    /*
     * Case 1-3: Mixing platforms and versioned features
     * 1: p javaee-7.0, v servlet-5.0
     * 2: p microProfile-3.2, v mpMetrics-2.2
     * 3: p javaee-7.0, v servlet-5.0, p microProfile-3.2, v mpMetrics-2.2
     */

    public static void platforms_pv_ee(VerifyCase testCase) {
        testCase.input.addPlatform(JAVAEE_70);
        testCase.input.addRoot(SERVLET_50);
    }

    public static void platforms_pv_mp(VerifyCase testCase) {
        testCase.input.addPlatform(MICROPROFILE_32);
        testCase.input.addRoot(MPMETRICS_22);
    }

    public static void platforms_pv_mixed(VerifyCase testCase) {
        platforms_pv_ee(testCase);
        platforms_pv_mp(testCase);
    }

    //

    /*
     * Case 2-3: Mixing environment variables and versioned features
     * 1: e javaee-7.0, v servlet-5.0
     * 2: e microProfile-3.2, v mpMetrics-2.2
     * 3: e javaee-7.0, v servlet-5.0, e microProfile-3.2, v mpMetrics-2.2
     */

    public static void platforms_ev_ee(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, JAVAEE_70);
        testCase.input.addRoot(SERVLET_50);
    }

    public static void platforms_ev_mp(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, MICROPROFILE_32);
        testCase.input.addRoot(MPMETRICS_22);
    }

    public static void platforms_ev_mixed(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, delimit(',', JAVAEE_70, MICROPROFILE_32));
        testCase.input.addRoot(SERVLET_50);
        testCase.input.addRoot(MPMETRICS_22);
    }

    /*
     * Case 1-2': Overriding environment variable with platform
     * 1: e javaee-7.0, p javaee-8.0 [Base 1]
     * 2: e microProfile-3.2, p microProfile-4.0 [Base 2]
     * 3: e javaee-7.0, p javaee-8.0, e microProfile-3.2, p microProfile-4.0 [Base 3]
     */

    public static void platforms_ep_ee_override(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, JAVAEE_70);
        testCase.input.addPlatform(JAVAEE_80);
    }

    public static void platforms_ep_mp_override(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, MICROPROFILE_32);
        testCase.input.addPlatform(MICROPROFILE_40);
    }

    public static void platforms_ep_mixed_override(VerifyCase testCase) {
        testCase.input.putEnv(PLATFORM_ENV_VAR, delimit(',', JAVAEE_70, MICROPROFILE_32));
        testCase.input.addPlatform(JAVAEE_80);
        testCase.input.addPlatform(MICROPROFILE_40);
    }
}
