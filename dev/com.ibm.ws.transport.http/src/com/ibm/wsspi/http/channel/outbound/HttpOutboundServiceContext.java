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
package com.ibm.wsspi.http.channel.outbound;

import java.io.IOException;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.exception.IllegalRequestObjectException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.exception.BodyCompleteException;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * This is the interface for an outbound http connection.
 * 
 * @ibm-private-in-use
 */
public interface HttpOutboundServiceContext extends HttpServiceContext {

    /**
     * Query the target address of this connection.
     * 
     * @return HttpAddress
     */
    HttpAddress getTargetAddress();

    /**
     * Set the request object this service context.
     * 
     * @param msg
     * @throws IllegalRequestObjectException
     */
    void setRequest(HttpRequestMessage msg) throws IllegalRequestObjectException;

    /**
     * When an application channel is about to place an non-active outbound
     * connection into their connection pool, they should call this method to
     * start a read on the socket. This will allow detection if the server
     * closes the socket while the outbound connection is in the pool, and can
     * then be properly cleaned up without wasted time trying to send the next
     * request on a dead socket.
     * <p>
     * <br>
     * If the callback's error method is used, then the application channel should
     * call close on the connection as it is no longer valid for use.
     * <p>
     * 
     * @param cb
     *            - application channel's callback
     * @param timeout
     *            - timeout in milliseconds to use on the read, use -1 to
     *            trigger the use of the standard read timeout on this HTTP channel.
     *            Typically, this timeout should be in the 1 to 5 minute range.
     * @return boolean - if this was called while a connection was still actively
     *         being used (i.e. before the clear() method is called) then this
     *         will
     *         return false and will not start the read ahead.
     * @see HttpOutboundServiceContext#init
     */
    boolean registerReadAhead(InterChannelCallback cb, int timeout);

    /**
     * When an outbound connection is taken from the application channel's
     * outbound pool, this method must be called in order to notify the HTTP
     * channel that the connection is now active again. The primary use is to
     * reset the read timeout to the regular HTTP timeout value, instead of
     * being offset by however much time it spent dormant in the pool.
     * 
     * @return boolean - if this returns false, then the connection was marked
     *         as dead by the read-ahead logic and should not be used by the
     *         application
     *         channel.
     * @see HttpOutboundServiceContext#registerReadAhead
     */
    boolean init();

    /**
     * This method will cause this outbound connection to start an immediate
     * read for the response headers as soon as the request headers are sent,
     * instead of waiting for the entire request message to go out. By doing
     * this, it allows the headers to be exchanged and then the bodies to be
     * sent back and forth, intermixed with each other.<br>
     * <p>
     * The application channel must call this method and then use one of the
     * sendRequestHeaders APIs. When that write completes, the read for the
     * response headers will begin. The sendRequestHeaders API will not return or
     * use the application callback until that read/parse completes. Any error
     * during the write, the read, or the parsing, will be handed to the caller as
     * an error from sendRequestHeaders.<br>
     * 
     * @return boolean (true means success)
     */
    boolean enableImmediateResponseRead();

    /**
     * If the application channel wants to see any temporary responses received,
     * any 1xx responses, then this API provides access for the channel. Each
     * time this is used, the next response message that is parsed will trigger
     * the callback on the application channel which can then access the message
     * through getResponse(). If the final response is already parsed, then the
     * caller is immediately notified and no read is started.
     * <p>
     * The default behavior is that finishRequestMessage() will not return until
     * the final response has been received. Once this method is used on a
     * connection, the finishRequestMessage() APIs will return as soon as the
     * write completes. It no longer indicates a response is ready, as the read
     * callback handed to this method will be used when the next response is ready
     * for the caller to inspect.
     * 
     * @param cb
     * @param forceQueue
     * @return VirtualConnection - if null, then the callback will be used at a
     *         later point. If non-null, then a response message is already parsed
     *         and
     *         available.
     * @throws NullPointerException
     *             if the input callback is null
     */
    VirtualConnection readNextResponse(InterChannelCallback cb, boolean forceQueue);

