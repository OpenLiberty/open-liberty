/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.mpjwt10.tck;

import java.util.HashSet;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                Mpjwt10TCKLauncher_mpjwt.class,
                Mpjwt10TCKLauncher_mpjwt_roles.class,
                Mpjwt10TCKLauncher_mpjwt_testUtils.class,
                DummyForQuarantine.class
})

public class FATSuite {

    // our default config pulls in cdi 1.2, but we need to check 2.0.
    // here's an easy way to do that.
    static HashSet<String> addfeatures = new HashSet<String>();
    static HashSet<String> removefeatures = new HashSet<String>();
    static {
        addfeatures.add("cdi-2.0");
        addfeatures.add("jaxrs-2.1");
        removefeatures.add("jaxrs-2.0");

    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction(removefeatures, addfeatures));

}
