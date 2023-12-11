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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;

import java.time.Duration;

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
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.VncRecordingContainer.VncRecordingFormat;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.fat.util.Props;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi.ui.internal.fat.app.SecureTestResource;
import io.openliberty.microprofile.openapi.ui.internal.fat.app.TestApplication;

@RunWith(FATRunner.class)
public class UIOauthTest {

    public static final String APP_NAME = "app";
    public static final String UI_PATH_PROPERTY = "uiPath";
    public static final String UI_PATH_VALUE = "/openapi/ui";
    public static final String CUSTOM_UI_PATH_VALUE = "/foo/bar";
    public static final String DOC_PATH_VALUE = "/bar/foo";
    public static final String OAUTH_CLIENT_NAME_PROPERTY = "clientName";
    public static final String OAUTH_CLIENT_NAME = "mp-ui";
    public static final String OAUTH_CLIENT_SECRET_PROPERTY = "clientSecret";
    public static final String OAUTH_CLIENT_SECRET = "abc";
    public static final String OAUTH_REDIRECT_HOST_PROPERTY = "host";
    public static final String BASIC_AUTH_USERNAME_PROPERTY = "testUsername";
    public static final String BASIC_AUTH_USERNAME = "testuser";
    public static final String BASIC_AUTH_PASSWORD_PROPERTY = "testPassword";
    public static final String BASIC_AUTH_PASSWORD = "testpassword";
    public static final String HOST = "host.testcontainers.internal";
    public static final String HOST_HTTPS_PORT_PROPERTY = "httpsPort";

    /** Wait for "long" tasks like initial page load or making a test request to the server */
    private static final Duration LONG_WAIT = Duration.ofSeconds(30);

    @Server("openapi-ui-custom-oauth-test")
    public static LibertyServer server;

    private static ServerConfiguration baseConfig;

