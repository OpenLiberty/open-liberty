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
package com.ibm.ws.http.channel.h2internal;

public class Constants {

    // The Frame Flag is one byte, each bit is a possible boolean presentation of a flag value
    static byte FRAME_FLAG_END_STREAM = 0x01;
    static byte FRAME_FLAG_END_HEADERS = 0x04;
    static byte FRAME_FLAG_PADDED = 0x08;
    static byte FRAME_FLAG_PRIORITY = 0x20;

    // Error codes are 32-bit fields that are used in RST_STREAM and GOAWAY frames to convey the reasons for the stream or connection error.
    // Room for 2 billion more error codes!
    public static int NO_ERROR = 0x0;
    public static int PROTOCOL_ERROR = 0x1;
    public static int INTERNAL_ERROR = 0x2;
    public static int FLOW_CONTROL_ERROR = 0x3;
    public static int SETTINGS_TIMEOUT = 0x4;
    public static int STREAM_CLOSED = 0x5;
    public static int FRAME_SIZE_ERROR = 0x6;
    public static int REFUSED_STREAM = 0x7;
    public static int CANCEL = 0x8;
    public static int COMPRESSION_ERROR = 0x9;
    public static int CONNECT_ERROR = 0xa;
    public static int ENHANCE_YOUR_CALM = 0xb;
    public static int INADEQUATE_SECURITY = 0xc;
    public static int HTTP_1_1_REQUIRED = 0xd;

    public static int SPEC_INITIAL_WINDOW_SIZE = 65535;

    public static enum Direction {
        READ_IN,
        WRITING_OUT
    }

    // Contants returned by the frame processing code, when asked to process a new buffer.
    // a non-negative return code is the position in the buffer where a new frame is assumed to start.
    // for this reason, these return codes are "int" and not an enumeration.
    // BP stands for Buffer Processing.
    public static final int BP_FRAME_ALREADY_COMPLETE = -3;
    public static final int BP_FRAME_IS_NOT_COMPLETE = -2;
    public static final int BP_FRAME_EXACTLY_COMPLETED = -1;

    public static final int READ_FRAME_BUFFER_SIZE = 8192;

    public static final byte MASK_80 = (byte) 0x80;
    public static final byte MASK_7F = (byte) 0x7F;

    public static final int BIGGEST_31BIT_SIGNED = 0x7FFFFFFF;
    public static final long LONG_31BIT_FILTER = 0xFFFFFFFF80000000L;

    public static final long INITIAL_CLOSE_TIME = 0;
}
