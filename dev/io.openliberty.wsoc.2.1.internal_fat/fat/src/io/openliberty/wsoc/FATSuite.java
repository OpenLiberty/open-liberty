/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.wsoc.tests.Basic21Test;
import io.openliberty.wsoc.tests.SSLBasic21Test;

/**
 * WebSocket tests for WebSocket 2.1.
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({
                Basic21Test.class,
                SSLBasic21Test.class
})
public class FATSuite {

    // EE11 requires Java 17
    // If we only specify EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    // If we are running on a Java version less than 17, have EE10 (EmptyAction) be the lite mode test to run.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
