/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.utility.DockerImageName;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsf23.fat.tests.JSF23BugFixes;
import com.ibm.ws.jsf23.fat.tests.JSF23CDIConfigByACPTests;
import com.ibm.ws.jsf23.fat.tests.JSF23CDIFacesInMetaInfTests;
import com.ibm.ws.jsf23.fat.tests.JSF23CDIFacesInWebXMLTests;
import com.ibm.ws.jsf23.fat.tests.JSF23CDIGeneralTests;
import com.ibm.ws.jsf23.fat.tests.JSF23CDIInjectionTests;
import com.ibm.ws.jsf23.fat.tests.JSF23SpecIssueTests;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * JSF 2.3 Tests
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

                JSF23CDIGeneralTests.class,
                JSF23CDIInjectionTests.class,
                JSF23CDIFacesInMetaInfTests.class,
                JSF23CDIFacesInWebXMLTests.class,
                JSF23CDIConfigByACPTests.class,
                JSF23SpecIssueTests.class,
                JSF23BugFixes.class
})

public class FATSuite extends TestContainerSuite {

    // EE10 requires Java 11.
    // EE11 requires Java 17
    // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    // If we are running with a Java version less than 11, have EE9 be the lite mode test to run.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

    public static DockerImageName getChromeImage() {
        if (FATRunner.ARM_ARCHITECTURE) {
            return DockerImageName.parse("seleniarm/standalone-chromium:4.8.3").asCompatibleSubstituteFor("selenium/standalone-chrome");
        } else {
            return DockerImageName.parse("selenium/standalone-chrome:4.8.3");
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
