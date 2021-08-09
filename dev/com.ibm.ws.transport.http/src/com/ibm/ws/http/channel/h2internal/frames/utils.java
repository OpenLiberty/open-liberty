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
package com.ibm.ws.http.channel.h2internal.frames;

import com.ibm.ws.http.channel.h2internal.Constants;

public class utils {

    static final byte FRAME_TYPE_DATA = 0x00;
    static final byte FRAME_TYPE_HEADERS = 0x01;
    static final byte FRAME_TYPE_PRIORITY = 0x02;
    static final byte FRAME_TYPE_RST_STREAM = 0x03;
    static final byte FRAME_TYPE_SETTINGS = 0x04;
    static final byte FRAME_TYPE_PUSH_PROMISE = 0x05;
    static final byte FRAME_TYPE_PING = 0x06;
    static final byte FRAME_TYPE_GOAWAY = 0x07;
    static final byte FRAME_TYPE_WINDOW_UPDATE = 0x08;
    static final byte FRAME_TYPE_CONTINUATION = 0x09;
    static final byte FRAME_TYPE_UNKNOWN = 0xf;

    static final int ZERO_STREAM_ID = 0; // makes reading the code easier to understand

    static final byte FRAME_TYPE_INDEX = 3;
    static final byte FRAME_FLAGS_INDEX = 4;
    static final byte FRAME_STREAM_START_INDEX = 5;
    static final int FRAME_STREAM_START_INDEX_INT = 5;

    public static String getErrorFromCode(int code) {
        switch (code) {
            case 0:
                return "NO_ERROR";
            case 1:
                return "PROTOCOL_ERROR";
            case 2:
                return "INTERNAL_ERROR";
            case 3:
                return "FLOW_CONTROL_ERROR";
            case 4:
                return "SETTINGS_TIMEOUT";
            case 5:
                return "STREAM_CLOSED";
            case 6:
                return "FRAME_SIZE_ERROR";
            case 7:
                return "REFUSED_STREAM";
            case 8:
                return "CANCEL";
            case 9:
                return "COMPRESSION_ERROR";
            case 0xa:
                return "CONNECT_ERROR";
            case 0xb:
                return "ENHANCE_YOUR_CALM";
            case 0xc:
                return "INADEQUATE_SECURITY";
            case 0xd:
                return "HTTP_1_1_REQUIRED";
        }
        return "UNDEFINED_ERROR";
    }

    public static void Move8BitstoByteArray(int in, byte[] out, int offset) {
        // move the 8 bit length in the int to the buffer
        out[offset] = (byte) (in & 0x000000FF);
    }

    public static void Move16BitstoByteArray(int in, byte[] out, int offset) {
        // move the 16 bit length in the int to the buffer, big endian
        out[offset] = (byte) ((in >> 8) & (0x000000FF));
        offset++;
        out[offset] = (byte) (in & 0x000000FF);
    }

    public static void Move24BitstoByteArray(int in, byte[] out, int offset) {
        // move the 24 bit length in the int to the buffer, big endian
        out[offset] = (byte) ((in >> 16) & (0x000000FF));
        offset++;
        out[offset] = (byte) ((in >> 8) & (0x000000FF));
        offset++;
        out[offset] = (byte) (in & 0x000000FF);
    }

    public static void Move31BitstoByteArray(int in, byte[] out, int offset) {
        // move the 31 bit length in the int to the buffer, big endian
        out[offset] = (byte) ((in >>> 24) & (0x0000007F)); // 7F, since the MSB of the int is not to be used
        offset++;
        out[offset] = (byte) ((in >> 16) & (0x000000FF));
        offset++;
        out[offset] = (byte) ((in >> 8) & (0x000000FF));
        offset++;
        out[offset] = (byte) (in & 0x000000FF);
    }

    public static void Move32BitstoByteArray(int in, byte[] out, int offset) {
        // move the 31 bit length in the int to the buffer, big endian
        out[offset] = (byte) ((in >>> 24) & (0x000000FF));
        offset++;
        out[offset] = (byte) ((in >> 16) & (0x000000FF));
        offset++;
        out[offset] = (byte) ((in >> 8) & (0x000000FF));
        offset++;
        out[offset] = (byte) (in & 0x000000FF);
    }

