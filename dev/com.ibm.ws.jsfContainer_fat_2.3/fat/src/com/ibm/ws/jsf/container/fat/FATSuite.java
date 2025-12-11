/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat;

import java.io.File;
import java.util.Locale;

import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import com.ibm.ws.jsf.container.fat.tests.CDIFlowsTests;
import com.ibm.ws.jsf.container.fat.tests.ClassloadingTest;
import com.ibm.ws.jsf.container.fat.tests.ErrorPathsTest;
import com.ibm.ws.jsf.container.fat.tests.JSF22BeanValidationTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22FlowsTests;
import com.ibm.ws.jsf.container.fat.tests.JSF22StatelessViewTests;
import com.ibm.ws.jsf.container.fat.tests.JSF23CDIGeneralTests;
import com.ibm.ws.jsf.container.fat.tests.JSF23WebSocketTests;
import com.ibm.ws.jsf.container.fat.tests.JSFContainerTest;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import io.openliberty.faces.fat.selenium.util.internal.CustomDriver;
import io.openliberty.faces.fat.selenium.util.internal.ExtendedWebDriver;

@RunWith(Suite.class)
@SuiteClasses({
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                JSFContainerTest.class,
                JSF22StatelessViewTests.class,
                JSF22BeanValidationTests.class,
                ErrorPathsTest.class,
                ClassloadingTest.class,
                JSF23CDIGeneralTests.class,
                JSF23WebSocketTests.class
})

public class FATSuite extends TestContainerSuite {

    private static final Class<?> c = FATSuite.class;

    private static BrowserWebDriverContainer<?> CHROME_CONTAINER = null;

    private static ExtendedWebDriver DRIVER;

    @ClassRule
    public static RepeatTests repeat;

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    static {
        if (JavaInfo.JAVA_VERSION >= 11) {
            // Repeating the full FAT for multiple features may exceed the 3 hour limit on Fyre Windows.
            // Skip the EE9 repeat on the windows platform when not running locally.
            if (isWindows && !FATRunner.FAT_TEST_LOCALRUN) {
                repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                                .andWith(FeatureReplacementAction.EE10_FEATURES());
            } else {
                // EE10 requires Java 11.
                // EE11 requires Java 17
                // If we only specify EE10/EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
                // If we are running with a Java version less than 11, have EE9 be the lite mode test to run.
                repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                                .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                                .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                                .andWith(FeatureReplacementAction.EE11_FEATURES());
            }

        } else {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(FeatureReplacementAction.EE9_FEATURES());
        }
    }

    public static final String MOJARRA_API_IMP = "publish/files/mojarra/javax.faces-2.3.9.jar";
    public static final String MOJARRA_API_IMP_40 = "publish/files/mojarra40/jakarta.faces-4.0.0.jar";
    public static final String MOJARRA_API_IMP_41 = "publish/files/mojarra41/jakarta.faces-4.1.0.jar";
    public static final String MYFACES_IMP = "publish/files/myfaces/myfaces-impl-2.3.11.jar";
    public static final String MYFACES_IMP_30 = "publish/files/myfaces30/myfaces-impl-3.0.3.jar";
    public static final String MYFACES_IMP_40 = "publish/files/myfaces40/myfaces-impl-4.0.3.jar";
    public static final String MYFACES_IMP_41 = "publish/files/myfaces41/myfaces-impl-4.1.1.jar";
    // For ErrorPathsTest#testBadImplVersion_MyFaces Test (apps need the correct api since the tests checks for a bad implementation)
    public static final String MYFACES_API = "publish/files/myfaces/myfaces-api-2.3.11.jar";
    public static final String MYFACES_API_30 = "publish/files/myfaces30/myfaces-api-3.0.3.jar";
    public static final String MYFACES_API_40 = "publish/files/myfaces40/myfaces-api-4.0.3.jar";
    public static final String MYFACES_API_41 = "publish/files/myfaces41/myfaces-api-4.1.1.jar";

    private static DockerImageName getChromeImage() {
        if (FATRunner.ARM_ARCHITECTURE) {
            return DockerImageName.parse("seleniarm/standalone-chromium:4.8.3").asCompatibleSubstituteFor("selenium/standalone-chrome");
        } else {
            return DockerImageName.parse("selenium/standalone-chrome:4.8.3");
        }
    }

    public static WebArchive addMojarra(WebArchive app) throws Exception {
        if (JakartaEEAction.isEE11Active()) {
            return app.addAsLibraries(new File("publish/files/mojarra41/").listFiles());
        } else if (JakartaEEAction.isEE10Active()) {
            return app.addAsLibraries(new File("publish/files/mojarra40/").listFiles());
        } else if (JakartaEEAction.isEE9Active()) {
            return app.addAsLibraries(new File("publish/files/mojarra30/").listFiles());
        }
        return app.addAsLibraries(new File("publish/files/mojarra/").listFiles());
    }

    public static WebArchive addMyFaces(WebArchive app) throws Exception {
        if (JakartaEEAction.isEE11Active()) {
            return app.addAsLibraries(new File("publish/files/myfaces41/").listFiles());
        } else if (JakartaEEAction.isEE10Active()) {
            return app.addAsLibraries(new File("publish/files/myfaces40/").listFiles());
        } else if (JakartaEEAction.isEE9Active()) {
            return app.addAsLibraries(new File("publish/files/myfaces30/").listFiles()).addAsLibraries(new File("publish/files/myfaces-libs/").listFiles());
        }
        return app.addAsLibraries(new File("publish/files/myfaces/").listFiles()).addAsLibraries(new File("publish/files/myfaces-libs/").listFiles());
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
