/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat.jakarta;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                MPContextProp1_3_EE9_Test.class,
                MPContextProp1_3_EE10_Test.class
})
public class FATSuite {

    public static final String MP50_CTX13_ID = MicroProfileActions.MP50_ID + "_CTX13";
    public static final String MP61_CTX13_ID = MicroProfileActions.MP61_ID + "_CTX13";

    public static final FeatureSet MP50_CTX13 = MicroProfileActions.MP50.addFeature("mpContextPropagation-1.3").build(MP50_CTX13_ID);
    public static final FeatureSet MP61_CTX13 = MicroProfileActions.MP61.addFeature("mpContextPropagation-1.3").build(MP61_CTX13_ID);

    //All MicroProfile CTX FeatureSets - must be descending order
    private static final FeatureSet[] ALL_CTX_SETS_ARRAY = { MP61_CTX13, MP50_CTX13 };
    private static final List<FeatureSet> ALL = Arrays.asList(ALL_CTX_SETS_ARRAY);

    public static RepeatTests repeat(String serverName, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(serverName, TestMode.FULL, ALL, firstFeatureSet, otherFeatureSets);
    }

}
