/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
@SuiteClasses({ MetricsMonitorTest.class, TestEnableDisableFeaturesTest.class })

public class FATSuite {

    /*
     * The below commented ClassRule is used to administer the execution of this FAT
     * for multiple version of mpMetrics-3.x.
     * 
     * It is left here, commented out, for later use when subsequent version of
     * mpMetrics-3.x are available (i.e when 3.1 is released)
     */
//    @ClassRule
//    public static RepeatTests r = RepeatTests.withoutModification()
//                    .andWith(new FeatureReplacementAction("mpMetrics-3.0", "mpMetrics-3.X").withID("MPM3X"));

}
