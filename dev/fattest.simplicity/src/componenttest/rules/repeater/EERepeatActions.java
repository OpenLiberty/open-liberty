/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatActions.EEVersion;

public class EERepeatActions {

    //The EE FeatureSet IDs
    public static final String EE6_ID = EE6FeatureReplacementAction.ID;
    public static final String EE7_ID = EE7FeatureReplacementAction.ID;
    public static final String EE8_ID = EE8FeatureReplacementAction.ID;
    public static final String EE9_ID = JakartaEE9Action.ID;
    public static final String EE10_ID = JakartaEE10Action.ID;
    public static final String EE11_ID = JakartaEE11Action.ID;

    //The EE FeatureSets
    public static final FeatureSet EE6 = new FeatureSet(EE6_ID, EE6FeatureReplacementAction.EE6_FEATURE_SET, EEVersion.EE6);
    public static final FeatureSet EE7 = new FeatureSet(EE7_ID, EE7FeatureReplacementAction.EE7_FEATURE_SET, EEVersion.EE7);
    public static final FeatureSet EE8 = new FeatureSet(EE8_ID, EE8FeatureReplacementAction.EE8_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet EE9 = new FeatureSet(EE9_ID, JakartaEE9Action.EE9_FEATURE_SET, EEVersion.EE9);
    public static final FeatureSet EE10 = new FeatureSet(EE10_ID, JakartaEE10Action.EE10_FEATURE_SET, EEVersion.EE10);
    public static final FeatureSet EE11 = new FeatureSet(EE11_ID, JakartaEE11Action.EE11_FEATURE_SET, EEVersion.EE11);

    //The FeatureSet for the latest EE version
    public static final FeatureSet LATEST = EE10;

    //All EE FeatureSets - must be descending order
    private static final FeatureSet[] ALL_SETS_ARRAY = { EE10, EE9, EE8, EE7, EE6 };
    private static final List<FeatureSet> ALL = Collections.unmodifiableList(Arrays.asList(ALL_SETS_ARRAY));

    /**
     * Get a RepeatTests instance for all EE versions. The LATEST will be run in LITE mode. The others will be run in FULL.
     *
     * @param  server The server to repeat on
     * @return        a RepeatTests instance
     */
    public static RepeatTests repeatAll(String server) {
        List<FeatureSet> otherFeatureSets = new ArrayList<>(ALL);
        otherFeatureSets.remove(LATEST);
        return RepeatActions.repeat(server, TestMode.FULL, ALL, LATEST, otherFeatureSets);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in FULL.
     *
     * @param  server           The server to repeat on
     * @param  firstFeatureSet  The first FeatureSet
     * @param  otherFeatureSets The other FeatureSets
     * @return                  a RepeatTests instance
     */
    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, List<FeatureSet> otherFeatureSets) {
        return RepeatActions.repeat(server, TestMode.FULL, ALL, firstFeatureSet, otherFeatureSets);
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
        return RepeatActions.repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets));
    }

}
