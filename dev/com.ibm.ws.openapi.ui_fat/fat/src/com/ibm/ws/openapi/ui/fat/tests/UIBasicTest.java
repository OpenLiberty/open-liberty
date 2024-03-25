/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.openapi.ui.fat.tests;

import static componenttest.selenium.SeleniumWaits.waitForElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;
import java.util.List;

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
import com.ibm.ws.openapi.ui.fat.app.TestResource;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

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
        driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true));
    }

    @After
    public void teardownTest() throws Exception {
        // Ensure clean closure of selenium components at end of test
        // As no server changes required, no need to stop the server
        driver.quit();
    }

    @Test
    public void testPublicUI() {
        driver.get("http://host.testcontainers.internal:" + server.getHttpDefaultPort() + "/api/explorer");
        testUI();
    }

    @Test
    public void testPrivateUI() throws Exception {
        //Reduce possibility that Server is not listening on its HTTPS Port
        //Especially for Windows if certificates are slow to create
        server.waitForSSLStart();

        driver.get("https://admin:test@host.testcontainers.internal:" + server.getHttpDefaultSecurePort() + "/ibm/api/explorer");
        testUI();
    }

    /**
     * Tests common to both the public and private UI
     *
     * @throws InterruptedException
     */
    private void testUI() {
        // Check the title loads
        WebElement title = waitForElement(driver, By.cssSelector("h2.title"), LONG_WAIT);
        assertThat("Page title", title.getText(), Matchers.containsString("Liberty REST APIs"));

        // Check the headerbar colour
        WebElement headerbar = waitForElement(driver, By.cssSelector("div.headerbar"));
        assertEquals("Headerbar colour", Color.fromString("#191c2c"), Color.fromString(headerbar.getCssValue("background-color")));

        // Check the headerbar has a background image. It's a data URL, so it's hard to assert it's actually correct, we just assert that there is one
        WebElement headerbarWrapper = waitForElement(headerbar, By.cssSelector("div.headerbar-wrapper"));
        assertThat("Headerbar image", headerbarWrapper.getCssValue("background-image"), startsWith("url(\"data:image/png"));

        // Check we can see and open the operation
        WebElement testGetOpBlock = waitForElement(driver, By.id("operations-default-testGet"));
        WebElement testGetButton = testGetOpBlock.findElement(By.tagName("button"));
        testGetButton.click();
        WebElement testGetDefaultResponse = waitForElement(testGetOpBlock, By.cssSelector("tr.response[data-code=\"default\"]"));
        assertNotNull("response line", testGetDefaultResponse);

        // Check the Required field renders correctly so we know the JS is being interpreted as UTF-8
        WebElement requiredField = waitForElement(testGetOpBlock, By.cssSelector("div.parameter__name.required"));
        assertThat("Code page UTF-8 not being used", requiredField.getText(), is("id *"));

        // test that all APIs are displayed correctly with correct colors
        assertThat(testGetOpBlock.getCssValue("background"), startsWith("rgba(31, 111, 240, 0.1)"));
        assertThat(testGetOpBlock.getCssValue("border-color"), is("rgb(31, 111, 240)"));
        WebElement testGetIcon = testGetOpBlock.findElement(By.tagName("span"));
        assertThat(testGetIcon.getCssValue("background"), startsWith("rgb(31, 111, 240)"));

        WebElement testDeleteOpBlock = waitForElement(driver, By.id("operations-default-testDelete"));
        assertThat(testDeleteOpBlock.getCssValue("background"), startsWith("rgba(224, 7, 7, 0.1)"));
        assertThat(testDeleteOpBlock.getCssValue("border-color"), is("rgb(224, 7, 7)"));
        WebElement testDeleteIcon = testDeleteOpBlock.findElement(By.tagName("span"));
        assertThat(testDeleteIcon.getCssValue("background"), startsWith("rgb(224, 7, 7)"));

        WebElement testPutOpBlock = waitForElement(driver, By.id("operations-default-testPut"));
        assertThat(testPutOpBlock.getCssValue("background"), startsWith("rgba(177, 99, 3, 0.1)"));
        assertThat(testPutOpBlock.getCssValue("border-color"), is("rgb(177, 99, 3)"));
        WebElement testPutIcon = testPutOpBlock.findElement(By.tagName("span"));
        assertThat(testPutIcon.getCssValue("background"), startsWith("rgb(177, 99, 3)"));

        WebElement testPostOpBlock = waitForElement(driver, By.id("operations-default-testPost"));
        assertThat(testPostOpBlock.getCssValue("background"), startsWith("rgba(32, 128, 80, 0.1)"));
        assertThat(testPostOpBlock.getCssValue("border-color"), is("rgb(32, 128, 80)"));
        WebElement testPostIcon = testPostOpBlock.findElement(By.tagName("span"));
        assertThat(testPostIcon.getCssValue("background"), startsWith("rgb(32, 128, 80)"));

        // Check that version stamp is present and color is correct
        WebElement versionStamp = waitForElement(driver, By.cssSelector("small.version-stamp"));
        assertThat(versionStamp.getCssValue("background"), startsWith("rgb(93, 130, 3)"));

        // Test filter box works as expected
        WebElement filterBox = waitForElement(driver, By.cssSelector("form.filter-wrapper"));
        WebElement filterTextBox = waitForElement(filterBox, By.cssSelector("input.filter-input"));
        WebElement filterButton = waitForElement(filterBox, By.cssSelector("button.filter-button"));
        filterTextBox.sendKeys("app");
        filterButton.click();
        // Check that the results load with 4 matches
        WebElement filterResults = waitForElement(driver, By.cssSelector("div.operation-tag-content"));
        List<WebElement> results = filterResults.findElements(By.cssSelector("span.opblock-summary-method"));
        assertThat(results.size(), is(4));

        filterTextBox.sendKeys("incorrectFilter");
        filterButton.click();
        // Check that the results load with zero matches
        waitForElement(driver, By.xpath("//*[text()=' No operations defined in spec!']"));
    }

}
