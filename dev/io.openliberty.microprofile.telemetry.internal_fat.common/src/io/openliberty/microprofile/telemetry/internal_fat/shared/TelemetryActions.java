/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.shared;

import java.util.Arrays;
import java.util.List;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.rules.repeater.RepeatTests;

public class TelemetryActions {
    public static final String MP14_MPTEL11_ID = MicroProfileActions.MP14_ID + "_MPTEL11";
    public static final String MP41_MPTEL11_ID = MicroProfileActions.MP41_ID + "_MPTEL11";
    public static final String MP50_MPTEL11_ID = MicroProfileActions.MP50_ID + "_MPTEL11";

    //Telemetry 2.0 repeats should not start with "MicroProfile_xx" as RepeatTestFilter uses startsWith() which makes them indisinguishable to mpTelemetry-1.1 in tests
    public static final String MP14_MPTEL20_ID = EE7FeatureReplacementAction.ID + "_MPTEL20_MP14";
    public static final String MP41_MPTEL20_ID = EE8FeatureReplacementAction.ID + "_MPTEL20_MP41";
    public static final String MP50_MPTEL20_ID = JakartaEEAction.EE9_ACTION_ID + "_MPTEL20_MP50";
    public static final String MP50_MPTEL20_JAVA8_ID = JakartaEEAction.EE9_ACTION_ID + "_MPTEL20_MP50_JAVA8";

    public static final FeatureSet MP14_MPTEL11 = MicroProfileActions.MP14
                    .addFeature("mpTelemetry-1.1")
                    .setMinJavaLevel(SEVersion.JAVA11)
                    .build(MP14_MPTEL11_ID);

    public static final FeatureSet MP41_MPTEL11 = MicroProfileActions.MP41
                    .addFeature("mpTelemetry-1.1")
                    .setMinJavaLevel(SEVersion.JAVA11)
                    .build(MP41_MPTEL11_ID);

    public static final FeatureSet MP50_MPTEL11 = MicroProfileActions.MP50
                    .addFeature("mpTelemetry-1.1")
                    .addFeature("mpReactiveMessaging-3.0")
                    .addFeature("mpReactiveStreams-3.0")
                    .setMinJavaLevel(SEVersion.JAVA11)
                    .build(MP50_MPTEL11_ID);

    public static final FeatureSet MP50_MPTEL20 = MicroProfileActions.MP50
                    .addFeature("mpTelemetry-2.0")
                    .addFeature("mpReactiveMessaging-3.0")
                    .addFeature("mpReactiveStreams-3.0")
                    .setMinJavaLevel(SEVersion.JAVA11)
                    .build(MP50_MPTEL20_ID);

    public static final FeatureSet MP50_MPTEL20_JAVA8 = MicroProfileActions.MP50
                    .addFeature("mpTelemetry-2.0")
                    .build(MP50_MPTEL20_JAVA8_ID);

    public static final FeatureSet MP41_MPTEL20 = MicroProfileActions.MP41
                    .addFeature("mpTelemetry-2.0")
                    .build(MP41_MPTEL20_ID);

    public static final FeatureSet MP14_MPTEL20 = MicroProfileActions.MP14
                    .addFeature("mpTelemetry-2.0")
                    .build(MP14_MPTEL20_ID);

    //All MicroProfile Telemetry FeatureSets - must be descending order
    private static final FeatureSet[] ALL_MPTEL_SETS_ARRAY = { MicroProfileActions.MP70_EE11,
                                                               MicroProfileActions.MP70_EE10,
                                                               MicroProfileActions.MP61,
                                                               MicroProfileActions.MP60,
                                                               MP50_MPTEL20,
                                                               MP50_MPTEL20_JAVA8,
                                                               MP50_MPTEL11,
                                                               MP41_MPTEL20,
                                                               MP41_MPTEL11,
                                                               MP14_MPTEL20,
                                                               MP14_MPTEL11 };

    private static final List<FeatureSet> ALL_MPTEL_SETS_LIST = Arrays.asList(ALL_MPTEL_SETS_ARRAY);

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

}
