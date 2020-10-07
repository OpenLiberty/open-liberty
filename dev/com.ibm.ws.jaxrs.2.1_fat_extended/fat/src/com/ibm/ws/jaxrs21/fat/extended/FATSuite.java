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

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.JakartaEE9Action;
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
                   .andWith(new JakartaEE9Action());
}
