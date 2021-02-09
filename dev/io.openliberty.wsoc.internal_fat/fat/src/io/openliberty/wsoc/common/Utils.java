/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 *
 */
public class Utils {

    /**
     * 
     */
    public Utils() {

    }

    public static String getReaderText(Reader reader) throws Exception {
        StringBuffer buf = new StringBuffer();
        int data;

        data = reader.read();

        while (data != -1) {
            buf.append((char) data);
            data = reader.read();
        }

        return buf.toString();
    }

    public static byte[] getInputStreamData(InputStream stream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int read;
        byte[] data = new byte[2048];

        while ((read = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, read);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public static byte[][] getRandomBinaryByteArray(int entries, int length) {
        Random r = new Random(System.currentTimeMillis());
        byte[][] data = new byte[entries][];

        for (int x = 0; x < entries; x++) {
            data[x] = new byte[length];
            r.nextBytes(data[x]);
        }
        return data;
    }

    public static byte[][] duplicateByteArray(byte[][] data) {

        byte retData[][] = new byte[data.length][];

        for (int x = 0; x < data.length; x++) {
            retData[x] = new byte[data[x].length];
            System.arraycopy(data[x], 0, retData[x], 0, data[x].length);
        }
        return retData;
    }

    public static ByteBuffer[] getRandomBinaryByteBuffer(int entries, int length) {
        //PLEASE DO NOT USE RANDON TEST DATA!!
        ByteBuffer[] bufs = new ByteBuffer[entries];

        Random r = new Random(System.currentTimeMillis());

        for (int x = 0; x < entries; x++) {
            byte[] data = new byte[length];
            r.nextBytes(data);
            bufs[x] = ByteBuffer.wrap(data);

        }
        return bufs;
    }

    public static ByteBuffer[] duplicateByteBuffers(ByteBuffer[] bufs) {

        ByteBuffer[] retBufs = new ByteBuffer[bufs.length];

        for (int x = 0; x < bufs.length; x++) {
            byte[] dd = bufs[x].array();
            byte[] aa = new byte[dd.length];
            System.arraycopy(dd, 0, aa, 0, aa.length);
            retBufs[x] = ByteBuffer.wrap(aa);
        }
        return retBufs;
    }

    public static void waitForFFDCToBeGenerated(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {

        }
    }

}
