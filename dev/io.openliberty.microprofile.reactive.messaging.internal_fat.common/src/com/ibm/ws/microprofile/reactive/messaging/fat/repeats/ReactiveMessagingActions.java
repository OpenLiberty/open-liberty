/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.repeats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.rules.repeater.RepeatTests;

public class ReactiveMessagingActions {
    public static final String MP50_RM30_ID = MicroProfileActions.MP50_ID + "_RM30";

    //MP50 runs on Java 8 but RM30 will only run on Java11 or higher
    public static final FeatureSet MP50_RM30 = MicroProfileActions.MP50.addFeature("mpReactiveMessaging-3.0").removeFeature("mpTelemetry-1.0").addFeature("mpTelemetry-1.1").setMinJavaLevel(SEVersion.JAVA11)
                    .build(MP50_RM30_ID);

    //All MicroProfile ReactiveMessaging FeatureSets - must be descending order
    private static final List<FeatureSet> ALL;

    static {
        ALL = new ArrayList<>(MicroProfileActions.ALL);
        //put the updated FeatureSet in just before MP21
        ALL.add(ALL.indexOf(MicroProfileActions.MP50), MP50_RM30);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode.
     * The others will be run in the mode specified by otherFeatureSetsTestMode.
     *
     * If {@code firstFeatureSet} isn't compatible with the current Java version, we try to
     * replace it with the newest set from {@code otherFeatureSets} that is compatible.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           An ORDERED list of all the FeatureSets which may apply to this test. Newest FeatureSet should be first. Oldest last.
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @param  skipTransformation       Skip transformation for actions
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeatWithoutTransformation(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets), true);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode.
     * The others will be run in the mode specified by otherFeatureSetsTestMode.
     *
     * If {@code firstFeatureSet} isn't compatible with the current Java version, we try to
     * replace it with the newest set from {@code otherFeatureSets} that is compatible.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           An ORDERED list of all the FeatureSets which may apply to this test. Newest FeatureSet should be first. Oldest last.
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @param  skipTransformation       Skip transformation for actions
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeatWithTransformation(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets), false);
    }

    public static RepeatTests reactive30Repeats(String serverName) {
        return repeatWithoutTransformation(serverName,
        TestMode.FULL,
        MicroProfileActions.MP70_EE11,
        MicroProfileActions.MP61,
        MP50_RM30);
    }

    public static RepeatTests repeatDefault(String serverName) {
        return repeatWithTransformation(serverName,
        TestMode.FULL,
        MicroProfileActions.MP70_EE11, //RM30 + EE11
        MicroProfileActions.MP61, //RM30 + EE10
        MP50_RM30, //RM30 + EE9
        MicroProfileActions.MP20); //RM10 + EE8
    }

    public static RepeatTests telemetryRepeats(String serverName) {
        return repeatWithoutTransformation(serverName,
        TestMode.FULL,
        MicroProfileActions.MP70_EE11,
        MicroProfileActions.MP60,
        MP50_RM30);
    }

    public static RepeatTests repeatAboveMP61(String serverName) {
        return repeatWithTransformation(serverName,
        TestMode.FULL,
        MicroProfileActions.MP70_EE11,
        MicroProfileActions.MP61);
    }

}
