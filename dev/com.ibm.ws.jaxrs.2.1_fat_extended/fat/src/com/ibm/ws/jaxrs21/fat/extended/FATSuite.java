/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat.extended;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AcceptLanguageTest.class,
                CDITest.class,
                ClassSubResTest.class,
                JsonbCharsetTest.class,
                JsonbContextResolverTest.class,
                JsonpTest.class,
                FormBehaviorTest.class,
                MutableHeadersTest.class,
                PackageJsonBTestNoFeature.class,
                PackageJsonBTestWithFeature.class,
                PatchTest.class,
                ProviderPriorityTest.class,
                SubResourceTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = 
        RepeatTests.withoutModificationInFullMode()
                   .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                   .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                   .andWith(FeatureReplacementAction.EE11_FEATURES());
}
