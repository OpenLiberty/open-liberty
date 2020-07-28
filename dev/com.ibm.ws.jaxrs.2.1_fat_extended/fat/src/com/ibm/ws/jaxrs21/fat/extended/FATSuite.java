/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat.extended;

import java.util.Set;
import java.util.HashSet;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                
                CDITest.class,
                ClassSubResTest.class,
                JsonbCharsetTest.class,
                JsonbContextResolverTest.class,
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
        RepeatTests.withoutModification()
                   .andWith(new FeatureReplacementAction(setOf("jaxrs-2.1", "jaxrs-2.0"), setOf("jaxrs-2.2")).withID("RESTEasy"));
    
    private static Set<String> setOf(String...strings) {
        Set<String> set = new HashSet<>();
        for (String s : strings) {
            set.add(s);
        }
        return set;
    }
}
