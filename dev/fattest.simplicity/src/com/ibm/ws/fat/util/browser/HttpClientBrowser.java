/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

/**
 * WebBrowser implementation for Apache's HttpClient<br>
 * http://hc.apache.org/httpclient-3.x/<br>
 * <br>
 * Tested with Jakarta Commons-HttpClient/3.1
 * 
 * @author Tim Burns
 */
public class HttpClientBrowser extends WebBrowserCore {

    protected static String IMPL;
    protected static HttpConnectionManager CONNECTION_MANAGER;
    protected NameValuePair[] formValues = null;

    static {
        // Tell the Jakarta Commons Logging (JCL) library which logging implementation to use
        // (this line makes it possible to configure HttpClient logging with logging.properties from the JRE Logger)
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        // Make it possible for HttpClient to handle https requests
        ProtocolSocketFactory psf = new EasySSLProtocolSocketFactory();
        Protocol.registerProtocol("https", new Protocol("https", psf, 443));
    }

    protected static HttpConnectionManager getHttpConnectionManager() {
        if (CONNECTION_MANAGER == null) { // no need to acquire a lock if CONNECTION_MANAGER is already set (most likely case)
            synchronized (HttpClientBrowser.class) {
                if (CONNECTION_MANAGER == null) { // if CONNECTION_MANAGER is still null after acquiring the lock
                    CONNECTION_MANAGER = new MultiThreadedHttpConnectionManager();
                    HttpConnectionManagerParams params = CONNECTION_MANAGER.getParams();
                    // Set the maximum number of connections that will be created for any particular HostConfiguration. Defaults to 2.
                    params.setDefaultMaxConnectionsPerHost(5);
                    // Set the maximum number of active connections. Defaults to 20.
                    params.setMaxTotalConnections(20);
                    // So you can 5 active connections to up to 4 hosts at the same time (5*4=20 connections)
                }
            }
        }
        return CONNECTION_MANAGER;
    }

    private final HttpClient client;

    protected HttpClientBrowser(File resultDirectory) {
        super(resultDirectory);
        this.client = new HttpClient(getHttpConnectionManager());
        // I don't want to offer up authentication credentials unless I'm asked for them
        this.client.getParams().setAuthenticationPreemptive(false);
    }

    @Override
    protected String getImplementationDescription() {
        if (IMPL == null) { // no need to acquire a lock if IMPL is already set (most likely case)
            synchronized (HttpClientBrowser.class) {
                if (IMPL == null) { // if IMPL is still null after acquiring the lock
                    HttpClientParams params = new HttpClient().getParams();
                    IMPL = (String) params.getParameter("http.useragent");
                }
            }
        }
        return IMPL;
    }

    @Override
    protected void performSetAuthorization(String userName, String password) throws Exception {
        // The WebBrowser object does not allow the client to be null at this point (only called if this browser is open)
        /*
         * (from Http Client documentation ...)
         * "Use default credentials with caution when developing applications
         * that may need to communicate with untrusted web sites or web
         * applications. When preemptive authentication is activated or
         * credentials are not explicitly given for a specific authentication
         * realm and host HttpClient will use default credentials to try to
         * authenticate with the target site. If you want to avoid sending
         * sensitive credentials to an untrusted site, narrow the credentials
         * scope as much as possible: always specify the host and, when known,
         * the realm the credentials are intended for. Setting credentials with
         * AuthScope.ANY authentication scope (null value for host and/or realm)
         * is highly discouraged in production applications. Doing this will
         * result in the credentials being sent for all authentication attempts
         * (all requests in the case of preemptive authentication). Use of this
         * setting should be limited to debugging only."
         * 
         * For the sake of this testing framework, I'm going to assume that all
         * credentials passed into this framework are not sensative, so I'm going
         * to "live dangerously" and always use default credentials.
         */
        Credentials defaultcreds = new UsernamePasswordCredentials(userName, password);
        this.client.getState().setCredentials(AuthScope.ANY, defaultcreds);
    }

    @Override
    protected void configureAcceptCookies() throws Exception {
        /* nothing to do; must set cookie policy for each HttpMethod. See configureMethodSettings(method) */
    }

    @Override
    protected void configureAutoRedirect() throws Exception {
        /* nothing to do; must set auto redirect policy for each HttpMethod. See configureMethodSettings(method) */
    }