    public static byte getIndexNumber(String x) {

        if (x == null) {
            return -1;
        }

        // match input header value to the hardcode spec table index. -1 if there is not match

        if (x.compareToIgnoreCase(":authority") == 0) {
            return 1;
        }

        if (x.compareToIgnoreCase(":method-GET") == 0) {
            return 2;
        }
        if (x.compareToIgnoreCase(":method-POST") == 0) {
            return 3;
        }

        if (x.compareToIgnoreCase(":path-/") == 0) {
            return 4;
        }
        if (x.compareToIgnoreCase(":path-/index.html") == 0) {
            return 5;
        }

        if (x.compareToIgnoreCase(":scheme-http") == 0) {
            return 6;
        }
        if (x.compareToIgnoreCase(":scheme-https") == 0) {
            return 7;
        }

        if (x.compareToIgnoreCase(":status-200") == 0) {
            return 8;
        }
        if (x.compareToIgnoreCase(":status-204") == 0) {
            return 9;
        }
        if (x.compareToIgnoreCase(":status-206") == 0) {
            return 10;
        }
        if (x.compareToIgnoreCase(":status-304") == 0) {
            return 11;
        }
        if (x.compareToIgnoreCase(":status-400") == 0) {
            return 12;
        }
        if (x.compareToIgnoreCase(":status-404") == 0) {
            return 13;
        }
        if (x.compareToIgnoreCase(":status-500") == 0) {
            return 14;
        }

        if (x.compareToIgnoreCase("accept-charset") == 0) {
            return 15;
        }

        if (x.compareToIgnoreCase("accept-encoding-gzip") == 0) {
            return 16;
        }
        if (x.compareToIgnoreCase("accept-encoding-deflate") == 0) {
            return 16;
        }

        if (x.compareToIgnoreCase("accept-language") == 0) {
            return 17;
        }
        if (x.compareToIgnoreCase("accept-ranges") == 0) {
            return 18;
        }
        if (x.compareToIgnoreCase("accept") == 0) {
            return 19;
        }
        if (x.compareToIgnoreCase("access-control-allow-origin") == 0) {
            return 20;
        }
        if (x.compareToIgnoreCase("age") == 0) {
            return 21;
        }
        if (x.compareToIgnoreCase("allow") == 0) {
            return 22;
        }
        if (x.compareToIgnoreCase("authorization") == 0) {
            return 23;
        }
        if (x.compareToIgnoreCase("cache-control") == 0) {
            return 24;
        }
        if (x.compareToIgnoreCase("content-disposition") == 0) {
            return 25;
        }
        if (x.compareToIgnoreCase("content-encoding") == 0) {
            return 26;
        }
        if (x.compareToIgnoreCase("content-language") == 0) {
            return 27;
        }
        if (x.compareToIgnoreCase("content-length") == 0) {
            return 28;
        }
        if (x.compareToIgnoreCase("content-location") == 0) {
            return 29;
        }
        if (x.compareToIgnoreCase("content-range") == 0) {
            return 30;
        }
        if (x.compareToIgnoreCase("content-type") == 0) {
            return 31;
        }
        if (x.compareToIgnoreCase("cookie") == 0) {
            return 32;
        }
        if (x.compareToIgnoreCase("date") == 0) {
            return 33;
        }
        if (x.compareToIgnoreCase("etag") == 0) {
            return 34;
        }
        if (x.compareToIgnoreCase("expect") == 0) {
            return 35;
        }
        if (x.compareToIgnoreCase("expires") == 0) {
            return 36;
        }
        if (x.compareToIgnoreCase("from") == 0) {
            return 37;
        }
        if (x.compareToIgnoreCase("host") == 0) {
            return 38;
        }
        if (x.compareToIgnoreCase("if-match") == 0) {
            return 39;
        }
        if (x.compareToIgnoreCase("if-modified-since") == 0) {
            return 40;
        }
        if (x.compareToIgnoreCase("if-none-match") == 0) {
            return 41;
        }
        if (x.compareToIgnoreCase("if-range") == 0) {
            return 42;
        }
        if (x.compareToIgnoreCase("if-unmodified-since") == 0) {
            return 43;
        }
        if (x.compareToIgnoreCase("last-modified") == 0) {
            return 44;
        }
        if (x.compareToIgnoreCase("link") == 0) {
            return 45;
        }
        if (x.compareToIgnoreCase("location") == 0) {
            return 46;
        }
        if (x.compareToIgnoreCase("max-forwards") == 0) {
            return 47;
        }
        if (x.compareToIgnoreCase("proxy-authenticate") == 0) {
            return 48;
        }
        if (x.compareToIgnoreCase("proxy-authorization") == 0) {
            return 49;
        }
        if (x.compareToIgnoreCase("range") == 0) {
            return 50;
        }
        if (x.compareToIgnoreCase("referer") == 0) {
            return 51;
        }
        if (x.compareToIgnoreCase("refresh") == 0) {
            return 52;
        }
        if (x.compareToIgnoreCase("retry-after") == 0) {
            return 53;
        }
        if (x.compareToIgnoreCase("server") == 0) {
            return 54;
        }
        if (x.compareToIgnoreCase("set-cookie") == 0) {
            return 55;
        }
        if (x.compareToIgnoreCase("strict-transport-security") == 0) {
            return 56;
        }
        if (x.compareToIgnoreCase("transfer-encoding") == 0) {
            return 57;
        }
        if (x.compareToIgnoreCase("user-agent") == 0) {
            return 58;
        }
        if (x.compareToIgnoreCase("vary") == 0) {
            return 59;
        }
        if (x.compareToIgnoreCase("via") == 0) {
            return 60;
        }
        if (x.compareToIgnoreCase("www-authenticate") == 0) {
            return 61;
        }

        return -1;
    }

