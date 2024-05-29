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
package com.ibm.ws.jsf23.fat.selenium_util;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * an extended web driver interface which takes the response into consideration
 * selenium does not have an official api but several webdrivers
 * allow access to the data via various means
 *
 * Another possibility would have been a proxy, but I could not find any properly
 * working proxy for selenium
 * 
 * Copied and Modified from https://github.com/jakartaee/faces/blob/1d71aae51f7d5ae684a3f43db0521b7e7e6aa4f6/tck/util/src/main/java/ee/jakarta/tck/faces/test/util/selenium/ExtendedWebDriver.java
 */
public interface ExtendedWebDriver extends WebDriver {

    /**
     * @return the innerText of the Page
     */
    String getPageText();

    /**
     * returns the innerText of the page in a blank reduced state (more than one blank is reduced to one
     * invisible blanks like nbsp are replaced by normal blanks)
     * @return 
     */
    String getPageTextReduced();

    /*
     * Return the wrapped remote web driver
     */
    RemoteWebDriver getRemoteWebDriver();


}
