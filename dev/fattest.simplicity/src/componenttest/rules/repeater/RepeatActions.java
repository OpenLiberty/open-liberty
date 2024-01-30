/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
import java.util.List;
import java.util.Optional;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;

public class RepeatActions {

    public static enum SEVersion {
        JAVA8(8), JAVA11(11), JAVA17(17), JAVA21(21);

        private SEVersion(int majorVersion) {
            this.majorVersion = majorVersion;
        }

        private final int majorVersion;

        //minor and micro versions could be added in future
        public int majorVersion() {
            return majorVersion;
        }
    }

    public static enum EEVersion {
        EE6(SEVersion.JAVA8), EE7(SEVersion.JAVA8), EE8(SEVersion.JAVA8), EE9(SEVersion.JAVA8), EE10(SEVersion.JAVA11), EE11(SEVersion.JAVA17);

        private EEVersion(SEVersion minJavaLevel) {
            this.minJavaLevel = minJavaLevel;
        }

        private final SEVersion minJavaLevel;

        SEVersion getMinJavaLevel() {
            return minJavaLevel;
        }
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in the mode specified by
     * otherFeatureSetsTestMode.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           An ORDERED list of all the FeatureSets which may apply to this test. Newest FeatureSet should be first. Oldest last.
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, List<FeatureSet> allFeatureSets, FeatureSet firstFeatureSet,
                                     FeatureSet... otherFeatureSets) {
        return repeat(server, otherFeatureSetsTestMode, allFeatureSets, firstFeatureSet, Arrays.asList(otherFeatureSets));
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode.
     * The others will be run in the mode specified by otherFeatureSetsTestMode.
     *
     * If {@code firstFeatureSet} isn't compatible with the current Java version, we try to
     * replace it with the newest set from {@code otherFeatureSets} that is compatible.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           An ORDERED list of all the FeatureSets which may apply to this test. Newest FeatureSet should be first. Oldest last.
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String server,
                                     TestMode otherFeatureSetsTestMode,
                                     List<FeatureSet> allFeatureSets,
                                     FeatureSet firstFeatureSet,
                                     List<FeatureSet> otherFeatureSets) {
        // if server is null use an empty array, else return an array with the server as the sole element
        String[] servers = server != null ? new String[] { server } : new String[] {};
        return repeat(servers, otherFeatureSetsTestMode, allFeatureSets, firstFeatureSet, otherFeatureSets);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode.
     * The others will be run in the mode specified by otherFeatureSetsTestMode.
     *
     * If {@code firstFeatureSet} isn't compatible with the current Java version, we try to
     * replace it with the newest set from {@code otherFeatureSets} that is compatible.
     *
     * @param  servers                  The servers to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           An ORDERED list of all the FeatureSets which may apply to this test. Newest FeatureSet should be first. Oldest last.
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String[] servers,
                                     TestMode otherFeatureSetsTestMode,
                                     List<FeatureSet> allFeatureSets,
                                     FeatureSet firstFeatureSet,
                                     List<FeatureSet> otherFeatureSets) {

        FeatureSet actualFirstFeatureSet = firstFeatureSet;
        List<FeatureSet> actualOtherFeatureSets = new ArrayList<>(otherFeatureSets);

        // If the firstFeatureSet requires a Java level higher than the one we're running, try to find a suitable replacement so we don't end up not running the test at all in LITE mode
        int currentJavaLevel = JavaInfo.forCurrentVM().majorVersion();
        if (currentJavaLevel < firstFeatureSet.getMinJavaLevel().majorVersion()) {

            // Find the newest feature set that's in otherFeatureSets and is compatible with the current java version
            Optional<FeatureSet> newestSupportedSet = allFeatureSets.stream()
                            .filter(s -> actualOtherFeatureSets.contains(s))
                            .filter(s -> s.getMinJavaLevel().majorVersion() <= currentJavaLevel)
                            .findFirst();

            if (newestSupportedSet.isPresent()) {
                actualFirstFeatureSet = newestSupportedSet.get();
                actualOtherFeatureSets.remove(actualFirstFeatureSet);
            }
        }
        RepeatTests r = RepeatTests.with(forFeatureSet(allFeatureSets, actualFirstFeatureSet, servers, TestMode.LITE));
        for (FeatureSet other : actualOtherFeatureSets) {
            r = r.andWith(forFeatureSet(allFeatureSets, other, servers, otherFeatureSetsTestMode));
        }
        return r;
    }

    /**
     * Get a FeatureReplacementAction instance for a given FeatureSet. It will be run in the mode specified.
     *
     * @param  allFeatureSets All known FeatureSets. The features not in the specified FeatureSet are removed from the repeat action
     * @param  featureSet     The FeatureSet to repeat with.
     * @param  servers        The servers to repeat on
     * @param  testMode       The test mode to run the FeatureSet
     * @return                A FeatureReplacementAction instance
     */
    public static FeatureReplacementAction forFeatureSet(List<FeatureSet> allFeatureSets, FeatureSet featureSet, String[] servers, TestMode testMode) {
        //First create a base FeatureReplacementAction
        //Need to use a FeatureReplacementAction which is specific to the EE version because it also contains the transformation code
        FeatureReplacementAction action = null;
        EEVersion eeVersion = featureSet.getEEVersion();
        if (eeVersion == EEVersion.EE6) {
            action = new EE6FeatureReplacementAction();
        } else if (eeVersion == EEVersion.EE7) {
            action = new EE7FeatureReplacementAction();
        } else if (eeVersion == EEVersion.EE8) {
            action = new EE8FeatureReplacementAction();
        } else if (eeVersion == EEVersion.EE9) {
            action = new JakartaEE9Action();
        } else if (eeVersion == EEVersion.EE10) {
            action = new JakartaEE10Action();
        } else if (eeVersion == EEVersion.EE11) {
            action = new JakartaEE11Action();
        } else {
            action = new FeatureReplacementAction();
        }
        action.withMinJavaLevel(featureSet.getMinJavaLevel());

        //add all the features from the primary FeatureSet
        action.addFeatures(featureSet.getFeatures());

        //remove all of features from the other FeatureSets
        for (FeatureSet featureSetToRemove : allFeatureSets) {
            if (!featureSetToRemove.equals(featureSet)) {
                action.removeFeatures(featureSetToRemove.getFeatures());
            }
        }
        //don't force features to be added if they were not required by the original server.xml
        action.forceAddFeatures(false);
        //set the ID
        action.withID(featureSet.getID());

        //set the server
        if (servers != null && servers.length > 0) {
            action.forServers(servers);
        }
        //set the test mode
        if (testMode != null) {
            action.withTestMode(testMode);
        }
        return action;
    }
}
