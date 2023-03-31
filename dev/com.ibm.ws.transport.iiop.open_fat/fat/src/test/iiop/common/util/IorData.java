/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
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
package test.iiop.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IorData {

    public final String host;
    public final int port;

    public IorData(String ior) throws IOException {
        byte[] bytes;
        InputStream in = new HexInputStream(ior.substring(4));
        try {
            bytes = new byte[in.available()];
            in.read(bytes);
        } finally {
            in.close();
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        // read in the byte order
        byte bom = bb.get();
        bb.order(bom == 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // read in the type id
        String type = readString(bb);
        System.out.println(type);

        int tpLen = readLong(bb);
        System.out.println("Expecting " + tpLen + " tagged profile(s).");
        for (int i = 0; i < tpLen; i++) {
            int profId = readLong(bb);
            // octet sequence length
            int profLen = bb.getInt();
            System.out.println("Found profile with id " + profId + " and length " + profLen);
            // read in or skip
            switch (profId) {
                case org.omg.IOP.TAG_INTERNET_IOP.value:
                    int startPos = bb.position();
                    byte newOrder = bb.get();
                    ByteOrder oldOrder = bb.order();
                    bb.order(newOrder == 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
                    byte major = bb.get();
                    byte minor = bb.get();
                    host = readString(bb);
                    port = (bb.getShort() & 0xFFFF);
                    bb.position(startPos + profLen);
                    bb.order(oldOrder);
                    System.out.printf("Found internet profile GIOP %d.%d with host %s and port %d", major, minor, host, port);
                    return;
                default:
                    bb.position(bb.position() + profLen);
            }
        }
        throw new IOException("IIOP information nor found in IOR :" + ior);
    }

    private static String readString(ByteBuffer bb) throws UnsupportedEncodingException {
        int strLen = readLong(bb);
        byte[] chars = new byte[strLen];
        bb.get(chars);
        return new String(chars, 0, strLen - 1, "ISO-8859-1");
    }

    private static int readLong(ByteBuffer bb) {
        align(4, bb);
        int strLen = bb.getInt();
        return strLen;
    }

    private static void align(int align, ByteBuffer bb) {
        int pos = bb.position();
        int toSkip = (align - pos % align) % align;
        bb.position(pos + toSkip);
    }

    public static void main(String[] args) throws Exception {
        String iorString = "IOR:00"
                           + "000000"
                           + "00000033"
                           + "524d493a746573742e69696f702e636f6d6d6f6e2e48656c6c6f536572766963653a3030303030303030303030303030303000"
                           + "00"
                           + "00000001"
                           + "00000000"
                           + "00000078"
                           + "000102000000000a6c6f63616c686f7374000b5200000035abacab3131343231383438393935005f526f6f74504f410048656c6c6f53657276696365504f41000048656c6c6f5365727669636500000000000001000000010000001c00000000000100010000000200010020050100010001010900000000";
    }
}
