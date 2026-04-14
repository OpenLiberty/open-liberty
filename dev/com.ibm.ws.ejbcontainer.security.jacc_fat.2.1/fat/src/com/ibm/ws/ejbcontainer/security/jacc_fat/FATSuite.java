/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.CheckpointEE10Action;
import componenttest.rules.repeater.CheckpointEE11Action;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                EJBJakarta10Test.class
})

public class FATSuite {

    public static final String EE11_SPEC_ID = JakartaEEAction.EE11_ACTION_ID + "_spec";

    /*
     * Run EE10 tests in LITE mode if Java 11, EE11 tests in LITE mode if >= Java 17 and run all tests in FULL mode.
     */
    /*@formatter:off*/
    public static RepeatTests defaultRepeat(String serverName) {
        return RepeatTests.with(ee10Action(serverName, false)
                                .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                        .andWith(ee11Action(serverName, false))
                        .andWith(ee11SpecAction(serverName, false));
    }

    public static RepeatTests defaultAndCheckpointRepeat(String serverName) {
        return defaultRepeat(serverName)
                        .andWith(ee10Action(serverName, true).fullFATOnly())
                        .andWith(ee11Action(serverName, true))
                        .andWith(ee11SpecAction(serverName, true));
    }
    /*@formatter:on*/

    public static FeatureReplacementAction ee10Action(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE10Action() : FeatureReplacementAction.EE10_FEATURES();
        return action.forServers(serverName).removeFeature("usr:jaccTestProvider-3.0").removeFeature("usr:authzTestProvider-3.0").addFeature("usr:jaccTestProvider-2.1");
    }

    public static FeatureReplacementAction ee11Action(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE11Action() : FeatureReplacementAction.EE11_FEATURES();
        return action.forServers(serverName).removeFeature("usr:jaccTestProvider-2.1").removeFeature("usr:authzTestProvider-3.0").addFeature("usr:jaccTestProvider-3.0");
    }

    public static FeatureReplacementAction ee11SpecAction(String serverName, boolean checkpointRepeat) {
        FeatureReplacementAction action = checkpointRepeat ? new CheckpointEE11Action() : FeatureReplacementAction.EE11_FEATURES();
        return action.forServers(serverName).removeFeature("usr:jaccTestProvider-2.1").removeFeature("usr:jaccTestProvider-3.0").addFeature("usr:authzTestProvider-3.0").withID(EE11_SPEC_ID);
    }
}
