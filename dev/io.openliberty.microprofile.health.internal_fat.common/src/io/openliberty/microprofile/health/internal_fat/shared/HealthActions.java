/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.internal_fat.shared;

import java.util.Arrays;
import java.util.List;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatTests;

public class HealthActions {

    public static final String MP14_MPHEALTH40_ID = EE7FeatureReplacementAction.ID + "_MPHEALTH40_MP14";
    public static final String MP41_MPHEALTH40_ID = EE8FeatureReplacementAction.ID + "_MPHEALTH40_MP41";

    public static final String MP61_MPHEALTH40_ID = MicroProfileActions.MP61_ID + "_MPHEALTH40";

    public static final FeatureSet MP41_MPHEALTH40 = MicroProfileActions.MP41.removeFeature("mpHealth-3.1").addFeature("mpHealth-4.0").build(MP41_MPHEALTH40_ID);

    public static final FeatureSet MP14_MPHEALTH40 = MicroProfileActions.MP14.removeFeature("mpHealth-1.0").addFeature("mpHealth-4.0").build(MP14_MPHEALTH40_ID);

    //All MicroProfile Health FeatureSets - must be descending order
    private static final FeatureSet[] ALL_MPHEALTH_SETS_ARRAY = { MicroProfileActions.MP70_EE11,
                                                                  MicroProfileActions.MP70_EE10,
                                                                  MicroProfileActions.MP61,
                                                                  MicroProfileActions.MP60,
                                                                  MicroProfileActions.MP50,
                                                                  MP41_MPHEALTH40,
                                                                  MicroProfileActions.MP41,
                                                                  MP14_MPHEALTH40 };

    private static final List<FeatureSet> ALL_MPTEL_SETS_LIST = Arrays.asList(ALL_MPHEALTH_SETS_ARRAY);

    /**
     * Get a repeat action which runs the given feature set
     * <p>
     * The returned FeatureReplacementAction can then be configured further
     *
     * @param server     the server to repeat on
     * @param featureSet the featureSet to repeat with
     * @return a FeatureReplacementAction
     */
    public static FeatureReplacementAction repeatFor(String server, FeatureSet featureSet) {
        return RepeatActions.forFeatureSet(ALL_MPTEL_SETS_LIST, featureSet, new String[] { server }, TestMode.FULL);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in FULL.
     *
     * @param server           The server to repeat on
     * @param firstFeatureSet  The first FeatureSet
     * @param otherFeatureSets The other FeatureSets
     * @return a RepeatTests instance
     */
    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return repeat(server, TestMode.FULL, firstFeatureSet, otherFeatureSets);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in the mode specified.
     *
     * @param server                   The server to repeat on
     * @param otherFeatureSetsTestMode The mode to run the other FeatureSets
     * @param firstFeatureSet          The first FeatureSet
     * @param otherFeatureSets         The other FeatureSets
     * @return a RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(server, otherFeatureSetsTestMode, ALL_MPTEL_SETS_LIST, firstFeatureSet, Arrays.asList(otherFeatureSets));
    }

    public static RepeatTests mpHealth40Repeats(String serverName) {
        return repeat(serverName,
                      MicroProfileActions.MP70_EE11,
                      MicroProfileActions.MP70_EE10,
                      MicroProfileActions.MP61,
                      MicroProfileActions.MP50,
                      MP41_MPHEALTH40,
                      MP14_MPHEALTH40);
    }

    public static RepeatTests health40Repeats() {
        return mpHealth40Repeats(FeatureReplacementAction.ALL_SERVERS);
    }

    public static RepeatTests mp60Repeat(String serverName) {
        return repeat(serverName, MicroProfileActions.MP60);
    }

    public static RepeatTests allMPRepeats(String serverName) {
        return repeat(serverName,
                      MicroProfileActions.MP70_EE11,
                      MicroProfileActions.MP70_EE10,
                      MicroProfileActions.MP61,
                      MicroProfileActions.MP60,
                      MP41_MPHEALTH40,
                      MP14_MPHEALTH40);
    }

}
