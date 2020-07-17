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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.remote.JMXServiceURL;

import org.junit.rules.ExternalResource;

import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserException;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.fat.util.jmx.JmxException;
import com.ibm.ws.fat.util.jmx.JmxServiceUrlFactory;
import com.ibm.ws.fat.util.jmx.mbeans.ApplicationMBean;
import com.ibm.ws.fat.util.jmx.mbeans.PluginConfigMBean;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.impl.LibertyServerWrapper;
import junit.framework.Assert;

/**
 * <p>Encapsulates a {@link LibertyServer} and provides helper methods. Automatically starts the server before the annotated test fixture starts.</p>
 * <p>Consider migrating methods from this class to {@link LibertyServer}.</p>
 *
 * @author Tim Burns
 */
@Deprecated
@LibertyServerWrapper
public class SharedServer extends ExternalResource {

    private static final Logger LOG = Logger.getLogger(SharedServer.class.getName());

    private final String serverName;
    private final boolean waitForSecurity;
    private LibertyServer server;
    private String[] featuresToInstall = new String[] {};

    /**
     * Convenience constructor; assumes security is disabled
     *
     * @param serverName the name of the {@link LibertyServer} to encapsulate
     */
    public SharedServer(String serverName) {
        this(serverName, false);
    }

    /**
     * Convenience constructor; assumes security is disabled
     *
     * @param serverName the name of the {@link LibertyServer} to encapsulate
     */
    public SharedServer(String serverName, String... featuresToInstall) {
        this(serverName, false);
        this.featuresToInstall = featuresToInstall;

    }

    /**
     * Primary constructor
     *
     * @param serverName      the name of the {@link LibertyServer} to encapsulate
     * @param waitForSecurity true if the {@link #startIfNotStarted()} method should wait for security-related methods before proceeding
     */
    public SharedServer(String serverName, boolean waitForSecurity) {
        this.serverName = serverName;
        this.waitForSecurity = waitForSecurity;
    }

    @Override
    protected void before() throws Exception{
        this.startIfNotStarted();
        // try {
        //     this.startIfNotStarted();
        // } catch (Exception e) {
        //     LOG.log(Level.INFO, "Failed to start shared server", e);
        //     throw e;
        // }
    }

    /**
     * @return the name of the liberty server; see the publish/servers directory for options
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @see    #getServerName()
     * @return the liberty server used by this test
     */
    public LibertyServer getLibertyServer() {
        if (this.server == null) {
            this.server = LibertyServerFactory.getLibertyServer(this.getServerName());
        }
        return this.server;
    }

    /**
     * Get the JMX connection URL of this server
     *
     * @return              a {@link JMXServiceURL} that allows you to invoke MBeans on the server
     * @throws JmxException
     *                          if the server can't be found,
     *                          the localConnector-1.0 feature is not enabled,
     *                          or the address file is not valid
     */
    public JMXServiceURL getJmxServiceUrl() throws JmxException {
        return JmxServiceUrlFactory.getInstance().getUrl(this.getLibertyServer());
    }

    /**
     * Retrieves an {@link ApplicationMBean} for a particular application on this server
     *
     * @param  applicationName the name of the application to operate on
     * @return                 an {@link ApplicationMBean}
     * @throws JmxException    if the object name for the input application cannot be constructed
     */
    public ApplicationMBean getApplicationMBean(String applicationName) throws JmxException {
        return new ApplicationMBean(this.getJmxServiceUrl(), applicationName);
    }

    /**
     * Retrieves a {@link PluginConfigMBean} for this server
     *
     * @return              a {@link PluginConfigMBean} for this server
     * @throws JmxException if the object name for the PluginConfigMBean cannot be constructed
     */
    public PluginConfigMBean getPluginConfigMBean() throws JmxException {
        return new PluginConfigMBean(this.getJmxServiceUrl());
    }

    /**
     * Get the WebResponse for the given resource using the given WebBrowser
     *
     * @param  browser
     * @param  resource
     * @return
     */
    public WebResponse getResponse(WebBrowser browser, String resource) throws Exception {
        String url = getServerUrl(true, resource);
        WebResponse response = browser.request(url);
        return response;
    }

    /**
     * Start the server if it is not running, or do nothing if it's already running
     *
     * @throws Exception if server start fails
     */
    public void startIfNotStarted() throws Exception {
        LibertyServer server = this.getLibertyServer();
        if (!server.isStarted()) {
            String delimiter = Props.getInstance().getProperty(Props.LOGGING_BREAK_SMALL);
            installRequiredFeatures();
            LOG.info(delimiter);
            LOG.info("Starting server: " + server.getServerName());
            LOG.info(delimiter);
            server.startServer(); // throws exception if start fails
            if (this.waitForSecurity) {
                server.waitForStringInLog(".*CWWKS4105I: LTPA configuration is ready.*");
                server.waitForStringInLog(".*CWWKS0008I: The security service is ready.*");
            }
            LOG.info(delimiter);
            LOG.info("Server is running: " + server.getServerName());
            LOG.info(delimiter);
        }
    }

