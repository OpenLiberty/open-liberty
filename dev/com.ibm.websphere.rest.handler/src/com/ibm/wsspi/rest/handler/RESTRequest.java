/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * <p>This interface encapsulates the artifacts pertaining to an HTTP request.</p>
 *
 * <p>Implementations of this interface are not guaranteed to be thread safe, and live only until the corresponding
 * {@link com.ibm.wsspi.rest.handler.RESTHandler#handleRequest(RESTRequest, RESTResponse)} method returns.</p>
 *
 * @ibm-spi
 */
public interface RESTRequest {

    /**
     * This method gives access to the incoming REST body, if any.
     *
     * @return a Reader over the payload.
     * @throws IOException if an I/O exception occurred.
     */
    public Reader getInput() throws IOException;

    /**
     * This method gives access to the InputStream.
     *
     * @return the InputStream for the payload.
     * @throws IOException if an I/O exception occurred.
     */
    public InputStream getInputStream() throws IOException;

    /**
     * This method gives access to incoming REST headers.
     *
     * @param key representing the header.
     * @return a matching value for the key, or null otherwise.
     */
    public String getHeader(String key);

    /**
     * Fetches the HTTP method (ie: GET, POST,DELETE, OPTIONS, PUT) corresponding to this REST request.
     *
     * @return a java.lang.String representation of the HTTP method.
     */
    public String getMethod();

    /**
     * <p>Returns the request URL, exactly as the request would have been made it.
     * This includes the protocol, host, port, path and query string (if specified).</p>
     * <p>For example:
     * if the requested URL is {@code https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2} then this method returns
     * {@code https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2}
     *
     * @return The complete URL used for the request
     */
    public String getCompleteURL();

    /**
     * <p>Returns the URL component of the request URL. The query string is not included.</p>
     * <p>For example:
     * if the requested URL is {@code https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2} then this method returns
     * {@code https://localhost:9443/ibm/api/myRoot/myPath}
     *
     * @return The URL of the request
     */
    public String getURL();

    /**
     * <p>Returns the URI component of the request URL. The query string is not included.</p>
     * <p>For example:
     * if the requested URL is {@code https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2} then this method returns {@code /ibm/api/myRoot/myPath}.
     *
     * @return The URI of the request
     */
    public String getURI();

    /**
     * <p>Returns the context path component of the request URL.</p>
     * <p>For example:
     * if the requested URL is {@code https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2} then this method returns {@code /ibm/api}.
     *
     * @return The context path of the request
     */
    public String getContextPath();

    /**
     * <p>Returns the path component of the request URL relative to the IBM API context root.
     * The query string is not included.</p>
     * <p>For example:
     * if the requested URL is {@code https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2} then this method returns {@code /myRoot/myPath}
     *
     * @return The requested path relative to the IBM API context root of the request
     */
    public String getPath();

    /**
     * <p>Returns the query string of the request URL.</p>
     * <p>For example:
     * if the requested URL is {@code https://localhost:9443/ibm/api/myRoot/myPath?param1=value1,param2=value2} then this method returns {@code param1=value1,param2=value2}.
     *
     * @return The query parameters of the request
     */
    public String getQueryString();

    /**
     * Fetches the value of a parameter. If the parameter contains multiple values the first one will be returned, but in that case the caller should
     * preferrably use {@link com.ibm.wsspi.rest.handler.RESTRequest#getParameterMap()}
     *
     * @param name a java.lang.String representing the key of the parameter to be fetched.
     * @return a java.lang.String representation of the parameter value or null if the parameter was not found.
     */
    public String getParameter(String name);

    /**
     * Fetches the values of a parameter.
     *
     * @param name a java.lang.String representing the key of the parameter to be fetched.
     * @return a java.lang.String[] representation of the parameter values or null if the parameter was not found.
     */
    public String[] getParameterValues(String name);

    /**
     * Fetches a map that contains all the requested parameters. The entry value is a java.lang.String array because there could be multiple values for the same parameter.
     *
     * @return a java.util.Map containing all requested parameter keys and their corresponding value(s).
     */
    public Map<String, String[]> getParameterMap();

    /**
     * Returns a java.security.Principal object containing the name of the current authenticated user.
     * If the user has not been authenticated, the method returns null.
     *
     * @return a java.security.Principal containing the name of the user making this request; null if the user has not been authenticated
     */
    public Principal getUserPrincipal();

    /**
     * Returns a boolean indicating whether the authenticated user is included in the specified logical "role".
     * If the user has not been authenticated, the method returns false
     *
     * @param role a String specifying the name of the role
     * @return a boolean indicating whether the user making this request belongs to a given role; false if the user has not been authenticated
     */
    public boolean isUserInRole(String role);

    /**
     * Returns the actual value of a path variable in the incoming request. Path variables are specified within the registered URL of a RESTHandler.
     *
     * <p>Example: A RESTHandler could register a root "/myRoot/{city}/schools/{school}", which would match to an incoming request
     * of "/myRoot/Burlington/schools/NotreDame", and thus getPathVariable("city") returns Burlington while getPathVariable("school") returns NotreDame.
     *
     * @param variable represents the name of the variable to fetch
     * @return the value in the incoming URL that matched the variable, or null if this variable did not match to the incoming URL.
     */
    public String getPathVariable(String variable);

    /**
     * Returns the preferred Locale that the client will accept content in,
     * based on the Accept-Language header. If the client request doesn't
     * provide an Accept-Language header, this method returns the default
     * locale for the server.
     *
     * @return the preferred Locale for the client
     */
    public Locale getLocale();

    /**
     * Returns an Enumeration of Locale objects indicating, in decreasing order
     * starting with the preferred locale, the locales that are acceptable to
     * the client based on the Accept-Language header. If the client request
     * doesn't provide an Accept-Language header, this method returns an
     * Enumeration containing one Locale, the default locale for the server.
     *
     * @return an Enumeration of preferred Locale objects for the client
     */
    public Enumeration<Locale> getLocales();

    /**
     * Returns the Internet Protocol (IP) address of the client or last proxy that sent the request.
     *
     * @return a String containing the IP address of the client that sent the request
     */
    public String getRemoteAddr();

    /**
     * Returns the fully qualified name of the client or the last proxy that sent the request.
     * If the engine cannot or chooses not to resolve the hostname (to improve performance),
     * this method returns the dotted-string form of the IP address.
     *
     * @return a String containing the fully qualified name of the client
     */
    public String getRemoteHost();

    /**
     * Returns the Internet Protocol (IP) source port of the client or last proxy that sent the request.
     *
     * @return an integer specifying the port number
     */
    public int getRemotePort();

    /**
     * To be used in conjunction with isMultiPartRequest, which should always be called before this method. This method
     * will throw an exception if the request is not a multipart request.
     *
     * @param partName multipart form part name
     * @return InputStream of the part if it exists or null otherwise
     */
    public InputStream getPart(String partName) throws IOException;

    /**
     * Returns true if this is a multipart form request. To be used in conjunction with getPart().
     *
     * @return true if this is a multipart request, false otherwise.
     */
    boolean isMultiPartRequest();

    /**
     * Gets the content type of the request sent to the server.
     *
     * @return contentType a String specifying the MIME type of the content.
     */
    public String getContentType();

    /*
     * Gets the session ID of the request
     *
     * @return String sessionID a String specifying the sessionID of the HTTP request
     */
    public String getSessionId();

}
