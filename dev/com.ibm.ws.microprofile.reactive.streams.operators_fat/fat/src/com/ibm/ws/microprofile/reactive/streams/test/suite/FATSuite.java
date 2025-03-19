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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
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
    public static final String MP_REACTIVE_STREAMS_30 = "mpReactiveStreams-3.0";

    public static final String MP50_RS30_ID = MicroProfileActions.MP50_ID + "_" + "RS30";

    //MP50 runs on Java 8 but RSO30 will only run on Java11 or higher
    public static final FeatureSet MP50_RS30 = MicroProfileActions.MP50.addFeature(MP_REACTIVE_STREAMS_30).setMinJavaLevel(SEVersion.JAVA11).build(MP50_RS30_ID);

    //All MicroProfile ReactiveMessaging FeatureSets - must be descending order
    private static final List<FeatureSet> ALL;

    static {
        ALL = new ArrayList<>(MicroProfileActions.ALL);
        //put the updated FeatureSet in just before MP50
        ALL.add(ALL.indexOf(MicroProfileActions.MP50), MP50_RS30);
    }

    public static RepeatTests repeatDefault(String serverName) {
        return repeat(serverName, TestMode.FULL,
                      MicroProfileActions.MP70_EE11, //RS30 + EE11
                      MicroProfileActions.MP61, //RS30 + EE10
                      FATSuite.MP50_RS30, //RS30 + EE9
                      MicroProfileActions.MP21);//RS10 + EE8
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
}
