/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.suite;

import java.util.HashSet;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                ReactiveStreamsContextTest.class,
                ReactiveStreamsTest.class,
                ReactiveJaxRSTest.class,
                ReactiveConcurrentWorkTest.class
})
public class FATSuite {
    public static final String MPRS10_ID = "MPRS10";
    public static final String MPRS30_ID = "MPRS30";

    public static final FeatureSet MPRS10 = MicroProfileActions.MP21.addFeature("mpReactiveStreams-1.0").build(MicroProfileActions.MP21_ID + "_" + MPRS10_ID);
    public static final FeatureSet MPRS30 = MicroProfileActions.MP50.addFeature("mpReactiveStreams-3.0").build(MicroProfileActions.MP50_ID + "_" + MPRS30_ID);

    public static final Set<FeatureSet> ALL;
    static {
        ALL = new HashSet<>(MicroProfileActions.ALL);
        ALL.addAll(MicroProfileActions.STANDALONE_ALL);
        ALL.add(MPRS10);
        ALL.add(MPRS30);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will always be run in LITE mode.
     * The others will run in the mode specified.
     *
     * @param server The server to repeat on
     * @param otherFeatureSetsTestMode The mode to repeate the other FeatureSets in
     * @param firstFeatureSet The first FeatureSet
     * @param otherFeatureSets The other FeatureSets
     * @return a RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, otherFeatureSets);
    }
}
