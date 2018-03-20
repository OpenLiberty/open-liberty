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
package com.ibm.ws.http2.test.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.hpack.H2Headers;
import com.ibm.ws.http2.test.CFWManager;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

/**
 *
 */
public class H2HeadersUtils {

    List<AbstractMap.SimpleEntry<String, String>> headers;

    H2HeaderTable readTable = new H2HeaderTable();
    H2HeaderTable writeTable = new H2HeaderTable();

    List<HeaderEntry> readReferenceSet;
    List<HeaderEntry> writeReferenceSet = new ArrayList<HeaderEntry>();
    List<HeaderEntry> emittedHeaderSet = new ArrayList<HeaderEntry>();

    WsByteBufferPoolManager bufferMgr;

    /**
     *
     */
    public H2HeadersUtils() {
        bufferMgr = CFWManager.getWsByteBufferPoolManager();
    }

    /**
     *
     * @param headerEntries
     * @return Header Block Fragment ready to be sent in a HeadersFrame
     * @throws CompressionException
     * @throws IOException
     */
    public byte[] encodeHeaders(List<HeaderEntry> headerEntries) throws CompressionException, IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for (HeaderEntry headerEntry : headerEntries) {
            byteStream.write(H2Headers.encodeHeader(writeTable, headerEntry.getH2HeaderField().getName(), headerEntry.getH2HeaderField().getValue(), headerEntry.getFormatType(),
                                                    headerEntry.isHuffman()));
        }
        return byteStream.toByteArray();
    }

    public List<H2HeaderField> decodeHeaders(byte[] headerBlockFragment) throws CompressionException, IOException {
        ArrayList<H2HeaderField> decodedHeaderList = new ArrayList<H2HeaderField>();
        WsByteBuffer headerBlockFragmentByteBuffer = bufferMgr.allocate(headerBlockFragment.length);
        headerBlockFragmentByteBuffer.put(headerBlockFragment);
        headerBlockFragmentByteBuffer.rewind();

        while (headerBlockFragmentByteBuffer.hasRemaining()) {
            decodedHeaderList.add(H2Headers.decodeHeader(headerBlockFragmentByteBuffer, readTable));
        }
        return decodedHeaderList;
    }

    public class Tuple<K, L> extends AbstractMap.SimpleEntry<K, L> {

        /**
         * @param key
         * @param value
         */
        public Tuple(K key, L value) {
            super(key, value);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof H2HeaderField)
                return o.equals(o);
            return false;
        }

    }

}
