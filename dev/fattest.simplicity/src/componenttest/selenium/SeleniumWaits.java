/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.selenium;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

/**
 * Utility methods for waiting for things in Selenium tests
 * <p>
 * After taking actions, Selenium tests often need to wait a short period of time for things to happen in the browser. Unfortunately the syntax for doing such a wait is a little
 * cumbersome. Selenium does have an option to implicitly wait on every operation, but this can't be combined with explicit waiting elsewhere in the test.
 */
public class SeleniumWaits {

    /** Default wait for "short" tasks which shouldn't need to wait for anything, like selecting a drop down */
    private static final Duration SHORT_WAIT = Duration.ofSeconds(5);

    /* No instances */
    private SeleniumWaits() {}

    /**
     * Wait for an element to appear, using the default short timeout
     *
     * @param  searchRoot       the point to start the search
     * @param  by               the locating mechanism
     * @return                  the located element
     * @throws TimeoutException if the element is not found within the timeout
     * @see                     #waitForElement(SearchContext, By, Duration)
     */
    public static WebElement waitForElement(SearchContext searchRoot, By by) {
        return waitForElement(searchRoot, by, SHORT_WAIT);
    }

    /**
     * Wait for an element to appear, using a custom timeout
     * <p>
     * Usage:
     *
     * <pre>
     * RemoteWebDriver driver = new RemoteWebDriver(...);
     * WebElement headerBar = waitForElement(driver, By.id("headerBar"), Duration.ofSeconds(10));
     * WebElement title = waitForElement(headerBar, By.tagName("h1"));
     * </pre>
     *
     * @param  searchRoot       the point to start the search
     * @param  by               the locating mechanism
     * @param  timeout          the timeout
     * @return                  the located element
     * @throws TimeoutException if the element is not found within the timeout
     */
    public static WebElement waitForElement(SearchContext searchRoot, By by, Duration timeout) {
        return new FluentWait<>(searchRoot)
                        .ignoring(NoSuchElementException.class)
                        .withTimeout(timeout)
                        .withMessage(() -> "Waiting for element " + by)
                        .until(c -> c.findElement(by));
    }

}
