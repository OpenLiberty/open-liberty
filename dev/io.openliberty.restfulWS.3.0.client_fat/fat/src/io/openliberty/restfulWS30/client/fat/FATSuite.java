/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.restfulWS30.client.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.restfulWS30.client.fat.test.PathParamTest;


@RunWith(Suite.class)
@SuiteClasses({
                ClientFeatureTest.class,
                SslTest.class,
                PathParamTest.class,
                RegisterRestClientTest.class
})
public class FATSuite { 
    @ClassRule
    public static RepeatTests r = 
        RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_11))
        .andWith(FeatureReplacementAction.EE10_FEATURES().setSkipTransformation(true).conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
        .andWith(FeatureReplacementAction.EE11_FEATURES().removeFeature("mpRestClient-3.0").addFeature("mpRestClient-4.0").setSkipTransformation(true));
}
