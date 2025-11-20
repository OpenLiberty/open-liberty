/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.testcontainers.Testcontainers;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.faces.fat.selenium.util.internal.ExtendedWebDriver;
import io.openliberty.faces.fat.selenium.util.internal.WebPage;
import io.openliberty.org.apache.myfaces40.fat.FATSuite;
import io.openliberty.org.apache.myfaces40.fat.JSFUtils;

/**
 * Tests for the execute="@this" and render="@this" within a nested composite
 * component.
 * This is a bug that was fixed for Faces 4.0, but still has an outstanding spec
 * interpretation to be resolved.
 * More tests might need to be added depending on the outcome of issue:
 * https://github.com/jakartaee/faces/issues/1567
 * As of now it looks like the issue is resolved and tests pass
 */
@RunWith(FATRunner.class)
public class AjaxRenderExecuteThisTest {

    protected static final Class<?> clazz = AjaxRenderExecuteThisTest.class;

    private static final String APP_NAME = "AjaxRenderExecuteThisTest";

    @Server("faces40_ajaxRenderExecuteThis")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    private static ExtendedWebDriver driver;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME + ".war",
                "io.openliberty.org.apache.faces40.fat.ajax.beans");

        // //Test differences between myfaces and mojarra if needed
        // app.addAsLibraries(new File("publish/files/mojarra40/").listFiles());
        // app.addAsDirectories("publish/files/permissions");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer(AjaxRenderExecuteThisTest.class.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(server.getHttpDefaultPort(), server.getHttpDefaultSecurePort());

        driver = FATSuite.getWebDriver();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private void fillFormSelenium(WebPage page, String form, String input, String value) {
        WebElement inputObj = page.findElement(By.id(form + ":inputs:" + input));
        // Can't use inputObj.clear() because this triggers a onchange event before we
        // have our desired values in the input leading to inputObj references being
        // stale (results in Selenium error)
        for (int i = 0; i < 20; i++) {
            inputObj.sendKeys(Keys.BACK_SPACE);
        }
        inputObj.sendKeys(value);
        // Click to trigger onchange event in Selenium by changing focus
        page.findElement(By.id(form)).click();
    }

    private void fillInputsSelenium(WebPage page, String form, List<WebElement> addressAttributes) {
        addressAttributes.clear();
        addressAttributes.add(0, (WebElement) page.findElement(By.id(form + ":inputs:street")));
        addressAttributes.add(1, (WebElement) page.findElement(By.id(form + ":inputs:city")));
        addressAttributes.add(2, (WebElement) page.findElement(By.id(form + ":inputs:state")));
    }

    private void replaceAllMessages(List<String> messageList, List<String> newMessages) {
        messageList.clear();
        messageList.addAll(newMessages);
    }

    private void assertListEqual(List<String> expectedMessages, String actualMessages) {
        for (String expectedMessage : expectedMessages) {
            try {
                assertTrue(actualMessages.contains(expectedMessage));
                int index = actualMessages.indexOf(expectedMessage);
                // If expectedMessage is at front of actualMessage get all of string after it,
                // create a new string without the expectedMessage
                actualMessages = index == 0 ? actualMessages.substring(expectedMessage.length())
                        : actualMessages.substring(0, index)
                                // If the expectedMessage goes until the end of the file nothing more needs to
                                // be done
                                + (index + expectedMessage.length() < actualMessages.length()
                                        ? actualMessages.substring(index + expectedMessage.length())
                                        : "");
            } catch (AssertionError ae) {
                throw new AssertionError("Expected message " + expectedMessage + " was was not in actual messages");
            }
        }

        try {
            assertTrue(actualMessages.trim().equals(""));
        } catch (AssertionError ae) {
            throw new AssertionError("Actual message(s) [" + actualMessages + "] where not expected.");
        }
    }

    private void runAjaxTest(WebPage page, String form, String input, String value, String assertString1,
            String assertString2, String assertString3, String... expMessages) {
        // Address attributes: street, city, state
        List<WebElement> addressAttributes = new ArrayList<>(3);
        String actualMessages;
        List<String> expectedMessages = new ArrayList<>();

        // Test street
        fillFormSelenium(page, form, input, value);
        // Need to wait for the inputs to update
        page.wait(Duration.ofMillis(4000));
        fillInputsSelenium(page, form, addressAttributes);

        Log.info(clazz, name.getMethodName(), "Attribute 0:" + addressAttributes.get(0).getAttribute("value"));
        Log.info(clazz, name.getMethodName(), "Attribute 1:" + addressAttributes.get(1).getAttribute("value"));
        Log.info(clazz, name.getMethodName(), "Attribute 2:" + addressAttributes.get(2).getAttribute("value"));

        actualMessages = page.findElement(By.id(form + ":messages")).getText();
        replaceAllMessages(expectedMessages, List.of(expMessages));

        Log.info(clazz, name.getMethodName(), "Actual:" + actualMessages);
        Log.info(clazz, name.getMethodName(), "Expected:" + expectedMessages);

        assertEquals("street contained unexpected result", assertString1,
                addressAttributes.get(0).getAttribute("value"));
        assertEquals("city contained unexpected result", assertString2, addressAttributes.get(1).getAttribute("value"));
        assertEquals("state contained unexpected result", assertString3,
                addressAttributes.get(2).getAttribute("value"));
        assertListEqual(expectedMessages, actualMessages);

        // Here is an example of what one part looked like:
        // https://github.com/OpenLiberty/open-liberty/blob/72ebaa8a218b5387acb57e05bac53cdfddf84894/dev/io.openliberty.org.apache.myfaces.4.0_fat/fat/src/io/openliberty/org/apache/myfaces40/fat/tests/AjaxRenderExecuteThisTest.java#L133
        // These parts were abstracted into this function to lower repetition as each of
        // the following tests had 3 parts that all ran the same
    }

    @Test
    public void testAjaxRender() throws Exception {
        String url = JSFUtils.createSeleniumURLString(server, APP_NAME, "/ajaxRenderExecuteThis.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(clazz, name.getMethodName(), page.getPageSource());

        // Test attributes
        String form = "testRender";

        runAjaxTest(page, form, "street", "aberdeen way", "aberdeen way", "", "", "setTestRenderStreet:aberdeen way",
                "setTestRenderCity:");
        runAjaxTest(page, form, "city", "big lake", "aberdeen way", "big lake", "", "setTestRenderStreet:aberdeen way",
                "setTestRenderCity:big lake");
        runAjaxTest(page, form, "state", "minnesota", "", "", "minnesota^", "setTestRenderState:minnesota");
    }

    @Test
    public void testAjaxExecuteThis() throws Exception {
        String url = JSFUtils.createSeleniumURLString(server, APP_NAME, "/ajaxRenderExecuteThis.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(clazz, name.getMethodName(), page.getPageSource());

        // Test attributes
        String form = "testExecuteThis";

        runAjaxTest(page, form, "street", "w 4th street", "w 4th street", "", "",
                "setTestExecuteThisStreet:w 4th street", "setTestExecuteThisCity:");
        runAjaxTest(page, form, "city", "red wing", "w 4th street", "red wing", "",
                "setTestExecuteThisStreet:w 4th street", "setTestExecuteThisCity:red wing");
        runAjaxTest(page, form, "state", "minnesota", "", "", "minnesota^", "setTestExecuteThisState:minnesota");
    }

    @Test
    public void testAjaxRenderThis() throws Exception {
        String url = JSFUtils.createSeleniumURLString(server, APP_NAME, "/ajaxRenderExecuteThis.xhtml");
        WebPage page = new WebPage(driver);
        page.get(url);
        page.waitForPageToLoad();

        Log.info(clazz, name.getMethodName(), page.getPageSource());

        // Test attributes
        String form = "testRenderThis";

        runAjaxTest(page, form, "street", "hilbert ave", "hilbert ave^", "^", "", "setTestRenderThisStreet:hilbert ave",
                "setTestRenderThisCity:");
        runAjaxTest(page, form, "city", "winona", "hilbert ave^^", "winona^", "",
                "setTestRenderThisStreet:hilbert ave^", "setTestRenderThisCity:winona");
        runAjaxTest(page, form, "state", "minnesota", "", "", "minnesota^", "setTestRenderThisState:minnesota");
    }
}
