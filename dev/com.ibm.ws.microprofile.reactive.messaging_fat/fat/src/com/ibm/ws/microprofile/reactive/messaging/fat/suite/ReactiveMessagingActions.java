/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

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
    public static final FeatureSet MP50_RM30 = MicroProfileActions.MP50.addFeature("mpReactiveMessaging-3.0").setMinJavaLevel(SEVersion.JAVA11).build(MP50_RM30_ID);

    //All MicroProfile ReactiveMessaging FeatureSets - must be descending order
    private static final List<FeatureSet> ALL;

    static {
        ALL = new ArrayList<>(MicroProfileActions.ALL);
        //put the updated FeatureSet in just before MP50
        ALL.add(ALL.indexOf(MicroProfileActions.MP50), MP50_RM30);
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
        return RepeatActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets));
    }

    public static RepeatTests repeatDefault(String server) {
        return repeat(server,
                      MicroProfileActions.MP70_EE11, //RM30 + EE11
                      MicroProfileActions.MP61, //RM30 + EE10
                      ReactiveMessagingActions.MP50_RM30, //RM30 + EE9
                      MicroProfileActions.MP20); //RM10 + EE8
    }

}