    /**
     * Start the server if it is not running, or do nothing if it's already running
     *
     * To start server without cleaning server directory or validating apps do:
     * startIfNotStarted(false, false, false);
     *
     * @throws Exception if server start fails
     */
    public void startIfNotStarted(boolean preClean, boolean cleanStart, boolean validateApps) throws Exception {
        LibertyServer server = this.getLibertyServer();
        if (!server.isStarted()) {
            String delimiter = Props.getInstance().getProperty(Props.LOGGING_BREAK_SMALL);
            installRequiredFeatures();
            LOG.info(delimiter);
            LOG.info("Starting server: " + server.getServerName());
            LOG.info(delimiter);
            server.startServerAndValidate(preClean, cleanStart, validateApps); // throws exception if start fails
            if (this.waitForSecurity) {
                server.waitForStringInLog(".*CWWKS4105I: LTPA configuration is ready.*");
                server.waitForStringInLog(".*CWWKS0008I: The security service is ready.*");
            }
            LOG.info(delimiter);
            LOG.info("Server is running: " + server.getServerName());
            LOG.info(delimiter);
        }
    }

    private void installRequiredFeatures() throws Exception {
        if (featuresToInstall.length > 0) {
            LibertyServer server = this.getLibertyServer();
            for (String feature : featuresToInstall) {
                server.copyFileToLibertyInstallRoot("lib/features", feature);
            }
        }
    }

