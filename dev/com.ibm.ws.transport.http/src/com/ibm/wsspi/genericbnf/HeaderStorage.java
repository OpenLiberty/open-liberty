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
package com.ibm.wsspi.genericbnf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This interface is for the storage of headers that are in the Augmented BNF
 * format, which looks like "Name: value". This provides the various methods
 * for interacting with the headers in storage, adding, removing, comparing,
 * etc.
 *
 * @ibm-private-in-use
 */
public interface HeaderStorage {

    /** Default value for various integers */
    int NOTSET = -1;

    /** English charset for various String conversions */
    Charset ENGLISH_CHARSET = StandardCharsets.ISO_8859_1;

    /**
     * Allow the debug context object to be set to the input Object for more
     * specialized debugging. A null input object will be ignored.
     *
     * @param o
     */
    void setDebugContext(Object o);

    /**
     * Access the first instance, if multiple exist, of the header. If the
     * header does not exist, then an empty header field is returned.
     *
     * @param name
     * @return HeaderField
     */
    HeaderField getHeader(String name);

    /**
     * Access the first instance, if multiple exist, of the header. If the
     * header does not exist, then an empty header field is returned.
     *
     * @param name
     * @return HeaderField
     */
    HeaderField getHeader(byte[] name);

    /**
     * Access the first instance, if multiple exist, of the header. If the
     * header does not exist, then an empty header field is returned.
     *
     * @param name
     * @return HeaderField
     */
    HeaderField getHeader(HeaderKeys name);

    /**
     * Access a list of all instances that exist of the input header. This
     * list is never null but may be empty.
     *
     * @param name
     * @return List<HeaderField>
     */
    List<HeaderField> getHeaders(String name);

    /**
     * Access a list of all instances that exist of the input header. This
     * list is never null but may be empty.
     *
     * @param name
     * @return List<HeaderField>
     */
    List<HeaderField> getHeaders(byte[] name);

    /**
     * Access a list of all instances that exist of the input header. This
     * list is never null but may be empty.
     *
     * @param name
     * @return List<HeaderField>
     */
    List<HeaderField> getHeaders(HeaderKeys name);

    /**
     * Access all of the header fields that exist in this message. If an
     * individual
     * header name exists multiple times, it will appear on this list for each
     * instance. This list is never null but may be empty.
     *
     * @return List<HeaderField>
     */
    List<HeaderField> getAllHeaders();

    /**
     * Query a list of all the unique header names found in this particular
     * message.
     * This list is never null but may be empty.
     *
     * @return List<String>
     */
    List<String> getAllHeaderNames();

    /**
     * Create a new instance of this header with the given byte[] value.
     * This will cause multiple headers to be sent in the object, i.e.
     * two Cache-Control headers.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void appendHeader(byte[] header, byte[] value);

    /**
     * Create a new instance of this header with the given value. The value is
     * only a subset of the input array, specified by the offset into the array
     * and the length from that point.
     *
     * @param header
     * @param value
     * @param offset
     * @param length
     * @throws IllegalArgumentException
     *             if input is invalid, including offset
     *             and length boundary checking
     */
    void appendHeader(byte[] header, byte[] value, int offset, int length);

    /**
     * Create a new instance of this header with the given String value.
     * This will cause multiple headers to be sent in the object, i.e.
     * two Cache-Control headers.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void appendHeader(byte[] header, String value);

    /**
     * Create a new instance of this header with the given byte[] value.
     * This will cause multiple headers to be sent in the object, i.e.
     * two Cache-Control headers.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void appendHeader(HeaderKeys header, byte[] value);

    /**
     * Create a new instance of this header with the given value. The value is
     * only a subset of the input array, specified by the offset into the array
     * and the length from that point.
     *
     * @param header
     * @param value
     * @param offset
     * @param length
     * @throws IllegalArgumentException
     *             if input is invalid, including offset
     *             and length boundary checking
     */
    void appendHeader(HeaderKeys header, byte[] value, int offset, int length);

    /**
     * Create a new instance of this header with the given String value.
     * This will cause multiple headers to be sent in the object, i.e.
     * two Cache-Control headers.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void appendHeader(HeaderKeys header, String value);

    /**
     * Create a new instance of this header with the given byte[] value.
     * This will cause multiple headers to be sent in the object, i.e.
     * two Cache-Control headers.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void appendHeader(String header, byte[] value);

    /**
     * Create a new instance of this header with the given value. The value is
     * only a subset of the input array, specified by the offset into the array
     * and the length from that point.
     *
     * @param header
     * @param value
     * @param offset
     * @param length
     * @throws IllegalArgumentException
     *             if input is invalid, including offset
     *             and length boundary checking
     */
    void appendHeader(String header, byte[] value, int offset, int length);

