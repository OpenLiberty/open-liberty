/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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
package com.ibm.ws.concurrent.mp.fat.jakarta;

import java.util.ArrayList;
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

    private static final List<FeatureSet> ALL;

    static {
        //The list of all features must be in decending order
        ALL = new ArrayList<>(MicroProfileActions.ALL);
        //put MP61_CTX13 just before MP61
        ALL.add(ALL.indexOf(MicroProfileActions.MP61), MP61_CTX13);
        //put MP50_CTX13 just before MP50
        ALL.add(ALL.indexOf(MicroProfileActions.MP50), MP50_CTX13);
    }

    public static RepeatTests repeat(String serverName, FeatureSet firstFeatureSet, FeatureSet... otherFeatureSets) {
        return RepeatActions.repeat(serverName, TestMode.FULL, ALL, firstFeatureSet, otherFeatureSets);
    }

}
