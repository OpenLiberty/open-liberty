/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel;

import java.util.Enumeration;
import java.util.Map;

import com.ibm.wsspi.genericbnf.exception.UnsupportedMethodException;
import com.ibm.wsspi.genericbnf.exception.UnsupportedSchemeException;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.SchemeValues;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;

/**
 * Interface extending the basic HTTP message with Request
 * specifics.
 *
 * @ibm-private-in-use
 */
public interface HttpRequestMessage extends HttpBaseMessage {

    // ******************************************************************
    // Request-Line specific methods
    // ******************************************************************

    /**
     * Return the Http method that was in the request.
     *
     * @return String
     */
    String getMethod();

    /**
     * Return the Http method that was in the request.
     *
     * @return MethodValues
     */
    MethodValues getMethodValue();

    /**
     * Allow the user to set the method prior to sending the Request.
     *
     * @param method
     * @throws UnsupportedMethodException
     */
    void setMethod(String method) throws UnsupportedMethodException;

    /**
     * Allow the user to set the method on this request object.
     *
     * @param method
     * @throws UnsupportedMethodException
     */
    void setMethod(byte[] method) throws UnsupportedMethodException;

    /**
     * Allow the user to set the method prior to sending the Request.
     *
     * @param method
     */
    void setMethod(MethodValues method);

    /**
     * Return the URI that was in the request. Note that this is
     * the actual URI [/path/page] only.
     *
     * @return String
     */
    String getRequestURI();

    /**
     * Return the request URI as a byte[]. This allows the caller
     * to translate it to a String using whatever encoding they
     * wish.
     *
     * @return byte[]
     */
    byte[] getRequestURIAsByteArray();

    /**
     * Return the requested URL. This will be of the form
     * scheme://host:port/URI.
     *
     * @return StringBuffer
     */
    StringBuffer getRequestURL();

    /**
     * Return the requested URL. This will be of the form
     * scheme://host:port/URI.
     *
     * @return String
     */
    String getRequestURLAsString();

    /**
     * Return the request URL as a byte[]. This allows the caller
     * to translate it to a String using whatever encoding they
     * wish.
     *
     * @return byte[]
     */
    byte[] getRequestURLAsByteArray();

    /**
     * Return any query options from the request URI, this will
     * be null if there were no options.
     *
     * @return String
     */
    String getQueryString();

    /**
     * Return the request query string as a byte[]. This allows the caller
     * to translate it to a String using whatever encoding they
     * wish. This will be null if no options were present.
     *
     * @return byte[]
     */
    byte[] getQueryStringAsByteArray();

    /**
     * Query the first value found for the input parameter name. This will
     * return null if the name is not found.
     *
     * This method will only search the URL for query data, not any formdata.
     *
     * @param name
     * @return String (first value or null)
     */
    String getParameter(String name);

    /**
     * Get access to the entire map of parameter name/value pairs.
     *
     * This method will only search the URL for query data, not any formdata.
     *
     * @return Map<String,String[]>
     */
    Map<String, String[]> getParameterMap();

    /**
     * Get a list of all of the parameter names found in this request message.
     *
     * This method will only search the URL for query data, not any formdata.
     *
     * @return Enumeration<String>
     */
    Enumeration<String> getParameterNames();

    /**
     * Get a list of all of the values found for a specific parameter name in
     * this request message.
     *
     * This method will only search the URL for query data, not any formdata.
     *
     * @param name
     * @return String[] or null if not found
     */
    String[] getParameterValues(String name);

    /**
     * Set the requested URL to the given string. This string will be
     * parsed into whatever distinct pieces are present (scheme, URI,
     * host, etc). Input can be :
     * [scheme://host[:port]]/URI[?querystring]
     * <p>
     * Note that only /URI is required.
     *
     * @param url
     */
    void setRequestURL(String url);

    /**
     * Set the requested URL to the given byte[]. This byte[] will be
     * parsed into whatever distinct pieces are present (scheme, URI,
     * host, etc). Input can be :
     * [scheme://host[:port]]/URI[?querystring]
     * <p>
     * Note that only /URI is required.
     *
     * @param url
     */
    void setRequestURL(byte[] url);

    /**
     * Allow the user to set the URI in the Request prior to sending it.
     * Valid input is only /uri, nothing else.
     *
     * @param uri
     */
    void setRequestURI(String uri);

    /**
     * Allow the user to set the URI in the HttpRequest to the given
     * byte array, prior to sending the request. Valid input is only
     * input of /uri, nothing else.
     *
     * @param uri
     */
    void setRequestURI(byte[] uri);

    /**
     * Query the target host string in the request URL. If there was no host in
     * the URL, then this will return null.
     *
     * @return String
     */
    String getURLHost();

    /**
     * Query the target port in the request URL. If it was not present, then
     * this will return NOTSET (-1).
     *
     * @return int
     */
    int getURLPort();

    /**
     * Query the virtual host target of this request. This will check the URL
     * first for any host value (name or IP), if it is not present, then it
     * will check the Host header. If that is not present, then it will return
     * null.
     *
     * @return String
     */
    String getVirtualHost();

    /**
     * Query the target port of this request. It will first check for a port
     * in the URL string and if not found, it will check the Host header. This
     * will return -1 if it is not found in either spot.
     *
     * @return int
     */
    int getVirtualPort();

    /**
     * Set the value of the query string section of the URL for an outgoing
     * request to the given String.
     *
     * @param query
     */
    void setQueryString(String query);

    /**
     * Set the value of the query string section of the URL for an outgoing
     * request to the given byte[].
     *
     * @param query
     */
    void setQueryString(byte[] query);

    /**
     * Query the value of the scheme (http/https) in the Request
     * and get it as a SchemeValues identifier.
     *
     * @return SchemeValues
     */
    SchemeValues getSchemeValue();

    /**
     * Query the value of the scheme (http/https) in the Request
     * and get it as a String.
     *
     * @return String
     */
    String getScheme();

    /**
     * Set the value of the scheme in the Request by using the
     * SchemeValues identifiers.
     *
     * @param scheme
     */
    void setScheme(SchemeValues scheme);

    /**
     * Set the value of the scheme in the Request by using a String.
     *
     * @param scheme
     * @throws UnsupportedSchemeException
     */
    void setScheme(String scheme) throws UnsupportedSchemeException;

    /**
     * Set the value of the scheme in the Request by using a byte[].
     *
     * @param scheme
     * @throws UnsupportedSchemeException
     */
    void setScheme(byte[] scheme) throws UnsupportedSchemeException;

    // ******************************************************************
    // Message specific methods
    // ******************************************************************

    /**
     * Create a duplicate of this message, including all headers and other
     * information.
     *
     * @return HttpRequestMessage
     */
    HttpRequestMessage duplicate();

    /*
     * Returns true if request supports push requests
     */
    boolean isPushSupported();

    /**
     * @param pushBuilder
     */
    void pushNewRequest(Http2PushBuilder pushBuilder);

    /**
     * @return
     */
    String getRemoteUser();

}
