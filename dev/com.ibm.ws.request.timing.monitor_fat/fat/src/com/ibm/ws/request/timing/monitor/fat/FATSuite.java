/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.request.timing.monitor.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                RequestTimingMbeanTest.class,
                RequestTimingEventTest.class,
                RequestTimingMetricsTest.class })
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(new FeatureReplacementAction("mpMetrics-3.0", "mpMetrics-2.3").withID("MPM23")).andWith(new FeatureReplacementAction("mpMetrics-2.3", "mpMetrics-2.2").withID("MPM22")).andWith(new FeatureReplacementAction("mpMetrics-2.2", "mpMetrics-2.0").withID("MPM20")).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("mpMetrics-2.0")).andWith(FeatureReplacementAction.EE10_FEATURES().removeFeature("mpMetrics-2.0"));

}
