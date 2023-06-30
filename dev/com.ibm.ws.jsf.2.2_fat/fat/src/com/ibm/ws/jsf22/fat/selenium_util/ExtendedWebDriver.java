package com.ibm.ws.jsf22.fat.selenium_util;

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
