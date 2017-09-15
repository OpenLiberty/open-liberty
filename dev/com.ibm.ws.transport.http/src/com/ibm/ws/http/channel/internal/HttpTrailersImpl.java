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
package com.ibm.ws.http.channel.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.BNFHeadersImpl;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.HttpTrailerGenerator;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * Class to store the "name: value" pairs for the HTTP trailer headers
 * 
 * Note there is no support for cookies in this implementation of the
 * HttpTrailers interface. A cookie header can be present in an http
 * trailer. The get/set and removal of such cookie headers is
 * currently not supported.
 * 
 */
public class HttpTrailersImpl extends BNFHeadersImpl implements HttpTrailers {

    /** Standard trace registration. */
    private static final TraceComponent tc = Tr.register(HttpTrailersImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Serialization ID field */
    private static final long serialVersionUID = -4872702714523122647L;

    /** Reference to the HTTP factory */
    private transient HttpObjectFactory myFactory = null;
    /**
     * Store all the known headers(key) and
     * their respective trailer generators(value)
     */
    private transient Map<HeaderKeys, HttpTrailerGenerator> knownTGs = new HashMap<HeaderKeys, HttpTrailerGenerator>();

    /**
     * Constructor for the trailer headers object.
     */
    public HttpTrailersImpl() {
        super();
    }

    /**
     * Initialize this trailer header storage object with certain
     * configuration information.
     * 
     * @param useDirect
     * @param outSize
     * @param inSize
     * @param cacheSize
     */
    public void init(boolean useDirect, int outSize, int inSize, int cacheSize) {
        super.init(useDirect, outSize, inSize, cacheSize);
    }

    /**
     * Set the factory that this object will use.
     * 
     * @param fact
     */
    public void setFactory(HttpObjectFactory fact) {
        this.myFactory = fact;
    }

    // *********************************************************
    // Methods for manipulating the trailers
    // *********************************************************

    /**
     * Query whether there is a deferred trailer header of the given name.
     * 
     * @param target
     * @return boolean (true if exists)
     */
    public boolean containsDeferredTrailer(String target) {
        return containsDeferredTrailer(findKey(target));
    }

    /**
     * Query whether there is a deferred trailer header of the given name.
     * 
     * @param target
     * @return boolean (true if exists)
     */
    public boolean containsDeferredTrailer(HeaderKeys target) {
        if (null == target) {
            return false;
        }
        return this.knownTGs.containsKey(target);
    }

    /**
     * Set a trailer based upon a not-yet established value.
     * 
     * When the deferred trailer is set, it is the users responsibility to
     * synchronize the deferred trailer list with the Trailer header field
     * up front. For instance if one sets the deferred trailer
     * HDR_CONTENT_LANGUAGE, then the trailer header in the head of the HTTP
     * request/response should contain "Trailer:Content-Language"
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
    public void setDeferredTrailer(HeaderKeys hdr, HttpTrailerGenerator htg) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setDeferredTrailer(HeaderKeys): " + hdr);
        }
        if (null == hdr) {
            throw new IllegalArgumentException("Null header name");
        }
        if (null == htg) {
            throw new IllegalArgumentException("Null value generator");
        }

        this.knownTGs.put(hdr, htg);
    }

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
     *            immedietly after the 0-size chunk is sent, but before closing or
     *            recycling the connection.
     * @throws IllegalArgumentException
     *             if any parameter is NULL or if the
     *             header represented is unsupported.
     */
    public void setDeferredTrailer(String hdr, HttpTrailerGenerator htg) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setDeferredTrailer(String): " + hdr);
        }
        if (null == hdr) {
            throw new IllegalArgumentException("Null header name");
        }
        if (null == htg) {
            throw new IllegalArgumentException("Null value generator");
        }
        this.knownTGs.put(findKey(hdr), htg);
    }

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
    public void removeDeferredTrailer(String hdr) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "removeDeferredTrailer(String): " + hdr);
        }
        if (null == hdr) {
            throw new IllegalArgumentException("Null header name");
        }
        this.knownTGs.remove(findKey(hdr));
    }

    /**
     * Remove a deferred trailer from the current list of trailers to generate
     * when serializing this object.
     * 
     * The user is responsible for maintaining consistency between the trailer
     * header field at the head of the HTTP request/response with the deferred
     * trailer list. For instance after removeDeferredTrailer(
     * HDR_CONTENT_LANGUAGE), is called the trailer header field should not
     * contain "Content-Language".
     * 
     * @param hdr
     *            the trailer to remove (i.e. 'Date').
     * @throws IllegalArgumentException
     *             if hdr is NULL.
     */
    public void removeDeferredTrailer(HeaderKeys hdr) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "removeDeferredTrailer(HeaderKeys): " + hdr);
        }
        if (null == hdr) {
            throw new IllegalArgumentException("Null header name");
        }
        this.knownTGs.remove(hdr);
    }

    /**
     * Compute all deferred headers.
     * 
     * <p>
     * All <code>HttpTrailerGenerator</code> will be called upon to create the value for their respective header. The values they create will automatically be added to the
     * Trailer-based <code>BNFHeaders</code> object for immediate serialization.
     * </p>
     * 
     */
    public void computeRemainingTrailers() {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "computeRemainingTrailers");
        }
        Iterator<HeaderKeys> knowns = this.knownTGs.keySet().iterator();
        while (knowns.hasNext()) {
            HeaderKeys key = knowns.next();
            setHeader(key, this.knownTGs.get(key).generateTrailerValue(key, this));
        }
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "computeRemainingTrailers");
        }
    }

    // ****************************************************************
    // Trailer object specific methods
    // ****************************************************************

    /**
     * Destroy this object.
     */
    public void destroy() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroy trailers: " + this);
        }
        super.destroy();
        if (null != this.myFactory) {
            this.myFactory.releaseTrailers(this);
            this.myFactory = null;
        }
    }

    /**
     * Create a duplicate version of these trailer headers.
     * 
     * @return HttpTrailersImpl
     */
    public HttpTrailersImpl duplicate() {
        if (null == this.myFactory) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Null factory, unable to duplicate: " + this);
            }
            return null;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Duplicating the trailer headers: " + this);
        }
        computeRemainingTrailers();
        HttpTrailersImpl msg = this.myFactory.getTrailers();
        super.duplicate(msg);
        return msg;
    }

    /**
     * Read an instance of this object from the input stream.
     * 
     * @param s
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void readExternal(ObjectInput s) throws IOException, ClassNotFoundException {

        super.readExternal(s);
        // nothing extra at this layer
    }

    /**
     * Write this object instance to the output stream.
     * 
     * @param s
     * @throws IOException
     */
    public void writeExternal(ObjectOutput s) throws IOException {
        // finish any outstanding trailers and then tell the BNF layer to write
        // them out
        computeRemainingTrailers();
        super.writeExternal(s);
    }

    /**
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#findKey(byte[], int, int)
     */
    protected HeaderKeys findKey(byte[] data, int offset, int length) {
        return HttpHeaderKeys.find(data, offset, length);
    }

    /**
     * see com.ibm.ws.genericbnf.impl.BNFHeadersImpl#findKey(byte[])
     */
    protected HeaderKeys findKey(byte[] name) {
        return HttpHeaderKeys.find(name, 0, name.length);
    }

    /**
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#findKey(java.lang.String)
     */
    protected HeaderKeys findKey(String name) {
        return HttpHeaderKeys.find(name);
    }

}