    public static String printArray(byte[] x) {
        StringBuffer buf = new StringBuffer();
        for (int i = 1; i <= x.length; i++) {
            if ((i % 16) == 0) {
                buf.append(String.format("0x%02X", x[i - 1]) + " ");
            } else {
                buf.append(String.format("0x%02X", x[i - 1]) + " ");
            }
        }
        return buf.toString();
    }

    public static String printCharArrayWithHex(char[] x, int length) {
        char c;
        byte b;
        int count = 0;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            count++;
            c = x[i];
            b = (byte) c;
            if (((count % 64) == 0) && (b != 0x0A)) {
                if (b == 0x0D) {
                    buf.append("<CR>\n");
                    count = 0;
                } else if ((b > 0) && (b < 127)) {
                    buf.append(c + "\n");
                    count = 0;
                } else {
                    buf.append(String.format("<0x%02X>", b) + " \n");
                    count = 0;
                }
            } else {
                if (b == 0x0A) {
                    buf.append("\n<LF>");
                    count = 0;
                } else if (b == 0x0D) {
                    buf.append("<CR>");
                } else if ((b > 0) && (b < 127)) {
                    buf.append(c);
                } else {
                    buf.append(String.format("<0x%02X>", b) + " ");
                }
            }
        }
        return buf.toString();
    }

    public static String printByteArrayWithHex(byte[] x, int length) {
        char c;
        byte b;
        int count = 0;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            count++;
            b = x[i];
            if (((count % 64) == 0) && (b != 0x0A)) {
                if (b == 0x0D) {
                    buf.append("<CR>\n");
                    count = 0;
                } else if ((b > 0) && (b < 127)) {
                    c = (char) b;
                    buf.append(c + "\n");
                    count = 0;
                } else {
                    buf.append(String.format("<0x%02X>", b) + " \n");
                    count = 0;
                }
            } else {
                if (b == 0x0A) {
                    buf.append("\n<LF>");
                    count = 0;
                } else if (b == 0x0D) {
                    buf.append("<CR>");
                } else if ((b > 0) && (b < 127)) {
                    c = (char) b;
                    buf.append(c);
                } else {
                    buf.append(String.format("<0x%02X>", b) + " ");
                }
            }
        }
        return buf.toString();
    }

    public static boolean getFlag(byte flags, int position) {
        byte[] bytes = new byte[1];
        bytes[0] = flags;
        return ((bytes[position / 8] & (1 << (position % 8))) != 0);
    }

    public static boolean getReservedBit(byte input) {
        return ((input & Constants.MASK_80) == Constants.MASK_80) ? true : false;
    }

}