    /**
     * If the application channel wants to start a read for the response headers
     * prior to sending the final request data, this API will let them register
     * that interest. If a non-temporary response arrives (any non-1xx status
     * code),
     * then the input callback will be notified of that message. This runs
     * independant of the outgoing request message state. Once registered, the
     * behavior of the finishRequestMessage APIs change such that the completion
     * of those APIs indicates the state of the final write only, it no longer
     * has any bearing on the state of the response message. The caller must wait
     * for their callback to be used to indicate the response message has arrived
     * or failed.<br>
     * 
     * @param cb
     * @see HttpOutboundServiceContext#deregisterEarlyRead
     */
    void registerEarlyRead(InterChannelCallback cb);

    /**
     * If the application channel has previously registered for the early read
     * interest, this API will allow them to cancel that interest. If an error
     * happens during the handling of the message, the caller might want to
     * shut down the outbound connection and must either wait for the early read
     * to complete or use this API to stop that read. This API does not mean that
     * the caller has to close the connection, just that they are no longer
     * interested in early read message notification. If used, the
     * finishRequestMessage
     * API will revert back to standard behavior of writing the final data and
     * starting the read for the response, which means that the return code
     * status from finishRequestMessage will again indicate the success or failure
     * of reading the response message.<br>
     * 
     * @return boolean - true means it was successfully disabled, false means that
     *         it already fired or was in the process of firing and thus could not
     *         be
     *         disabled, or was never registered in the first place.
     */
    boolean deregisterEarlyRead();

    // ********************************************************
    // Sending body buffer methods
    // ********************************************************

    /**
     * Send the headers for the outgoing request synchronously.
     * 
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if the headers have already been sent
     */
    void sendRequestHeaders() throws IOException, MessageSentException;

    /**
     * Send the headers for the outgoing request asynchronously.
     * 
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     * 
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return
     * code will always be null and the callback always used.
     * 
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if the headers have already been sent
     */
    VirtualConnection sendRequestHeaders(InterChannelCallback cb, boolean bForce) throws MessageSentException;

    /**
     * Send the given body buffers for the outgoing request synchronously.
     * If chunked encoding is set, then each call to this method will be
     * considered a "chunk" and encoded as such. If the message is
     * Content-Length defined, then the buffers will simply be sent out with no
     * modifications.
     * 
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers.
     * 
     * @param body
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    void sendRequestBody(WsByteBuffer[] body) throws IOException, MessageSentException;

    /**
     * Send the given body buffers for the outgoing request asynchronously.
     * If chunked encoding is set, then each call to this method will be
     * considered a "chunk" and encoded as such. If the message is
     * Content-Length defined, then the buffers will simply be sent out with no
     * modifications.
     * 
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers.
     * 
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     * 
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return
     * code will always be null and the callback always used.
     * 
     * @param body
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    VirtualConnection sendRequestBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException;

    /**
     * Send an array of raw body buffers out synchronously. This method will
     * avoid any body modifications, such as compression or chunked-encoding.
     * 
     * @param body
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    void sendRawRequestBody(WsByteBuffer[] body) throws IOException, MessageSentException;

    /**
     * Send an array of raw body buffers out asynchronously. This method will
     * avoid any body modifications, such as compression or chunked-encoding.
     * <p>
     * This will return null if the data is being written asynchronously and the
     * provided callback will be used when finished. However, if the write could
     * complete automatically, then the callback will not be used and instead a
     * non-null VC will be returned back.
     * <p>
     * The force parameter allows the caller to force the asynchronous call and to
     * always have the callback used, thus the return code will always be null.
     * 
     * @param body
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    VirtualConnection sendRawRequestBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException;

    /**
     * Send the given body buffers for the outgoing request synchronously.
     * If chunked encoding is set, then these buffers will be considered a
     * "chunk" and encoded as such. If the message is Content-Length defined,
     * then the buffers will simply be sent out with no modifications. This
     * marks the end of the outgoing message. This method will return when the
     * response has been received and parsed.
     * 
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers. If this was a chunked encoded
     * message, then the zero-length chunk is automatically appended.
     * 
     * @param body
     *            (last set of buffers to send, null if no body data)
     * @throws IOException
     *             -- if a socket error occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    void finishRequestMessage(WsByteBuffer[] body) throws IOException, MessageSentException;

    /**
     * Send the given body buffers for the outgoing request asynchronously.
     * If chunked encoding is set, then these buffers will be considered a
     * "chunk" and encoded as such. If the message is Content-Length defined,
     * then the buffers will simply be sent out with no modifications. This
     * marks the end of the outgoing message. The callback will be called when
     * the response has been received and parsed.
     * 
     * Note: if headers have not already been sent, then the first call to
     * this method will send the headers. If this was a chunked encoded
     * message, then the zero-length chunk is automatically appended.
     * 
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible
     * for handling that situation in their code. A null return code means
     * that the async write is in progress.
     * 
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return
     * code will always be null and the callback always used.
     * 
     * @param body
     *            (last set of body data, null if no body information)
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    VirtualConnection finishRequestMessage(WsByteBuffer[] body, InterChannelCallback cb, boolean bForce) throws MessageSentException;

    /**
     * Finish sending the request message with the optional input body buffers.
     * These can be null if there is no more actual body data to send. This
     * method will avoid any body modification, such as compression or chunked
     * encoding and simply send the buffers as-is. If the headers have not
     * been sent yet, then they will be prepended to the input data.
     * <p>
     * This method will return when the response has been received and the headers
     * parsed.
     * 
     * @param body
     *            -- null if there is no body data
     * @throws IOException
     *             -- if a socket exception occurs
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    void finishRawRequestMessage(WsByteBuffer[] body) throws IOException, MessageSentException;

    /**
     * Finish sending the request message asynchronously. The body buffers can
     * be null if there is no more actual body. This method will avoid any
     * body modifications, such as compression or chunked-encoding. If the
     * headers have not been sent yet, then they will be prepended to the input
     * data.
     * <p>
     * If the asynchronous write and the read of the response can be done
     * immediately, then this will return a VirtualConnection and the caller's
     * callback will not be used. If this returns null, then the callback will be
     * used when the response is received and parsed.
     * <p>
     * The force flag allows the caller to force the asynchronous communication
     * such that the callback is always used.
     * 
     * @param body
     *            -- null if there is no more body data
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *             -- if a finishMessage API was already used
     */
    VirtualConnection finishRawRequestMessage(WsByteBuffer[] body, InterChannelCallback cb, boolean bForce) throws MessageSentException;

