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
import componenttest.custom.junit.runner.RepeatTestFilter;

public class TelemetryActions {
    public static final String MP14_MPTEL11_ID = MicroProfileActions.MP14_ID + "_MPTEL11";
    public static final String MP41_MPTEL11_ID = MicroProfileActions.MP41_ID + "_MPTEL11";
    public static final String MP50_MPTEL11_ID = MicroProfileActions.MP50_ID + "_MPTEL11";

    //Telemetry 2.0 repeats should not start with "MicroProfile_xx" as RepeatTestFilter uses startsWith() which makes them indisinguishable to mpTelemetry-1.1 in tests
    public static final String MP14_MPTEL20_ID = EE7FeatureReplacementAction.ID + "_MPTEL20_MP14";
    public static final String MP41_MPTEL20_ID = EE8FeatureReplacementAction.ID + "_MPTEL20_MP41";
    public static final String MP50_MPTEL20_ID = JakartaEEAction.EE9_ACTION_ID + "_MPTEL20_MP50";
    public static final String MP50_MPTEL20_JAVA8_ID = JakartaEEAction.EE9_ACTION_ID + "_MPTEL20_MP50_JAVA8";

    public static final String MP61_MPTEL20_ID = MicroProfileActions.MP61_ID + "_MPTEL20";

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

    //Telemetry 2.0 is not included in the MicroProfile 6.1 umbrella feature but they are compatible.
    public static final FeatureSet MP61_MPTEL20 = MicroProfileActions.MP61
                    .removeFeature("mpTelemetry-1.1")
                    .addFeature("mpTelemetry-2.0")
                    .build(MP61_MPTEL20_ID);

    //All MicroProfile Telemetry FeatureSets - must be descending order
    private static final FeatureSet[] ALL_MPTEL_SETS_ARRAY = { MicroProfileActions.MP70_EE11,
                                                               MicroProfileActions.MP70_EE10,
                                                               MicroProfileActions.MP61,
                                                               MicroProfileActions.MP60,
                                                               MP61_MPTEL20,
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

    public static RepeatTests telemetry10and11Repeats(String serverName) {
        return repeat(serverName, MicroProfileActions.MP61, MP14_MPTEL11, MP41_MPTEL11, MP50_MPTEL11,
                      MicroProfileActions.MP60);
    }

    public static RepeatTests telemetry20Repeats(String serverName) {
        return repeat(serverName, MicroProfileActions.MP70_EE11, MP14_MPTEL20, MP41_MPTEL20, MP50_MPTEL20,
                      MP50_MPTEL20_JAVA8, MP61_MPTEL20, MicroProfileActions.MP70_EE10);
    }

    public static RepeatTests telemetry20Repeats() {
        return telemetry20Repeats(FeatureReplacementAction.ALL_SERVERS);
    }

    /*
     * This returns one repeat for every released version of MPTelemetry; the latest 1.0, 1.1, etc.
     * It also returns a repeat to cover ongoing development if that is not covered by one of the above.
     */
    public static RepeatTests latestTelemetryRepeats(String serverName) {
        return repeat(serverName,
                      MicroProfileActions.MP70_EE11,
                      MicroProfileActions.MP70_EE10,
                      MicroProfileActions.MP61,
                      MicroProfileActions.MP60);
    }

    /*
     * This returns one repeat for every released version of MPTelemetry >= 2.0; the latest 2.0, and more to come when they exist.
     * It also returns a repeat to cover ongoing development if that is not covered by one of the above.
     */
    public static RepeatTests latestTelemetry20Repeats() {
        return latestTelemetry20Repeats(FeatureReplacementAction.ALL_SERVERS);
    }

    public static RepeatTests latestTelemetry20Repeats(String serverName) {
        return repeat(serverName, MicroProfileActions.MP70_EE11);
    }

    public static RepeatTests telemetry11Repeats(String serverName) {
        return repeat(serverName, MP14_MPTEL11, MP41_MPTEL11, MP50_MPTEL11, MicroProfileActions.MP61);
    }

    public static RepeatTests mp60Repeat(String serverName) {
        return repeat(serverName, MicroProfileActions.MP60);
    }

    public static RepeatTests allMPRepeats(String serverName) {
        return repeat(serverName,
                      MicroProfileActions.MP70_EE11,
                      MicroProfileActions.MP60,
                      MP61_MPTEL20,
                      MP14_MPTEL11,
                      MP41_MPTEL11,
                      MP50_MPTEL11,
                      MicroProfileActions.MP61,
                      MP14_MPTEL20,
                      MP41_MPTEL20,
                      MP50_MPTEL20,
                      MP50_MPTEL20_JAVA8,
                      MicroProfileActions.MP70_EE10);
    }

    public static boolean mpTelemetry20IsActive(){
        if (RepeatTestFilter.isRepeatActionActive(MP14_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP41_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP50_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP50_MPTEL20_JAVA8_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP61_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE10_ID) ||
            RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE11_ID)) {
            return true;
       }
       return false;
    }

    public static boolean mpTelemetry20BelowEE10IsActive(){
        if (RepeatTestFilter.isRepeatActionActive(MP50_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP50_MPTEL20_JAVA8_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP61_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE10_ID) ||
            RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP70_EE11_ID)) {
            return true;
       }
       return false;
    }

    public static boolean EE7orEE8IsActive(){
        if (RepeatTestFilter.isRepeatActionActive(MP14_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP41_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP14_MPTEL11_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP41_MPTEL11_ID)) {
            return true;
       }
       return false;
    }

    public static boolean EE7orEE8Mp20IsActive(){
        if (RepeatTestFilter.isRepeatActionActive(MP14_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP41_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP14_MPTEL11_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP41_MPTEL11_ID)) {
            return true;
       }
       return false;
    }

    public static boolean mpTelemetry20EE7orEE8IsActive(){
        if (RepeatTestFilter.isRepeatActionActive(MP14_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP41_MPTEL20_ID)) {
            return true;
       }
       return false;
    }

    public static boolean mpTelemetryEE7IsActive(){
        if (RepeatTestFilter.isRepeatActionActive(MP14_MPTEL20_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP14_MPTEL11_ID)) {
            return true;
       }
       return false;
    }

    public static boolean mpTelemetry11EE7orEE8IsActive(){
        if (RepeatTestFilter.isRepeatActionActive(MP14_MPTEL11_ID) ||
            RepeatTestFilter.isRepeatActionActive(MP41_MPTEL11_ID)) {
            return true;
       }
       return false;
    }
}
