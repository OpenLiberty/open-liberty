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
import java.util.Properties;

/**
 * <p>
 * Simulates an Internet browser like Internet Explorer or Firefox, but uses one
 * of a variety of implementations, including HttpUnit and Http Client. Both
 * HttpUnit and Http Client can be used in some situations, but not others, so
 * having a WebBrowser object makes it easier to switch between the two
 * implementations. The WebBrowser object also prints messages that describe the
 * browser's activity in a standard fashion. If the programmer desires a
 * particular implementation (HttpUnit or Http Client), a WebBrowser of a
 * particular type should be requested. Otherwise, a default implementation will
 * be used.
 * </p><p>
 * Multiple WebBrowser instances may be opened concurrently, and many threads
 * can use a single instance at the same time. (While WebBrowser instances are
 * thread-safe, there is a limit to the number of active connections allowed by
 * the underlying implementation. For Http Client, that limit is 5 active
 * connections to 4 different hosts).
 * </p><p>
 * The user is responsible for verifying that the observed behavior of the web
 * browser is appropriate for the given situation (based on the response code or
 * page text returned by each page navigation). In other words, whenever you
 * navigate to a page, you should <i>always</i> verify either that (A) the
 * correct page text is returned or (B) the expected response code is returned.
 * In cases where an error code is expected (4xx or 5xx), an exception will be
 * thrown to explain the error code, so this exception should be checked if
 * expected.
 * </p><p>
 * Note that HtmlUnit was not used (instead of Http Client) because the only way
 * to disable cookies in Html Unit is at a global level; in other words, all
 * instances of all active WebBrowser objects would need to disable or enable
 * cookies, and individual browsers could not choose whether to enable or
 * disable cookies individually. (unverified)
 * </p><p>
 * JavaScript and HTML Forms are not supported. Frames are supported, but nested
 * frames are only partially supported.
 * </p>
 * 
 * @author Tim Burns
 */
public interface WebBrowser {

    /**
     * Sets authentication credentials for this instance on all hosts, ports,
     * realms and authentication schemes.
     * 
     * @param userName
     *            The user name
     * @param password
     *            The password
     * @throws WebBrowserException
     *             If credentials can not be set
     */
    public void setAuthorization(String userName, String password) throws WebBrowserException;

    /**
     * Configures this instance to accept cookies
     * 
     * @param acceptCookies
     *            true to accept cookies, false to reject cookies
     * @throws WebBrowserException
     *             if the cookie policy can not be set
     */
    public void setAcceptCookies(boolean acceptCookies) throws WebBrowserException;

    /**
     * Configures this instance to automatically follow redirects
     * 
     * @param autoRedirect
     *            true to automatically follow redirects
     * @throws WebBrowserException
     *             if the redirect policy can not be set
     */
    public void setAutoRedirect(boolean autoRedirect) throws WebBrowserException;

    /**
     * Retrieve the directory where web responses are automatically stored. Null
     * indicates that responses will not be automatically stored.
     * 
     * @return the directory where web responses are automatically stored.
     */
    public File getResultDirectory();

    /**
     * Retrieve the unique identifier for this instance
     * 
     * @return a unique identifier for this instance
     */
    public int getNumber();

    /**
     * Submits an HTTP request to the given URL. The internal state of the
     * browser will update based on the result of this operation. Note that a
     * <code>WebBrowser</code> does not keep any record of previous pages where
     * the user has navigated, so there is no "Back" or "Forward" button.
     * Navigation to paths on the local file system is not supported.
     * 
     * @param url
     *            The destination URL; normally starts with "http://..." or
     *            "https://..."
     * @return the response from navigating to the specified url
     * @throws WebBrowserException
     *             if navigation fails
     */
    public WebResponse request(String url) throws WebBrowserException;

    public int request(String url, int expectedStatusCode) throws WebBrowserException;

    /**
     * Resets the state of this client, removing all cookies etc
     */
    public void reset() throws WebBrowserException;

    /**
     * Attempts to "close" all windows associated with this browser instance.
     * This operation is implementation-dependent, and not guaranteed to have
     * any effect on the state of this instance.
     */
    public void close() throws WebBrowserException;

    /**
     * This method sets POSTMethod form values
     * 
     * @param values - Parameters to set on Post method.
     */
    public void setFormValues(Properties values);

}
