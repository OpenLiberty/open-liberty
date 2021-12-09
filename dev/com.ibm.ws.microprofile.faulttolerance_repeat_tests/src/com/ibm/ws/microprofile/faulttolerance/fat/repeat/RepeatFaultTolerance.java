/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.fat.repeat;

import java.util.HashSet;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

/**
 * Contains static methods for creating standard RepeatTests rules for Fault Tolerance tests
 *
 * MicroProfile 1.2 = Fault Tolerance 1.0 (Original Implementation)
 * MicroProfile 1.3 = Fault Tolerance 1.0
 * MicroProfile 1.4 = Fault Tolerance 1.1
 * MicroProfile 2.0 = Fault Tolerance 1.1
 * MicroProfile 2.1 = Fault Tolerance 1.1
 * MicroProfile 2.2 = Fault Tolerance 2.0 (Improved Implementation)
 * MicroProfile 3.0 = Fault Tolerance 2.0
 * MicroProfile 3.2 = Fault Tolerance 2.0
 * MicroProfile 3.3 = Fault Tolerance 2.1
 * MicroProfile 4.0 = Fault Tolerance 3.0
 * MicroProfile 4.1 = Fault Tolerance 3.0
 * MicroProfile 5.0 = Fault Tolerance 4.0 (Transformed Impl for EE9)
 */
public class RepeatFaultTolerance {

    public static final String MP21_METRICS20_ID = "MP21_METRICS20";

    public static final FeatureSet MP21_METRICS20 = MicroProfileActions.MP21.removeFeature("mpMetrics-1.1").addFeature("mpMetrics-2.0").build(MP21_METRICS20_ID);

    public static final Set<FeatureSet> ALL;
    static {
        ALL = new HashSet<>(MicroProfileActions.ALL);
        ALL.add(MP21_METRICS20);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will always be run in LITE mode.
     * The others will run in the mode specified.
     *
     * @param server                   The server to repeat on
     * @param otherFeatureSetsTestMode The mode to repeate the other FeatureSets in
     * @param firstFeatureSet          The first FeatureSet
     * @param otherFeatureSets         The other FeatureSets
     * @return a RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return MicroProfileActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, otherFeatureSets);
    }

    /**
     * Create a FeatureReplacementAction for MicroProfile 2.1 (FT 1.1) plus MP Metrics 2.0
     *
     * @param server The server to repeat on
     * @return the new action
     */
    public static FeatureReplacementAction ft11metrics20Features(String server) {
        return MicroProfileActions.forFeatureSet(ALL, MP21_METRICS20, server, TestMode.LITE);
    }

    /**
     * Return a rule to repeat tests for MicroProfile 5.0, 4.0 and 2.0.
     * This translates to FT 4.0, 3.0 and 1.1 respectively.
     * <p>
     * FT 1.x has a mostly separate implementation from 2.x and higher.
     * FT 4.0 is a transformed version of 3.0, which works on EE9.
     *
     * This is the default set of repeats for most FT tests. It provides good coverage of most code paths.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault(String server) {
        return repeat(server, TestMode.FULL, MicroProfileActions.MP50, MicroProfileActions.MP40, MicroProfileActions.MP20);
    }

    /**
     * Return a rule to repeat tests for FT 1.0, 1.1, 2.0, 2.1, 3.0 and 4.0.
     * 4.0 will be in LITE mode, the others in FULL mode.
     * <p>
     * We run a few tests using this rule so that we have some coverage of all implementations
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatAll(String server) {
        return repeat(server, TestMode.LITE, MicroProfileActions.MP50, MicroProfileActions.MP13, MicroProfileActions.MP20, MicroProfileActions.MP22, MicroProfileActions.MP30,
                      MicroProfileActions.MP32,
                      MicroProfileActions.MP33, MicroProfileActions.MP40);
    }

    /**
     * Repeat on FaultTolerance 2.0 and above (MP22 and above). MP50 (FT 4.0) will be in LITE mode, the others in FULL mode.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeat20AndAbove(String server) {
        return repeat(server, TestMode.FULL, MicroProfileActions.MP50, MicroProfileActions.MP40, MicroProfileActions.MP22, MicroProfileActions.MP30, MicroProfileActions.MP32,
                      MicroProfileActions.MP33);
    }

}
