/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
 * Representation of an HTTP request message provided by the dispatcher to any
 * HTTP container.
 */
public interface HttpRequest {

    /**
     * Query the request method of this message, such as POST or GET.
     *
     * @return String
     */
    String getMethod();

    /**
     * Query the URI of this message, which is only /uri with no query string.
     *
     * @return String
     */
    String getURI();

    /**
     * Query the full URL of this message, in the form of scheme://host:port/uri&lt;?query&gt;.
     *
     * @return String
     */
    String getURL();

    /**
     * Query the protocol version of this message. It will be in the form of
     * "HTTP/&lt;major&gt;.&lt;minor&gt;". This is never null.
     *
     * @return String
     */
    String getVersion();

    /**
     * Query the protocol scheme of this message. This will be "HTTP" or "HTTPS".
     *
     * @return String
     */
    String getScheme();

    /**
     * Query the URL query string information. This might be null if not present.
     *
     * @return String
     */
    String getQuery();

    /**
     * Query the virtual host target of this message. It might exist in the URL
     * or the Host header, and may or may not match the actual socket target. If
     * it is not set in either of those two locations, a null is returned.
     *
     * @return String
     */
    String getVirtualHost();

    /**
     * Query the virtual port of this request message. It might exist in the URL
     * or the Host header, and may or may not match the actual socket port. If it
     * is not set in either of those two locations, a -1 is returned.
     *
     * @return int
     */
    int getVirtualPort();

    /**
     * Access the possible content-length header of this message. It will return
     * -1L if no header exists.
     *
     * @return long
     */
    long getContentLength();

    /**
     * Access the first instance found for the given header name. This might be
     * null if no instance was found.
     *
     * @param name
     * @return String
     */
    String getHeader(String name);

    /**
     * Access a list of all header values found for the given header name. This
     * list is never null, but might be empty.
     *
     * @param name
     * @return List<String>
     */
    List<String> getHeaders(String name);

    /**
     * Access a list of all header names found in this message. This list is never
     * null, but might be empty.
     *
     * @return List<String>
     */
    List<String> getHeaderNames();

    /**
     * Access the first cookie instance in the message with the provided name. This
     * might be null if no match is found.
     *
     * @param name
     * @return HttpCookie
     */
    HttpCookie getCookie(String name);

    /**
     * Access the list of all cookies matching the provided name. This list is never
     * null, but might be empty.
     *
     * @param name
     * @return List<HttpCookie>
     */
    List<HttpCookie> getCookies(String name);

    /**
     * Access the list of all cookies found in this message. The list is never null,
     * but might be empty.
     *
     * @return List<HttpCookie>
     */
    List<HttpCookie> getCookies();

    /**
     * Access the possible body of the request message. This is never null but might
     * be an empty stream.
     *
     * @return HttpInputStream
     */
    HttpInputStream getBody();

    /*
     * Access the list of names for all trailer headers in the request
     *
     * @return List<String>
     */
    List<String> getTrailerNames();

    /*
     * Access the value of a trailer header.
     *
     * @return String
     */
    String getTrailer(String name);

    /*
     * Returns true if trailers are not expected or ready to read.
     * Trailers are not expected if:
     * message is not chunked;
     * trailer header is not present;
     * HTTP version is 1.0 or earlier.
     *
     * @return boolean
     */
    boolean isTrailersReady();

}
