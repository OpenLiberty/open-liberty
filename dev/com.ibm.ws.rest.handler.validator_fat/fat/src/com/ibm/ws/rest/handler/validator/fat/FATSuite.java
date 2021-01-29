/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                ValidateDataSourceTest.class,
                ValidateJCATest.class,
                ValidateJMSTest.class,
                ValidateDSCustomLoginModuleTest.class,
                ValidateOpenApiSchemaTest.class
})

public class FATSuite {
// TODO: Enable this once a jakarta enabled mpopenapi is available
//    @ClassRule
//    public static RepeatTests r = RepeatTests.with(new EmptyAction())
//                    .andWith(FeatureReplacementAction.EE9_FEATURES());

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(null, TestMode.FULL,
                                                             MicroProfileActions.MP40,
                                                             MicroProfileActions.MP30,
                                                             MicroProfileActions.MP20);

    @BeforeClass
    public static void setup() throws Exception {
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpUtils.setDefaultAuth("adminuser", "adminpwd");
    }
}