/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.browser;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * <code>WebBrowserCore</code> models the basic behavior of a
 * <code>WebBrowser</code>. It logs standard information about the browser's
 * activity, and produces a unique identifier for every <code>WebBrowserCore</code>
 * instance that's constructed.
 * </p>
 * <p>
 * All implementation-specific work is passed down to subclasses of
 * <code>WebBrowserCore</code>, through a set of "perform" methods.
 * </p>
 * 
 * @author Tim Burns
 */
public abstract class WebBrowserCore implements WebBrowser {

    private static final String CLASS_NAME = WebBrowserCore.class.getName();
    private static Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final AtomicInteger BROWSER_COUNTER = new AtomicInteger();
    protected static final boolean DEFAULT_AUTO_REDIRECT_POLICY = true;
    protected static final boolean DEFAULT_ACCEPT_COOKIE_POLICY = true;

    private final int number;
    protected final AtomicInteger responseCounter;
    protected boolean acceptCookies;
    protected boolean autoRedirect;
    protected final File resultDirectory;
    protected String humanReadableString;

    protected abstract String getImplementationDescription();

    protected WebBrowserCore(File resultDirectory) {
        this.resultDirectory = resultDirectory;
        this.number = BROWSER_COUNTER.incrementAndGet();
        this.responseCounter = new AtomicInteger();
        LOG.info("Opening " + this.toString() + " using " + this.getImplementationDescription());
        this.acceptCookies = DEFAULT_ACCEPT_COOKIE_POLICY;
        this.autoRedirect = DEFAULT_AUTO_REDIRECT_POLICY;
        this.humanReadableString = null; // initialize upon first invocation of toString()
    }

    @Override
    public String toString() {
        if (this.humanReadableString == null) {
            this.humanReadableString = this.getHumanReadableString();
        }
        return this.humanReadableString;
    }

    /**
     * Produces a human-readable identifier for this instance; internally used
     * for logging.
     * 
     * @return a human-readable identifier for this instance. Must not return
     *         null!
     */
    protected String getHumanReadableString() {
        StringBuffer name = new StringBuffer();
        name.append("Web Browser ");
        name.append(this.getNumber());
        return name.toString();
    }

    @Override
    public int getNumber() {
        return this.number;
    }

    @Override
    public File getResultDirectory() {
        return this.resultDirectory;
    }

    protected abstract void performSetAuthorization(String userName, String password) throws Throwable;

    @Override
    public final void setAuthorization(String userName, String password) throws WebBrowserException {
        String method = "setAuthorization";
        LOG.info(this.toString() + " will now authorize as user \"" + userName + "\" with password \"" + password + "\"");
        try {
            this.performSetAuthorization(userName, password);
        } catch (Throwable e) {
            String error = this.toString() + " was unable to configure its authorization settings.";
            LOG.logp(Level.WARNING, CLASS_NAME, method, error, e);
            throw new WebBrowserException(error, e);
        }
    }

    /**
     * Submits an HTTP request and retrieves a response.
     * 
     * @param url
     *            the location of the resource you want to request
     * @return A response object populated with state information
     * @throws Throwable
     *             if a problem occurs making the request or building the
     *             response
     */
    protected abstract WebResponse submitRequest(String url, int number) throws Throwable;

    @Override
    public WebResponse request(String url) throws WebBrowserException {
        if (url == null) {
            return null;
        }
        int requestNum = this.responseCounter.incrementAndGet();
        LOG.info("Submitting " + this.toString() + " Request " + requestNum + ": " + url);
        try {
            return this.submitRequest(url, requestNum);
        } catch (Throwable e) {
            throw new WebBrowserException(this.toString()
                                          + " failed to complete request "
                                          + requestNum
                                          + "; the URL was "
                                          + url
                                          + ".  This failure may have been caused by one or more underlying problems: please check test case output to confirm that the test was configured and started correctly.  Possible causes: the server does not exist, the server failed to start, the application failed to install, the application failed to start, specific product features are not behaving correctly, etc.", e);
        }
    }

    /**
     * Throws an exception if the status code indicates a client/server error. See
     * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">Status Code Definitions</a>.<br>
     * <br>
     * <ol>
     * <li>Informational 1xx</li>
     * <li>Successful 2xx</li>
     * <li>Redirection 3xx</li>
     * <li>Client Error 4xx</li>
     * <li>Server Error 5xx</li>
     * </ol>
     * 
     * @param statusCode
     * @param url
     * @throws WebBrowserException
     */
    protected void checkStatusCode(int statusCode, String url) throws WebBrowserException {
        if (statusCode >= 400) {
            throw new WebBrowserException("Server returned HTTP response code: " + statusCode + " for URL: " + url);
        }
        if (statusCode < 100) { // most likely indicates a WebBrowser implementation error
            throw new WebBrowserException(this.getImplementationDescription() + " may have processed an HTTP request incorrectly.  Encountered HTTP response code: " + statusCode
                                          + " for URL: " + url);
        }
    }

    protected abstract void configureAcceptCookies() throws Throwable;

    @Override
    public void setAcceptCookies(boolean acceptCookies) throws WebBrowserException {
        LOG.info("Setting the cookie policy in " + this.toString());
        try {
            this.acceptCookies = acceptCookies;
            this.configureAcceptCookies();
            if (this.acceptCookies) {
                LOG.info(this.toString() + " is now accepting cookies.");
            } else {
                LOG.info(this.toString() + " is now rejecting cookies.");
            }
        } catch (Throwable e) {
            throw new WebBrowserException(this.toString() + " was unable to change its cookie policy.", e);
        }
    }

    protected abstract void configureAutoRedirect() throws Throwable;

    @Override
    public void setAutoRedirect(boolean autoRedirect) throws WebBrowserException {
        LOG.info("Setting the automatic redirect policy in " + this.toString());
        try {
            this.autoRedirect = autoRedirect;
            this.configureAutoRedirect();
            if (this.autoRedirect) {
                LOG.info(this.toString() + " will now automatically follow HTTP redirects (status code 302, etc).");
            } else {
                LOG.info(this.toString() + " will not automatically follow HTTP redirects.");
            }
        } catch (Throwable e) {
            throw new WebBrowserException(this.toString() + " was unable to change its auto redirect policy.", e);
        }
    }

    protected abstract void performReset() throws Throwable;

    @Override
    public void reset() throws WebBrowserException {
        LOG.info("Clearing the state of " + this.toString());
        try {
            this.performReset();
        } catch (Throwable cause) {
            throw new WebBrowserException("Unable to clear the state of " + this.toString(), cause);
        }
    }

    protected abstract void performClose() throws Throwable;

    @Override
    public void close() throws WebBrowserException {
        LOG.info("Closing " + this.toString() + ".  (This operation is not guaranteed to have any effect on the state of this instance)");
        try {
            this.performClose();
        } catch (Throwable cause) {
            throw new WebBrowserException("Unable to close " + this.toString(), cause);
        }
    }

}
