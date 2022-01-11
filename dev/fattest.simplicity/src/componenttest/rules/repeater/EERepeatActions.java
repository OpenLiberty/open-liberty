/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests.EEVersion;

public class EERepeatActions extends RepeatActions {

    //The EE FeatureSets
    public static final FeatureSet EE6 = new FeatureSet(EE6FeatureReplacementAction.ID, EE6FeatureReplacementAction.EE6_FEATURE_SET, EEVersion.EE6);
    public static final FeatureSet EE7 = new FeatureSet(EE7FeatureReplacementAction.ID, EE7FeatureReplacementAction.EE7_FEATURE_SET, EEVersion.EE7);
    public static final FeatureSet EE8 = new FeatureSet(EE8FeatureReplacementAction.ID, EE8FeatureReplacementAction.EE8_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet EE9 = new FeatureSet(JakartaEE9Action.ID, JakartaEE9Action.EE9_FEATURE_SET, EEVersion.EE9);
    public static final FeatureSet EE10 = new FeatureSet(JakartaEE10Action.ID, JakartaEE10Action.EE10_FEATURE_SET, EEVersion.EE10);

    //The FeatureSet for the latest EE version
    public static final FeatureSet LATEST = EE10;

    //All EE FeatureSets
    private static final FeatureSet[] ALL_SETS_ARRAY = { EE6, EE7, EE8, EE9, EE10 };
    public static final Set<FeatureSet> ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ALL_SETS_ARRAY)));

    /**
     * Get a RepeatTests instance for all EE versions. The LATEST will be run in LITE mode. The others will be run in FULL.
     *
     * @param  server The server to repeat on
     * @return        a RepeatTests instance
     */
    public static RepeatTests repeatAll(String server) {
        Set<FeatureSet> others = new HashSet<>(ALL);
        others.remove(LATEST);
        return repeat(server, TestMode.FULL, ALL, LATEST, others);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in FULL.
     *
     * @param  server           The server to repeat on
     * @param  firstFeatureSet  The first FeatureSet
     * @param  otherFeatureSets The other FeatureSets
     * @return                  a RepeatTests instance
     */
    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, Set<FeatureSet> otherFeatureSets) {
        return repeat(server, TestMode.FULL, ALL, firstFeatureSet, otherFeatureSets);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in FULL.
     * Usage: The following example will repeat the tests using EE versions 8, 9 and 10 (8 in LITE mode, the others in FULL).
     *
     * <pre>
     * <code>
     * &#64;ClassRule
     * public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE8, EERepeatActions.EE9, EERepeatActions.EE10);
     * </code>
     * </pre>
     *
     * @param  server           The server to repeat on
     * @param  firstFeatureSet  The first FeatureSet
     * @param  otherFeatureSets The other FeatureSets
     * @return                  a RepeatTests instance
     */
    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return repeat(server, TestMode.FULL, firstFeatureSet, otherFeatureSets);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in the mode specified by otherFeatureSetsTestMode
     * Usage: The following example will repeat the tests using EE versions 8, 9 and 10.
     * 10 will be in LITE mode, the others in FULL mode.
     *
     * <pre>
     * <code>
     * &#64;ClassRule
     * public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, TestMode.FULL, EERepeatActions.EE10, EERepeatActions.EE8, EERepeatActions.EE9);
     * </code>
     * </pre>
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, otherFeatureSets);
    }

}
