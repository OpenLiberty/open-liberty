/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.auth.data.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                DefaultPrincipalMappingTest.class,
                SingleFeatureDefaultPrincipalMappingTest.class,
                Java2SecurityTest.class
})
public class FATSuite {

    /*
     * Run EE9 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new JakartaEE9Action().removeFeature("appSecurity-1.0").addFeature("appSecurity-4.0"));
}
