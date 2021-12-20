/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.dns.impl.netty;

import io.netty.buffer.ByteBuf;

/**
 * Class to represent a zone/name in dns
 * 
 * A name is comprised of a sequence of labels each beginning with a 1 byte
 * count field
 *
 * Example: ---------------------------------------------------
 * |7|h|a|d|r|i|c|k|7|r|a|l|e|i|g|h|3|i|b|m|3|c|o|m|0|
 * ---------------------------------------------------
 * 
 * Compression Example: The label(s) after raleigh are 0x0c bytes from the start
 * of the Dns message
 * 
 * ---------------------------------------------------
 * |7|h|a|d|r|i|c|k|7|r|a|l|e|i|g|h|0xc0|0x0c
 * ---------------------------------------------------
 * 
 */
public class Name {
    private String _name;
    private String[] _nameArray;

    private static int MAX_NUMBER_LABELS = 255;
    // private static int MAX_LABEL_SIZE = 63;

    public Name(String name) {
        _name = name;
    }

    protected Name(ByteBuf buffer) {
        int labels = 0;
        StringBuffer stringBuffer = new StringBuffer(1024);

        _nameArray = new String[MAX_NUMBER_LABELS];
//        System.out.println("new Name ");

        /** get the name string */
        readLabel(buffer, labels);

        /** convert the array to a stringBuffer */
        for (int i = 0; _nameArray[i] != null; i++) {
            String s = (String) _nameArray[i];
            stringBuffer.append((s) + ("."));
        }

        _name = stringBuffer.toString();

    }

    private void readLabel(ByteBuf buffer, int labels) {

        byte length = 0;

        while ((length = buffer.readByte()) != 0) {
            int current = buffer.readerIndex();
            /** handle domain name compression recursively */
            if (compressionCheck(buffer, length)) {
                readLabel(buffer, labels);
                buffer.readerIndex(current + 1);
                /** compression is always last in the sequence of labels */
                break;
            } else {
                /** get the label */
                byte[] b = new byte[length];

                buffer.readBytes(b);
                _nameArray[labels++] = new String(b);
            }
        }

    }

    private boolean compressionCheck(ByteBuf buffer, byte curByte) {
        final byte MASK = (byte) 0xc0;
        int offset = 0;

        if ((curByte & MASK) == MASK) {
            byte nextByte = buffer.readByte();

            offset |= ((int) (curByte & ~MASK)) << 8;
            offset |= nextByte & 0x00ff;

            /** if tcp, Dns length field embedded at the start of Dns Message */

//        	if (buffer.readerIndex() > 0){
//        	System.out.println("offset " + offset);
//        		buffer.readerIndex(offset + 2);
//                System.out.println("compressionCheck buffer=" + buffer  );
//        	}
//        	else {
            buffer.readerIndex(offset);
//        	}
            return true;
        }

        return false;
    }

    protected void toBuffer(ByteBuf buffer) {

        /** special case root domain denote by 0 */
        if (_name.equals("")) {
            buffer.writeByte((byte) 0);
            return;
        }
        /** . is as special character in regexp */
        String[] sa = _name.split("\\.");

        /** write out the name */

        for (int i = 0; i < sa.length; i++) {
            buffer.writeByte((byte) sa[i].length());
            buffer.writeBytes(sa[i].getBytes());
        }
        /** put the trailing . */
        buffer.writeByte((byte) 0);
    }

    public String getString() {
        return _name;
    }

    protected short length() {
        int length = 0;
        /** . is as special character in regexp */
        String[] sa = _name.split("\\.");

        /** write out the name */
        for (int i = 0; i < sa.length; i++) {
            length += 1 + sa[i].length();
        }

        /** one more for the trailing zero */
        length++;
        return (short) length;
    }

    public String toString() {
        return _name.toString();
    }
}
