/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsf22.fat.tests.CDIConfigByACPTests;
import com.ibm.ws.jsf22.fat.tests.CDIFacesInMetaInfTests;
import com.ibm.ws.jsf22.fat.tests.CDIFacesInWebXMLTests;
import com.ibm.ws.jsf22.fat.tests.CDIFlowsTests;
import com.ibm.ws.jsf22.fat.tests.CDITests;
import com.ibm.ws.jsf22.fat.tests.JSF22APARSeleniumTests;
import com.ibm.ws.jsf22.fat.tests.JSF22AparTests;
import com.ibm.ws.jsf22.fat.tests.JSF22BeanValidationTests;
import com.ibm.ws.jsf22.fat.tests.JSF22FlowsTests;
import com.ibm.ws.jsf22.fat.tests.JSF22MiscellaneousTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ResetValuesAndAjaxDelayTests;
import com.ibm.ws.jsf22.fat.tests.JSF22StatelessViewTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ThirdPartyApiTests;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.faces.fat.selenium.util.internal.CustomDriver;
import io.openliberty.faces.fat.selenium.util.internal.ExtendedWebDriver;

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
                JSF22StatelessViewTests.class,
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                CDIConfigByACPTests.class,
                CDIFacesInMetaInfTests.class,
                CDIFacesInWebXMLTests.class,
                CDITests.class,
                JSF22BeanValidationTests.class,
                JSF22AparTests.class,
                JSF22ThirdPartyApiTests.class,
                // Selenium tests
                JSF22MiscellaneousTests.class,
                JSF22ResetValuesAndAjaxDelayTests.class,
                JSF22APARSeleniumTests.class
})
public class FATSuite extends TestContainerSuite {

    private static final Class<?> c = FATSuite.class;

    private static BrowserWebDriverContainer<?> CHROME_CONTAINER = null;

    private static ExtendedWebDriver DRIVER;

    // EE10 requires Java 11.
    // EE11 requires Java 17
    // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());

    private static DockerImageName getChromeImage() {
        if (FATRunner.ARM_ARCHITECTURE) {
            return DockerImageName.parse("seleniarm/standalone-chromium:4.8.3").asCompatibleSubstituteFor("selenium/standalone-chrome");
        } else {
            return DockerImageName.parse("selenium/standalone-chrome:4.8.3");
        }
    }

    public static ExtendedWebDriver getWebDriver() throws Exception {
        int retryCount = 3;
        while(DRIVER == null && retryCount > 0) {
            Log.info(c, "getWebDriver", "Attempting to initialize WebDriver, attempts remaining: " + retryCount);
            try {
                CHROME_CONTAINER = new BrowserWebDriverContainer<>(getChromeImage()).withCapabilities(new ChromeOptions())
                            .withAccessToHost(true)
                            .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"
                CHROME_CONTAINER.start();
                DRIVER = new CustomDriver(new RemoteWebDriver(CHROME_CONTAINER.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));
            } catch (Exception ex1) {
                Log.info(c, "getWebDriver", "Failed to initialize WebDriver on attempt. Exception: " + ex1.getMessage(), ex1);
                retryCount--;
                try {
                    Thread.sleep(10000); // wait for 10 seconds before retrying
                } catch (InterruptedException ie) {}
            }
        }
        if(DRIVER == null) {
            throw new Exception("Failed to initialize WebDriver after multiple attempts! See log for details.");
        }
        return DRIVER;
    }


    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

        /*
     * Tear down the WebDriver and Chrome container after all tests have run.
     */
    @AfterClass
    public static void tearDownWebDriver() {
        Log.info(c, "tearDownWebDriver", "Tearing down WebDriver and Chrome container.");
        if (DRIVER != null) {
            try {
                DRIVER.quit(); // closes all sessions and terminutes the webdriver
            } catch (Exception e) {
                Log.info(c, "tearDownWebDriver", "Exception occurred while quitting WebDriver: " + e.getMessage(), e);
            }
            DRIVER = null;
        }
        if (CHROME_CONTAINER != null) {
            try {
                CHROME_CONTAINER.stop();
            } catch (Exception e) {
                Log.info(c, "tearDownWebDriver", "Exception occurred while stopping Chrome container: " + e.getMessage(), e);
            }
            CHROME_CONTAINER = null;
        }
    }

}
