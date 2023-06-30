package com.ibm.ws.jsf22.fat.selenium_util;
/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GPL-2.0 with Classpath-exception-2.0 which
 * is available at https://openjdk.java.net/legal/gplv2+ce.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 or Apache-2.0
 */

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
*  Used in conjunction with WebPage to behave similarly as HTMLUnit's HtmlPage
* 
* Copied over from ChromeDevtoolsDriver 
* - https://github.com/jakartaee/faces/blob/1d71aae51f7d5ae684a3f43db0521b7e7e6aa4f6/tck/util/src/main/java/ee/jakarta/tck/faces/test/util/selenium/ChromeDevtoolsDriver.java
* Modified because chrome dev tools is unavailable via Test Containers 
* - Created https://github.com/testcontainers/testcontainers-java/issues/7242 requesting chrome dev tool support 
* 
*/
public class CustomDriver implements ExtendedWebDriver {

    RemoteWebDriver driver;

    public CustomDriver(RemoteWebDriver driver){
        this.driver = driver;
    }

    public RemoteWebDriver getRemoteWebDriver(){
        return this.driver;
    }

    @Override
    public String getPageText() {
        String head = this.driver.findElement(By.tagName("head")).getAttribute("innerText").replaceAll("[\\s\\n ]", " ");
        String body = this.driver.findElement(By.tagName("body")).getAttribute("innerText").replaceAll("[\\s\\n ]", " ");
        return head + " " + body;
    }

    @Override
    public String getPageTextReduced() {
        String head = this.driver.findElement(By.tagName("head")).getAttribute("innerText");
        String body = this.driver.findElement(By.tagName("body")).getAttribute("innerText");
        // handle blanks and nbsps
        return (head + " " + body).replaceAll("[\\s\\u00A0]+", " ");
    }

    @Override
    public WebDriver.Options manage() {
        return driver.manage();
    }

    @Override
    public WebDriver.Navigation navigate() {
        return driver.navigate();
    }
    
    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    public WebDriver.TargetLocator switchTo() {
        return driver.switchTo();
    }

    public void close() {
        driver.close();
    }

    public void quit() {
        driver.quit();
    }

    public void get(String url) {
        driver.get(url);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getPageSource() {
        return driver.getPageSource();
    }


    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    public List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }

}
