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

import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.HeaderStorage;

/**
 * Object to handle the HTTP trailer headers. This allows controling
 * the deferred headers, as well as controling specific headers
 * immediately (i.e deferring creation of a Date header versus setting
 * the Date right now).
 *
 * Deferred headers will be figured out at the actual marshalling time
 * or when explicitly triggered. This contains several APIs for setting,
 * removing, or querying the existance of deferred headers.
 *
 * @ibm-private-in-use
 */
public interface HttpTrailers extends HeaderStorage {

    // *********************************************************
    // Methods for manipulating the trailers
    // *********************************************************

    /**
     * Query whether there is a deferred trailer header of the given name.
     *
     * @param target
     * @return boolean (true if exists)
     */
    boolean containsDeferredTrailer(String target);

    /**
     * Query whether there is a deferred trailer header of the given name.
     *
     * @param target
     * @return boolean (true if exists)
     */
    boolean containsDeferredTrailer(HeaderKeys target);

    /**
     * Set a trailer based upon a not-yet established value.
     *
     * When the deferred trailer is set, it is the users responsibilityto
     * synchronize the deferred trailer list with the Trailer header field
     * upfront. For instance if one sets the deferred trailer
     * HDR_CONTENT_LANGUAGE, then the trailer header in the head of the HTTP
     * request/response should contain "Trailer: Content-Language"
     *
     * @param hdr
     *            the header to use.
     * @param htg
     *            the object which will generate the value for this trailer
     *            dynamically. An <code>HttpTrailerGenerator</code> is called
     *            immediately after the 0-size chunk is sent, but before closing
     *            or recycling the connection.
     * @throws IllegalArgumentException
     *             if any parameter is NULL or if the
     *             header represented is unsupported.
     */
    void setDeferredTrailer(HeaderKeys hdr, HttpTrailerGenerator htg);

    /**
     * Set a trailer based upon a not-yet established value.
     *
     * When the deferred trailer is set, it is the users responsibility
     * to synchronize the deferred trailer list with the Trailer header field.
     * For instance if one sets the deferred trailer "test1", then the trailer
     * header in the outbound HTTP Request/Response should contain,
     * "Trailer: test1"
     *
     * @param hdr
     *            the header to use.
     * @param htg
     *            the object which will generate the value for this trailer
     *            dynamically. An <code>HttpTrailerGenerator</code> is called
     *            immediately after the 0-size chunk is sent, but before closing
     *            or recycling the connection.
     * @throws IllegalArgumentException
     *             if any parameter is NULL or if the
     *             header represented is unsupported.
     */
    void setDeferredTrailer(String hdr, HttpTrailerGenerator htg);

    /**
     * Remove a deferred trailer from the current list of trailers to generate
     * when serializing this object.
     *
     * The user is responsible for maintaining consistency between the trailer
     * header field at the head of the HTTP request/response with the deferred
     * trailer list. For instance after removeDeferredTrailer("test1"), is
     * called the trailer header field should not contain "test1".
     *
     * @param hdr
     *            the trailer name to remove (i.e. 'Date').
     * @throws IllegalArgumentException
     *             if hdr is NULL.
     */
    void removeDeferredTrailer(String hdr);

    /**
     * Remove a deferred trailer from the current list of trailers to generate
     * when serializing this object.
     *
     * The user is responsible for maintaining consistency between the trailer
     * header field at the head of the HTTP request/response with the deferred
     * trailer list. For instance after removeDeferredTrailer(HDR_CONTENT_-
     * LANGUAGE), is called the trailer header field should not contain
     * "Content-Language".
     *
     * @param hdr
     *            the trailer to remove (i.e. 'Date').
     * @throws IllegalArgumentException
     *             if hdr is NULL.
     */
    void removeDeferredTrailer(HeaderKeys hdr);

    /**
     * Compute all deferred headers.
     *
     * <p>
     * All <code>HttpTrailerGenerator</code> will be called upon to create the
     * value for their respective header. The values they create will
     * automatically be added to the Trailer-based <code>BNFHeaders</code> object
     * for immediate serialization.
     * </p>
     *
     */
    void computeRemainingTrailers();

    // ****************************************************************
    // Trailer object specific methods
    // ****************************************************************

    /**
     * clear out these headers for reuse.
     *
     */
    void clear();

}
