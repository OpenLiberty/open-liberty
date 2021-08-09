/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util;

import java.io.File;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestWatcher;

import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

/**
 * <p>A test that stores messages logged during execution to special directories and files.</p>
 * <p>Instances of this class should not be run in parallel. If they are, weird things will happen with log files.</p>
 *
 * @author Tim Burns
 */
@Deprecated
public abstract class LoggingTest {

    /** Prints extra messages to the log to distinguish test A from test B. JUnit requires this variable to be public. */
    @Rule
    public final TestWatcher watchman = new FatWatcher();

    /** JUnit requires this variable to be public. Generally, nothing but the declaring class should use it outside of JUnit. */
    @ClassRule
    public static FatLogHandler TEST_FIXTURE_LOG_HANDLER = new FatLogHandler().setLogHandler(new LogHandler());

    /** JUnit requires this variable to be public. Generally, nothing but the declaring class should use it outside of JUnit. */
    @Rule
    public FatLogHandler testCaseLogHandler = new FatLogHandler().setLogHandler(new LogHandler()).setParent(TEST_FIXTURE_LOG_HANDLER);

    /**
     * <p>Retrieves the unique directory where log information will be stored for this test fixture.</p>
     * <p>Use this method when you need to store debug information that's applicable to multiple tests. If you need to store debug information that's only applicable to the current
     * test, use {@link #getTestCaseLogDirectory()} instead.</p>
     *
     * @return the unique directory where log information will be stored for this test fixture
     */
    protected File getTestFixtureLogDirectory() {
        return TEST_FIXTURE_LOG_HANDLER.getLogDirectory();
    }

    /**
     * <p>Retrieves the unique directory where log information will be stored for this test case.</p>
     * <p>Use this method when you need to store debug information that's only applicable to the current test. If you need to store debug information that's applicable to multiple
     * tests, use {@link #getTestFixtureLogDirectory()} instead.</p>
     *
     * @return the unique directory where log information will be stored for this test case
     */
    protected File getTestCaseLogDirectory() {
        return testCaseLogHandler.getLogDirectory();
    }

    /**
     * <p>Creates a new {@link WebBrowser} instance for this test case.</p>
     * <p>The first request from this instance will establish a new HttpSession, and response information will be stored in the test case log directory.</p>
     * <p>Use this method when you only need to access the same {@link WebBrowser} within a single test case. If you need to reuse the same {@link WebBrowser} with multiple tests,
     * use {@link #createWebBrowserForTestFixture()} instead.</p>
     *
     * @return a new {@link WebBrowser} instance for this test case
     */
    protected WebBrowser createWebBrowserForTestCase() {
        return WebBrowserFactory.getInstance().createWebBrowser(getTestCaseLogDirectory());
    }

    /**
     * <p>Creates a new WebBrowser instance for this test fixture.</p>
     * <p>The first request from this instance will establish a new HttpSession, and response information will be stored in the test fixture log directory.</p>
     * <p>Use this method when you need to reuse the same {@link WebBrowser} with multiple tests. If you only need to access the same {@link WebBrowser} within a single test
     * case, use {@link #createWebBrowserForTestCase()} instead.</p>
     *
     * @return a new WebBrowser instance for this test case
     */
    protected WebBrowser createWebBrowserForTestFixture() {
        return WebBrowserFactory.getInstance().createWebBrowser(this.getTestFixtureLogDirectory());
    }

    abstract protected SharedServer getSharedServer();

    protected final void verifyBadUrl(String resource) throws Exception {
        verifyBadUrl(createWebBrowserForTestCase(), resource);
    }

    protected final void verifyBadUrl(WebBrowser wb, String resource) throws Exception {
        getSharedServer().verifyBadUrl(wb, resource);
    }

    protected final WebResponse verifyResponse(String resource, String expectedResponse) throws Exception {
        return verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse);
    }

    protected final WebResponse verifyResponse(String resource, String expectedResponse, int numberToMatch, String extraMatch) throws Exception {
        return verifyResponse(createWebBrowserForTestCase(), resource, expectedResponse, numberToMatch, extraMatch);
    }

    protected final WebResponse verifyResponse(String resource, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return verifyResponse(createWebBrowserForTestCase(), resource, expectedResponses, unexpectedResponses);
    }

    protected final WebResponse verifyResponse(String resource, String... expectedResponses) throws Exception {
        return verifyResponse(createWebBrowserForTestCase(), resource, expectedResponses);
    }

    protected final WebResponse verifyResponse(WebBrowser wb, String resource, String expectedResponse) throws Exception {
        return getSharedServer().verifyResponse(wb, resource, expectedResponse);
    }

    protected final WebResponse verifyResponse(WebBrowser wb, String resource, String expectedResponse, int numberToMatch, String extraMatch) throws Exception {
        return getSharedServer().verifyResponse(wb, resource, expectedResponse, numberToMatch, extraMatch);
    }

    protected final WebResponse verifyResponse(WebBrowser wb, String resource, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        return getSharedServer().verifyResponse(wb, resource, expectedResponses, unexpectedResponses);
    }

    protected final WebResponse verifyResponse(WebBrowser wb, String resource, String[] expectedResponses) throws Exception {
        return getSharedServer().verifyResponse(wb, resource, expectedResponses);
    }

    protected final void verifyStatusCode(String resource, int expectedStatus) throws Exception {
        verifyStatusCode(createWebBrowserForTestCase(), resource, expectedStatus);
    }

    protected final void verifyStatusCode(WebBrowser wb, String resource, int expectedStatus) throws Exception {
        getSharedServer().verifyStatusCode(wb, resource, expectedStatus);
    }
}
