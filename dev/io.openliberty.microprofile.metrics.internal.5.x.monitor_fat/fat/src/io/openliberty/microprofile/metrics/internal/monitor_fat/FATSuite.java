/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics.internal.monitor_fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({ComputedMetricsTest.class, MetricsMonitorTest.class,
        TestEnableDisableFeaturesTest.class})
public class FATSuite {

    /*
     * The below commented ClassRule is used to administer the execution of this FAT for
     * multiple version of mpMetrics-5.x.
     * 
     * It is left here, commented out, for later use when subsequent version of
     * mpMetrics-5.x are available (i.e when 5.1 is released)
     */
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction("mpMetrics-5.0", "mpMetrics-5.1").withID("MPM51").forceAddFeatures(false));


}
