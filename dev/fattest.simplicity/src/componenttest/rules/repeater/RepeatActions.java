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
import java.util.HashSet;
import java.util.Set;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests.EEVersion;

public class RepeatActions {

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in the mode specified by
     * otherFeatureSetsTestMode.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           All known FeatureSets. The features not in the current FeatureSet are removed from the repeat
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, Set<FeatureSet> allFeatureSets, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        Set<FeatureSet> others = new HashSet<>(Arrays.asList(otherFeatureSets));
        return repeat(server, otherFeatureSetsTestMode, allFeatureSets, firstFeatureSet, others);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in the mode specified by
     * otherFeatureSetsTestMode.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           All known FeatureSets. The features not in the current FeatureSet are removed from the repeat
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, Set<FeatureSet> allFeatureSets, FeatureSet firstFeatureSet,
                                     Set<FeatureSet> otherFeatureSets) {
        RepeatTests r = RepeatTests.with(forFeatureSet(allFeatureSets, firstFeatureSet, server, TestMode.LITE));
        for (FeatureSet other : otherFeatureSets) {
            r = r.andWith(forFeatureSet(allFeatureSets, other, server, otherFeatureSetsTestMode));
        }
        return r;
    }

    /**
     * Get a FeatureReplacementAction instance for a given FeatureSet. It will be run in the mode specified.
     *
     * @param  allFeatureSets All known FeatureSets. The features not in the specified FeatureSet are removed from the repeat action
     * @param  featureSet     The FeatureSet to repeat with.
     * @param  server         The server to repeat on
     * @param  testMode       The test mode to run the FeatureSet
     * @return                A FeatureReplacementAction instance
     */
    public static FeatureReplacementAction forFeatureSet(Set<FeatureSet> allFeatureSets, FeatureSet featureSet, String server, TestMode testMode) {
        FeatureReplacementAction action = null;
        EEVersion eeVersion = featureSet.getEEVersion();
        if (eeVersion == EEVersion.EE7)
            action = new EE7FeatureReplacementAction();
        else if (eeVersion == EEVersion.EE8)
            action = new EE8FeatureReplacementAction();
        else if (eeVersion == EEVersion.EE9)
            action = new JakartaEE9Action();
        else if (eeVersion == EEVersion.EE10)
            action = new JakartaEE10Action();
        else
            action = new FeatureReplacementAction();
        action.addFeatures(featureSet.getFeatures());
        for (FeatureSet featureSetToRemove : allFeatureSets) {
            if (!featureSetToRemove.equals(featureSet)) {
                action.removeFeatures(featureSetToRemove.getFeatures());
            }
        }
        action.forceAddFeatures(false);
        action.withID(featureSet.getID());

        if (server != null) {
            action.forServers(server);
        }
        if (testMode != null) {
            action.withTestMode(testMode);
        }
        return action;
    }
}
