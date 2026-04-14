/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.CheckpointEE10Action;
import componenttest.rules.repeater.CheckpointEE11Action;
import componenttest.rules.repeater.CheckpointEE8Action;
import componenttest.rules.repeater.CheckpointEE9Action;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                DynamicJACCFeatureTest.class,
                EJBJarMixExtInWarEarMergeConflictXMLBindingsTest.class,
                EJBJarMixExtMergeConflictXMLBindingsTest.class,
                EJBJarMixM07ExtInWarEarTest.class,
                EJBJarMixM08ExtInWarTest.class,
                EJBJarMixMC06InWarTest.class,
                EJBJarX01InWarEarTest.class,
                PureAnnA01InWarEarTest.class,
                PureAnnA01InWarTest.class,
                PureAnnA05InWarEarTest.class, //Never ran in the native ejb
                PureAnnA07InWarInheritanceTest.class,
                PureAnnAppBndXMLBindingsInWarEarTest.class,
                PureAnnAppBndXMLBindingsTest.class, //Changed to run in Full mode.
                PureAnnMergeConflictXMLBindingsInWarEarTest.class, //Modified not to run the PureAnnTestBase
                PureAnnMergeConflictXMLBindingsTest.class, //Modified not to run the PureAnnTestBase and also removed from the lite bucket
})
public class FATSuite {
    public static final Set<String> EE8_FEATURES;
    private static final String[] EE8_FEATURES_ARRAY = {
                                                         "usr:jaccTestProvider-1.0"
    };

    public static final Set<String> EE9_FEATURES;
    private static final String[] EE9_FEATURES_ARRAY = {
                                                         "usr:jaccTestProvider-2.0"
    };

    public static final Set<String> EE10_FEATURES;
    private static final String[] EE10_FEATURES_ARRAY = {
                                                          "usr:jaccTestProvider-2.1"
    };

    private static final Set<String> EE11_FEATURES;
    private static final String[] EE11_FEATURES_ARRAY = {
                                                          "usr:jaccTestProvider-3.0"
    };

    private static final Set<String> EE11_SPEC_FEATURES;
    private static final String[] EE11_SPEC_FEATURES_ARRAY = {
                                                               "usr:authzTestProvider-3.0"
    };

    static {
        EE8_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE8_FEATURES_ARRAY)));
        EE9_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));
        EE10_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE10_FEATURES_ARRAY)));
        EE11_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE11_FEATURES_ARRAY)));
        EE11_SPEC_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE11_SPEC_FEATURES_ARRAY)));
    }

    public static final String EE11_SPEC_ID = JakartaEEAction.EE11_ACTION_ID + "_spec";

    /*
     * Run EE9 tests in LITE mode if Java 8, EE10 tests in LITE mode if >= Java 11, EE11 tests in LITE mode if >= Java 17 and run all tests in FULL mode.
     */
    /*@formatter:off*/
    public static RepeatTests defaultRepeat(String serverName) {
        return RepeatTests.with(ee8Action(serverName, false).fullFATOnly())
                        .andWith(ee9Action(serverName, false).conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                        .andWith(ee10Action(serverName, false).conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                        .andWith(ee11Action(serverName, false))
                        .andWith(ee11SpecAction(serverName, false));
    }

    public static RepeatTests defaultAndCheckpointRepeat(String serverName) {
        return defaultRepeat(serverName)
                        .andWith(ee8Action(serverName, true).fullFATOnly())
                        .andWith(ee9Action(serverName, true).fullFATOnly())
                        .andWith(ee10Action(serverName, true).fullFATOnly())
                        .andWith(ee11Action(serverName, true))
                        .andWith(ee11SpecAction(serverName, true));
    }
    /*@formatter:on*/

    public static FeatureReplacementAction ee8Action(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE8Action() : FeatureReplacementAction.EE8_FEATURES();
        return action.forServers(serverName).removeFeatures(EE9_FEATURES).removeFeatures(EE10_FEATURES).removeFeatures(EE11_FEATURES).removeFeatures(EE11_SPEC_FEATURES).addFeatures(EE8_FEATURES);
    }

    public static FeatureReplacementAction ee9Action(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE9Action() : FeatureReplacementAction.EE9_FEATURES();
        return action.forServers(serverName).removeFeatures(EE8_FEATURES).removeFeatures(EE10_FEATURES).removeFeatures(EE11_FEATURES).removeFeatures(EE11_SPEC_FEATURES).addFeatures(EE9_FEATURES);
    }

    public static FeatureReplacementAction ee10Action(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE10Action() : FeatureReplacementAction.EE10_FEATURES();
        return action.forServers(serverName).removeFeatures(EE8_FEATURES).removeFeatures(EE9_FEATURES).removeFeatures(EE11_FEATURES).removeFeatures(EE11_SPEC_FEATURES).addFeatures(EE10_FEATURES);
    }

    public static FeatureReplacementAction ee11Action(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE11Action() : FeatureReplacementAction.EE11_FEATURES();
        return action.forServers(serverName).removeFeatures(EE8_FEATURES).removeFeatures(EE9_FEATURES).removeFeatures(EE10_FEATURES).removeFeatures(EE11_SPEC_FEATURES).addFeatures(EE11_FEATURES);
    }

    public static FeatureReplacementAction ee11SpecAction(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE11Action() : FeatureReplacementAction.EE11_FEATURES();
        return action.forServers(serverName).removeFeatures(EE8_FEATURES).removeFeatures(EE9_FEATURES).removeFeatures(EE10_FEATURES).removeFeatures(EE11_FEATURES).addFeatures(EE11_SPEC_FEATURES).withID(EE11_SPEC_ID);
    }

}
