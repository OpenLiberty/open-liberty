/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.hpack;

import java.util.List;

public class HpackConstants {

    public static final String HPACK_CHAR_SET = "US-ASCII";
    public static final int STATIC_TABLE_SIZE = 61;

    public static byte MASK_40 = 0x40; // 0100 0000
    public static byte MASK_00 = 0x00; // 0000 0000
    public static byte MASK_10 = 0x10; // 0001 0000

    public static byte MASK_0F = (byte) 0x0F; //0000 1111
    public static byte MASK_1F = (byte) 0x1F; //0001 1111
    public static byte MASK_2F = (byte) 0x2F; //0010 1111
    public static byte MASK_4F = (byte) 0x4F; //0100 1111
    public static byte MASK_80 = (byte) 0x80; //1000 0000
    public static byte MASK_8F = (byte) 0x8F; //1000 1111

    public final static String AUTHORITY = ":authority";
    public final static String METHOD = ":method";
    public final static String PATH = ":path";
    public final static String SCHEME = ":scheme";
    public final static String STATUS = ":status";

    public final static int AUTHORITY_HASH = ":authority".hashCode();
    public final static int METHOD_HASH = ":method".hashCode();
    public final static int PATH_HASH = ":path".hashCode();
    public final static int SCHEME_HASH = ":scheme".hashCode();
    public final static int STATUS_HASH = ":status".hashCode();

    public static final List<String> connectionSpecificHeaderList = java.util.Arrays.asList(new String[] { "Keep-Alive", "Proxy-Connection",
                                                                                                           "Transfer-Encoding", "Upgrade" });

    /*
     * This value is determined by the SETTINGS_HEADER_TABLE_SIZE setting
     * of the SETTINGS frame. A change in the maximum size of the dynamic
     * table is signaled via a dynamic table size update. This MUST
     * occur at the beginning of the first header block following the change
     * to the dynamic table size. This follows a settings acknowledgement.
     *
     * todo: Does initial make sense?
     */
    public static int INITIAL_SETTINGS_HEADER_TABLE_SIZE = 4096; //HTTP 2.0 Spec 6.5.2

    public enum LiteralIndexType {
        INDEX,
        NOINDEXING,
        NEVERINDEX
    }

    public enum ByteFormatType {
        INDEXED, // Section 6.1
        INCREMENTAL, // Section 6.2.1. Literal Header Field with Incremental Indexing
        NOINDEXING, // Section 6.2.2. Literal Header Field without Indexing
        NEVERINDEX, // Section 6.2.3. Literal Header Field never Indexed
        HUFFMAN,
        NOHUFFMAN,
        TABLE_UPDATE
    }

    enum DecodingState {
        PSEUDOHEADER,
        HEADER
    }

    enum PseudoHeader {
        AUTHORITY,
        METHOD,
        PATH,
        SCHEME,
        STATUS
    }

}
