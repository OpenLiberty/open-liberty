/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi.ui.internal.fat.tests;

import static componenttest.selenium.SeleniumWaits.waitForElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
 * A basic test that we can open the UI and that it appears to have loaded correctly
 */
@RunWith(FATRunner.class)
public class UIBasicTest {

    /** Wait for "long" tasks like initial page load or making a test request to the server */
    private static final Duration LONG_WAIT = Duration.ofSeconds(30);

    public static final String APP_NAME = "app";

    @Server("openapi-ui-test")
    public static LibertyServer server;

    @Rule
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>().withCapabilities(new ChromeOptions())
                                                                                  .withAccessToHost(true)
                                                                                  .withRecordingMode(VncRecordingMode.RECORD_FAILING,
                                                                                                     Props.getInstance().getFileProperty(Props.DIR_LOG),
                                                                                                     VncRecordingFormat.MP4)
                                                                                  .withLogConsumer(new SimpleLogConsumer(UIBasicTest.class, "selenium-driver"));

    private RemoteWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addClass(TestResource.class);

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    @Before
    public void setupTest() {
        driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
    }

    @Test
    public void testUI() {
        driver.get("http://host.testcontainers.internal:" + server.getHttpDefaultPort() + "/openapi/ui");

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
        WebElement testGetOpBlock = waitForElement(driver, By.id("operations-default-get_test"));
        WebElement testGetButton = testGetOpBlock.findElement(By.tagName("button"));
        testGetButton.click();
        WebElement testGet200Response = waitForElement(testGetOpBlock, By.cssSelector("tr.response[data-code=\"200\"]"));
        assertNotNull("200 response line", testGet200Response);
    }

}
