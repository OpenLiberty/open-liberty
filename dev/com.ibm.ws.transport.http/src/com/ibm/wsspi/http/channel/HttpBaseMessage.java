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

import java.nio.charset.Charset;

import com.ibm.ws.http.channel.internal.HttpTrailersImpl;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.channel.cookies.CookieHandler;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.ExpectValues;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;

/**
 * Class representing all of the common data to every HTTP message. This
 * controls
 * the header storage, the trailer header storage, etc. It is extended by the
 * Request and Response messages that add their specifics to the base message.
 */
public interface HttpBaseMessage extends HeaderStorage, CookieHandler {

    /** Used between URIs and query strings */
    byte QUESTIONMARK = '?';
    /** Static representation of the equals character */
    byte EQUALS = '=';
    /** Comma separator used with lists of values */
    byte COMMA = ',';

    // *******************************************************
    // message specific methods
    // *******************************************************

    /**
     * Query whether or not this particular message is incoming (meaning
     * that it is being parsed and not created manually). An InboundSvcCtxt
     * would have an incoming Request, but an OutboundSvcCtxt would have
     * an incoming Response
     *
     * @return boolean
     */
    boolean isIncoming();

    /**
     * Query whether or not this message is considered "committed" by the
     * application channel.
     *
     * @return boolean
     */
    boolean isCommitted();

    /**
     * Allow an application channel to set this message as committed which
     * they can then check later in their code with the isCommitted() method.
     * The message cannot be "uncommitted" without being cleared entirely.
     *
     */
    void setCommitted();

    /**
     * Clear this object for re-use
     */
    void clear();

    /**
     * Clear this object completely
     */
    void destroy();

    /**
     * Query whether a body is expected to be present with this message. Note
     * that this is only an expectation and not a definitive answer. This will
     * check the necessary headers, status codes, etc, to see if any indicate
     * a body should be present. Without actually reading for a body, this
     * cannot be sure however.
     *
     * @return boolean (true -- a body is expected to be present)
     */
    boolean isBodyExpected();

    /**
     * Query whether or not a body is allowed to be present for this
     * message. This is not whether a body is present, but rather only
     * whether it is allowed to be present.
     *
     * @return boolean (true if allowed)
     */
    boolean isBodyAllowed();

    // ***************************************************************
    // HTTP headers specific related methods
    // ***************************************************************

    /**
     * Set the Content-Length header to the given number of bytes
     *
     * @param length
     */
    void setContentLength(long length);

    /**
     * Query the value of the Content-Length header as a byte number
     *
     * @return int
     */
    long getContentLength();

    /**
     * Set the Connection header to a specific constant (i.e. CLOSE).
     * If you want more specific information (token=field) then you
     * must use the setHeader() API with some character representation
     *
     * @param value
     */
    void setConnection(ConnectionValues value);

    /**
     * Set the Connection header to the input list of defined values.
     * These will be set in the order of the array.
     *
     * @param values
     */
    void setConnection(ConnectionValues[] values);

    /**
     * Query the Connection header and receive an array representing
     * the types found in the header. If CONN_UNDEF is returned, check
     * the getByteArray() method to find out what the "undefined"
     * value was exactly. This array is returned in the order of the
     * tokens found in the header, thus "Connection: TE, Keep-Alive"
     * would return { CONN_TE, CONN_KEEPALIVE }. A CONN_NOTSET means
     * that the header is not present.
     *
     * @return ConnectionValues[]
     */
    ConnectionValues[] getConnection();

    /**
     * Quick method to check whether the Connection header contains
     * the "Keep-Alive" token currently.
     *
     * @return boolean
     */
    boolean isKeepAliveSet();

    /**
     * Quick method to check whether the Connection header has been
     * set to any value yet or not.
     *
     * @return boolean
     */
    boolean isConnectionSet();

    /**
     * Set the Content-Encoding header to the given encoding identifier
     *
     * @param value
     */
    void setContentEncoding(ContentEncodingValues value);

    /**
     * Set the Content-Encoding header to the given list of values,
     * in the same order as the array.
     *
     * @param values
     */
    void setContentEncoding(ContentEncodingValues[] values);

    /**
     * Query the value of the Content-Encoding header. This will
     * be returned as an array of values that were found, or
     * CONTENT_ENCODING_NOTSET if the header is not present. If
     * the value found does not match one of the existing values,
     * a CONTENT_ENCODING_UNDEF value will be returned with the
     * actual data being reported through the getByteArray() method.
     * The array is reported in the order in which values were
     * found in the header.
     *
     * @return ContentEncodingValues[]
     */
    ContentEncodingValues[] getContentEncoding();

