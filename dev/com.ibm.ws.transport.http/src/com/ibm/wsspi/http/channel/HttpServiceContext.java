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
package com.ibm.wsspi.http.channel;

import java.net.InetAddress;

import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpDateFormat;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;

/**
 * Basic service context that adds HTTP specific methods to the lower level
 * generic stream interface.
 * 
 * @ibm-private-in-use
 */
public interface HttpServiceContext {

    // ********************************************************
    // accessing/setting the two messages for the connection
    // ********************************************************

    /**
     * Gets the request message associated with this service context.
     * 
     * @return HttpRequestMessage
     */
    HttpRequestMessage getRequest();

    /**
     * Get the response message associated with this service context.
     * 
     * @return HttpResponseMessage
     */
    HttpResponseMessage getResponse();

    /**
     * This method will inform this service context to re-take ownership of
     * the request message. This is useful in a proxy-type environment where
     * individual messages may be passed from service context to service
     * context and at some point need to be reset back to the originator.
     */
    void resetRequestOwnership();

    /**
     * This method will inform this service context to re-take ownership of
     * the response message. This is useful in a proxy-type environment where
     * individual messages may be passed from service context to service
     * context and at some point need to be reset back to the originator.
     */
    void resetResponseOwnership();

    // ********************************************************
    // Methods specific to reading/writing of bodies
    // ********************************************************

    /**
     * Query whether or not the entire outgoing message has been sent already.
     * 
     * @return boolean
     */
    boolean isMessageSent();

    /**
     * Query whether the entire incoming message (headers and body) has
     * been completely read yet.
     * 
     * @return boolean
     */
    boolean isIncomingMessageFullyRead();

    /**
     * Query the amount of bytes sent out so far with the outbound message
     * body. This does not include the amount of data for the message headers.
     * 
     * @return int (0 or more)
     */
    long getNumBytesWritten();

    // ********************************************************
    // connection specific methods
    // ********************************************************

    /**
     * Query whether an object is part of a persistent connection (or
     * wants to be possibly). Returns false if connection header equals
     * CLOSE and true otherwise. Use the getConnection() or getHeader()
     * APIs for more specific information.
     * 
     * @return boolean
     */
    boolean isPersistent();

    /**
     * Query whether this is part of a secure connection or not.
     * 
     * @return boolean
     */
    boolean isSecure();

    /**
     * Get the SSL context of the connection.
     * 
     * @return SSL connection context interface or null if no SSL is in
     *         the chain.
     */
    SSLConnectionContext getSSLContext();

    /**
     * Query whether or not the outgoing message is configured for auto zlib
     * encoding by the service context. If this is true, then the body buffers
     * provided by the caller are expected to be plain text and the channel
     * will call the compression methods to encode them. If this is false,
     * which is the default, then the body buffers will be sent out as-is with
     * no compression applied.
     * 
     * @return boolean
     */
    boolean isZlibEncoded();

    /**
     * This method will signify whether the caller wishes the service context to
     * apply zlib compression to the outgoing buffers. The default is false,
     * which means that no compression will be applied to the outgoing buffers
     * regardless of the Content-Encoding header value. If this is set to
     * true, then the service context will take the plain text buffers provided
     * by the caller and apply the zlib compression to them prior to sending
     * them out. It will verify the Content-Encoding header is correctly set
     * to match the encoding.
     * 
     * @param flag
     * @return boolean -- true means zlib compression was successfully enabled
     *         and false means it was not (i.e. it is not supported)
     */
    boolean setZlibEncoded(boolean flag);

    /**
     * Query whether this service context supports the automatic compression
     * of outgoing buffers using the zlib encoding format. If this returns
     * false, then any call to setZlibEncoded() will also return false.
     * 
     * @return boolean
     */
    boolean isZlibEncodingSupported();

    /**
     * Query whether or not the outgoing message is configured for auto gzip
     * encoding by the service context. If this is true, then the body buffers
     * provided by the caller are expected to be plain text and the channel
     * will call the compression methods to encode them. If this is false,
     * which is the default, then the body buffers will be sent out as-is with
     * no compression applied.
     * 
     * @return boolean
     */
    boolean isGZipEncoded();

    /**
     * This method will signify whether the caller wishes the service context to
     * apply gzip compression to the outgoing buffers. The default is false,
     * which means that no compression will be applied to the outgoing buffers
     * regardless of the Content-Encoding header value. If this is set to
     * true, then the service context will take the plain text buffers provided
     * by the caller and apply the gzip compression to them prior to sending
     * them out. It will verify the Content-Encoding header is correctly set
     * to match the encoding.
     * 
     * @param flag
     * @return boolean -- true means gzip compression was successfully enabled
     *         and false means it was not (i.e. it is not supported)
     */
    boolean setGZipEncoded(boolean flag);

    /**
     * Query whether this service context supports the automatic compression
     * of outgoing buffers using the gzip encoding format. If this returns
     * false, then any call to setGZipEncoded() will also return false.
     * 
     * @return boolean
     */
    boolean isGZipEncodingSupported();

