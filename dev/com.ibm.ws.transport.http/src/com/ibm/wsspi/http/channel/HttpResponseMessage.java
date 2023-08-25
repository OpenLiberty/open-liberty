/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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

import com.ibm.wsspi.http.channel.values.StatusCodes;

/**
 * Interface extending the base HTTP message with Response
 * specifics
 *
 * @ibm-private-in-use
 */
public interface HttpResponseMessage extends HttpBaseMessage {

    // ******************************************************************
    // Response-line specific methods
    // ******************************************************************

    /**
     * Query the status-code (200, 404, etc) from the response
     *
     * @return int
     */
    int getStatusCodeAsInt();

    /**
     * Get the status code as an enumerated type.
     *
     * @return StatusCodes
     */
    StatusCodes getStatusCode();

    /**
     * Set the status code of the response message. An input code that does
     * not match an existing defined StatusCode will create a new "Undefined"
     * code where the getByteArray() API will return the input code as a
     * byte[].
     *
     * @param code
     */
    void setStatusCode(int code);

    /**
     * Using the defined StatusCodes, set the status-code and the
     * reason-phrase to the default matching phrase.
     *
     * @param code
     */
    void setStatusCode(StatusCodes code);

    /**
     * Query the value of the reason phrase ("Ok", "Not Found", etc)
     * in the response object
     *
     * @return String
     */
    String getReasonPhrase();

    /**
     * Get the reason phrase as a byte array.
     *
     * @return bytes
     */
    byte[] getReasonPhraseBytes();

    /**
     * Set the value of the reason phrase to the given reason string
     *
     * @param reason
     */
    void setReasonPhrase(String reason);

    /**
     * Set the value of the reason phrase to the given reason byte array
     *
     * @param reason
     */
    void setReasonPhrase(byte[] reason);

    /**
     * Get total bytes written excluding headers;
     *
     * @return
     */
    long getBytesWritten();

    // ******************************************************************
    // Message specific methods
    // ******************************************************************

    /**
     * Create a duplicate of this message, including all headers and other
     * information.
     *
     * @return HttpResponseMessage
     */
    HttpResponseMessage duplicate();

}
