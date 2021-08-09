/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will always be run in LITE mode. The others will run in the mode specified.
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
     * Return a rule to repeat tests for FT 1.1 and 3.0
     * <p>
     * This is the default because FT 1.* and 2.*+ have a mostly separate implementation so we want to ensure both are tested
     * mp20Features includes FT 1.1, and as it is an older version it will only run in full mode.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatDefault(String server) {
        return repeat(server, TestMode.FULL, MicroProfileActions.MP40, MicroProfileActions.MP20);
    }

    /**
     * Return a rule to repeat tests for FT 1.0, 1.1, 2.0, 2.1 and 3.0
     * <p>
     * We run a few tests using this rule so that we have some coverage of all implementations
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeatAll(String server) {
        return repeat(server, TestMode.LITE, MicroProfileActions.MP13, MicroProfileActions.MP20, MicroProfileActions.MP22, MicroProfileActions.MP30, MicroProfileActions.MP32,
                      MicroProfileActions.MP33, MicroProfileActions.MP40);
    }

    /**
     * Repeat on FaultTolerance 2.0 and above (MP22 and above). MP40 will be in LITE mode, the others in FULL mode.
     *
     * @param server the server name
     * @return the RepeatTests rule
     */
    public static RepeatTests repeat20AndAbove(String server) {
        return repeat(server, TestMode.FULL, MicroProfileActions.MP40, MicroProfileActions.MP22, MicroProfileActions.MP30, MicroProfileActions.MP32,
                      MicroProfileActions.MP33);
    }

}
