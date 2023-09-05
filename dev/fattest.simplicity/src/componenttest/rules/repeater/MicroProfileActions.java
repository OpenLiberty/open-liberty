/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

public class MicroProfileActions {

    private static final String[] MP10_FEATURES_ARRAY = { "microProfile-1.0",
                                                          "cdi-1.2",
                                                          "jaxrs-2.0",
                                                          "jsonp-1.0" };

    private static final String[] MP12_FEATURES_ARRAY = { "microProfile-1.2",
                                                          "servlet-3.1",
                                                          "cdi-1.2",
                                                          "jaxrs-2.0",
                                                          "jsonp-1.0",
                                                          "mpConfig-1.1",
                                                          "mpFaultTolerance-1.0",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.0",
                                                          "mpMetrics-1.0" };

    private static final String[] MP13_FEATURES_ARRAY = { "microProfile-1.3",
                                                          "servlet-3.1",
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

    private static final String[] MP14_FEATURES_ARRAY = { "microProfile-1.4",
                                                          "servlet-3.1",
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

    private static final String[] MP20_FEATURES_ARRAY = { "microProfile-2.0",
                                                          "servlet-4.0",
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

    private static final String[] MP21_FEATURES_ARRAY = { "microProfile-2.1",
                                                          "servlet-4.0",
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

    private static final String[] MP22_FEATURES_ARRAY = { "microProfile-2.2",
                                                          "servlet-4.0",
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

    private static final String[] MP30_FEATURES_ARRAY = { "microProfile-3.0",
                                                          "servlet-4.0",
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

    private static final String[] MP32_FEATURES_ARRAY = { "microProfile-3.2",
                                                          "servlet-4.0",
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

    private static final String[] MP33_FEATURES_ARRAY = { "microProfile-3.3",
                                                          "servlet-4.0",
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

    private static final String[] MP40_FEATURES_ARRAY = { "microProfile-4.0",
                                                          "servlet-4.0",
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

    private static final String[] MP41_FEATURES_ARRAY = { "microProfile-4.1",
                                                          "servlet-4.0",
                                                          "cdi-2.0",
                                                          "jaxrs-2.1",
                                                          "jaxrsClient-2.1",
                                                          "jsonb-1.0",
                                                          "jsonp-1.1",
                                                          "mpConfig-2.0",
                                                          "mpFaultTolerance-3.0",
                                                          "mpHealth-3.1",
                                                          "mpJwt-1.2",
                                                          "mpMetrics-3.0",
                                                          "mpOpenAPI-2.0",
                                                          "mpOpenTracing-2.0",
                                                          "mpRestClient-2.0" };

    private static final String[] MP50_FEATURES_ARRAY = { "microProfile-5.0",
                                                          "servlet-5.0",
                                                          "cdi-3.0",
                                                          "restfulWS-3.0",
                                                          "restfulWSClient-3.0",
                                                          "jsonb-2.0",
                                                          "jsonp-2.0",
                                                          "mpConfig-3.0",
                                                          "mpFaultTolerance-4.0",
                                                          "mpHealth-4.0",
                                                          "mpJwt-2.0",
                                                          "mpOpenAPI-3.0",
                                                          "mpMetrics-4.0",
                                                          "mpOpenTracing-3.0",
                                                          "mpRestClient-3.0" };

    private static final String[] MP60_FEATURES_ARRAY = { "microProfile-6.0",
                                                          "cdi-4.0",
                                                          "restfulWS-3.1",
                                                          "restfulWSClient-3.1",
                                                          "jsonb-3.0",
                                                          "jsonp-2.1",
                                                          "mpConfig-3.0",
                                                          "mpFaultTolerance-4.0",
                                                          "mpHealth-4.0",
                                                          "mpJwt-2.1",
                                                          "mpOpenAPI-3.1",
                                                          "mpMetrics-5.0",
                                                          "mpTelemetry-1.0",
                                                          "mpRestClient-3.0" };

    private static final String[] MP61_FEATURES_ARRAY = { "microProfile-6.1",
                                                          "cdi-4.0",
                                                          "restfulWS-3.1",
                                                          "restfulWSClient-3.1",
                                                          "jsonb-3.0",
                                                          "jsonp-2.1",
                                                          "mpConfig-3.1",
                                                          "mpFaultTolerance-4.0",
                                                          "mpHealth-4.0",
                                                          "mpJwt-2.1",
                                                          "mpOpenAPI-3.1",
                                                          "mpMetrics-5.1",
                                                          "mpTelemetry-1.1",
                                                          "mpRestClient-3.0" };

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
    private static final Set<String> MP41_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP41_FEATURES_ARRAY)));
    private static final Set<String> MP50_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP50_FEATURES_ARRAY)));
    private static final Set<String> MP60_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP60_FEATURES_ARRAY)));
    private static final Set<String> MP61_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP61_FEATURES_ARRAY)));

    //The FeatureSet IDs. Since these will be used as the RepeatAction IDs, they can also be used in annotations such as @SkipForRepeat
    public static final String MP10_ID = EE7FeatureReplacementAction.ID + "_MicroProfile_10";
    public static final String MP12_ID = EE7FeatureReplacementAction.ID + "_MicroProfile_12";
    public static final String MP13_ID = EE7FeatureReplacementAction.ID + "_MicroProfile_13";
    public static final String MP14_ID = EE7FeatureReplacementAction.ID + "_MicroProfile_14";
    public static final String MP20_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_20";
    public static final String MP21_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_21";
    public static final String MP22_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_22";
    public static final String MP30_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_30";
    public static final String MP32_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_32";
    public static final String MP33_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_33";
    public static final String MP40_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_40";
    public static final String MP41_ID = EE8FeatureReplacementAction.ID + "_MicroProfile_41";
    public static final String MP50_ID = JakartaEE9Action.ID + "_MicroProfile_50";
    public static final String MP60_ID = JakartaEE10Action.ID + "_MicroProfile_60";
    public static final String MP61_ID = JakartaEE10Action.ID + "_MicroProfile_61";

    //The MicroProfile FeatureSets
    public static final FeatureSet MP10 = new FeatureSet(MP10_ID, MP10_FEATURE_SET, EEVersion.EE7);
    public static final FeatureSet MP12 = new FeatureSet(MP12_ID, MP12_FEATURE_SET, EEVersion.EE7);
    public static final FeatureSet MP13 = new FeatureSet(MP13_ID, MP13_FEATURE_SET, EEVersion.EE7);
    public static final FeatureSet MP14 = new FeatureSet(MP14_ID, MP14_FEATURE_SET, EEVersion.EE7);
    public static final FeatureSet MP20 = new FeatureSet(MP20_ID, MP20_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP21 = new FeatureSet(MP21_ID, MP21_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP22 = new FeatureSet(MP22_ID, MP22_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP30 = new FeatureSet(MP30_ID, MP30_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP32 = new FeatureSet(MP32_ID, MP32_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP33 = new FeatureSet(MP33_ID, MP33_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP40 = new FeatureSet(MP40_ID, MP40_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP41 = new FeatureSet(MP41_ID, MP41_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP50 = new FeatureSet(MP50_ID, MP50_FEATURE_SET, EEVersion.EE9);
    public static final FeatureSet MP60 = new FeatureSet(MP60_ID, MP60_FEATURE_SET, EEVersion.EE10);
    public static final FeatureSet MP61 = new FeatureSet(MP61_ID, MP61_FEATURE_SET, EEVersion.EE10);

    //All MicroProfile FeatureSets, needs to be in order for repeat(String, TestMode, Set, FeatureSet, Set)
    private static final FeatureSet[] ALL_SETS_ARRAY = { MP10, MP12, MP13, MP14, MP20, MP21, MP22, MP30, MP32, MP33, MP40, MP41, MP50, MP60, MP61 };
    public static final Set<FeatureSet> ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ALL_SETS_ARRAY)));

    //TODO: These feature sets are only used by the EE Compatibility Tests and don't make sense in other contexts. We should move them to those tests.

    private static final String[] STANDALONE8_FEATURES_ARRAY = { "mpContextPropagation-1.0",
                                                                 "mpContextPropagation-1.2",
                                                                 "mpGraphQL-1.0",
                                                                 "mpLRA-1.0",
                                                                 "mpLRACoordinator-1.0",
                                                                 "mpReactiveMessaging-1.0",
                                                                 "mpReactiveStreams-1.0" };

    private static final String[] STANDALONE9_FEATURES_ARRAY = { "mpContextPropagation-1.3",
                                                                 "mpGraphQL-2.0",
                                                                 "mpReactiveStreams-3.0" };

    private static final String[] STANDALONE10_FEATURES_ARRAY = { "mpContextPropagation-1.3",
                                                                  "mpGraphQL-2.0",
                                                                  "mpReactiveStreams-3.0" };

    private static final Set<String> STANDALONE8_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(STANDALONE8_FEATURES_ARRAY)));
    private static final Set<String> STANDALONE9_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(STANDALONE9_FEATURES_ARRAY)));
    private static final Set<String> STANDALONE10_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(STANDALONE10_FEATURES_ARRAY)));

    public static final String STANDALONE8_ID = EE8FeatureReplacementAction.ID + "_STANDALONE";
    public static final String STANDALONE9_ID = JakartaEE9Action.ID + "_STANDALONE";
    public static final String STANDALONE10_ID = JakartaEE10Action.ID + "_STANDALONE";

    public static final FeatureSet MP_STANDALONE8 = new FeatureSet(STANDALONE8_ID, STANDALONE8_FEATURE_SET, EEVersion.EE8);
    public static final FeatureSet MP_STANDALONE9 = new FeatureSet(STANDALONE9_ID, STANDALONE9_FEATURE_SET, EEVersion.EE9);
    public static final FeatureSet MP_STANDALONE10 = new FeatureSet(STANDALONE10_ID, STANDALONE10_FEATURE_SET, EEVersion.EE10);

    //All MicroProfile Standalone FeatureSets
    private static final FeatureSet[] ALL_STANDALONE_SETS_ARRAY = { MP_STANDALONE8, MP_STANDALONE9, MP_STANDALONE10 };
    public static final Set<FeatureSet> STANDALONE_ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ALL_STANDALONE_SETS_ARRAY)));

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
     * Usage: The following example will repeat the tests using MicroProfile versions 1.2, 1.3, 1.4, 3.3 and 4.0 (1.2 in LITE mode, the others in FULL).
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
        return repeat(server, TestMode.FULL, firstFeatureSet, otherFeatureSets);
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
            Collections.reverse(allSetsList); // Reverse list so newest MP version is first in list

            Collection<FeatureSet> candidateFeatureSets = otherFeatureSets;

            // Find the newest MP feature set that's in otherFeatureSets and is compatible with the current java version
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
