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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNot.not;
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

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi.ui.internal.fat.app.TestApplication;
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
                                   .addClasses(TestResource.class, TestApplication.class);

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
        //close the browser at end of test to ensure it no connections are kept open by client
        driver.quit();
    }

    @Test
    public void testHttpUI() {
        driver.get("http://host.testcontainers.internal:" + server.getHttpDefaultPort() + "/openapi/ui");
        testUI();
    }

    @Test
    public void testHttpsUI() throws Exception {
        //Reduce possibility that Server is not listening on its HTTPS Port
        //Especially for Windows if certificates are slow to create
        server.waitForSSLStart();

        driver.get("https://host.testcontainers.internal:" + server.getHttpDefaultSecurePort() + "/openapi/ui");
        testUI();
    }

    public void testUI() {
        // Check the title loads
        WebElement title = waitForElement(driver, By.cssSelector("h2.title"), LONG_WAIT);
        assertThat("Page title", title.getText(), Matchers.containsString("Generated API"));

        // Check the title and description loads with no links
        WebElement info = waitForElement(driver, By.cssSelector("div.info"));
        assertThat("Page info", info.getAttribute("innerHTML"), not(Matchers.containsString("href")));

        // Check the headerbar colour
        WebElement headerbar = waitForElement(driver, By.cssSelector("div.headerbar"));
        assertEquals("Headerbar colour", Color.fromString("#191c2c"), Color.fromString(headerbar.getCssValue("background-color")));

        // Check the headerbar has a background image. It's a data URL, so it's hard to assert it's actually correct, we just assert that there is one
        WebElement headerbarWrapper = waitForElement(headerbar, By.cssSelector("div.headerbar-wrapper"));
        assertThat("Headerbar image", headerbarWrapper.getCssValue("background-image"), startsWith("url(\"data:image/png"));

        // Check that the footer loads with 4 links
        WebElement footer = waitForElement(driver, By.cssSelector("div.footer"));
        List<WebElement> links = footer.findElement(By.tagName("ul")).findElements(By.tagName("li"));
        assertThat(links.size(), is(4));
        for (WebElement link : links) {
            assertThat("Footer links", link.getAttribute("innerHTML"), Matchers.containsString("href"));
        }

        // Check we can see and open the operation
        WebElement testGetOpBlock = waitForElement(driver, By.id("operations-default-get_test__id_"));
        WebElement testGetButton = testGetOpBlock.findElement(By.tagName("button"));
        testGetButton.click();
        WebElement testGet200Response = waitForElement(testGetOpBlock, By.cssSelector("tr.response[data-code=\"200\"]"));
        assertNotNull("200 response line", testGet200Response);

        // Test that all APIs are displayed with the correct colours
        assertThat(testGetOpBlock.getCssValue("background"), startsWith("rgba(31, 111, 240, 0.1)"));
        assertThat(testGetOpBlock.getCssValue("border-color"), is("rgb(31, 111, 240)"));
        WebElement testGetIcon = testGetOpBlock.findElement(By.tagName("span"));
        assertThat(testGetIcon.getCssValue("background"), startsWith("rgb(31, 111, 240)"));

        WebElement testDeleteOpBlock = waitForElement(driver, By.id("operations-default-delete_test__id_"));
        assertThat(testDeleteOpBlock.getCssValue("background"), startsWith("rgba(224, 7, 7, 0.1)"));
        assertThat(testDeleteOpBlock.getCssValue("border-color"), is("rgb(224, 7, 7)"));
        WebElement testDeleteIcon = testDeleteOpBlock.findElement(By.tagName("span"));
        assertThat(testDeleteIcon.getCssValue("background"), startsWith("rgb(224, 7, 7)"));

        WebElement testPutOpBlock = waitForElement(driver, By.id("operations-default-put_test__id_"));
        assertThat(testPutOpBlock.getCssValue("background"), startsWith("rgba(177, 99, 3, 0.1)"));
        assertThat(testPutOpBlock.getCssValue("border-color"), is("rgb(177, 99, 3)"));
        WebElement testPutIcon = testPutOpBlock.findElement(By.tagName("span"));
        assertThat(testPutIcon.getCssValue("background"), startsWith("rgb(177, 99, 3)"));

        WebElement testPostOpBlock = waitForElement(driver, By.id("operations-default-post_test__id_"));
        assertThat(testPostOpBlock.getCssValue("background"), startsWith("rgba(32, 128, 80, 0.1)"));
        assertThat(testPostOpBlock.getCssValue("border-color"), is("rgb(32, 128, 80)"));
        WebElement testPostIcon = testPostOpBlock.findElement(By.tagName("span"));
        assertThat(testPostIcon.getCssValue("background"), startsWith("rgb(32, 128, 80)"));

        // Check that version stamp is present with correct colors
        WebElement versionStamp = waitForElement(driver, By.cssSelector("small.version-stamp"));
        assertThat(versionStamp.getCssValue("background"), startsWith("rgb(93, 130, 3)"));

        // Check the Required field renders correctly so we know the JS is being interpreted as UTF-8
        WebElement requiredField = waitForElement(testGetOpBlock, By.cssSelector("div.parameter__name.required"));
        assertThat("Code page UTF-8 not being used", requiredField.getText(), is("id *"));
    }

}