    /**
     * Builds the URL of the Liberty server.
     *
     * @param  http true for HTTP, false for HTTPS
     * @param  path additional information to append to the returned URL
     * @return      the URL of the Liberty server
     */
    public String getServerUrl(boolean http, String path) {
        LibertyServer server = this.getLibertyServer();
        StringBuilder url = new StringBuilder();
        if (http) {
            url.append("http://");
        } else {
            url.append("https://");
        }
        url.append(server.getHostname()); // trust Simplicity to provide host
        url.append(":");
        if (http) {
            url.append(server.getHttpDefaultPort()); // trust Simplicity to provide port
        } else {
            url.append(server.getHttpDefaultSecurePort()); // trust Simplicity to provide port
        }
        if (path != null) {
            url.append(path);
        }
        return url.toString();
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies that the HTTP response body contains the text specified by
     * <code>expectedResponse</code>.
     *
     * @param  webBrowser       the browser used to submit the request
     * @param  resource         the resource on the shared server to request
     * @param  expectedResponse a subset of the text expected from the HTTP response
     * @return                  the HTTP response (in case further validation is required)
     * @throws Exception        if the <code>expectedResponse</code> is not contained in the HTTP response body
     */
    public WebResponse verifyResponse(WebBrowser webBrowser, String resource, String expectedResponse) throws Exception {
        String url = this.getServerUrl(true, resource);
        WebResponse response = webBrowser.request(url);
        LOG.info("Response from webBrowser: " + response.getResponseBody());
        response.verifyResponseBodyContains(expectedResponse);
        return response;
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies than an exception is thrown. For testing that servlet URL does not exist.
     * <code>expectedResponse</code>.
     *
     * @param  webBrowser the browser used to submit the request
     * @param  resource   the resource on the shared server to request
     *
     * @return            the HTTP response (in case further validation is required)
     * @throws Exception  if the <code>expectedResponse</code> is not contained in the HTTP response body
     */
    public void verifyBadUrl(WebBrowser webBrowser, String resource) throws Exception {
        String url = this.getServerUrl(true, resource);
        WebResponse response = null;
        try {
            response = webBrowser.request(url);
        } catch (com.ibm.ws.fat.util.browser.WebBrowserException wbe) {
            LOG.info("Caught WebBrowserException for resource [ " + resource + "].  This is expected.");
            return; // success
        }

        LOG.info("Unexpected response from webBrowser: [" + response == null ? null : response.getResponseBody() + "]");
        throw new Exception("Unexpected response from webBrowser");
    }

    /**
     *
     * @param  regex     regex to search for
     * @param  url       URL to fetch
     * @return           the first capture, or null if no captures in the regex
     * @throws Exception If the <code>webBrowser</code> throws and Exception
     */
    public String assertRegexInresponse(WebBrowser wb, String url, String regex) throws Exception {
        String returnString = null;
        WebResponse wr = wb.request(url);
        Pattern pat = Pattern.compile(regex);
        String response = wr.getResponseBody();
        Matcher m = null;
        boolean found = false;
        for (String s : response.split("\n")) {
            System.out.println("Line:" + s);
            m = pat.matcher(s);
            boolean matches = m.find();
            System.out.println("Does it match " + regex + "? " + matches);
            if (matches) {
                found = true;
                break;
            }
        }
        Assert.assertTrue("Didn't find match for regex " + regex +
                          " in response " + response, found);
        System.out.println("Group count is " + m.groupCount());
        if (m != null && m.groupCount() > 0) {
            System.out.println("First Hit i s " + m.group(1));
            returnString = m.group(1);
        }
        return returnString;
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies that the HTTP response body contains the all of the supplied text
     * specified by the array of * <code>expectedResponses</code>
     *
     * @param  webBrowser        the browser used to submit the request
     * @param  resource          the resource on the shared server to request
     * @param  expectedResponses an array of the different subsets of the text expected from the HTTP response
     * @return                   the HTTP response (in case further validation is required)
     * @throws Exception         if the <code>expectedResponses</code> is not contained in the HTTP response body
     */
    public WebResponse verifyResponse(WebBrowser webBrowser, String resource, String[] expectedResponses) throws Exception {
        String url = this.getServerUrl(true, resource);
        WebResponse response = webBrowser.request(url);
        LOG.info("Response from webBrowser: " + response.getResponseBody());
        for (String textToFind : expectedResponses) {
            response.verifyResponseBodyContains(textToFind);
        }

        return response;
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies that the HTTP response body contains the all of the supplied text
     * specified by the array of * <code>expectedResponses</code> and doesn't contain any
     * unexpected responses.
     *
     * @param  webBrowser          the browser used to submit the request
     * @param  resource            the resource on the shared server to request
     * @param  expectedResponses   an array of the different subsets of the text expected from the HTTP response
     * @param  unexpectedResponses an array of the different subsets of the text that must not be found in the HTTP response
     * @return                     the HTTP response (in case further validation is required)
     * @throws Exception           if the <code>expectedResponse</code> is not contained in the HTTP response body or the unexpected responses are
     *                                 found in the response body.
     */
    public WebResponse verifyResponse(WebBrowser webBrowser, String resource, String[] expectedResponses, String[] unexpectedResponses) throws Exception {
        WebResponse response = verifyResponse(webBrowser, resource, expectedResponses);

        // Now check that we don't have any unexpected responses
        for (String textNotExpectedToFind : unexpectedResponses) {
            response.verifyResponseBodyDoesNotContain(textNotExpectedToFind);
        }

        return response;
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies that the HTTP response status code equals the expected status code
     * specified by <code>statusCode</code>.
     *
     * @param  webBrowser the browser used to submit the request
     * @param  resource   the resource on the shared server to request
     * @param  statusCode the expected HTTP response status code
     * @throws Exception  if the <code>statusCode</code> is not the actual status code returned in the HTTP response
     */
    public void verifyStatusCode(WebBrowser webBrowser, String resource, int statusCode) throws WebBrowserException {
        String url = this.getServerUrl(true, resource);

        int actualStatusCode = webBrowser.request(url, statusCode);

        if (actualStatusCode != statusCode) {
            throw new WebBrowserException("The expected status code was not thrown.  Expected " + statusCode + ", was " + actualStatusCode);
        } else {
            LOG.info("Expected status code retrieved: " + statusCode);
        }
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies that the HTTP response body contains the text specified by
     * <code>expectedResponse</code>.
     *
     * @param  webBrowser       the browser used to submit the request
     * @param  resource         the resource on the shared server to request
     * @param  expectedResponse a subset of the text expected from the HTTP response
     * @return                  the HTTP response (in case further validation is required)
     * @throws Exception        if the <code>expectedResponse</code> is not contained in the HTTP response body
     */
    public WebResponse verifyResponse(WebBrowser webBrowser, String resource, String expectedResponse, int numberToMatch, String extraMatch) throws Exception {
        String url = this.getServerUrl(true, resource);
        WebResponse response = webBrowser.request(url);
        LOG.info("Response from webBrowser: " + response.getResponseBody());
        response.verifyResponseBodyWithRepeatMatchAndExtra(expectedResponse, numberToMatch, extraMatch);
        return response;
    }

    /**
     * Looks for messages in the server log to indicate that an application has started.
     * You only need to call this method if the application isn't in the dropins directory.
     *
     * @param  applicationName the name of the application whose startup you want to verify
     * @throws Exception       if the application doesn't apear to be running
     */
    public void verifyAppHasStarted(String applicationName) throws Exception {
        String message = "CWWKZ0001I:.*" + applicationName;
        LOG.info("Waiting for message to appear in log: " + message);
        this.getLibertyServer().waitForStringInLog(message);
    }

    public void logInfo(String s) {
        LOG.info(s);
    }
}