    protected void configureMethodSettings(HttpMethod method) throws Exception {
        if (method != null) {
            method.setFollowRedirects(this.autoRedirect);
            HttpMethodParams params = method.getParams();
            params.setSoTimeout(10 * 60 * 1000); // Quit waiting if an HTTP request takes longer than 10 minutes.  Required to prevent the test bucket from hanging when WebSphere hangs.  I can't find a way to set an equivalent timeout in HttpUnit.
            if (this.acceptCookies) {
                params.setCookiePolicy(CookiePolicy.RFC_2109);
            } else {
                params.setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
            }
        }
    }

    protected void addAll(NameValuePair[] source, List<MyNameValuePair> dest) {
        if (source != null && dest != null) {
            for (int i = 0; i < source.length; i++) {
                dest.add(new MyNameValuePair(source[i].getName(), source[i].getValue()));
            }
        }
    }

    @Override
    protected WebResponse submitRequest(String url, int requestNum) throws Exception {
        WebResponse response = new WebResponse(this, requestNum);
        response.setUrl(url);
        response.setRequestedUrl(url);
        this.populateResponse(response);
        return response;
    }

    @Override
    public int request(String url, int expectedStatusCode) throws WebBrowserException {
        HttpMethod method = getHTTPMethod(url);
        //boolean rv = false;
        int rv = -1;
        try {
            client.executeMethod(method);
            //rv = (expectedStatusCode == method.getStatusCode());
            rv = method.getStatusCode();
        } catch (HttpException e) {
            throw new WebBrowserException(e);
        } catch (IOException ioe) {
            throw new WebBrowserException(ioe);
        } finally {
            method.releaseConnection(); // be sure the connection is released back to the connection manager
        }
        return rv;
    }

    /*
     * @Override
     * protected WebResponse submitExpectedFailedRequest(String url, int requestNum) throws Exception {
     * WebResponse response = new WebResponse(this, requestNum);
     * response.setUrl(url);
     * response.setRequestedUrl(url);
     * this.populateResponse(response);
     * return response;
     * }
     */

    protected void populateResponse(WebResponse response) throws Exception {
        // Choose the HttpMethod request type
        HttpMethod method = getHTTPMethod(response.url);

        //configure HttpMethod Settings
        this.configureMethodSettings(method);

        int rc = -1;
        try {
            // start the timer
            response.setStart();

            // Invoke the HttpMethod
            this.client.executeMethod(method);

            // Collect state information for later reference
            rc = method.getStatusCode();
            response.setUrl(method.getURI().toString()); // must reset the URL in case any redirects occurred
            response.setResponseCode(rc);
            response.setResponseBody(method.getResponseBodyAsString());
            this.addAll(this.client.getState().getCookies(), response.cookies);
            this.addAll(method.getRequestHeaders(), response.requestHeaders);
            this.addAll(method.getResponseHeaders(), response.responseHeaders);
        } finally {
            /*-
             * From the HttpClient JavaDoc:
             * Releases the connection being used by this HTTP method. In
             * particular the connection is used to read the response(if there
             * is one) and will be held until the response has been read. If the
             * connection can be reused by other HTTP methods it is NOT closed
             * at this point.
             */
            method.releaseConnection(); // be sure the connection is released back to the connection manager
        }

        // Populate frames
        response.parseFrames();
        for (WebResponse frame : response.frames) {
            this.populateResponse(frame);
        }
        response.setStop();

        // if necessary, throw a status code exception AFTER the connection has been released
        // if possible, save response output to a file prior to throwing an exception
        response.save();
        this.checkStatusCode(rc, response.url); // don't call getUrl() to reduce logging 
    }

    @Override
    protected void performReset() throws Exception {
        /*-
         * From the HttpClient JavaDoc:
         * Clears the state information (all cookies, credentials and proxy
         * credentials).
         */
        this.client.getState().clear();
    }

    @Override
    protected void performClose() throws Exception {
        // nothing to do
    }

    private HttpMethod getHTTPMethod(String url) {
        HttpMethod method;
        if (formValues != null) {
            method = new PostMethod(url);
            ((PostMethod) method).addParameters(formValues);
        } else {
            method = new GetMethod(url);
        }
        return method;
    }

    @Override
    public void setFormValues(Properties values) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        if (values != null) {
            for (Map.Entry<Object, Object> entry : values.entrySet()) {
                nameValuePairs.add(new NameValuePair((String) entry.getKey(), (String) entry.getValue()));
            }
        }

        this.formValues = nameValuePairs.toArray(new NameValuePair[] {});
    }
}
