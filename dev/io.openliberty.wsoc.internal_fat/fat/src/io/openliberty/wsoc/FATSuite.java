/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
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
import io.openliberty.wsoc.tests.BasicTest;
import io.openliberty.wsoc.tests.Cdi12Test;
import io.openliberty.wsoc.tests.Cdi20Test;
import io.openliberty.wsoc.tests.Cdi20TxTest;
import io.openliberty.wsoc.tests.MiscellaneousTest;
import io.openliberty.wsoc.tests.SecureTest;
import io.openliberty.wsoc.tests.TraceTest;
import io.openliberty.wsoc.tests.WebSocket11Test;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({
                BasicTest.class,
                WebSocket11Test.class,
                Cdi12Test.class,
                Cdi20Test.class,
                Cdi20TxTest.class,
                MiscellaneousTest.class,
                SecureTest.class,
                TraceTest.class
})
public class FATSuite {

    // websocket-1.0 is not part of EE6/7/8, so we need to do a manual replacement
    // EE10 requires Java 11.
    // EE11 requires Java 17
    // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    // If we are running with a Java version less than 11, have EE9 be the lite mode test to run.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .removeFeature("websocket-1.0")
                                    .addFeature("websocket-2.0")
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                    .removeFeature("websocket-1.0")
                                    .addFeature("websocket-2.1")
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES()
                                    .removeFeature("websocket-1.0")
                                    .addFeature("websocket-2.2"));

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
