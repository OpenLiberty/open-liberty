/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

public class MicroProfileActions {

    private static final String[] MP10_FEATURES_ARRAY = { "cdi-1.2",
                                                          "jaxrs-2.0",
                                                          "jsonp-1.0" };

    private static final String[] MP12_FEATURES_ARRAY = { "servlet-3.1",
                                                          "cdi-1.2",
                                                          "jaxrs-2.0",
                                                          "jsonp-1.0",
                                                          "mpConfig-1.1",
                                                          "mpFaultTolerance-1.0",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.0",
                                                          "mpMetrics-1.0" };

    private static final String[] MP13_FEATURES_ARRAY = { "servlet-3.1",
                                                          "cdi-1.2",
                                                          "jaxrs-2.0",
                                                          "jaxrsClient-2.0",
                                                          "jsonp-1.0",
                                                          "mpConfig-1.2",
                                                          "mpFaultTolerance-1.0",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.0",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.0",
                                                          "mpRestClient-1.0" };

    private static final String[] MP14_FEATURES_ARRAY = { "servlet-3.1",
                                                          "cdi-1.2",
                                                          "jaxrs-2.0",
                                                          "jaxrsClient-2.0",
                                                          "jsonp-1.0",
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-1.1",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.1",
                                                          "mpRestClient-1.1" };

    private static final String[] MP20_FEATURES_ARRAY = { "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-1.1",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.1",
                                                          "mpRestClient-1.1" };

    private static final String[] MP21_FEATURES_ARRAY = { "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-1.1",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.2",
                                                          "mpRestClient-1.1" };

