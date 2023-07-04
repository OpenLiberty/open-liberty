/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

public class ReactiveMessagingActions {
    public static final String MP20_ID = MicroProfileActions.MP20_ID + "_MPRM10";
    public static final String MP41_ID = MicroProfileActions.MP41_ID + "_MPRM10";
    public static final String MP50_ID = MicroProfileActions.MP50_ID + "_MPRM30";
    public static final String MP60_ID = MicroProfileActions.MP60_ID + "_MPRM30";
    public static final String MP61_ID = MicroProfileActions.MP61_ID + "_MPRM30";
    public static final FeatureSet MP20 = MicroProfileActions.MP20.addFeature("mpReactiveMessaging-1.0").build(MP20_ID);
    public static final FeatureSet MP41 = MicroProfileActions.MP41.addFeature("mpReactiveMessaging-1.0").build(MP41_ID);
    public static final FeatureSet MP50 = MicroProfileActions.MP50.addFeature("mpReactiveMessaging-3.0").build(MP50_ID);
    public static final FeatureSet MP60 = MicroProfileActions.MP60.addFeature("mpReactiveMessaging-3.0").build(MP60_ID);
    public static final FeatureSet MP61 = MicroProfileActions.MP61.addFeature("mpReactiveMessaging-3.0").build(MP61_ID);

    public static final Set<FeatureSet> ALL = new HashSet<>(MicroProfileActions.ALL);
    static {
        ALL.add(MP20);
        ALL.add(MP41);
        ALL.add(MP50);
        ALL.add(MP60);
        ALL.add(MP61);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in FULL.
     *
     * @param server The server to repeat on
     * @param firstFeatureSet The first FeatureSet
     * @param otherFeatureSets The other FeatureSets
     * @return a RepeatTests instance
     */
    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return repeat(server, TestMode.FULL, firstFeatureSet, otherFeatureSets);
    }

    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return MicroProfileActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets));
    }
}
