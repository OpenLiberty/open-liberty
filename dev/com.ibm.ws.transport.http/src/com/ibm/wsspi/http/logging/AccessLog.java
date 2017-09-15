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
package com.ibm.wsspi.http.logging;

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

/**
 * NCSA access log file wrapper.
 *
 */
public interface AccessLog extends LogFile {

    /** List of NCSA format types allowed */
    enum Format {
        /** NCSA Common format */
        COMMON,
        /** NCSA Combined format */
        COMBINED
    }

    /**
     * Record a request/response exchange to the access log using the input
     * information.
     *
     * @param request
     * @param response
     * @param version
     *            (request version, may not match the object)
     * @param userId
     *            (user id from request, may not match the object)
     * @param remoteAddr
     *            (remote client IP address)
     * @param numBytes
     */
    void log(HttpRequestMessage request, HttpResponseMessage response, String version, String userId, String remoteAddr, long numBytes);

    /**
     * Record the pre-formatted access log information. The message is expected
     * to be properly formatted with a trailing \r\n.
     *
     * @param message
     */
    void log(byte[] message);

    /**
     * Record the pre-formatted access log information. The message is expected
     * to be properly formatted with a trailing \r\n.
     *
     * @param message
     */
    void log(String message);

    /**
     * Query what NCSA format the log file is configured to use.
     *
     * @return Format
     */
    Format getFormat();

    /**
     * Set this access log to use the input format.
     *
     * @param format
     */
    void setFormat(Format format);

}
