/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat;

import java.io.File;
import java.io.FileOutputStream;

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

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.faces.fat.selenium.util.internal.CustomDriver;
import io.openliberty.faces.fat.selenium.util.internal.ExtendedWebDriver;
import io.openliberty.org.apache.myfaces40.fat.tests.AcceptInputFileTest;
import io.openliberty.org.apache.myfaces40.fat.tests.AjaxRenderExecuteThisTest;
import io.openliberty.org.apache.myfaces40.fat.tests.AnnotationLiteralsTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ClientWindowScopedTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ExtensionlessMappingTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ExternalContextAddResponseCookieTest;
import io.openliberty.org.apache.myfaces40.fat.tests.Faces40ThirdPartyApiTests;
import io.openliberty.org.apache.myfaces40.fat.tests.Faces40URNTest;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesConfigTest;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesContextGetLifecycleTest;
import io.openliberty.org.apache.myfaces40.fat.tests.Html5Tests;
import io.openliberty.org.apache.myfaces40.fat.tests.InputTextTypeTest;
import io.openliberty.org.apache.myfaces40.fat.tests.LayoutAttributeTests;
import io.openliberty.org.apache.myfaces40.fat.tests.MultipleInputFileTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ProgrammaticFaceletTests;
import io.openliberty.org.apache.myfaces40.fat.tests.SelectItemTests;
import io.openliberty.org.apache.myfaces40.fat.tests.SubscribeToEventTest;
import io.openliberty.org.apache.myfaces40.fat.tests.UIViewRootGetDoctypeTest;
import io.openliberty.org.apache.myfaces40.fat.tests.WebSocketTests;
import io.openliberty.org.apache.myfaces40.fat.tests.bugfixes.MyFaces4628Test;
import io.openliberty.org.apache.myfaces40.fat.tests.bugfixes.MyFaces4658Test;

@RunWith(Suite.class)
@SuiteClasses({
                AcceptInputFileTest.class,
                AnnotationLiteralsTest.class,
                ClientWindowScopedTest.class,
                ExtensionlessMappingTest.class,
                ExternalContextAddResponseCookieTest.class,
                Faces40ThirdPartyApiTests.class,
                Faces40URNTest.class,
                FacesConfigTest.class,
                FacesContextGetLifecycleTest.class,
                InputTextTypeTest.class,
                LayoutAttributeTests.class,
                ProgrammaticFaceletTests.class,
                SelectItemTests.class,
                SubscribeToEventTest.class,
                UIViewRootGetDoctypeTest.class,
                Faces40URNTest.class,
                Html5Tests.class,
                MyFaces4628Test.class,
                // Selenium Tests
                AjaxRenderExecuteThisTest.class,
                MultipleInputFileTest.class,
                MyFaces4658Test.class,
                WebSocketTests.class,

})

public class FATSuite extends TestContainerSuite {

    private static final Class<?> c = FATSuite.class;

    private static BrowserWebDriverContainer<?> CHROME_CONTAINER = null;

    private static ExtendedWebDriver DRIVER;

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES().setSkipTransformation(true));

    /**
     * Utility method that will write xmlContent to output.txt and
     * when running locally will also write to a file under output/servers/[yourServer]/logs/output/
     *
     * @param xmlContent - Content from an XmlPage
     * @param fileName   - Name of the file, typically in the form [testname].[subtest].html
     */
    public static final void logOutputForDebugging(LibertyServer server, String xmlContent, String fileName) {
        //always output to log
        Log.info(FATSuite.class, "writeOutputToFile", xmlContent);

        if (!FATRunner.FAT_TEST_LOCALRUN) {
            return;
        }

        //log to separate file locally
        File outputDir = new File(server.getLogsRoot(), "output");
        File outputFile = new File(outputDir, fileName);
        outputDir.mkdirs();

        try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
            fos.write(xmlContent.getBytes());
        } catch (Exception e) {
            //ignore only using for debugging
        }
    }

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
