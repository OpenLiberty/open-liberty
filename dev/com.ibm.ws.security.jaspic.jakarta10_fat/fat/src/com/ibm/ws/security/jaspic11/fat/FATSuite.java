/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.jaspic11.fat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        JASPIServerAuthRegistrationModuleTest.class
})

public class FATSuite {

    private static final Set<String> EE10_11_FEATURES;
    private static final String[] EE10_11_FEATURES_ARRAY = {
            "usr:jaspicUserTestFeature-3.0"
    };
    static {
        EE10_11_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE10_11_FEATURES_ARRAY)));
    }

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_17)).andWith(FeatureReplacementAction.EE11_FEATURES().setSkipTransformation(true));

    //public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new JakartaEE10Action().removeFeatures(EE78_FEATURES).removeFeatures(EE9_FEATURES).addFeatures(EE10_FEATURES));

}
