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
import com.ibm.ws.jsf22.fat.tests.CDIConfigByACPTests;
import com.ibm.ws.jsf22.fat.tests.CDIFacesInMetaInfTests;
import com.ibm.ws.jsf22.fat.tests.CDIFacesInWebXMLTests;
import com.ibm.ws.jsf22.fat.tests.CDIFlowsTests;
import com.ibm.ws.jsf22.fat.tests.CDITests;
import com.ibm.ws.jsf22.fat.tests.JSF22APARSeleniumTests;
import com.ibm.ws.jsf22.fat.tests.JSF22AparTests;
import com.ibm.ws.jsf22.fat.tests.JSF22AppConfigPopTests;
import com.ibm.ws.jsf22.fat.tests.JSF22BeanValidationTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ClientWindowTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentRendererTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSF22FlashEventsTests;
import com.ibm.ws.jsf22.fat.tests.JSF22FlowsTests;
import com.ibm.ws.jsf22.fat.tests.JSF22IncludeTest;
import com.ibm.ws.jsf22.fat.tests.JSF22InputFileTests;
import com.ibm.ws.jsf22.fat.tests.JSF22LocalizationTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSF22MiscLifecycleTests;
import com.ibm.ws.jsf22.fat.tests.JSF22MiscellaneousTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ResetValuesAndAjaxDelayTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ResourceLibraryContractHtmlUnit;
import com.ibm.ws.jsf22.fat.tests.JSF22StatelessViewTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ThirdPartyApiTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ViewActionAndPhaseIdTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ViewPoolingTests;
import com.ibm.ws.jsf22.fat.tests.JSFCompELTests;
import com.ibm.ws.jsf22.fat.tests.JSFHtml5Tests;
import com.ibm.ws.jsf22.fat.tests.JSFHtmlUnit;
import com.ibm.ws.jsf22.fat.tests.JSFServerTest;
import com.ibm.ws.jsf22.fat.tests.JSFSimpleHtmlUnit;

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
                JSFServerTest.class,
                JSFHtmlUnit.class,
                JSFSimpleHtmlUnit.class,
                JSF22StatelessViewTests.class,
                JSFHtml5Tests.class,
                JSF22ResourceLibraryContractHtmlUnit.class,
                JSF22ComponentTesterTests.class,
                JSF22ClientWindowTests.class,
                JSF22ComponentRendererTests.class,
                JSFCompELTests.class,
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                JSF22MiscellaneousTests.class,
                JSF22ViewActionAndPhaseIdTests.class,
                JSF22ResetValuesAndAjaxDelayTests.class,
                JSF22MiscLifecycleTests.class,
                JSF22AppConfigPopTests.class,
                JSF22FlashEventsTests.class,
                CDIConfigByACPTests.class,
                CDIFacesInMetaInfTests.class,
                CDIFacesInWebXMLTests.class,
                CDITests.class,
                JSF22BeanValidationTests.class,
                JSF22ViewPoolingTests.class,
                JSF22IncludeTest.class,
                JSF22InputFileTests.class,
                JSF22LocalizationTesterTests.class,
                JSF22AparTests.class,
                JSF22ThirdPartyApiTests.class,
                JSF22APARSeleniumTests.class
})
public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static RepeatTests repeat;

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    static {
        // EE10 requires Java 11.
        // EE11 requires Java 17
        // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
        if (isWindows && !FATRunner.FAT_TEST_LOCALRUN) {
            // Repeating the full fat for all features may exceed the 3 hour limit on Fyre Windows and causes random build breaks.
            // Skip EE9 on the windows platform when not running locally.
            // If we are running with a Java version less than 11, have EE8 be the lite mode test to run.
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE8_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES());
        } else {
            // If we are running with a Java version less than 11, have EE9 be the lite mode test to run.
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES());
        }
    }

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
