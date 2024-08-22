/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.CheckpointEE10Action;
import componenttest.rules.repeater.CheckpointEE11Action;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                EJBJakarta10Test.class
})

public class FATSuite {
    /*
     * Run EE10 tests in LITE mode if Java 11, EE11 tests in LITE mode if >= Java 17 and run all tests in FULL mode.
     */
    public static RepeatTests defaultRepeat(String serverName) {
        return RepeatTests.with(FeatureReplacementAction.EE10_FEATURES().forServers(serverName).conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17)).andWith(FeatureReplacementAction.EE11_FEATURES().forServers(serverName).removeFeature("usr:jaccTestProvider-2.1").addFeature("usr:jaccTestProvider-3.0"));
    }

    public static RepeatTests defaultAndCheckpointRepeat(String serverName) {
        return defaultRepeat(serverName).andWith(new CheckpointEE10Action().forServers(serverName).removeFeature("usr:jaccTestProvider-3.0").addFeature("usr:jaccTestProvider-2.1").fullFATOnly()).andWith(new CheckpointEE11Action().forServers(serverName).removeFeature("usr:jaccTestProvider-2.1").addFeature("usr:jaccTestProvider-3.0"));
    }

}
