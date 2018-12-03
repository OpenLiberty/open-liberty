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

    static final String[] MP13_FEATURES_ARRAY = { "mpConfig-1.2", "mpFaultTolerance-1.0", "servlet-3.1", "cdi-1.2", "appSecurity-2.0" };
    static final Set<String> MP13_FEATURE_SET = new HashSet<>(Arrays.asList(MP13_FEATURES_ARRAY));
    static final String MP13_FEATURES_ID = "MICROPROFILE13";

    static final String[] MP20_FEATURES_ARRAY = { "mpConfig-1.3", "mpFaultTolerance-1.1", "servlet-4.0", "cdi-2.0", "appSecurity-3.0" };
    static final Set<String> MP20_FEATURE_SET = new HashSet<>(Arrays.asList(MP20_FEATURES_ARRAY));
    static final String MP20_FEATURES_ID = "MICROPROFILE20";

    static final String[] FT20_FEATURES_ARRAY = { "mpConfig-1.3", "mpFaultTolerance-2.0", "servlet-4.0", "cdi-2.0", "appSecurity-3.0" };
    static final Set<String> FT20_FEATURE_SET = new HashSet<>(Arrays.asList(FT20_FEATURES_ARRAY));
    static final String FT20_FEATURES_ID = "FAULTTOLERANCE20";

    static final Set<String> ALL_FEATURE_SET = new HashSet<>();
    static {
        ALL_FEATURE_SET.addAll(MP13_FEATURE_SET);
        ALL_FEATURE_SET.addAll(MP20_FEATURE_SET);
        ALL_FEATURE_SET.addAll(FT20_FEATURE_SET);
    }

    public static FeatureReplacementAction mp20Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, MP20_FEATURE_SET)
                        .withID(MP20_FEATURES_ID)
                        .forServers(server);
    }

    public static FeatureReplacementAction mp13Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, MP13_FEATURE_SET)
                        .withID(MP13_FEATURES_ID)
                        .forServers(server);
    }

    public static FeatureReplacementAction ft20Features(String server) {
        return new FeatureReplacementAction(ALL_FEATURE_SET, FT20_FEATURE_SET)
                        .withID(FT20_FEATURES_ID)
                        .forServers(server);
    }

    /**
     * Return a rule to repeat tests for FT 1.1 and 2.0
     * <p>
     * This is the default because FT 1.1 and 2.0 have a mostly separate implementation so we want to ensure both are tested
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault(String server) {
        return RepeatTests.with(mp20Features(server))
                        .andWith(ft20Features(server));
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
                        .andWith(ft20Features(server));
    }

}
