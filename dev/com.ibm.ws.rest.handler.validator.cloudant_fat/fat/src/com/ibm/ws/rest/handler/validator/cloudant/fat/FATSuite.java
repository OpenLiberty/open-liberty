/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.cloudant.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                ValidateCloudantTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() // run all tests as-is (e.g. EE8 features)
                    .andWith(new JakartaEE9Action()); // run all tests again with EE9 features+packages

    static {
        // TODO: temporary debug setting so we can further investigate intermittent
        // testcontainers ping issues on remote build machines
        System.setProperty("javax.net.debug", "all");
    }

    @BeforeClass
    public static void setup() throws Exception {
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpUtils.setDefaultAuth("adminuser", "adminpwd");
    }

}
