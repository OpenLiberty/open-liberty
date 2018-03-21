/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test;

import java.math.BigInteger;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class utils {

    public static final int IO_DEFAULT_TIMEOUT = 5000;
    public static final int IO_DEFAULT_BUFFER_SIZE = 4090;

    public static final byte FRAME_TYPE_DATA = 0x00;
    public static final byte FRAME_TYPE_HEADERS = 0x01;
    public static final byte FRAME_TYPE_PRIORITY = 0x02;
    public static final byte FRAME_TYPE_RST_STREAM = 0x03;
    public static final byte FRAME_TYPE_SETTINGS = 0x04;
    public static final byte FRAME_TYPE_PUSH_PROMISE = 0x05;
    public static final byte FRAME_TYPE_PING = 0x06;
    public static final byte FRAME_TYPE_GOAWAY = 0x07;
    public static final byte FRAME_TYPE_WINDOW_UPDATE = 0x08;
    public static final byte FRAME_TYPE_CONTINUATION = 0x09;

    public static final int ZERO_STREAM_ID = 0; // makes reading the code easier to understand

    public static final byte FRAME_TYPE_INDEX = 3;
    public static final byte FRAME_FLAGS_INDEX = 4;
    public static final byte FRAME_STREAM_START_INDEX = 5;
    public static final int FRAME_STREAM_START_INDEX_INT = 5;

    /**
     * CLIENT_PREFACE_STRING = PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n
     */
    public static final byte[] CLIENT_PREFACE_STRING_IN_BYTES = new BigInteger("505249202a20485454502f322e300d0a0d0a534d0d0a0d0a", 16).toByteArray();

    public static final String h1_1UpgradeHeader = "HTTP/1.1 101 Switching Protocols<CR>\n<LF>Connection: Upgrade<CR>\n<LF>Upgrade: h2c<CR>\n<LF><CR>\n<LF>";

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

    public static long getIntFromByteArray(byte[] out, int offset) {
        int firstByte = 0;
        int secondByte = 0;
        int thirdByte = 0;
        int fourthByte = 0;

        firstByte = 0x000000FF & out[offset];
        offset++;
        secondByte = 0x000000FF & out[offset];
        offset++;
        thirdByte = 0x000000FF & out[offset];
        offset++;
        fourthByte = 0x000000FF & out[offset];

        return (firstByte << 24
                | secondByte << 16
                | thirdByte << 8
                | fourthByte)
               & 0xFFFFFFFFL;
    }

    public static long getIntFromByteBuffer(WsByteBuffer out) {
        int firstByte = 0;
        int secondByte = 0;
        int thirdByte = 0;
        int fourthByte = 0;

        firstByte = 0x000000FF & out.get();
        secondByte = 0x000000FF & out.get();
        thirdByte = 0x000000FF & out.get();
        fourthByte = 0x000000FF & out.get();

        return firstByte << 24
               | secondByte << 16
               | thirdByte << 8
               | fourthByte
                 & 0xFFFFFFFFL;
    }

    public static char getShortFromByteArray(byte[] out, int offset) {
        int firstByte = 0;
        int secondByte = 0;

        firstByte = 0x000000FF & out[offset];
        offset++;
        secondByte = 0x000000FF & out[offset];
        offset++;

        return (char) (firstByte << 8 | secondByte);
    }

    public static char getShortFromByteBuffer(WsByteBuffer out) {
        int firstByte = 0;
        int secondByte = 0;

        firstByte = 0x000000FF & out.get();
        secondByte = 0x000000FF & out.get();

        return (char) (firstByte << 8 | secondByte);
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

    public static void printArray(byte[] x) {
        for (int i = 1; i <= x.length; i++) {
            if ((i % 16) == 0) {
                System.out.println(String.format("0x%02X", x[i - 1]) + " ");
            } else {
                System.out.print(String.format("0x%02X", x[i - 1]) + " ");
            }
        }
    }

    public static void printCharArrayWithHex(char[] x, int length) {
        char c;
        byte b;
        int count = 0;
        for (int i = 0; i < length; i++) {
            count++;
            c = x[i];
            b = (byte) c;
            if (((count % 64) == 0) && (b != 0x0A)) {
                if (b == 0x0D) {
                    System.out.println("<CR>");
                    count = 0;
                } else if ((b > 0) && (b < 127)) {
                    System.out.println(c);
                    count = 0;
                } else {
                    System.out.println(String.format("<0x%02X>", b) + " ");
                    count = 0;
                }
            } else {
                if (b == 0x0A) {
                    System.out.print("\n<LF>");
                    count = 0;
                } else if (b == 0x0D) {
                    System.out.print("<CR>");
                } else if ((b > 0) && (b < 127)) {
                    System.out.print(c);
                } else {
                    System.out.print(String.format("<0x%02X>", b) + " ");
                }
            }
        }
    }

    public static String printByteArrayWithHex(byte[] x, int length) {
        StringBuilder response = new StringBuilder();
        char c;
        byte b;
        int count = 0;
        for (int i = 0; i < length; i++) {
            count++;
            b = x[i];
            if (((count % 64) == 0) && (b != 0x0A)) {
                if (b == 0x0D) {
                    //System.out.println("<CR>");
                    response.append("<CR>");
                    //count = 0;
                } else if ((b > 0) && (b < 127)) {
                    c = (char) b;
                    //System.out.println(c);
                    response.append(c);
                    //count = 0;
                } else {
                    //System.out.println(String.format("<0x%02X>", b) + " ");
                    //TODO I don't think we need this... prints <0x00>
                    //response.append(String.format("<0x%02X>", b) + " ");
                    //count = 0;
                }
                count = 0;
            } else {
                if (b == 0x0A) {
                    //System.out.print("\n<LF>");
                    response.append("\n<LF>");
                    count = 0;
                } else if (b == 0x0D) {
                    //System.out.print("<CR>");
                    response.append("<CR>");
                } else if ((b > 0) && (b < 127)) {
                    c = (char) b;
                    //System.out.print(c);
                    response.append(c);
                } else {
                    //System.out.print(String.format("<0x%02X>", b) + " ");
                    //TODO I don't think we need this... prints <0x00>
                    //response.append(String.format("<0x%02X>", b) + " ");
                }
            }
        }
        return response.toString();
    }

}
