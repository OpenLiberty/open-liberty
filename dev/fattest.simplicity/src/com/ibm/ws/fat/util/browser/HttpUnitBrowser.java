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
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.meterware.httpunit.ClientProperties;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebWindow;

/**
 * WebBrowser implementation for HttpUnitt<br>
 * http://httpunit.sourceforge.net/<br>
 * <br>
 * Tested with httpunit/1.5<br>
 * <br>
 * Limitations:
 * <ul>
 * <li>There's no supported way to determine request headers; not sure how to determine request headers for some frames</li>
 * <li>Frames are automatically loaded; can't control when the load, can't measure load time</li>
 * <li>Can't enable trace (HttpUnit has no trace)</li>
 * <li>There's no way to determine the originally requested URL for frames (sometimes)</li>
 * </ul>
 * 
 * @author Tim Burns
 * 
 */
public class HttpUnitBrowser extends WebBrowserCore {

    private static final String CLASS_NAME = HttpUnitBrowser.class.getName();
    private static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private static final String IMPL;

    static {
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(false); // mysteriously, an exception is still thrown on 401
        HttpUnitOptions.setExceptionsThrownOnScriptError(true);

        ClientProperties props = ClientProperties.getDefaultProperties();
        IMPL = props.getUserAgent();

        String method = "(clinit)";
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Initializing " + IMPL);
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Default Character Set                                     : " + HttpUnitOptions.getDefaultCharacterSet());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Default Content Type                                      : " + HttpUnitOptions.getDefaultContentType());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Exceptions thrown on Error Status (4xx or 5xx)            : " + HttpUnitOptions.getExceptionsThrownOnErrorStatus());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Exceptions thrown on script error                         : " + HttpUnitOptions.getExceptionsThrownOnScriptError());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Redirect delay                                            : " + HttpUnitOptions.getRedirectDelay());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Check Content Length (forbid partially received messages) : " + HttpUnitOptions.isCheckContentLength());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Default Application Code Name                             : " + props.getApplicationCodeName());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Default Application Name                                  : " + props.getApplicationName());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Default Application Version                               : " + props.getApplicationVersion());
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Accept Cookies by Default                                 : " + WebBrowserCore.DEFAULT_ACCEPT_COOKIE_POLICY);
        LOGGER.logp(Level.INFO, CLASS_NAME, method, "Auto Redirect by Default                                  : " + WebBrowserCore.DEFAULT_AUTO_REDIRECT_POLICY);
    }

    private final MyWebConversation wc;

    protected HttpUnitBrowser(File resultDirectory) {
        super(resultDirectory);
        this.wc = new MyWebConversation();
        this.configureClientProperties();
    }

    @Override
    protected String getImplementationDescription() {
        return IMPL;
    }

    @Override
    protected void performSetAuthorization(String userName, String password) throws Exception {
        // The WebBrowser object does not allow the wc to be null at this point (only called if this browser is open)
        this.wc.setAuthorization(userName, password);
    }

    private void configureClientProperties() {
        ClientProperties props = this.wc.getClientProperties();
        props.setAcceptCookies(this.acceptCookies);
        props.setAutoRedirect(this.autoRedirect);
    }

    @Override
    protected void configureAcceptCookies() throws Exception {
        this.configureClientProperties();
    }

    @Override
    protected void configureAutoRedirect() throws Exception {
        this.configureClientProperties();
    }

    @Override
    protected WebResponse submitRequest(String url, int requestNum) throws Exception {
        WebResponse result = new WebResponse(this, requestNum); // sets start time
        /*
         * (from HttpUnit javadoc ...)
         * 
         * Submits a GET method request and returns a response.
         */
        result.setUrl(url);
        result.setRequestedUrl(url);
        this.populateResponse(this.wc.getResponse(url), result);
        return result;
    }

    protected void populateResponse(com.meterware.httpunit.WebResponse source, WebResponse dest) throws Exception {
        URL url = source.getURL();
        String urlString = null;
        if (url != null) {
            // source.getURL() sometimes returns null for WebResponse objects returned from wc.getFrameContents(frameName)
            urlString = url.toString();
            dest.setUrl(urlString); // resolves frame URL, but no change on top level frames

            // not sure how to determine request headers without the URL
            Dictionary<String, String> headers = this.wc.getHeaderFields(url);
            if (headers != null) {
                Enumeration<String> keys = headers.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    dest.addRequestHeader(key, headers.get(key));
                }
            }
        }
        dest.setResponseBody(source.getText());
        int rc = source.getResponseCode();
        dest.setResponseCode(rc);
        String[] cookieNames = this.wc.getCookieNames();
        for (int i = 0; i < cookieNames.length; i++) {
            String cookieValue = this.wc.getCookieValue(cookieNames[i]);
            dest.addCookie(cookieNames[i], cookieValue);
        }
        String[] headerNames = source.getHeaderFieldNames();
        for (int i = 0; i < headerNames.length; i++) {
            String headerValue = source.getHeaderField(headerNames[i]);
            dest.addResponseHeader(headerNames[i], headerValue);
        }
        String contentType = source.getContentType();
        if ("text/html".equals(contentType)) {
            /*-
             * For some reason, HttpUnit throws an exception when you try to
             * parse frames in a content type other than HTML; for example:
             * "com.meterware.httpunit.NotHTMLException: The content type of the
             * response is 'text/xml': it must be 'text/html' in order to be
             * recognized as HTML"
             */
            String[] frameNames = source.getFrameNames();
            for (int i = 0; i < frameNames.length; i++) {
                WebResponse frame = null;
                // when frames have no name, HttpUnit names them by their object's toString method ...
                if (frameNames[i] == null || frameNames[i].contains("com.meterware.httpunit.WebFrame")) {
                    frame = new WebResponse(this, dest.getNumber(), i);
                } else {
                    frame = new WebResponse(this, dest.getNumber(), frameNames[i], i);
                }
                frame.setStart(dest.start); // HttpUnit auto-requests all child frames; there's no way to determine the actual start time
                this.populateResponse(this.wc.getFrameContents(frameNames[i]), frame);
                dest.addFrame(frame);
            }
        }

        dest.setStop();

        // if possible, save response output to a file prior to throwing an exception
        dest.save();
        this.checkStatusCode(rc, urlString);
    }

    @Override
    protected void performReset() throws Exception {
        /*-
         * From the HttpUnit JavaDoc:
         * Resets the state of this client, removing all cookies, frames, and
         * per-client headers.
         */
        this.wc.clearContents();
    }

    @Override
    protected void performClose() throws Exception {
        WebWindow[] windows = this.wc.getOpenWindows();
        for (int i = 0; i < windows.length; i++) {
            windows[i].close();
        }
    }

    /**
     * By default, HttpUnit forces you to guess the names of the request headers
     * that it uses. In order to deduce the request header names, you need to
     * increase the visibility of one of WebConversation's protected methods.
     * 
     * @author Tim Burns
     * 
     */
    static class MyWebConversation extends WebConversation {
        @Override
        public Dictionary<String, String> getHeaderFields(URL targetURL) {
            return super.getHeaderFields(targetURL);
        }
    }

    @Override
    // Does not need to be implemented
    public void setFormValues(Properties values) {}

    @Override
    // Does not need to be implemented
    public int request(String url, int expectedStatusCode) throws WebBrowserException {
        return 0;
    }
}
