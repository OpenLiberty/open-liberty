/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.configprops;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;

@RunWith(Suite.class)
@SuiteClasses(JCAConfigPropsTest.class)
public class FATSuite {

    /*
     * EE7 will run in full mode only.
     * EE9 will run in full mode only.
     * EE10 will run in full and lite mode.
     */
    @ClassRule
    public static RepeatTests repeat;

    static {
        // EE10 requires Java 11.  If we only specify EE10 for lite mode it will cause no tests to run which causes an error.
        // If we are running on Java 8 have EE9 be the lite mode test to run.
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE10_FEATURES());
        } else {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES());
        }

    }
}