    /**
     * If the headers, plus optional body buffers, are being written and an
     * error occurs, the channel will attempt a reconnect to the target and
     * resend of those buffers. This will avoid the overhead of the caller
     * performing the reconnect and the outbound request being re-marshalled.
     * The rewrite will only be attempted once per send call, and only when
     * the headers are present in the buffers. If the headers have already
     * been sent, then the reconnect is impossible without the re-marshalling
     * of the request headers.
     * <p>
     * This method will let the caller prevent the reconnect/rewrite if they
     * choose. Note that this affects this single connection only and not any
     * other outbound request connection.
     * 
     * @return boolean (true means success)
     */
    boolean disallowRewrites();

    /**
     * If the headers, plus optional body buffers, are being written and an
     * error occurs, the channel will attempt a reconnect to the target and
     * resend of those buffers. This will avoid the overhead of the caller
     * performing the reconnect and the outbound request being re-marshalled.
     * The rewrite will only be attempted once per send call, and only when
     * the headers are present in the buffers. If the headers have already
     * been sent, then the reconnect is impossible without the re-marshalling
     * of the request headers.
     * <p>
     * This method will let the caller enable the reconnect/rewrites if they were
     * previously disabled through the disallowRewrites() API. This affects this
     * single connection only and will stay in effect until turned off at a later
     * point. This will also allow the caller to override the channel
     * configuration option for this single connection.
     * 
     * @return boolean (true means success)
     */
    boolean allowRewrites();

    // ********************************************************
    // Retrieving body buffer methods
    // ********************************************************

