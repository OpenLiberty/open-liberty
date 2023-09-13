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
package com.ibm.wsspi.http.channel.inbound;

import java.io.IOException;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.exception.IllegalResponseObjectException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.HttpServiceContext;
import com.ibm.wsspi.http.channel.error.HttpError;
import com.ibm.wsspi.http.channel.exception.BodyCompleteException;
import com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException;

/**
 * This is the interface for an inbound Http connection.
 *
 * @ibm-private-in-use
 */
public interface HttpInboundServiceContext extends HttpServiceContext {

    /**
     * Set the response object in the service context for usage.
     *
     * @param msg
     * @throws IllegalResponseObjectException
     */
    void setResponse(HttpResponseMessage msg) throws IllegalResponseObjectException;

    // ********************************************************
    // Sending body buffer methods
    // ********************************************************

    /**
     * Send the headers for the outgoing response synchronously.
     *
     * @throws IOException
     *                                  -- if a socket exception occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    void sendResponseHeaders() throws IOException, MessageSentException;

    /**
     * Send the headers for the outgoing response asynchronously. The
     * callback will be called when finished.
     * <p>
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible for
     * handling that situation in their code. A null return code means that the
     * async write is in progress.
     * <p.>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     *
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    VirtualConnection sendResponseHeaders(InterChannelCallback cb, boolean bForce) throws MessageSentException;

    /**
     * Send the given body buffers for the outgoing response synchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * each call to this method will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out without modifications.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers.
     *
     * @param body
     * @throws IOException
     *                                  -- if a socket exception occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    void sendResponseBody(WsByteBuffer[] body) throws IOException, MessageSentException;

    /**
     * Send the given body buffers for the outgoing response asynchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * each call to this method will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out with no modifications.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers.
     * <p>
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible for
     * handling that situation in their code. A null return code means that the
     * async write is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     *
     * @param body
     * @param callback
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    VirtualConnection sendResponseBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException;

    /**
     * Send an array of raw body buffers out synchronously. This method will
     * avoid any body modifications, such as compression or chunked-encoding.
     *
     * @param body
     * @throws IOException
     *                                  -- if a socket error occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    void sendRawResponseBody(WsByteBuffer[] body) throws IOException, MessageSentException;

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
     *                                  -- if a finishMessage API was already used
     */
    VirtualConnection sendRawResponseBody(WsByteBuffer[] body, InterChannelCallback callback, boolean bForce) throws MessageSentException;

    /**
     * Send the given body buffers for the outgoing response synchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * these buffers will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out with no modifications. This marks the end
     * of the outgoing message.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers. If this was a chunked encoded message, then
     * the zero-length chunk is automatically appended.
     *
     * @param body
     *                 (last set of buffers to send, null if no body data)
     * @throws IOException
     *                                  -- if a socket exception occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    void finishResponseMessage(WsByteBuffer[] body) throws IOException, MessageSentException;

    /**
     * Send the given body buffers for the outgoing response asynchronously.
     * If chunked encoding is set or if the partialbody flag is set, then
     * these buffers will be considered a "chunk" and encoded
     * as such. If the message is Content-Length defined, then the buffers
     * will simply be sent out with no modifications. This marks the end
     * of the outgoing message.
     * <p>
     * Note: if headers have not already been sent, then the first call to this
     * method will send the headers. If this was a chunked encoded message, then
     * the zero-length chunk is automatically appended.
     * <p>
     * If the write can be done immediately, the VirtualConnection will be
     * returned and the callback will not be used. The caller is responsible for
     * handling that situation in their code. A null return code means that the
     * async write is in progress.
     * <p>
     * The boolean bForce parameter allows the caller to force the asynchronous
     * action even if it could be handled immediately. The return code will always
     * be null and the callback always used.
     *
     * @param body
     *                   (last set of body data, null if no body information)
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    VirtualConnection finishResponseMessage(WsByteBuffer[] body, InterChannelCallback cb, boolean bForce) throws MessageSentException;

    /**
     * Finish sending the response message with the optional input body buffers.
     * These can be null if there is no more actual body data to send. This
     * method will avoid any body modification, such as compression or chunked
     * encoding and simply send the buffers as-is. If the headers have not
     * been sent yet, then they will be prepended to the input data.
     *
     * @param body
     *                 -- null if there is no body data
     * @throws IOException
     *                                  -- if a socket exception occurs
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    void finishRawResponseMessage(WsByteBuffer[] body) throws IOException, MessageSentException;

    /**
     * Finish sending the response message asynchronously. The body buffers can
     * be null if there is no more actual body. This method will avoid any
     * body modifications, such as compression or chunked-encoding. If the
     * headers have not been sent yet, then they will be prepended to the input
     * data.
     * <p>
     * If the asynchronous write can be done immediately, then this will return a
     * VirtualConnection and the caller's callback will not be used. If this
     * returns null, then the callback will be used when complete.
     * <p>
     * The force flag allows the caller to force the asynchronous communication
     * such that the callback is always used.
     *
     * @param body
     *                   -- null if there is no more body data
     * @param cb
     * @param bForce
     * @return VirtualConnection
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    VirtualConnection finishRawResponseMessage(WsByteBuffer[] body, InterChannelCallback cb, boolean bForce) throws MessageSentException;

    /**
     * Sends an error code and page back to the client asynchronously and
     * closes the connection.
     *
     * @param error
     * @throws MessageSentException
     *                                  -- if a finishMessage API was already used
     */
    void sendError(HttpError error) throws MessageSentException;

