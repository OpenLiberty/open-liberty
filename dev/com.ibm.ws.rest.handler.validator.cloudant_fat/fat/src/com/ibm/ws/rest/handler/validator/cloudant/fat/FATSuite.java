/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.cloudant.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpsRequest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class
})
public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() // run all tests as-is (e.g. EE8 features)
                    .andWith(FeatureReplacementAction.EE9_FEATURES().alwaysAddFeature("servlet-5.0")) // run all tests again with EE9 features+packages
                    .andWith(FeatureReplacementAction.EE10_FEATURES().alwaysAddFeature("servlet-6.0"))
                    .andWith(FeatureReplacementAction.EE11_FEATURES().alwaysAddFeature("servlet-6.1"));

    static {
        // TODO: temporary debug setting so we can further investigate intermittent
        // testcontainers ping issues on remote build machines
        System.setProperty("javax.net.debug", "all");
    }

    public static HttpsRequest createHttpsRequestWithAdminUser(LibertyServer server, String path) {
        return new HttpsRequest(server, path).allowInsecure().basicAuth("adminuser", "adminpwd");
    }

}
