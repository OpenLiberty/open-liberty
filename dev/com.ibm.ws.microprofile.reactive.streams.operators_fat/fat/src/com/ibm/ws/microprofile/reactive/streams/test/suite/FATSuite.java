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
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                ReactiveStreamsContextTest.class,
                ReactiveStreamsTest.class,
                ReactiveJaxRSTest.class,
                ReactiveConcurrentWorkTest.class
})
public class FATSuite {
    public static final String MP_REACTIVE_STREAMS_10 = "mpReactiveStreams-1.0";
    public static final String MP_REACTIVE_STREAMS_30 = "mpReactiveStreams-3.0";

    public static final String MPRSO10_ID = MicroProfileActions.MP21_ID + "_" + "mpRSO10";
    public static final String MPRSO30_MP50_ID = MicroProfileActions.MP50_ID + "_" + "mpRSO30";
    public static final String MPRSO30_MP60_ID = MicroProfileActions.MP60_ID + "_" + "mpRSO30";
    public static final String MPRSO30_MP61_ID = MicroProfileActions.MP61_ID + "_" + "mpRSO30";

    public static final FeatureSet MPRSO10 = MicroProfileActions.MP21.addFeature(MP_REACTIVE_STREAMS_10).build(MPRSO10_ID);
    public static final FeatureSet MPRSO30_MP50 = MicroProfileActions.MP50.addFeature(MP_REACTIVE_STREAMS_30).setMinJavaLevel(SEVersion.JAVA11).build(MPRSO30_MP50_ID);
    public static final FeatureSet MPRSO30_MP60 = MicroProfileActions.MP60.addFeature(MP_REACTIVE_STREAMS_30).build(MPRSO30_MP60_ID);
    public static final FeatureSet MPRSO30_MP61 = MicroProfileActions.MP61.addFeature(MP_REACTIVE_STREAMS_30).build(MPRSO30_MP61_ID);

    public static final Set<FeatureSet> ALL;
    static {
        ALL = new HashSet<>(MicroProfileActions.ALL);
        ALL.add(MPRSO10);
        ALL.add(MPRSO30_MP50);
        ALL.add(MPRSO30_MP60);
        ALL.add(MPRSO30_MP61);
    }

    public static RepeatTests repeatDefault(String serverName) {
        return repeat(serverName, TestMode.LITE, FATSuite.MPRSO30_MP61, FATSuite.MPRSO10, FATSuite.MPRSO30_MP50, FATSuite.MPRSO30_MP60);
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
        return MicroProfileActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, otherFeatureSets);
    }
}