    /**
     * Set the Transfer-Encoding header to one of the encoding
     * identifiers.
     *
     * @param value
     */
    void setTransferEncoding(TransferEncodingValues value);

    /**
     * Set the Transfer-Encoding header to the input list of values,
     * in the same order as the array.
     *
     * @param values
     */
    void setTransferEncoding(TransferEncodingValues[] values);

    /**
     * Query the value of the Transfer-Encoding header. This will
     * be returned as an array of values that were found, or
     * TRANSFER_ENCODING_NOTSET if the header is not present. If
     * the value found does not match one of the existing values,
     * a TRANSFER_ENCODING_UNDEF value will be returned with the
     * actual data being reported through the getByteArray() method.
     * The array is returned in the same order as the values found
     * in the header.
     *
     * @return TransferEncodingValues[]
     */
    TransferEncodingValues[] getTransferEncoding();

    /**
     * Quick method to check whether the Transfer-Encoding header
     * contains the "chunked" token currently.
     *
     * @return boolean
     */
    boolean isChunkedEncodingSet();

    /**
     * Informs the channel to add a Date header with the current
     * timestamp. Caller does not need to get the value and format
     * as this API will handle that.
     *
     */
    void setCurrentDate();

    /**
     * Set the Expect header to the given single ExpectValues object.
     *
     * @param value
     */
    void setExpect(ExpectValues value);

    /**
     * Query the current value of the HTTP Expect header.
     *
     * @return byte[]
     */
    byte[] getExpect();

    /**
     * Query whether or not the current HTTP Expect header on the message
     * contains the sequence "100-continue".
     *
     * @return boolean (true if present)
     */
    boolean isExpect100Continue();

    /**
     * Query the MIME type of the HTTP body (e.g. text/html). This will return
     * null if the type is not known.
     *
     * @return String
     */
    String getMIMEType();

    /**
     * Set the MIME type of the HTTP body to the input string.
     *
     * @param type
     */
    void setMIMEType(String type);

    /**
     * Returns a named mapping between sequences of sixteen-bit Unicode
     * characters and sequences of bytes in the body of the flow. If this is
     * not explicitly set, then it will return the mapping for ISO-8859-1.
     *
     * @return Charset
     */
    Charset getCharset();

    /**
     * Sets the named mapping between sequences of sixteen-bit Unicode
     * characters and sequences of bytes in the body of the flow.
     *
     * @param set
     */
    void setCharset(Charset set);

    // ***************************************************************
    // Trailer header methods
    // ***************************************************************

    /**
     * When parsing an incoming object or creating a local one, if the
     * Trailer header is set, then an object will be created to store
     * the HTTP trailer headers. This method will query the existance
     * of that object, receiving null if there is no Trailer header
     * set yet.
     *
     * @return HttpTrailers
     */
    HttpTrailers getTrailers();

    // ***************************************************************
    // Common HTTP first line methods
    // ***************************************************************

    /**
     * Return the Http version from the request. An HTTP_VERSION_UNDEF
     * response indicates that the value did not match any of the
     * existing versions and the caller should use the getByteArray()
     * method to find the exact version string.
     *
     * @return VersionValues
     */
    VersionValues getVersionValue();

    /**
     * Return the Http version from the request
     *
     * @return String
     */
    String getVersion();

    /**
     * Allow the user to set the version for this request
     *
     * @param version
     */
    void setVersion(VersionValues version);

    /**
     * Allow the user to set the version for this request
     *
     * @param version
     * @throws UnsupportedProtocolVersionException
     */
    void setVersion(String version) throws UnsupportedProtocolVersionException;

    /**
     * Allow the user to set the version for this request
     *
     * @param version
     * @throws UnsupportedProtocolVersionException
     */
    void setVersion(byte[] version) throws UnsupportedProtocolVersionException;

    /**
     * Get the message start time
     */
    long getStartTime();

    /**
     * Get the message end time;
     */
    long getEndTime();

    /**
     * Get the service context associated to this message
     */
    HttpServiceContext getServiceContext();

    /**
     * Obtain a trailer object for adding response trailers
     *
     * @return HttpTrailers object for adding trailers
     */
    public HttpTrailersImpl createTrailers();

}