    /**
     * During updates to view recordings regardless of result replace
     * BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING
     * with
     * BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL
     */
    @Rule
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>().withCapabilities(new ChromeOptions())
                                                                                  .withAccessToHost(true)
                                                                                  .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING,
                                                                                                     Props.getInstance().getFileProperty(Props.DIR_LOG),
                                                                                                     VncRecordingFormat.MP4)
                                                                                  .withLogConsumer(new SimpleLogConsumer(UIBasicTest.class, "selenium-driver"));

    private RemoteWebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addClasses(SecureTestResource.class, TestApplication.class);

        ShrinkHelper.exportDropinAppToServer(server, war, ShrinkHelper.DeployOptions.SERVER_ONLY);

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());
    }

    @Before
    public void setupTest() throws Exception {
        //EnvVars are lost if defined in BeforeClass on the second test despite the fact they don't actually change so reset for every test

        //configure authentication
        server.addEnvVar(BASIC_AUTH_USERNAME_PROPERTY, BASIC_AUTH_USERNAME);
        server.addEnvVar(BASIC_AUTH_PASSWORD_PROPERTY, BASIC_AUTH_PASSWORD);

        //Configure Oauth
        server.addEnvVar(OAUTH_CLIENT_NAME_PROPERTY, OAUTH_CLIENT_NAME);
        server.addEnvVar(OAUTH_CLIENT_SECRET_PROPERTY, OAUTH_CLIENT_SECRET);
        server.addEnvVar(OAUTH_REDIRECT_HOST_PROPERTY, HOST);
        server.addEnvVar(HOST_HTTPS_PORT_PROPERTY, Integer.toString(server.getHttpDefaultSecurePort()));

        //store copy of base config for restoration when running individual tests to stop any potential bleeding
        baseConfig = server.getServerConfiguration();

        // make sure we have a clean driver between tests that can handle self-signed certs
        driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true));
    }

    @After
    public void teardownTest() throws Exception {
        //close the browser before stopping the server to reduce changes of open connections
        driver.quit();
        try {
            server.stopServer();
        } finally {
            // Restore Config to original after test to prevent potential config bleeding between tests
            server.updateServerConfiguration(baseConfig);
        }
    }

    @Test
    public void defaultPathOAuthTest() throws Exception {
        // Set the OAuth Endpoint configuration
        server.addEnvVar(UI_PATH_PROPERTY, UI_PATH_VALUE);

        server.startServer();
        server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl", 60000);
        OAuthTest(UI_PATH_VALUE);

    }

    @Test
    public void customPathOAuthTest() throws Exception {
        // Set the mpOpenAPI endpoint configuration
        ServerConfiguration config = server.getServerConfiguration();
        config.getMpOpenAPIElement().setUiPath(CUSTOM_UI_PATH_VALUE);
        config.getMpOpenAPIElement().setDocPath(DOC_PATH_VALUE);
        server.updateServerConfiguration(config);
        // Set the OAuth Endpoint configuration
        server.addEnvVar(UI_PATH_PROPERTY, CUSTOM_UI_PATH_VALUE);

        server.startServer();
        //Reduce possibility that Server is not listening on its HTTPS Port
        //Especially for Windows if certificates are slow to create
        server.waitForSSLStart();

        OAuthTest(CUSTOM_UI_PATH_VALUE);
    }

    public void OAuthTest(String uiPath) {
        //Need to use HTTPS path for the test for OAuth to actually work
        driver.get("https://" + HOST + ":" + server.getHttpDefaultSecurePort() + uiPath);

        //Store the id of the original window as we need to know when we go through the oauth login flow
        //We don't need to wait for the page to load to know which one we are after
        String originalWindow = driver.getWindowHandle();

        // Check the title loads as initial validation page has loaded
        WebElement title = waitForElement(driver, By.cssSelector("h2.title"), LONG_WAIT);
        assertThat("Page title", title.getText(), Matchers.containsString("Generated API"));

        //get the GET `/test` operation from `default`
        WebElement getOperation = waitForElement(driver, By.id("operations-default-get_test"));
        //check that the operation is "unlocked"
        //we will use the lock object later to check that it has been updated
        WebElement lock = getOperation.findElement(By.cssSelector("button.authorization__btn"));
        WebElement lockSvg = lock.findElement(By.tagName("svg"));
        assertThat(lockSvg.getAttribute("class"), equalTo("unlocked"));

        //Check the Authorize button is available and click it
        WebElement authorizeButton = waitForElement(driver, By.xpath("//span[contains(.,'Authorize')]"));
        authorizeButton.click();

        //Get the overall Modal
        WebElement authModal = waitForElement(driver, By.cssSelector("div.modal-ux"));

        //Check the Modal head element as we need it later
        WebElement oauthModalHead = authModal.findElement(By.cssSelector("div.modal-ux-header"));
        assertThat("Authorization header has appeared", oauthModalHead.findElement(By.cssSelector("h3")).getText(), Matchers.containsString("Available authorizations"));

        // Get the OAuth Authentication container - As we only have OAuth Enabled it is the only one we have
        WebElement authContainer = authModal.findElement(By.cssSelector("div.auth-container"));
        assertThat("Modal header containers OAuth", authContainer.findElement(By.cssSelector("h4")).getText(), Matchers.containsString("oauth (OAuth2, authorizationCode)"));

        //Fill out OAuth fields
        WebElement clientNameField = authContainer.findElement(By.id("client_id_authorizationCode"));
        clientNameField.sendKeys(OAUTH_CLIENT_NAME);
        WebElement clientSecretField = authContainer.findElement(By.id("client_secret_authorizationCode"));
        clientSecretField.sendKeys(OAUTH_CLIENT_SECRET);
        // Tick the scope tick box
        WebElement Scope = authContainer.findElement(By.xpath("//label[@for='test-authorizationCode-checkbox-oauth']"));
        Scope.findElement(By.cssSelector("span")).click();

        //click "Authorize"
        WebElement authBtn = authContainer.findElement(By.cssSelector("button.btn.modal-btn.auth.authorize"));
        //This opens a new tab in the browser which Selenium does not yet know about and it itself is not focused on so we need to switch to it
        authBtn.click();

        //Switch to new tab
        new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> driver.getWindowHandles().size() == 2);
        for (String windowId : driver.getWindowHandles()) {
            //as we already have the first tab's handle we go to the other one
            if (!windowId.equals(originalWindow)) {
                driver.switchTo().window(windowId);
            }
        }

        //We are presented with a login page where we provide our credentials
        WebElement userField = waitForElement(driver, By.name("j_username"), LONG_WAIT);
        userField.sendKeys(BASIC_AUTH_USERNAME);
        WebElement passwordField = waitForElement(driver, By.name("j_password"));
        passwordField.sendKeys(BASIC_AUTH_PASSWORD);

        WebElement loginBtn = waitForElement(driver, By.name("submitButton"));
        loginBtn.click();

        WebElement allowOnceBtn = waitForElement(driver, By.xpath("//input[@value='Allow once']"));
        allowOnceBtn.click();

        // should now have a single tab which is the original openapi page
        new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> driver.getWindowHandles().size() == 1);
        // Switch back to original window
        driver.switchTo().window(originalWindow);

        //The javascript may not have completed the update on the auth button from `Authorize` to `Logout`, so we wait for an element that should be a logout button
        waitForElement(authContainer, By.xpath("//button[text()='Logout']"));

        //check Auth button text is what has changed to `Logout` knowing that there is a Logout button
        assertThat("Auth button text should now state logout", authBtn.getText().equals("Logout"));

        //close Authorization modal
        oauthModalHead.findElement(By.cssSelector("button.close-modal")).click();

        //Check the lock status on operation - should be "locked"
        //Refetch the lock svg element as the previous one is no longer attached to the page,
        // so cannot be reused. however the button is kept, so that can be reused as the anchor
        lockSvg = lock.findElement(By.tagName("svg"));
        assertThat("Check that lock button is now in 'locked' state", lockSvg.getAttribute("class"), equalTo("locked"));

        WebElement getOperationButton = getOperation.findElement(By.cssSelector("button"));
        getOperationButton.click();
        WebElement tryOutButton = waitForElement(getOperation, By.cssSelector("button.btn.try-out__btn"));
        tryOutButton.click();
        //get the GET `/test` operation from `default`
        getOperation = waitForElement(driver, By.id("operations-default-get_test"));
        WebElement executeButton = waitForElement(getOperation, By.cssSelector("button.btn.execute"));
        executeButton.click();
        getOperation = waitForElement(driver, By.id("operations-default-get_test"));
        WebElement testGet200Response = waitForElement(getOperation, By.cssSelector("tr.response[data-code=\"200\"]"));
        assertNotNull("200 response line", testGet200Response);
    }

}
