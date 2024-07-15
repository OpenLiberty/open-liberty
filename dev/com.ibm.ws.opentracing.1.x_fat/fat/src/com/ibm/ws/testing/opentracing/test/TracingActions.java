/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.testing.opentracing.test;

import java.util.Arrays;
import java.util.List;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatTests;

public class TracingActions {
	public static final String MP14_MPOT11_ID = MicroProfileActions.MP14_ID + "_MPOT11";
	public static final String MP21_MPOT12_ID = MicroProfileActions.MP21_ID + "_MPOT12";
	public static final String MP30_MPOT13_ID = MicroProfileActions.MP30_ID + "_MPOT13";

	public static final FeatureSet MP14_MPOT11 = MicroProfileActions.MP14
			.addFeature("mpOpenTracing-1.1")
			.addFeature("usr:opentracingMock-1.1")
			.build(MP14_MPOT11_ID);

	public static final FeatureSet MP21_MPOT12 = MicroProfileActions.MP21
			.addFeature("mpOpenTracing-1.2")
			.addFeature("usr:opentracingMock-1.2")
			.build(MP21_MPOT12_ID);

	public static final FeatureSet MP30_MPOT13 = MicroProfileActions.MP30
			.addFeature("mpOpenTracing-1.3")
			.addFeature("usr:opentracingMock-1.3")
			.build(MP30_MPOT13_ID);

	// All MicroProfile Tracing FeatureSets - must be descending order
	private static final FeatureSet[] ALL_MPOT_SETS_ARRAY = { MP30_MPOT13, MP21_MPOT12, MP14_MPOT11 };
	private static final List<FeatureSet> ALL_MPOT_SETS_LIST = Arrays.asList(ALL_MPOT_SETS_ARRAY);

	public static RepeatTests defaultRepeat(String server) {
		return TracingActions.repeat(server, TracingActions.MP30_MPOT13, TracingActions.MP21_MPOT12, TracingActions.MP14_MPOT11);
	}
	
	public static RepeatTests defaultRepeatMP14(String server) {
		return TracingActions.repeat(server, TracingActions.MP21_MPOT12, TracingActions.MP14_MPOT11);
	}
	
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
		return RepeatActions.forFeatureSet(ALL_MPOT_SETS_LIST, featureSet, new String[] { server }, TestMode.FULL);
	}

	/**
	 * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet
	 * will be run in LITE mode. The others will be run in FULL.
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
	 * Get a RepeatTests instance for the given FeatureSets. The first FeatureSet
	 * will be run in LITE mode. The others will be run in the mode specified.
	 *
	 * @param server                   The server to repeat on
	 * @param otherFeatureSetsTestMode The mode to run the other FeatureSets
	 * @param firstFeatureSet          The first FeatureSet
	 * @param otherFeatureSets         The other FeatureSets
	 * @return a RepeatTests instance
	 */
	public static RepeatTests repeat(String server, TestMode otherFeatureSetsTestMode, FeatureSet firstFeatureSet,
			FeatureSet... otherFeatureSets) {
		return RepeatActions.repeat(server, otherFeatureSetsTestMode, ALL_MPOT_SETS_LIST, firstFeatureSet,
				Arrays.asList(otherFeatureSets));
	}

}