    /**
     * Create a new instance of this header with the given String value.
     * This will cause multiple headers to be sent in the object, i.e.
     * two Cache-Control headers.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void appendHeader(String header, String value);

    /**
     * Query how many instances of the target header are currently stored in
     * this message.
     *
     * @param header
     * @return int (0 if none)
     */
    int getNumberOfHeaderInstances(String header);

    /**
     * Query whether a header is present in storage.
     *
     * @param header
     * @return boolean
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    boolean containsHeader(byte[] header);

    /**
     * Query whether a header is present in storage.
     *
     * @param header
     * @return boolean
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    boolean containsHeader(HeaderKeys header);

    /**
     * Query whether a header is present in storage.
     *
     * @param header
     * @return boolean
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    boolean containsHeader(String header);

    /**
     * Query how many instances of the target header are currently stored in
     * this message.
     *
     * @param header
     * @return int (0 if none)
     */
    int getNumberOfHeaderInstances(byte[] header);

    /**
     * Query how many instances of the target header are currently stored in
     * this message.
     *
     * @param header
     * @return int (0 if none)
     */
    int getNumberOfHeaderInstances(HeaderKeys header);

    /**
     * Remove all instances of this header from storage.
     *
     * @param header
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void removeHeader(byte[] header);

    /**
     * Remove a specific instance of this header from storage.
     *
     * @param header
     * @param instance
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void removeHeader(byte[] header, int instance);

    /**
     * Remove all instances of a particular header from storage
     *
     * @param header
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void removeHeader(HeaderKeys header);

    /**
     * Remove a specific instance of a header from storage.
     *
     * @param header
     * @param instance
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void removeHeader(HeaderKeys header, int instance);

    /**
     * Remove all instances of this header from storage.
     *
     * @param header
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void removeHeader(String header);

    /**
     * Remove a specific instance of this header from storage.
     *
     * @param header
     * @param instance
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void removeHeader(String header, int instance);

    /**
     * Remove all of the headers from storage, cleaning up the memory
     *
     */
    void removeAllHeaders();

    /**
     * Set the header to the given byte[] value, erasing any existing values.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void setHeader(byte[] header, byte[] value);

    /**
     * Set the header to the input value, erasing any existing values. The value
     * is only a subset of the input array, specified by the offset into the array
     * and the length from that point.
     *
     * @param header
     * @param value
     * @param offset
     * @param length
     * @throws IllegalArgumentException
     *             if input is invalid, including offset
     *             and length boundary checking
     */
    void setHeader(byte[] header, byte[] value, int offset, int length);

    /**
     * Set the header to the given string value, erasing any existing values.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void setHeader(byte[] header, String value);

    /**
     * Set the header to the given byte[] value, erasing any existing values.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void setHeader(HeaderKeys header, byte[] value);

    /**
     * Set the header to the input value, erasing any existing values. The value
     * is only a subset of the input array, specified by the offset into the array
     * and the length from that point.
     *
     * @param header
     * @param value
     * @param offset
     * @param length
     * @throws IllegalArgumentException
     *             if input is invalid, including offset
     *             and length boundary checking
     */
    void setHeader(HeaderKeys header, byte[] value, int offset, int length);

    /**
     * Set the header to the given string value, erasing any existing values.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void setHeader(HeaderKeys header, String value);

    /**
     * Set the header to the given byte[] value, erasing any existing values.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void setHeader(String header, byte[] value);

    /**
     * Set the header to the input value, erasing any existing values. The value
     * is only a subset of the input array, specified by the offset into the array
     * and the length from that point.
     *
     * @param header
     * @param value
     * @param offset
     * @param length
     * @throws IllegalArgumentException
     *             if input is invalid, including offset
     *             and length boundary checking
     */
    void setHeader(String header, byte[] value, int offset, int length);

    /**
     * Set the header to the given string value, erasing any existing values.
     * <p>
     * Any String encoding or decoding on the value will be performed with the
     * ISO-8859-1 charset.
     *
     * @param header
     * @param value
     * @throws IllegalArgumentException
     *             if input is invalid
     */
    void setHeader(String header, String value);

    /**
     * Set the limit on the number of headers allowed to be set on this message.
     *
     * @param number
     * @throws IllegalArgumentException
     *             if number is negative or zero
     */
    void setLimitOnNumberOfHeaders(int number);

    /**
     * Query the current limit on number of headers allowed in the message.
     *
     * @return int
     */
    int getLimitOnNumberOfHeaders();

    /**
     * Set the limit on the size of individual protocol tokens, ie. header names
     * or header values, etc, when parsing inbound messages.
     *
     * @param size
     * @throws IllegalArgumentException
     *             if size is negative or zero
     */
    void setLimitOfTokenSize(int size);

    /**
     * Query the current limit on the size of protocol tokens.
     *
     * @return int
     */
    int getLimitOfTokenSize();
}
