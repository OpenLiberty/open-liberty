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
import com.ibm.ws.jsf22.fat.tests.JSF22AppConfigPopTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ClientWindowTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentRendererTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSF22FlashEventsTests;
import com.ibm.ws.jsf22.fat.tests.JSF22IncludeTest;
import com.ibm.ws.jsf22.fat.tests.JSF22InputFileTests;
import com.ibm.ws.jsf22.fat.tests.JSF22LocalizationTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSF22MiscLifecycleTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ResourceLibraryContractHtmlUnit;
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