    // ********************************************************
    // Retrieving body buffer methods
    // ********************************************************

    /**
     * Get all of the remaining body buffers for the request synchronously.
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
     *                                      -- if a socket exception happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */
    WsByteBuffer[] getRequestBodyBuffers() throws IOException, IllegalHttpBodyException;

    /**
     * Read in all of the body buffers for the request asynchronously.
     * If the body is encoded or compressed, then that is removed and
     * unencoded buffers returned.
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
     *                                   -- if the entire body has already been read
     */
    VirtualConnection getRequestBodyBuffers(InterChannelCallback callback, boolean bForce) throws BodyCompleteException;

    /**
     * This gets the next body buffer. If the body is encoded/compressed,
     * then the encoding is removed and the "next" buffer returned.
     * <p>
     * A null response means that there is no body left to read.
     * <p>
     * Once given a buffer, we keep no record of that buffer. It is the users
     * responsibility to release it.
     *
     * @return WsByteBuffer
     * @throws IOException
     *                                      -- if a socket exception happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */
    WsByteBuffer getRequestBodyBuffer() throws IOException, IllegalHttpBodyException;

    /**
     * This gets the next body buffer asynchronously. If the body is encoded
     * or compressed, then the encoding is removed and the "next" buffer
     * returned. Null callbacks are not allowed and will trigger a null pointer
     * exception.
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
     *                                   -- if the entire body has already been read
     */
    VirtualConnection getRequestBodyBuffer(InterChannelCallback callback, boolean bForce) throws BodyCompleteException;

    /**
     * Retrieve the next buffer of the request message's body. This will give
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
     *                                      -- if a socket exception happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */
    WsByteBuffer getRawRequestBodyBuffer() throws IOException, IllegalHttpBodyException;

    /**
     * Retrieve all remaining buffers of the request message's body. This will
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
     *                                      -- if a socket exceptions happens
     * @throws IllegalHttpBodyException
     *                                      -- if a malformed request body is
     *                                      present such that the server should send an HTTP 400 Bad Request
     *                                      back to the client.
     */
    WsByteBuffer[] getRawRequestBodyBuffers() throws IOException, IllegalHttpBodyException;

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
     *                                   -- if the entire body has already been read
     */
    VirtualConnection getRawRequestBodyBuffer(InterChannelCallback cb, boolean bForce) throws BodyCompleteException;

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
     *                                   -- if the entire body has already been read
     */
    VirtualConnection getRawRequestBodyBuffers(InterChannelCallback cb, boolean bForce) throws BodyCompleteException;

    /**
     * @return
     */
    boolean useForwardedHeadersInAccessLog();

    /**
     * @return
     */
    String getForwardedRemoteProto();

    /**
     * @return
     */
    String getForwardedRemoteAddress();

    /**
     * @return
     */
    String getForwardedRemoteHost();

}
