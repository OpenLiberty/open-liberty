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
import java.io.OutputStream;
import java.io.Writer;

/**
 * <p>This interface encapsulates the artifacts pertaining to an HTTP response.
 * 
 * <p>Implementations of this interface are not guaranteed to be thread safe, and live only until the corresponding
 * {@link com.ibm.wsspi.rest.handler.RESTHandler#handleRequest(RESTRequest, RESTResponse)} method returns.
 * 
 * @ibm-spi
 */
public interface RESTResponse {

    /**
     * This method provides access to write the outbound body of the corresponding REST response.
     * Either this method or getOutputStream() may be called to write the body, not both
     * 
     * @return a Writer over the outbound body.
     * @throws IOException if an I/O exception occurred.
     */
    public Writer getWriter() throws IOException;

    /**
     * This method provides access to write to the outstream of the corresponding REST response.
     * Either this method or getWriter() may be called to write the body, not both
     * 
     * @return a OutputStream to the outbound Response OutputStream.
     * @throws IOException if an I/O exception occurred.
     */
    public OutputStream getOutputStream() throws IOException;

    /**
     * Sets a response header with the given key and value. If a response header was already set with the key, the new value will
     * override the old one.
     * 
     * @param key of the header.
     * @param value of the header.
     */
    public void setResponseHeader(String key, String value);

    /**
     * Adds a response header with the given key and value. This method allows response headers to have multiple values.
     * 
     * @param key of the header.
     * @param value of the header.
     */
    public void addResponseHeader(String key, String value);

    /**
     * Sets the response status code.
     * 
     * @param statusCode the HTTP status code
     */
    public void setStatus(int statusCode);

    /**
     * Sends an error response using the specified status code. This RESTResponse object should not be used after this method.
     * 
     * @param statusCode the HTTP status code
     * @throws IOException if an I/O exception occurred.
     */
    public void sendError(int statusCode) throws IOException;

    /**
     * Sends an error response using the specified status code and error message. This RESTResponse object should not be used after this method.
     * 
     * @param statusCode the HTTP status code
     * @param msg the error message
     * @throws IOException if an I/O exception occurred.
     */
    public void sendError(int statusCode, String msg) throws IOException;

    /**
     * Sets the content type of the response being sent to the client.
     * 
     * @param contentType a String specifying the MIME type of the content.
     */
    public void setContentType(String contentType);

    /**
     * Sets the length of the content body in the response.
     * 
     * @param len an integer specifying the length of the content being returned to the client.
     */
    public void setContentLength(int len);

    /**
     * Sets the character encoding (MIME charset) of the response being sent to the client, for example, to UTF-8.
     * 
     * @param charset a String specifying only the character set defined by IANA Character Sets (http://www.iana.org/assignments/character-sets)
     */
    public void setCharacterEncoding(String charset);

}
