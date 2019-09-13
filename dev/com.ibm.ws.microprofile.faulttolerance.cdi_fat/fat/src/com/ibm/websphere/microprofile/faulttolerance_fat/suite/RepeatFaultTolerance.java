/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.suite;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Contains static methods for creating standard RepeatTests rules for Fault Tolerance tests
 */
public class RepeatFaultTolerance {

    static final String[] MP13_FEATURES_ARRAY = { "mpConfig-1.2", "mpFaultTolerance-1.0", "servlet-3.1", "cdi-1.2", "appSecurity-2.0", "mpMetrics-1.0" };
    static final Set<String> MP13_FEATURE_SET = new HashSet<>(Arrays.asList(MP13_FEATURES_ARRAY));
    public static final String MP13_FEATURES_ID = "MICROPROFILE13";

    static final String[] MP20_FEATURES_ARRAY = { "mpConfig-1.3", "mpFaultTolerance-1.1", "servlet-4.0", "cdi-2.0", "appSecurity-3.0", "mpMetrics-1.1" };
    static final Set<String> MP20_FEATURE_SET = new HashSet<>(Arrays.asList(MP20_FEATURES_ARRAY));
    public static final String MP20_FEATURES_ID = "MICROPROFILE20";

    static final String[] FT20_METRICS11_ARRAY = { "mpConfig-1.3", "mpFaultTolerance-2.0", "servlet-4.0", "cdi-2.0", "appSecurity-3.0", "mpMetrics-1.1" };
    static final Set<String> FT20_METRICS11_FEATURE_SET = new HashSet<>(Arrays.asList(FT20_METRICS11_ARRAY));
    public static final String FT20_METRICS11_ID = "FT20_METRICS11";

    static final String[] MP30_FEATURES_ARRAY = { "mpConfig-1.3", "mpFaultTolerance-2.0", "servlet-4.0", "cdi-2.0", "appSecurity-3.0", "mpMetrics-2.0" };
    static final Set<String> MP30_FEATURE_SET = new HashSet<>(Arrays.asList(MP30_FEATURES_ARRAY));
    public static final String MP30_FEATURES_ID = "MICROPROFILE30";

    static final String[] FT11_METRICS20_ARRAY = { "mpConfig-1.3", "mpFaultTolerance-1.1", "servlet-4.0", "cdi-2.0", "appSecurity-3.0", "mpMetrics-2.0" };
    static final Set<String> FT11_METRICS20_FEATURE_SET = new HashSet<>(Arrays.asList(FT11_METRICS20_ARRAY));
    public static final String FT11_METRICS20_ID = "FT11_METRICS20";

    static final Set<String> ALL_FEATURE_SET = new HashSet<>();
    static {
        ALL_FEATURE_SET.addAll(MP13_FEATURE_SET);
        ALL_FEATURE_SET.addAll(MP20_FEATURE_SET);
        ALL_FEATURE_SET.addAll(FT20_METRICS11_FEATURE_SET);
        ALL_FEATURE_SET.addAll(MP30_FEATURE_SET);
        ALL_FEATURE_SET.addAll(FT11_METRICS20_FEATURE_SET);
    }

    public static FeatureReplacementAction mp20Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, MP20_FEATURE_SET)
                        .withID(MP20_FEATURES_ID)
                        .forceAddFeatures(false)
                        .forServers(server);
    }

    public static FeatureReplacementAction mp13Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, MP13_FEATURE_SET)
                        .withID(MP13_FEATURES_ID)
                        .forceAddFeatures(false)
                        .forServers(server);
    }

    public static FeatureReplacementAction ft20metrics11Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, FT20_METRICS11_FEATURE_SET)
                        .withID(FT20_METRICS11_ID)
                        .forceAddFeatures(false)
                        .forServers(server);
    }

    public static FeatureReplacementAction mp30Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, MP30_FEATURE_SET)
                        .withID(MP30_FEATURES_ID)
                        .forceAddFeatures(false)
                        .forServers(server);
    }

    public static FeatureReplacementAction ft11metrics20Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, FT11_METRICS20_FEATURE_SET)
                        .withID(FT11_METRICS20_ID)
                        .forceAddFeatures(false)
                        .forServers(server);
    }

    /**
     * Return a rule to repeat tests for FT 1.1 and 2.0
     * <p>
     * This is the default because FT 1.1 and 2.0 have a mostly separate implementation so we want to ensure both are tested
     * mp20Features includes FT 1.1, as it is an older version it will only run in full mode.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault(String server) {
        return RepeatTests.with(mp20Features(server).fullFATOnly())
                        .andWith(mp30Features(server));
    }

    /**
     * Return a rule to repeat tests for FT 1.0, 1.1 and 2.0
     * <p>
     * We run a few tests using this rule so that we have some coverage of all implementations
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatAll(String server) {
        return RepeatTests.with(mp13Features(server))
                        .andWith(mp20Features(server))
                        .andWith(ft20metrics11Features(server))
                        .andWith(mp30Features(server));
    }

}