    /**
     * Query whether or not the outgoing message is configured for auto x-gzip
     * encoding by the service context. If this is true, then the body buffers
     * provided by the caller are expected to be plain text and the channel
     * will call the compression methods to encode them. If this is false,
     * which is the default, then the body buffers will be sent out as-is with
     * no compression applied.
     * 
     * @return boolean
     */
    boolean isXGZipEncoded();

    /**
     * This method will signify whether the caller wishes the service context to
     * apply x-gzip compression to the outgoing buffers. The default is false,
     * which means that no compression will be applied to the outgoing buffers
     * regardless of the Content-Encoding header value. If this is set to
     * true, then the service context will take the plain text buffers provided
     * by the caller and apply the x-gzip compression to them prior to sending
     * them out. It will verify the Content-Encoding header is correctly set
     * to match the encoding.
     * 
     * @param flag
     * @return boolean -- true means x-gzip compression was successfully enabled
     *         and false means it was not (i.e. it is not supported)
     */
    boolean setXGZipEncoded(boolean flag);

    /**
     * Query whether this service context supports the automatic compression
     * of outgoing buffers using the x-gzip encoding format. If this returns
     * false, then any call to setXGZipEncoded() will also return false.
     * 
     * @return boolean
     */
    boolean isXGZipEncodingSupported();

    /**
     * The timeout used when reading data from the incoming message
     * can be changed from the default channel timeout by using this
     * method. This stays in effect until changed again or the
     * connection is closed.
     * <p>
     * Note that the input time is expected to be in milliseconds.
     * 
     * @param time
     *            (must not be less than HttpChannelConfig.MIN_TIMEOUT)
     * @throws IllegalArgumentException
     *             (if too low)
     */
    void setReadTimeout(int time) throws IllegalArgumentException;

    /**
     * The timeout used when writing data for the outgoing message
     * can be changed from the default channel timeout by using this
     * method. This stays in effect until changed again or the
     * connection is closed.
     * <p>
     * Note that the input time is expected to be in milliseconds.
     * 
     * @param time
     *            (must not be less than HttpChannelConfig.MIN_TIMEOUT)
     * @throws IllegalArgumentException
     *             (if too low)
     */
    void setWriteTimeout(int time) throws IllegalArgumentException;

    /**
     * Query the current value of the read timeout for incoming message data.
     * The integer returned is the timeout in milliseconds.
     * 
     * @return int
     */
    int getReadTimeout();

    /**
     * Query the current value of the write timeout for outgoing message data.
     * the integer returned is the timeout in milliseconds.
     * 
     * @return int
     */
    int getWriteTimeout();

    /**
     * Similar to the cancel read/write APIs, this is used to abort IO attempts
     * on the connection. Any current IO will be immediately timed out and any
     * future IO attempts will automatically received an IOException.
     */
    void abort();

    /**
     * Attempt to cancel any outstanding read being performed. This might include
     * a read for message headers or the message body. This is a best-effort
     * attempt as the read may already have been completed by the time of this
     * call. If it is successfully canceled, then it triggers an immediate
     * timeout. If the original read was synchronous then that call will throw
     * a timeout exception. If it was asynchronous then the original callback
     * will have it's error method triggered with a timeout exception.
     * 
     * @return boolean on whether the cancel attempt was made, if the
     *         implementation
     *         does not support canceling the read then false is returned.
     */
    boolean cancelOutstandingRead();

    /**
     * Attempt to cancel any outstanding write being performed. This might include
     * a write of message headers or the message body. This is a best-effort
     * attempt as the write may already have been completed by the time of this
     * call. If it is successfully canceled, then it triggers an immediate
     * timeout. If the original write was synchronous then that call will throw
     * a timeout exception. If it was asynchronous then the original callback
     * will have it's error method triggered with a timeout exception.
     * 
     * @return boolean on whether the cancel attempt was made, if the
     *         implementation
     *         does not support canceling the write then false is returned.
     */
    boolean cancelOutstandingWrite();

    // ********************************************************
    // service context specific methods
    // ********************************************************

    /**
     * Clear out a service context for re-use.
     * 
     */
    void clear();

    /**
     * Returns the address of the remote end of this connection, could
     * be the client that created the inbound request or a server that
     * is the target of the outbound request.
     * 
     * @return InetAddress
     */
    InetAddress getRemoteAddr();

    /**
     * Returns the address of the local side of the connection.
     * 
     * @return InetAddress
     */
    InetAddress getLocalAddr();

    /**
     * Query the value of the port at the remote end of the connection.
     * 
     * @return int
     */
    int getRemotePort();

    /**
     * Returns the local server port (9080, etc).
     * 
     * @return int
     */
    int getLocalPort();

    /**
     * Access the date format utility class. This includes support for the various
     * HTTP spec date styles.
     * 
     * @return HttpDateFormat
     */
    HttpDateFormat getDateFormatter();

    /**
     * Access the string encoding utility class.
     * 
     * @return EncodingUtils
     */
    EncodingUtils getEncodingUtils();

    /**
     * Set the start time for this request
     */
    void setStartTime();

    /**
     * @return request start time with nanosecond precision
     */
    long getStartNanoTime();

    /**
     * Reset the start time for this request
     */
    void resetStartTime();
}
