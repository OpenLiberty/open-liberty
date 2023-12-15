/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.ui.internal.fat.tests;

import static componenttest.selenium.SeleniumWaits.waitForElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.Color;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode;
import org.testcontainers.containers.VncRecordingContainer.VncRecordingFormat;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.fat.util.Props;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi.ui.internal.fat.app.TestResource;

/**
 * A Test of UI Path rewriting
 */
@RunWith(FATRunner.class)
public class UICustomPathTest {

    public static final String APP_NAME = "app";
    public static final String UI_PROPERTY_NAME = "customUIPath";
    public static final String DOC_PROPERTY_NAME = "customDocPath";
    public static final String UI_PATH_VALUE = "/foo/bar";
    public static final String DOC_PATH_VALUE = "/get/docs/here";
    /**
     * Wait for "long" tasks like initial page load or making a test request to the server
     */
    private static final Duration LONG_WAIT = Duration.ofSeconds(30);

    @Server("openapi-ui-custom-test")
    public static LibertyServer server;

    @Rule
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>().withCapabilities(new ChromeOptions()).withAccessToHost(true)
                                                                                  .withRecordingMode(VncRecordingMode.RECORD_FAILING,
                                                                                                     Props.getInstance().getFileProperty(Props.DIR_LOG),
                                                                                                     VncRecordingFormat.MP4)
                                                                                  .withLogConsumer(new SimpleLogConsumer(UICustomPathTest.class, "selenium-driver"));

    private RemoteWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war").addClass(TestResource.class);

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        //Set the new path value
        server.addEnvVar(UI_PROPERTY_NAME, UI_PATH_VALUE);
        server.addEnvVar(DOC_PROPERTY_NAME, DOC_PATH_VALUE);

        server.startServer();

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    @After
    public void teardownTest() throws Exception {
        // Close the browser after tests to ensure selenium is clean between tests
        driver.quit();
    }

    @Before
    public void setupTest() {
        driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        driver.get("http://host.testcontainers.internal:" + server.getHttpDefaultPort() + UI_PATH_VALUE);
    }

    @Test
    public void testCustomUIPath() {
        // Check the title loads
        WebElement title = waitForElement(driver, By.cssSelector("h2.title"), LONG_WAIT);
        assertThat("Page title", title.getText(), Matchers.containsString("Generated API"));

        // Check the headerbar colour
        WebElement headerbar = waitForElement(driver, By.cssSelector("div.headerbar"));
        assertEquals("Headerbar colour", Color.fromString("#191c2c"), Color.fromString(headerbar.getCssValue("background-color")));

        // Check the headerbar has a background image. It's a data URL, so it's hard to assert it's actually correct, we just assert that there is one
        WebElement headerbarWrapper = waitForElement(headerbar, By.cssSelector("div.headerbar-wrapper"));
        assertThat("Headerbar image", headerbarWrapper.getCssValue("background-image"), startsWith("url(\"data:image/png"));

        // Check we can see and open the operation
        WebElement testGetOpBlock = waitForElement(driver, By.id("operations-default-get_test__id_"));
        WebElement testGetButton = testGetOpBlock.findElement(By.tagName("button"));
        testGetButton.click();
        WebElement testGet200Response = waitForElement(testGetOpBlock, By.cssSelector("tr.response[data-code=\"200\"]"));
        assertNotNull("200 response line", testGet200Response);
    }
}
