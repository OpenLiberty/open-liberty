/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.FATSuite;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

import com.ibm.ws.jsf22.fat.JSFUtils;
import io.openliberty.faces.fat.selenium.util.internal.CustomDriver;
import io.openliberty.faces.fat.selenium.util.internal.ExtendedWebDriver;
import io.openliberty.faces.fat.selenium.util.internal.WebPage;


/**
 * Tests to execute on the jsfTestServer2 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22InputFileTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22InputFile";
    // static Props testProps = Props.getInstance();

    protected static final Class<?> c = JSF22InputFileTests.class;

    private static ExtendedWebDriver driver;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        boolean isEE10 = JakartaEEAction.isEE10OrLaterActive();

        ShrinkHelper.defaultDropinApp(jsfTestServer2, "JSF22InputFile.war",
                                      isEE10 ? "com.ibm.ws.jsf22.fat.input.faces40" : "com.ibm.ws.jsf22.fat.input.jsf22");

        jsfTestServer2.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(jsfTestServer2.getHttpDefaultPort(), jsfTestServer2.getHttpDefaultSecurePort());

        driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));

        driver.getRemoteWebDriver().setFileDetector(new LocalFileDetector());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }

        driver.quit(); // closes all sessions and terminutes the webdriver
    }

    @After
    public void clearCookies()
    {
        driver.getRemoteWebDriver().manage().deleteAllCookies();
        jsfTestServer2.resetLogMarks();
    }

    /**
     * inputFile defect - This test copies a file to the ServerRoot (so we have a full path) and then
     * sets the file to be uploaded. It then attempts to upload the file, if it works, the word 'SUCCESS'
     * will be present in the subsequently loaded page.
     *
     * Note: Even though we are copying the file to 'ServerRoot' that is just so we can figure out/know where
     * it is. This lets us create the path to it so it can be selected later on.
     *
     * @throws Exception
     */
    @Test
    public void testInputFile() throws Exception {
        jsfTestServer2.copyFileToLibertyServerRoot("JSF22InputFileCONTENT.txt");

        Log.info(c, name.getMethodName(), jsfTestServer2.getServerRoot());

        File fileToUpload = new File(jsfTestServer2.getServerRoot() + File.separator + "JSF22InputFileCONTENT.txt");

        Log.info(c, name.getMethodName(), "File to Upload --  Using FILE --> " + fileToUpload.toString());
        if (fileToUpload.exists()) {
            Log.info(c, name.getMethodName(), "File to Upload -->  Found file: " + fileToUpload.toString());
        }

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "fileUploadTest.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            HtmlFileInput fileInput = (HtmlFileInput) page.getElementById("form1:file1");
            fileInput.setValueAttribute(fileToUpload.toString());
            HtmlSubmitInput uploadButton = (HtmlSubmitInput) page.getElementById("form1:uploadButton");

            HtmlPage page2 = uploadButton.click();
            Log.info(c, name.getMethodName(), page2.asText());
            assertTrue(page2.asText().contains("SUCCESS"));
        }
    }

    /**
     * Scenario:
     * - MultiPart Form 
     * - An ajax enabled h:inputFile tag
     * 
     * - Verifies the file upload works via XHR requests (with type multi part form data). NOTE: Development project stage is used to ensure any errors are alerts.
     * @throws Exception
     */
    @Test
    public void testAjaxInputFile() throws Exception {
        jsfTestServer2.copyFileToLibertyServerRoot("AjaxJSF22InputFileCONTENT.txt");

        Log.info(c, name.getMethodName(), jsfTestServer2.getServerRoot());

        File fileToUpload = new File(jsfTestServer2.getServerRoot() + File.separator + "AjaxJSF22InputFileCONTENT.txt");

        Log.info(c, name.getMethodName(), "File to Upload --  Using FILE --> " + fileToUpload.toString());
        if (fileToUpload.exists()) {
            Log.info(c, name.getMethodName(), "File to Upload -->  Found file: " + fileToUpload.toString());
        }

        String url = JSFUtils.createSeleniumURLString(jsfTestServer2, contextRoot, "AjaxFileUploadTest.jsf");;
        System.out.println(url);
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(c, name.getMethodName(), page.getPageSource());

        jsfTestServer2.setMarkToEndOfLog();
        WebElement element = page.findElement(By.id("form1:file1"));
        element.sendKeys(fileToUpload.getAbsolutePath());

        //Ensure the correct content type is used. 
        assertNotNull("The 'multipart/form-data; boundary=' content type was not found!", jsfTestServer2.waitForStringInTraceUsingMark(".*multipart/form-data; boundary=.*"));

        Log.info(c, name.getMethodName(), page.getPageSource());

        page.waitForCondition(driver -> page.isInPage("File Size: 12"));

        page.findElement(By.id("form1:uploadButton")).click();

        Log.info(c, name.getMethodName(), page.getPageSource());

        page.waitForCondition(driver -> page.isInPage("Ajax-SUCCESS"));
    }

    private static File generateTempFile(String name, String ext, String content) throws IOException {
        Path path = Files.createTempFile(name, "." + ext);
        Files.write(path, content.getBytes(), StandardOpenOption.APPEND);
        return path.toFile();
    }
}
