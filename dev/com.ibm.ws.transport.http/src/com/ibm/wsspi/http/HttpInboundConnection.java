/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.net.InetAddress;

/**
 * Representation of an inbound HTTP connection that the dispatcher will provide
 * to containers.
 */
public interface HttpInboundConnection {

    /**
     * Access the local/server side address of the connection.
     *
     * @param canonical if true, attempts to find fully qualified domain name
     *
     * @return String host name
     * @see InetAddress#getHostName()
     * @see InetAddress#getCanonicalHostName()
     */
    String getLocalHostName(boolean canonical);

    /**
     * Access the local/server side address of the connection.
     *
     * @return String IP Address
     */
    String getLocalHostAddress();

    /**
     * Access the local/server side port of the connection.
     *
     * @return int
     */
    int getLocalPort();

    /**
     * @return The concatenated canonicalHostName:port string
     *         representing this inbound connection (for use when looking
     *         up associated resources)
     */
    String getLocalHostAlias();

    /**
     * Access the remote/client side address of the connection.
     *
     * @param canonical if true, attempts to find fully qualified domain name
     *
     * @return String host name
     * @see InetAddress#getHostName()
     * @see InetAddress#getCanonicalHostName()
     */
    String getRemoteHostName(boolean canonical);

    /**
     * Access the remote/client side address of the connection.
     *
     * @return String IP Address
     */
    String getRemoteHostAddress();

    /**
     * Access the remote/client side port of the connection.
     *
     * @return int
     */
    int getRemotePort();

    /**
     * Access the request message object for this connection.
     *
     * @return HttpRequest
     */
    HttpRequest getRequest();

    /**
     * Access the response message object for this connection.
     *
     * @return HttpResponse
     */
    HttpResponse getResponse();

    /**
     * Access any SSL information, if this connection was secure. This will return
     * null if it was not a secure connection.
     *
     * @return SSLContext
     */
    SSLContext getSSLContext();

    /**
     * Access the string encoding utility class. This is never null.
     *
     * @return EncodingUtils
     */
    EncodingUtils getEncodingUtils();

    /**
     * Access the HTTP date format utility class. This includes support for all the various
     * HTTP date styles, and is never null.
     *
     * @return HttpDateFormat
     */
    HttpDateFormat getDateFormatter();

    /**
     * When a container is finished with the connection, this API signals that
     * and allows the dispatcher to complete any remaining work for the connection.
     * The exception passed is used in the decision whether to perform a read for
     * another request or to simply close the connection. Errors while reading or
     * writing during this request/response exchange would be passed along here.
     *
     * @param e
     */
    void finish(Exception e);

    /**
     * This will indicate whether or not private headers should be trusted on this connection.
     *
     * @return true if trusted/private headers should be used, false if not.
     */
    boolean useTrustedHeaders();

    /**
     * Return the value of the specified trusted header if trusted headers
     * are enabled for this connection.
     *
     * @param headerKey Trusted header to find
     * @return value of trusted header or null.
     */
    String getTrustedHeader(String headerKey);

    /**
     * Get the requested host based on the Host and/or private headers.
     * <p>
     * per Servlet spec, this is similar to getServerName:
     * Returns the host name of the server to which the request was sent.
     * It is the value of the part before ":" in the Host header value, if any,
     * or the resolved server name, or the server IP address.
     */
    String getRequestedHost();

    /**
     * Get the requested port based on the Host and/or private headers.
     *
     * per Servlet spec, this is similar to getServerPort:
     * Returns the port number to which the request was sent. It is the value of
     * the part after ":" in the Host header value, if any, or the server port
     * where the client connection was accepted on.
     */
    int getRequestedPort();

}
