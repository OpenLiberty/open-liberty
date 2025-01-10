/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatActions.EEVersion;

public class MicroProfileActions {

    private static final Class<MicroProfileActions> c = MicroProfileActions.class;

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
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-1.1",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.1",
                                                          "mpRestClient-1.1",
                                                          "mpContextPropagation-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0", //standalone
                                                          "mpReactiveMessaging-1.0", //standalone
                                                          "mpReactiveStreams-1.0" }; //standalone

    private static final String[] MP21_FEATURES_ARRAY = { "microProfile-2.1",
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-1.1",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.2",
                                                          "mpRestClient-1.1",
                                                          "mpContextPropagation-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0", //standalone
                                                          "mpReactiveMessaging-1.0", //standalone
                                                          "mpReactiveStreams-1.0" }; //standalone

    private static final String[] MP22_FEATURES_ARRAY = { "microProfile-2.2",
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-2.0",
                                                          "mpHealth-1.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-1.1",
                                                          "mpOpenAPI-1.0",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.2",
                                                          "mpContextPropagation-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0", //standalone
                                                          "mpReactiveMessaging-1.0", //standalone
                                                          "mpReactiveStreams-1.0" }; //standalone

    private static final String[] MP30_FEATURES_ARRAY = { "microProfile-3.0",
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-2.0",
                                                          "mpHealth-2.0",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-2.0",
                                                          "mpOpenAPI-1.1",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.3",
                                                          "mpContextPropagation-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0", //standalone
                                                          "mpReactiveMessaging-1.0", //standalone
                                                          "mpReactiveStreams-1.0" }; //standalone

    private static final String[] MP32_FEATURES_ARRAY = { "microProfile-3.2",
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-1.3",
                                                          "mpFaultTolerance-2.0",
                                                          "mpHealth-2.1",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-2.2",
                                                          "mpOpenAPI-1.1",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.3",
                                                          "mpContextPropagation-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0", //standalone
                                                          "mpReactiveMessaging-1.0", //standalone
                                                          "mpReactiveStreams-1.0" }; //standalone

    private static final String[] MP33_FEATURES_ARRAY = { "microProfile-3.3",
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-1.4",
                                                          "mpFaultTolerance-2.1",
                                                          "mpHealth-2.2",
                                                          "mpJwt-1.1",
                                                          "mpMetrics-2.3",
                                                          "mpOpenAPI-1.1",
                                                          "mpOpenTracing-1.3",
                                                          "mpRestClient-1.4",
                                                          "mpContextPropagation-1.0", //standalone
                                                          "mpGraphQL-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0", //standalone
                                                          "mpReactiveMessaging-1.0", //standalone
                                                          "mpReactiveStreams-1.0" }; //standalone

    private static final String[] MP40_FEATURES_ARRAY = { "microProfile-4.0",
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-2.0",
                                                          "mpFaultTolerance-3.0",
                                                          "mpHealth-3.0",
                                                          "mpJwt-1.2",
                                                          "mpMetrics-3.0",
                                                          "mpOpenAPI-2.0",
                                                          "mpOpenTracing-2.0",
                                                          "mpRestClient-2.0",
                                                          "mpContextPropagation-1.2", //standalone
                                                          "mpGraphQL-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0" }; //standalone

    private static final String[] MP41_FEATURES_ARRAY = { "microProfile-4.1",
                                                          "servlet-4.0", //ee8
                                                          "cdi-2.0", //ee8
                                                          "jaxrs-2.1", //ee8
                                                          "jaxrsClient-2.1", //ee8
                                                          "jsonb-1.0", //ee8
                                                          "jsonp-1.1", //ee8
                                                          "mpConfig-2.0",
                                                          "mpFaultTolerance-3.0",
                                                          "mpHealth-3.1",
                                                          "mpJwt-1.2",
                                                          "mpMetrics-3.0",
                                                          "mpOpenAPI-2.0",
                                                          "mpOpenTracing-2.0",
                                                          "mpRestClient-2.0",
                                                          "mpContextPropagation-1.2", //standalone
                                                          "mpGraphQL-1.0", //standalone
                                                          "mpLRA-1.0", //standalone
                                                          "mpLRACoordinator-1.0" }; //standalone

    private static final String[] MP50_FEATURES_ARRAY = { "microProfile-5.0",
                                                          "servlet-5.0", //ee9
                                                          "cdi-3.0", //ee9
                                                          "restfulWS-3.0", //ee9
                                                          "restfulWSClient-3.0", //ee9
                                                          "jsonb-2.0", //ee9
                                                          "jsonp-2.0", //ee9
                                                          "mpConfig-3.0",
                                                          "mpFaultTolerance-4.0",
                                                          "mpHealth-4.0",
                                                          "mpJwt-2.0",
                                                          "mpOpenAPI-3.0",
                                                          "mpMetrics-4.0",
                                                          "mpOpenTracing-3.0",
                                                          "mpRestClient-3.0",
                                                          "mpContextPropagation-1.3", //standalone
                                                          "mpGraphQL-2.0" }; //standalone

    private static final String[] MP60_FEATURES_ARRAY = { "microProfile-6.0",
                                                          "cdi-4.0", //ee10
                                                          "restfulWS-3.1", //ee10
                                                          "restfulWSClient-3.1", //ee10
                                                          "jsonb-3.0", //ee10
                                                          "jsonp-2.1", //ee10
                                                          "mpConfig-3.0",
                                                          "mpFaultTolerance-4.0",
                                                          "mpHealth-4.0",
                                                          "mpJwt-2.1",
                                                          "mpOpenAPI-3.1",
                                                          "mpMetrics-5.0",
                                                          "mpTelemetry-1.0",
                                                          "mpRestClient-3.0",
                                                          "mpContextPropagation-1.3", //standalone
                                                          "mpGraphQL-2.0", //standalone
                                                          "mpReactiveMessaging-3.0", //standalone
                                                          "mpReactiveStreams-3.0" };//standalone

    private static final String[] MP61_FEATURES_ARRAY = { "microProfile-6.1",
                                                          "cdi-4.0", //ee10
                                                          "restfulWS-3.1", //ee10
                                                          "restfulWSClient-3.1", //ee10
                                                          "jsonb-3.0", //ee10
                                                          "jsonp-2.1", //ee10
                                                          "mpConfig-3.1",
                                                          "mpFaultTolerance-4.0",
                                                          "mpHealth-4.0",
                                                          "mpJwt-2.1",
                                                          "mpOpenAPI-3.1",
                                                          "mpMetrics-5.1",
                                                          "mpTelemetry-1.1",
                                                          "mpRestClient-3.0",
                                                          "mpContextPropagation-1.3", //standalone
                                                          "mpGraphQL-2.0", //standalone
                                                          "mpReactiveMessaging-3.0", //standalone
                                                          "mpReactiveStreams-3.0" };//standalone

    private static final String[] MP70_EE10_FEATURES_ARRAY = { "microProfile-7.0",
                                                               "cdi-4.0", //ee10
                                                               "restfulWS-3.1", //ee10
                                                               "restfulWSClient-3.1", //ee10
                                                               "jsonb-3.0", //ee10
                                                               "jsonp-2.1", //ee10
                                                               "mpConfig-3.1",
                                                               "mpFaultTolerance-4.1",
                                                               "mpHealth-4.0",
                                                               "mpJwt-2.1",
                                                               "mpOpenAPI-4.0",
                                                               "mpTelemetry-2.0",
                                                               "mpRestClient-4.0",
                                                               "mpMetrics-5.1", //standalone
                                                               "mpContextPropagation-1.3", //standalone
                                                               "mpGraphQL-2.0", //standalone
                                                               "mpReactiveMessaging-3.0", //standalone
                                                               "mpReactiveStreams-3.0" };//standalone

    private static final String[] MP70_EE11_FEATURES_ARRAY = { "microProfile-7.0",
                                                               "cdi-4.1", //ee11
                                                               "restfulWS-4.0", //ee11
                                                               "restfulWSClient-4.0", //ee11
                                                               "jsonb-3.0", //ee11
                                                               "jsonp-2.1", //ee11
                                                               "mpConfig-3.1",
                                                               "mpFaultTolerance-4.1",
                                                               "mpHealth-4.0",
                                                               "mpJwt-2.1",
                                                               "mpOpenAPI-4.0",
                                                               "mpTelemetry-2.0",
                                                               "mpRestClient-4.0",
                                                               "mpMetrics-5.1", //standalone
                                                               "mpContextPropagation-1.3", //standalone
                                                               "mpGraphQL-2.0", //standalone
                                                               "mpReactiveMessaging-3.0", //standalone
                                                               "mpReactiveStreams-3.0" };//standalone

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
    private static final Set<String> MP70_EE10_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP70_EE10_FEATURES_ARRAY)));
    private static final Set<String> MP70_EE11_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MP70_EE11_FEATURES_ARRAY)));

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
    public static final String MP70_EE10_ID = JakartaEE10Action.ID + "_MicroProfile_70";
    public static final String MP70_EE11_ID = JakartaEE11Action.ID + "_MicroProfile_70";
    public static final String MP70_EE11_APP_MODE_ID = MP70_EE11_ID + "_App_Mode";

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
    public static final FeatureSet MP70_EE10 = new FeatureSet(MP70_EE10_ID, MP70_EE10_FEATURE_SET, EEVersion.EE10);
    public static final FeatureSet MP70_EE11 = new FeatureSet(MP70_EE11_ID, MP70_EE11_FEATURE_SET, EEVersion.EE11);
    public static final FeatureSet MP70_EE11_APP_MODE = new FeatureSet(MP70_EE11_APP_MODE_ID, MP70_EE11_FEATURE_SET, EEVersion.EE11);

    //All MicroProfile FeatureSets - must be descending order
    private static final FeatureSet[] ALL_SETS_ARRAY = { MP70_EE11, MP70_EE10, MP61, MP60, MP50, MP41, MP40, MP33, MP32, MP30, MP22, MP21, MP20, MP14, MP13, MP12, MP10 };
    public static final List<FeatureSet> ALL = Collections.unmodifiableList(Arrays.asList(ALL_SETS_ARRAY));

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
     * @param  servers          The servers to repeat on
     * @param  firstFeatureSet  The first FeatureSet
     * @param  otherFeatureSets The other FeatureSets
     * @return                  a RepeatTests instance
     */
    public static RepeatTests repeat(String[] servers, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return repeat(servers, TestMode.FULL, firstFeatureSet, otherFeatureSets);
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
        return repeat(new String[] { server }, otherFeatureSetsTestMode, firstFeatureSet, otherFeatureSets);
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
     * @param  skipTransformation       Skip transformation for actions
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, boolean skipTransformation, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return repeat(new String[] { server }, otherFeatureSetsTestMode, skipTransformation, firstFeatureSet, otherFeatureSets);
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
     * @param  servers                  The servers to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String[] servers, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(servers, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets), false);
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
     * @param  servers                  The servers to repeat on
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  skipTransformation       Skip transformation for actions
     * @param  firstFeatureSet          The first FeatureSet to repeat with. This is run in LITE mode.
     * @param  otherFeatureSets         The other FeatureSets to repeat with. These are in the mode specified by otherFeatureSetsTestMode
     * @return                          A RepeatTests instance
     */
    public static RepeatTests repeat(String[] servers, TestMode otherFeatureSetsTestMode, boolean skipTransformation, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(servers, otherFeatureSetsTestMode, ALL, firstFeatureSet, Arrays.asList(otherFeatureSets), skipTransformation);
    }

    /**
     * Get a repeat test for the following FeatureSets, with a predicate that will be used to filter out the FeatureSets.
     * The first FeatureSet to pass the predicate will be run in LITE mode. The others will be run in FULL.
     *
     * @param  serverName  the server to repeat on
     * @param  predicate   a predicate that will filter feature sets. If the predicate returns true, the feature set will be run
     * @param  featureSet  the first feature set
     * @param  featureSets the feature sets
     * @return             a RepeatTests instance
     */
    public static RepeatTests repeatIf(String serverName, Predicate<FeatureSet> predicate, FeatureSet featureSet, FeatureSet... featureSets) {
        return repeatIf(serverName, predicate, TestMode.FULL, false, featureSet, featureSets);
    }

    /**
     * Get a repeat test for the following FeatureSets, with a predicate that will be used to filter out the FeatureSets.
     * The first FeatureSet to pass the predicate will be run in LITE mode. The others will be run in {@code otherFeatureSetsTestMode}.
     *
     * @param  serverName               the server to repeat on
     * @param  predicate                a predicate that will filter feature sets. If the predicate returns true, the feature set will be run
     * @param  otherFeatureSetsTestMode The test mode to run the otherFeatureSets
     * @param  skipTransformation       Skip transformation for actions
     * @param  featureSet               the first feature set
     * @param  featureSets              the feature sets
     * @return                          a RepeatTests instance
     */
    public static RepeatTests repeatIf(String serverName, Predicate<FeatureSet> predicate, TestMode otherFeatureSetsTestMode,
                                       boolean skipTransformation, FeatureSet featureSet, FeatureSet... featureSets) {

        final String m = "repeatIf";

        List<FeatureSet> featureSetsMerged = new ArrayList<FeatureSet>();
        featureSetsMerged.add(featureSet);
        featureSetsMerged.addAll(Arrays.asList(featureSets));

        Log.info(c, m, "enter. Testing " +
                       featureSetsMerged.stream().map(FeatureSet::getID).collect(Collectors.joining(", ")));

        featureSetsMerged.removeIf((FeatureSet fs) -> (!predicate.test(fs)));

        if (featureSetsMerged.isEmpty()) {
            Log.info(c, m, "found no acceptable FeatureSets");
            return RepeatTests.with(new DisabledAction());
        }

        Log.info(c, m, "found the following aceptable FeatureSets " +
                       featureSetsMerged.stream().map(FeatureSet::getID).collect(Collectors.joining(", ")));

        FeatureSet firstAction = featureSetsMerged.remove(0);
        FeatureSet[] otherActions = featureSetsMerged.toArray(new FeatureSet[0]);

        return repeat(serverName, otherFeatureSetsTestMode, skipTransformation, firstAction, otherActions);
    }
}
