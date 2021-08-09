/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.hpack;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.ByteFormatType;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.LiteralIndexType;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 *
 */
public class IntegerRepresentation {

    /*
     * An integer is represented in two parts: a prefix that fills the current
     * octet and an optional list of octets that are used if the integer
     * values does not fit within the prefix.
     *
     * Otherwise, all of the bits of the prefix are set to 1, and the value,
     * decreased by 2^N-1, is encoded using a list of one or more octets.
     * The most significant bit of each octet is used as a continuation
     * flag: its value is set to 1 except for the last octet in the list.
     * The remaining bits of the octets are used to encode the decreased
     * value.
     *
     * +---+---+---+---+---+---+---+---+
     * | ? | ? | ? | Value |
     * +---+---+---+---+---+---+---+---+
     * Integer value encoded within the prefix (shown for N = 5)
     *
     * +---+---+---+---+---+---+---+---+
     * | ? | ? | ? | 1 1 1 1 1 |
     * +---+---+---+---+---+---+---+---+
     * | 1 | Value - (2^N-1) LSB |
     * +---+---+---+---+---+---+---+---+
     * ...
     * +---+---+---+---+---+---+---+---+
     * | 0 | Value - (2^N-1) MSB |
     * +---+---+---+---+---+---+---+---+
     * Integer value encoded after the prefix (shown for N = 5)
     *
     * Integers are represented in two parts:
     * - a prefix that fills the current octet
     * - an optional list of octets that are used if the integer value does not
     * fit within the prefix
     *
     * N is the number of bits of the prefix.
     *
     *
     * To consider: Integer encodings that exceed implementation limits --
     * in value or octet length -- MUST be treated as decoding errors.
     * Different limits can be set for each of the different uses of integers,
     * based on implementation constraints.
     */

    protected static byte[] encode(int I, LiteralIndexType type) throws CompressionException, UnsupportedEncodingException {

        ByteFormatType formatType;
        switch (type) {
            case INDEX:
                //This path is only called when encoding with intent to index but there isn't
                //an existing table entry that matches this header name/value. Thus, indexing
                //will be incremental.
                formatType = ByteFormatType.INCREMENTAL;
                break;
            case NOINDEXING:
                formatType = ByteFormatType.NOINDEXING;
                break;
            case NEVERINDEX:
                formatType = ByteFormatType.NEVERINDEX;
                break;
            default:
                //Unknown indexing type
                throw (new CompressionException("Unrecognized byte format used during integer encoding."));
        }
        return encode(I, formatType);
    }

    protected static byte[] encode(int I, ByteFormatType type) throws CompressionException, UnsupportedEncodingException {
        //First byte needs to have a special format. If it's the first byte
        //for a header, it will specify literal indexing type. If it is the
        //first byte for determining a header's key/value String length,
        //it will specify whether the header key/value is Huffman encoded.

        int N;
        byte format;

        switch (type) {
            case INDEXED:
            case HUFFMAN:
                N = 7;
                format = HpackConstants.MASK_80;
                break;
            case INCREMENTAL:
                N = 6;
                format = HpackConstants.MASK_40;
                break;
            case NOINDEXING:
                N = 4;
                format = HpackConstants.MASK_00;
                break;
            case NEVERINDEX:
                N = 4;
                format = HpackConstants.MASK_10;
                break;
            case NOHUFFMAN:
                N = 7;
                format = HpackConstants.MASK_00;
                break;
            // shouldn't be another format this would be unknown case
            default:
                throw (new CompressionException("Unrecognized byte format used during integer encoding."));
        }

        return encode(I, format, N);

    }

    public static byte[] encode(int I, byte format, int N) throws UnsupportedEncodingException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        if (I < (HpackUtils.ipow(2, N) - 1)) {
            //Integer fits within the bits reserved for integer representation for
            //this literal indexing type. Format the byte with both the indexing type
            //and integer representation.
            ba.write(HpackUtils.format((byte) I, format));

        } else {
            //The integer will need more than one byte in order to represent it in
            //binary form. Format the first byte according to the indexing type and
            //allowed integer representation bits.
            ba.write(HpackUtils.format((byte) (HpackUtils.ipow(2, N) - 1), format));

            //Now iterate and create all the necessary bytes until the integer can be
            //fully represented.
            I = I - (HpackUtils.ipow(2, N) - 1);
            while (I >= 128) {
                ba.write((byte) (I % 128 + 128));
                I = I / 128;
            }
            ba.write((byte) (I));
        }

        return ba.toByteArray();

    }

    protected static int decode(WsByteBuffer buffer, ByteFormatType type) throws CompressionException {
        int N = -1;
        switch (type) {
            case INDEXED:
            case HUFFMAN:
            case NOHUFFMAN:
                N = 7;
                break;

            case INCREMENTAL:
                N = 6;
                break;

            case TABLE_UPDATE:
                N = 5;
                break;

            case NOINDEXING:
            case NEVERINDEX:
                N = 4;
                break;

            default:
                throw new CompressionException("Encoding Exception: Unknown Indexing Type");
        }
        return decode(buffer, N);
    }

    /**
     * Decodes a provided byte array that was encoded using an N-bit
     * prefix.
     *
     * @param ba
     * @param N
     * @return
     * @throws HeaderFieldDecodingException
     */
    public static int decode(WsByteBuffer headerBlock, int N) {

        // if (!headerBlock.hasRemaining()) {
        //     throw new HeaderFieldDecodingException("No length to decode");
        // }

        int I = HpackUtils.getLSB(headerBlock.get(), N);

        if (I < HpackUtils.ipow(2, N) - 1) {
            return I;
        } else {
            int M = 0;
            boolean done = false;

            byte b;
            while (done == false) {
                // If there are no further elements, this is an invalid HeaderBlock.
                // If this decode method is called, there should always be header
                // key value bytes after the integer representation.
                //  if (!headerBlock.hasRemaining()) {
                //     throw new HeaderFieldDecodingException("");
                // }
                b = headerBlock.get();

                I = I + ((b) & 127) * HpackUtils.ipow(2, M);
                M = M + 7;
                if (((b & 128) == 128) == false)
                    done = true;
            }

            return I;
        }

    }
}