/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({DefaultHistogramBucketsTest.class,
		BadHistogramTimerConfigTest.class, HistogramConfigFieldTest.class,
		TimerConfigFieldTest.class})

public class FATSuite {
	@ClassRule
	public static RepeatTests r = RepeatTests.withoutModification()
			.andWith(FeatureReplacementAction.EE9_FEATURES()
					.addFeature("timedexit-1.0").removeFeature("mpMetrics-3.0")
					.addFeature("mpMetrics-4.0"));
}
