/**
 * Copyright Werner Punz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Original code stemming 100% from me, hence relicense from EPL
package com.ibm.ws.jsf22.fat.selenium_util;

import org.openqa.selenium.*;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Mimics the html unit webpage
 * Modified from original source as Chrome Dev Tools are not accessible when used with TestContainers
 * See issue https://github.com/testcontainers/testcontainers-java/issues/7242
 */
public class WebPage {

    public static final Duration STD_TIMEOUT = Duration.ofMillis(8000);
    public static final Duration LONG_TIMEOUT = Duration.ofMillis(16000);

    protected ExtendedWebDriver webDriver;

    public WebPage(ExtendedWebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public WebDriver getWebDriver() {
        return (WebDriver) webDriver;
    }

    public void setWebDriver(ExtendedWebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public void get(String url) {
        webDriver.get(url);
    }

    public String getTitle() {
        return webDriver.getTitle();
    }

    /**
     * waits for a certain condition is met, until a timeout is hit. In case of exceeding the condition, a runtime exception
     * is thrown!
     *
     * @param isTrue the condition lambda to check
     * @param timeout timeout duration
     */
    public <V> void waitForCondition(Function<? super WebDriver, V> isTrue, Duration timeout) {
        synchronized (webDriver) {
            WebDriverWait wait = new WebDriverWait(webDriver, timeout);
            wait.until(isTrue);
        }
    }

    /**
     * The same as before, but with the long default timeout of LONG_TIMEOUT (16000ms)
     *
     * @param isTrue condition lambda
     */
    public <V> void waitForCondition(Function<? super WebDriver, V> isTrue) {
        synchronized (webDriver) {
            WebDriverWait wait = new WebDriverWait(webDriver, LONG_TIMEOUT);
            wait.until(isTrue);
        }
    }

    /**
     * Wait for a certain period of time
     *
     * @param timeout the timeout to wait (note due to the asynchronous nature of the web drivers, any code running on the
     * browser itself will proceed (aka javascript) only the client operations are stalled.
     */
    public void wait(Duration timeout) {
        synchronized (webDriver) {
            try {
                webDriver.wait(timeout.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * wait until the current Ajax request targeting the same page has stopped and then tests for blocking running scripts
     * still running. A small initial delay cannot hurt either
     */
    public void waitReqJs() {
        this.wait(STD_TIMEOUT);
    }

    /**
     * waits for backgrounds processes on the browser to complete
     *
     * @param timeOut the timeout duration until the wait can proceed before being interupopted
     */
    public void waitForPageToLoad(Duration timeOut) {
        ExpectedCondition<Boolean> expectation =
            driver -> webDriver.getRemoteWebDriver().executeScript("return document.readyState")
                               .equals("complete");

        synchronized (webDriver) {
            WebDriverWait wait = new WebDriverWait(webDriver, timeOut);
            wait.until(expectation);
        }
    }

        /**
     * wait until the page load is finished
     */
    public void waitForPageToLoad() {
        waitForPageToLoad(STD_TIMEOUT);
    }

    /**
     * conditional waiter and checker which checks whether the page text is present we add our own waiter internally,
     * because pageSource always delivers
     *
     * @param text to check
     * @return true in case of found false in case of found after our standard timeout is reached
     */
    public boolean isInPageText(String text) {
        try {
            // values are not returned by getPageText
            String values = getInputValues();

            waitForCondition(webDriver1 -> (webDriver.getPageText() + values).contains(text), STD_TIMEOUT);
            return true;
        } catch (TimeoutException ex) {
            // timeout is wanted in this case and should result in a false
            return false;
        }
    }

    public boolean isInPageTextReduced(String text) {
        try {
            String values = getInputValues();
            waitForCondition(webDriver1 -> (webDriver.getPageTextReduced() + values.replaceAll("\\s+", " ")).contains(text), STD_TIMEOUT);
            return true;
        } catch (TimeoutException ex) {
            // timeout is wanted in this case and should result in a false
            return false;
        }
    }

    public boolean matchesPageText(String regexp) {
        try {
            waitForCondition(webDriver1 -> webDriver.getPageText().matches(regexp), STD_TIMEOUT);
            return true;
        } catch (TimeoutException ex) {
            // timeout is wanted in this case and should result in a false
            return false;
        }
    }

    /**
     * adds the reduced page text functionality to the regexp match
     *
     * @param regexp
     * @return
     */
    public boolean matchesPageTextReduced(String regexp) {
        try {
            waitForCondition(webDriver1 -> webDriver.getPageTextReduced().matches(regexp), STD_TIMEOUT);
            return true;
        } catch (TimeoutException ex) {
            // timeout is wanted in this case and should result in a false
            return false;
        }
    }

    /**
     * conditional waiter and checker which checks whether a text is not in the page we add our own waiter internally,
     * because pageSource always delivers
     *
     * @param text to check
     * @return true in case of found false in case of found after our standard timeout is reached
     */
    public boolean isNotInPageText(String text) {
        try {
            String values = getInputValues();
            waitForCondition(webDriver1 -> !(webDriver.getPageText() + values).contains(text), STD_TIMEOUT);
            return true;
        } catch (TimeoutException ex) {
            // timeout is wanted in this case and should result in a false
            return false;
        }
    }

    /**
     * conditional waiter and checker which checks whether a text is in the page we add our own waiter internally, because
     * pageSource always delivers this version of isInPage checks explicitly the full markup not only the text
     *
     * @param text to check
     * @return true in case of found false in case of found after our standard timeout is reached
     */
    public boolean isInPage(String text) {
        try {
            String values = getInputValues();
            waitForCondition(webDriver1 -> (webDriver.getPageSource() + values).contains(text), STD_TIMEOUT);
            return true;
        } catch (TimeoutException ex) {
            // timeout is wanted in this case and should result in a false
            return false;
        }
    }

    /**
     * conditional waiter and checker which checks whether a text is not in the page we add our own waiter internally,
     * because pageSource always delivers we need to add two different condition checkers herte because a timeout
     * automatically throws internally an error which is mapped to false We therefore cannot simply wait for the condition
     * either being met or timeout with one method
     *
     * @param text to check
     * @return true in case of found false in case of found after our standard timeout is reached
     */
    public boolean isNotInPage(String text) {
        try {
            String values = getInputValues();
            waitForCondition(webDriver1 -> !(webDriver.getPageSource() + values).contains(text), STD_TIMEOUT);
            return true;
        } catch (TimeoutException ex) {
            // timeout is wanted in this case and should result in a false
            return false;
        }
    }

    /**
     * conditional waiter and checker which checks whether a text is in the page we add our own waiter internally, because
     * pageSource always delivers we need to add two different condition checkers herte because a timeout automatically
     * throws internally an error which is mapped to false We therefore cannot simply wait for the condition either being
     * met or timeout with one method
     *
     * @param text to check
     * @return true in case of found false in case of found after our standard timeout is reached
     */
    public boolean isInPage(String text, boolean allowExceptions) {
        try {
            String values = getInputValues();
            waitForCondition(webDriver1 -> (webDriver.getPageSource() + values).contains(text), STD_TIMEOUT);
            return true;
        } catch (TimeoutException exception) {
            if (allowExceptions) {
                throw exception;
            }
            exception.printStackTrace();
            return false;
        }
    }


    public WebElement findElement(By by) {
        return webDriver.findElement(by);
    }

    public List<WebElement> findElements(By by) {
        return webDriver.findElements(by);
    }

        public String getPageSource() {
        return webDriver.getPageSource();
    }

    /**
     * Convenience method to get all anchor elmements
     *
     * @return a list of a hrefs as WebElements
     */
    public List<WebElement> getAnchors() {
        return webDriver.findElements(By.cssSelector("a[href]"));
    }

        private String getInputValues() {
        return webDriver.findElements(By.cssSelector("input, textarea, select")).stream()
                .map(webElement -> webElement.getAttribute("value")).reduce("", (str1, str2) -> str1 + " " + str2);
    }

}
