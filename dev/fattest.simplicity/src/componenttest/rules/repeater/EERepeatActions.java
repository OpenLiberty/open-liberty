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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.topology.impl.JavaInfo;

public class EERepeatActions {

    //The EE FeatureSet IDs
    public static final String EE6_ID = EE6FeatureReplacementAction.ID;
    public static final String EE7_ID = EE7FeatureReplacementAction.ID;
    public static final String EE8_ID = EE8FeatureReplacementAction.ID;
    public static final String EE9_ID = JakartaEE9Action.ID;
    public static final String EE10_ID = JakartaEE10Action.ID;

    //The EE FeatureSets
    public static final FeatureSet EE6 = new FeatureSet(EE6_ID, EE6FeatureReplacementAction.EE6_FEATURE_SET, EEVersion.EE6);
    public static final FeatureSet EE7 = new FeatureSet(EE7_ID, EE7FeatureReplacementAction.EE7_FEATURE_SET, EEVersion.EE7);
    public static final FeatureSet EE8 = new FeatureSet(EE8_ID, EE8FeatureReplacementAction.EE8_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet EE9 = new FeatureSet(EE9_ID, JakartaEE9Action.EE9_FEATURE_SET, EEVersion.EE9);
    public static final FeatureSet EE10 = new FeatureSet(EE10_ID, JakartaEE10Action.EE10_FEATURE_SET, EEVersion.EE10);

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
        return repeat(server, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets));
    }

    /**
     * As {@link RepeatActions#repeat(String, TestMode, Set, FeatureSet, Set)} except that if {@code firstFeatureSet} isn't compatible with the current Java version, we try to
     * replace it with the newest set from {@code otherFeatureSets} that is compatible.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           All known FeatureSets. The features not in the current FeatureSet are removed from the repeat
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    private static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, Set<FeatureSet> allFeatureSets, FeatureSet firstFeatureSet,
                                      Collection<FeatureSet> otherFeatureSets) {

        // If the firstFeatureSet requires a Java level higher than the one we're running, try to find a suitable replacement so we don't end up not running the test at all in LITE mode
        int currentJavaLevel = JavaInfo.forCurrentVM().majorVersion();
        if (currentJavaLevel < firstFeatureSet.getEEVersion().getMinJavaLevel()) {

            List<FeatureSet> allSetsList = new ArrayList<>(Arrays.asList(ALL_SETS_ARRAY));
            Collections.reverse(allSetsList); // Reverse list so newest EE version is first in list

            Collection<FeatureSet> candidateFeatureSets = otherFeatureSets;

            // Find the newest EE feature set that's in otherFeatureSets and is compatible with the current java version
            Optional<FeatureSet> newestSupportedSet = allSetsList.stream()
                            .filter(s -> candidateFeatureSets.contains(s))
                            .filter(s -> s.getEEVersion().getMinJavaLevel() <= currentJavaLevel)
                            .findFirst();

            if (newestSupportedSet.isPresent()) {
                firstFeatureSet = newestSupportedSet.get();
                otherFeatureSets = new ArrayList<>(otherFeatureSets);
                otherFeatureSets.remove(newestSupportedSet.get());
            }
        }
        return RepeatActions.repeat(server, otherFeatureSetsTestMode, allFeatureSets, firstFeatureSet, otherFeatureSets);
    }
}
