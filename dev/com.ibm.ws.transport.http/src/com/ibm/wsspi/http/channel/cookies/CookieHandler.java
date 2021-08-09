/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.cookies;

import java.util.List;

import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * The <code>CookieHandler</code> interface defines a set of interfaces
 * for the addition and deletion of Cookies.
 * 
 * @ibm-private-in-use
 */
public interface CookieHandler {

    /**
     * When the only required bit of information from a cookie is the value
     * from the name=value parameter, this API can be used to get just that.
     * It will avoid any extra object creation such as Strings or Cookies
     * themselves.
     * 
     * Any leading or trailing whitespace will be removed from the value;
     * however, no error checking is performed to see if any whitespace in the
     * middle is encapsulated with quotes... whatever is found will be returned.
     * Thus [Cookie: id=12 34] would return "12  34" as the value for "id".
     * 
     * @param name
     * @return byte[] (null if target cookie was not found)
     */
    byte[] getCookieValue(String name);

    /**
     * This will return a list of all of the cookie values present in the message
     * for the input cookie name. The List contains the series of cookie value
     * Strings.
     * 
     * @param name
     * @return List of String objects-- if no cookies of this name exist, then
     *         an empty list is returned
     */
    List<String> getAllCookieValues(String name);

    /**
     * This searches the message looking for a matching Cookie instance in
     * the cookie headers.
     * 
     * @param name
     *            the unique name of the <code>Cookie</code> defined in an
     *            HTTP <b>Cookie:</b> header.
     * @return the <code>Cookie</code> for the specified name.
     *         NULL will be returned if it does not exist.
     */
    HttpCookie getCookie(String name);

    /**
     * This will return a List of all of the cookies present in the message.
     * 
     * @return List -- if no cookies exist, then an empty list is returned
     */
    List<HttpCookie> getAllCookies();

    /**
     * This will return a list of all of the cookies present in the message
     * for the input cookie name.
     * 
     * @param name
     * @return List of Cookie objects -- if no cookies of this name exist, then
     *         an empty list is returned
     */
    List<HttpCookie> getAllCookies(String name);

    /**
     * This allows the caller to put a Cookie object directly into the message.
     * 
     * @param cookie
     * @param cookieHeader
     * @return TRUE if the cookie was set successfully otherwise returns FALSE.
     *         if the constraints are violated and the cookie could not
     *         be added.
     * @throws IllegalArgumentException
     *             if cookie version is invalid
     */
    boolean setCookie(HttpCookie cookie, HttpHeaderKeys cookieHeader);

    /**
     * This allows the caller to create a cookie in the message that matches
     * the input cookie name and value.
     * 
     * @param name
     * @param value
     * @param cookieHeader
     * @return TRUE if the cookie was set successfully otherwise returns FALSE.
     *         if the constraints are violated and the cookie could not
     *         be added.
     */
    boolean setCookie(String name, String value, HttpHeaderKeys cookieHeader);

    /**
     * Removes a cookie from the input header of the message.
     * 
     * @param name
     * @param cookieHeader
     * @return TRUE if a cookie with this name was removed, FALSE if the
     *         cookie could not be deleted. The removal would fail if
     *         the cookie with the specified name does not exist or if the
     *         constraints were not satisfied.
     */
    boolean removeCookie(String name, HttpHeaderKeys cookieHeader);

    /**
     * Test for the existence of a cookie with a particular name and type.
     * Note that it is faster to simply use the getCookie() API and check for
     * null explicitly.
     * 
     * @param name
     *            the name of the cookie to search for.
     * @param cookieHeader
     *            the type of cookie to search for.
     * @return TRUE if the cookie exists, FALSE otherwise.
     */
    boolean containsCookie(String name, HttpHeaderKeys cookieHeader);

}
