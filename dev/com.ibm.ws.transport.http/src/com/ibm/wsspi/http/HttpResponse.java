/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.util.List;

/**
 * Representation of an HTTP response message provided by the dispatcher to any
 * HTTP container.
 */
public interface HttpResponse {

    /**
     * Set the status code to the input value.
     *
     * @param code
     */
    void setStatus(int code);

    /**
     * Set the reason phrase to the input value.
     *
     * @param phrase
     */
    void setReason(String phrase);

    /**
     * Set the HTTP version of this message to the input value. This is expected
     * to be of the form "HTTP/<major>.<minor>".
     *
     * @param version
     */
    void setVersion(String version);

    /**
     * Set the content-length header of this message.
     *
     * @param length
     */
    void setContentLength(long length);

    /**
     * Set a header on the message using the provided name and value pair. This
     * will replace any currently existing instances of the header name.
     *
     * @param name
     * @param value
     * @see #addHeader(String, String)
     */
    void setHeader(String name, String value);

    /**
     * Append a header on the message using the provided name and value pair. This
     * will be added to any current instance of the header name.
     *
     * @param name
     * @param value
     * @see #setHeader(String, String)
     */
    void addHeader(String name, String value);

    /**
     * Add a cookie object to the message.
     *
     * @param cookie
     */
    void addCookie(HttpCookie cookie);

    /**
     * Remove a cookie object from the message.
     *
     * @param cookie
     */
    void removeCookie(HttpCookie cookie);

    /**
     * Remove the target header from the message.
     *
     * @param name
     */
    void removeHeader(String name);

    /**
     * Remove all headers and cookies from the message.
     */
    void removeAllHeaders();

    /**
     * Check whether the response headers have been committed and sent out on
     * the network or not.
     *
     * @return boolean
     */
    boolean isCommitted();

    /**
     * Check whether the connection is persistent or not.
     *
     * @return boolean
     */
    boolean isPersistent();

    /**
     * Resets the status code, version, and headers.
     *
     */
    void reset();

    /**
     * Query the current status code of this message.
     *
     * @return int
     */
    int getStatus();

    /**
     * Query the current reason phrase of this message.
     *
     * @return String
     */
    String getReason();

    /**
     * Query the current HTTP version of this message. It will be in the form of
     * "HTTP/<major>.<minor>".
     *
     * @return String
     */
    String getVersion();

    /**
     * Query the current content-length header of the message. This will be -1L
     * if the header is not set.
     *
     * @return long
     */
    long getContentLength();

    /**
     * Access the value associated with the first instance of the target header
     * name. This will return null if no instance was found.
     *
     * @param name
     * @return String
     */
    String getHeader(String name);

    /**
     * Access a list of all header values for a given header name. This list is
     * never null, but might be empty.
     *
     * @param name
     * @return List<String>
     */
    List<String> getHeaders(String name);

    /**
     * Access a list of all header values in this message. This list is never null,
     * but might be empty.
     *
     * @return List<String>
     */
    List<String> getHeaders();

    /**
     * Access a list of all header names found in this message. This list is never
     * null, but might be empty.
     *
     * @return List<String>
     */
    List<String> getHeaderNames();

    /**
     * Access the first instance of a cookie with the provided name. This might
     * be null if no match is found.
     *
     * @param name
     * @return HttpCookie
     */
    HttpCookie getCookie(String name);

    /**
     * Access a list of all cookie instances matching the provided name. This list
     * is never null, but might be empty.
     *
     * @param name
     * @return List<HttpCookie>
     */
    List<HttpCookie> getCookies(String name);

    /**
     * Access a list of all cookies found in this message. The list is never null,
     * but might be empty.
     *
     * @return List<HttpCookie>
     */
    List<HttpCookie> getCookies();

    /**
     * Access the output stream representation of the body for this message.
     *
     * @return HttpOutputStream
     */
    HttpOutputStream getBody();

    /**
     * Sets a trailer header to follow the body of this message
     *
     * @param name
     * @param value
     */
    void setTrailer(String name, String value);

    /**
     * Write trailer when all have been set.
     *
     * @param name
     * @param value
     */
    void writeTrailers();
}
