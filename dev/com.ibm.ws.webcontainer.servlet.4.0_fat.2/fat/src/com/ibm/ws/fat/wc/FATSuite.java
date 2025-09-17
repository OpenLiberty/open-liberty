/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.fat.wc.tests.WCPartitionedAttributeTests;
import com.ibm.ws.fat.wc.tests.WCPartitionedCookieAttributeSecurityTest;
import com.ibm.ws.fat.wc.tests.WCResponseHeadersTest;
import com.ibm.ws.fat.wc.tests.WCSameSiteCookieAttributeSecurityTest;
import com.ibm.ws.fat.wc.tests.WCSameSiteCookieAttributeTests;
import com.ibm.ws.fat.wc.tests.WCSameSiteIncompatibleClientsTests;
import com.ibm.ws.fat.wc.tests.WebSphereServletEventListenerTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * Servlet 4.0 Tests
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * Make sure to distinguish FULL mode tests using
 * <code>@Mode(TestMode.FULL)</code>. Tests default to
 * use LITE mode (<code>@Mode(TestMode.LITE)</code>).
 *
 * By default only LITE mode tests are run. To also run
 * full mode tests a property must be specified:
 *
 * -Dfat.test.mode=FULL.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                WCResponseHeadersTest.class,
                WCSameSiteCookieAttributeTests.class,
                WCSameSiteCookieAttributeSecurityTest.class,
                WCPartitionedAttributeTests.class,
                WCPartitionedCookieAttributeSecurityTest.class,
                WCSameSiteIncompatibleClientsTests.class,
                WebSphereServletEventListenerTest.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests repeat;

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    static {
        // EE10 requires Java 11.
        // EE11 requires Java 17
        // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
        if (isWindows && !FATRunner.FAT_TEST_LOCALRUN) {
            // Repeating the full fat for all features may exceed the 3 hour limit on Fyre Windows and causes random build breaks.
            // Skip EE9 on the windows platform when not running locally.
            // If we are running with a Java version less than 11, have EE8 (EmptyAction) be the lite mode test to run.
            repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES());
        } else {
            // If we are running with a Java version less than 11, have EE9 be the lite mode test to run.
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES());
        }
    }

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