    private static final String[] MP22_FEATURES_ARRAY = { "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-2.0",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.2" };

    private static final String[] MP30_FEATURES_ARRAY = { "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-2.0",
                                                          "mpHealth-2.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-2.0",
                                                          "mpOpenAPI-1.1",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.3" };

    private static final String[] MP32_FEATURES_ARRAY = { "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-2.0",
                                                          "mpHealth-2.1",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-2.2",
                                                          "mpOpenAPI-1.1",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.3" };

    private static final String[] MP33_FEATURES_ARRAY = { "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-1.4",
                                                          "mpFaultTolerance-2.1",
                                                          "mpHealth-2.2",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-2.3",
                                                          "mpOpenAPI-1.1",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.4" };

    private static final String[] MP40_FEATURES_ARRAY = { "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-2.0",
                                                          "mpFaultTolerance-3.0",
                                                          "mpHealth-3.0",
                                                          "mpJwt-1.2",
                                                          "mpMetrics-3.0",
                                                          "mpOpenAPI-2.0",
                                                          "mpOpenTracing-2.0",
                                                          "mpRestClient-2.0" };

    private static final Set<String> MP10_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP10_FEATURES_ARRAY)));
    private static final Set<String> MP12_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP12_FEATURES_ARRAY)));
    private static final Set<String> MP13_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP13_FEATURES_ARRAY)));
    private static final Set<String> MP14_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP14_FEATURES_ARRAY)));
    private static final Set<String> MP20_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP20_FEATURES_ARRAY)));
    private static final Set<String> MP21_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP21_FEATURES_ARRAY)));
    private static final Set<String> MP22_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP22_FEATURES_ARRAY)));
    private static final Set<String> MP30_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP30_FEATURES_ARRAY)));
    private static final Set<String> MP32_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP32_FEATURES_ARRAY)));
    private static final Set<String> MP33_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP33_FEATURES_ARRAY)));
    private static final Set<String> MP40_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP40_FEATURES_ARRAY)));

    //The FeatureSet IDs. Since these will be used as the RepeatAction IDs, they can also be used in annotations such as @SkipForRepeat
    public static final String MP10_ID = "MicroProfile_10";
    public static final String MP12_ID = "MicroProfile_12";
    public static final String MP13_ID = "MicroProfile_13";
    public static final String MP14_ID = "MicroProfile_14";
    public static final String MP20_ID = "MicroProfile_20";
    public static final String MP21_ID = "MicroProfile_21";
    public static final String MP22_ID = "MicroProfile_22";
    public static final String MP30_ID = "MicroProfile_30";
    public static final String MP32_ID = "MicroProfile_32";
    public static final String MP33_ID = "MicroProfile_33";
    public static final String MP40_ID = "MicroProfile_40";

    //The MicroProfile FeatureSets
    public static final FeatureSet MP10 = new FeatureSet(MP10_ID, MP10_FEATURE_SET);
    public static final FeatureSet MP12 = new FeatureSet(MP12_ID, MP12_FEATURE_SET);
    public static final FeatureSet MP13 = new FeatureSet(MP13_ID, MP13_FEATURE_SET);
    public static final FeatureSet MP14 = new FeatureSet(MP14_ID, MP14_FEATURE_SET);
    public static final FeatureSet MP20 = new FeatureSet(MP20_ID, MP20_FEATURE_SET);
    public static final FeatureSet MP21 = new FeatureSet(MP21_ID, MP21_FEATURE_SET);
    public static final FeatureSet MP22 = new FeatureSet(MP22_ID, MP22_FEATURE_SET);
    public static final FeatureSet MP30 = new FeatureSet(MP30_ID, MP30_FEATURE_SET);
    public static final FeatureSet MP32 = new FeatureSet(MP32_ID, MP32_FEATURE_SET);
    public static final FeatureSet MP33 = new FeatureSet(MP33_ID, MP33_FEATURE_SET);
    public static final FeatureSet MP40 = new FeatureSet(MP40_ID, MP40_FEATURE_SET);

    //The FeatureSet for the latest MicrotProfile version
    public static final FeatureSet LATEST = MP40;

    //All MicroProfile FeatureSets
    private static final FeatureSet[] ALL_SETS_ARRAY = { MP10, MP12, MP13, MP14, MP20, MP21, MP22, MP30, MP32, MP33, MP40 };
    public static final Set<FeatureSet> ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ALL_SETS_ARRAY)));

    /**
     * Get a RepeatTests instance for the given FeatureSets. All FeatureSets will be run in LITE mode.
     * Usage: The following example will repeat the tests using MicroProfile versions 1.2, 1.3, 1.4, 3.3 and 4.0, all in LITE mode
     *
     * <pre>
     * <code>
     * &#64;ClassRule
     * public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP12, MicroProfileActions.MP13,
     *                                    MicroProfileActions.MP14, MicroProfileActions.MP33, MicroProfileActions.MP40);
     * </code>
     * </pre>
     *
     * @param  server           The server to repeat on
     * @param  firstFeatureSet  The first FeatureSet
     * @param  otherFeatureSets The other FeatureSets
     * @return                  a RepeatTests instance
     */
    public static RepeatTests repeat(String server, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return repeat(server, TestMode.LITE, firstFeatureSet, otherFeatureSets);
    }

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in the mode specified by otherFeatureSetsTestMode
     * Usage: The following example will repeat the tests using MicroProfile versions 1.2, 1.3, 1.4, 3.3 and 4.0.
     * 4.0 will be in LITE mode, the others in FULL mode.
     *
     * <pre>
     * <code>
     * &#64;ClassRule
     * public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, TestMode.FULL, MicroProfileActions.MP40, MicroProfileActions.MP12,
     *                                    MicroProfileActions.MP13, MicroProfileActions.MP14, MicroProfileActions.MP33);
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

    /**
     * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet will be run in LITE mode. The others will be run in the mode specified by
     * otherFeatureSetsTestMode.
     *
     * This method is only intended to be used when extending MicroProfileActions to add in additional FeatureSets. Those additional FeatureSets should be
     * combined with the ALL set and passed in as allFeatureSets.
     *
     * @param  server                   The server to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  allFeatureSets           All known FeatureSets. The features not in the current FeatureSet are removed from the repeat
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, Set<FeatureSet> allFeatureSets, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        RepeatTests r = RepeatTests.with(forFeatureSet(allFeatureSets, firstFeatureSet, server, TestMode.LITE));
        for (int i = 0; i < otherFeatureSets.length; i++) {
            r = r.andWith(forFeatureSet(allFeatureSets, otherFeatureSets[i], server, otherFeatureSetsTestMode));
        }
        return r;
    }

    /**
     * Get a FeatureReplacementAction instance for a given FeatureSet. It will be run in the mode specified.
     *
     * @param  allFeatureSets All known FeatureSets. The features not in the specified FeatureSet are removed from the repeat action
     * @param  featureSet     The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  server         The server to repeat on
     * @param  testMode       The test mode to run the FeatureSet
     * @return                A FeatureReplacementAction instance
     */
    public static FeatureReplacementAction forFeatureSet(Set<FeatureSet> allFeatureSets, FeatureSet featureSet, String server, TestMode testMode) {
        FeatureReplacementAction action = new FeatureReplacementAction();
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