    /**
     * Get all of the remaining body buffers for the response synchronously.
     * If the body is encoded or compressed, then that is removed and
     * unencoded buffers returned.
     * <p>
     * A null response means that there is no body left to read.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     * 
     * @return WsByteBuffer[]
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    WsByteBuffer[] getResponseBodyBuffers() throws IOException, IllegalHttpBodyException;

    /**
     * Read in all of the body buffers for the response asynchronously.
     * If the body is encoded or compressed, then that is removed and
     * unencoded buffers returned. Null callbacks are not allowed and
     * will trigger a NullPointerException.
     * <p>
     * If the asynchronous request is fulfilled on the same thread, then this
     * connection's VirtualConnection will be returned and the callback will not
     * be used. A null return code means that an asynchronous read is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     * 
     * @param callback
     * @param bForce
     * @return VirtualConnection (null if an async read is in progress,
     *         non-null if data is ready)
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    VirtualConnection getResponseBodyBuffers(InterChannelCallback callback, boolean bForce) throws BodyCompleteException;

    /**
     * This gets the next body buffer. If the body is encoded/compressed,
     * then the encoding is removed and the "next" buffer returned.
     * <p>
     * A null response means that there is no body left to read.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     * <p>
     * 
     * @return WsByteBuffer
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    WsByteBuffer getResponseBodyBuffer() throws IOException, IllegalHttpBodyException;

    /**
     * This gets the next body buffer asynchronously. If the body is encoded
     * or compressed, then the encoding is removed and the "next" buffer
     * returned. Null callbacks are not allowed and will trigger a
     * NullPointerException.
     * <p>
     * If the asynchronous request is fulfilled on the same thread, then this
     * connection's VirtualConnection will be returned and the callback will not
     * be used. A null return code means that an asynchronous read is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     * 
     * @param callback
     * @param bForce
     * @return VirtualConnection (null if an async read is in progress,
     *         non-null if data is ready)
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    VirtualConnection getResponseBodyBuffer(InterChannelCallback callback, boolean bForce) throws BodyCompleteException;

    /**
     * Retrieve the next buffer of the response message's body. This will give
     * the buffer without any modifications, avoiding decompression or chunked
     * encoding removal.
     * <p>
     * A null buffer will be returned if there is no more data to get.
     * <p>
     * The caller is responsible for releasing these buffers when complete as the
     * HTTP Channel does not keep track of them.
     * 
     * @return WsByteBuffer
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    WsByteBuffer getRawResponseBodyBuffer() throws IOException, IllegalHttpBodyException;

    /**
     * Retrieve all remaining buffers of the response message's body. This will
     * give the buffers without any modifications, avoiding decompression or
     * chunked encoding removal.
     * <p>
     * A null buffer array will be returned if there is no more data to get.
     * <p>
     * The caller is responsible for releasing these buffers when complete as the
     * HTTP Channel does not keep track of them.
     * 
     * @return WsByteBuffer[]
     * @throws IOException
     *             -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *             -- if the body was malformed
     */
    WsByteBuffer[] getRawResponseBodyBuffers() throws IOException, IllegalHttpBodyException;

    /**
     * Retrieve the next buffer of the body asynchronously. This will avoid any
     * body modifications, such as decompression or removal of chunked-encoding
     * markers.
     * <p>
     * If the read can be performed immediately, then a VirtualConnection will be
     * returned and the provided callback will not be used. If the read is being
     * done asychronously, then null will be returned and the callback used when
     * complete. The force input flag allows the caller to force the asynchronous
     * read to always occur, and thus the callback to always be used.
     * <p>
     * The caller is responsible for releasing these buffers when finished with
     * them as the HTTP Channel keeps no reference to them.
     * 
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    VirtualConnection getRawResponseBodyBuffer(InterChannelCallback cb, boolean bForce) throws BodyCompleteException;

    /**
     * Retrieve any remaining buffers of the body asynchronously. This will
     * avoid any body modifications, such as decompression or removal of
     * chunked-encoding markers.
     * <p>
     * If the read can be performed immediately, then a VirtualConnection will be
     * returned and the provided callback will not be used. If the read is being
     * done asychronously, then null will be returned and the callback used when
     * complete. The force input flag allows the caller to force the asynchronous
     * read to always occur, and thus the callback to always be used.
     * <p>
     * The caller is responsible for releasing these buffers when finished with
     * them as the HTTP Channel keeps no reference to them.
     * 
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws BodyCompleteException
     *             -- if the entire body has already been read
     */
    VirtualConnection getRawResponseBodyBuffers(InterChannelCallback cb, boolean bForce) throws BodyCompleteException;

    OutboundConnectionLink getLink();

    TCPConnectionContext getTSC();

    /**
     * When an an 101 upgraded response is returned, a read might return header data
     * and the first part of the upgraded response.  This method makes that extra
     * data available.
     */
    WsByteBuffer getRemainingData();

}
