/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.utility.DockerImageName;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsf22.fat.tests.JSFServerTest;
import com.ibm.ws.jsf22.fat.tests.JSF22ResourceLibraryContractHtmlUnit;
import com.ibm.ws.jsf22.fat.tests.JSF22MiscLifecycleTests;
import com.ibm.ws.jsf22.fat.tests.JSFHtml5Tests;
import com.ibm.ws.jsf22.fat.tests.JSF22FlashEventsTests;
import com.ibm.ws.jsf22.fat.tests.JSFSimpleHtmlUnit;
import com.ibm.ws.jsf22.fat.tests.JSF22ViewActionAndPhaseIdTests;

import com.ibm.ws.jsf22.fat.tests.JSF22ViewPoolingTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSFHtmlUnit;
import com.ibm.ws.jsf22.fat.tests.JSFCompELTests;
import com.ibm.ws.jsf22.fat.tests.JSF22AppConfigPopTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentRendererTests;
import com.ibm.ws.jsf22.fat.tests.JSF22IncludeTest;
import com.ibm.ws.jsf22.fat.tests.JSF22InputFileTests;
import com.ibm.ws.jsf22.fat.tests.JSF22LocalizationTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ClientWindowTests;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * JSF 2.2 Tests
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * By default only lite mode tests are run.
 *
 * Add "-Dfat.test.mode=full" to the end of your command, to run
 * the bucket in full mode.
 *
 * Tests will also run with JSF 2.3 feature due to @ClassRule RepeatTests
 */
@RunWith(Suite.class)
@SuiteClasses({
                //jsfServer1
                JSFServerTest.class,
                JSF22ResourceLibraryContractHtmlUnit.class,
                JSF22MiscLifecycleTests.class,
                JSFHtml5Tests.class,
                JSF22FlashEventsTests.class,
                JSFSimpleHtmlUnit.class, // CheckPoint Enabled
                JSF22ViewActionAndPhaseIdTests.class,

                //jsfServer2
                JSF22ViewPoolingTests.class,
                JSF22ComponentTesterTests.class,
                JSFHtmlUnit.class,
                JSFCompELTests.class,
                JSF22AppConfigPopTests.class,
                JSF22ComponentRendererTests.class, // CheckPoint Enabled
                JSF22InputFileTests.class,
                JSF22LocalizationTesterTests.class,
                JSF22ClientWindowTests.class,

                //jsf22IncludeTestServer
                JSF22IncludeTest.class
})
public class FATSuite extends TestContainerSuite {

    // EE10 requires Java 11.
    // EE11 requires Java 17
    // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
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
