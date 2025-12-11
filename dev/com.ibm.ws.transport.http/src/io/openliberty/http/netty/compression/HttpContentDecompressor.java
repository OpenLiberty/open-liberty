/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.compression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.zip.DataFormatException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.http.channel.compression.DecompressionHandler;
import com.ibm.wsspi.http.channel.compression.DeflateInputHandler;
import com.ibm.wsspi.http.channel.compression.GzipInputHandler;
import com.ibm.wsspi.http.channel.compression.IdentityInputHandler;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;

public class HttpContentDecompressor {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpContentDecompressor.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /**
     * Decompresses the given buffer using the appropriate decompression handler based on the content encoding header
     * and HttpOption settings. It then process the payload data while validating against the configured tolerance
     * limits. 
     * @param buffer the input buffer containing compressed data
     * @param config the Http configuration options
     * @param contentEncoding the content encoding header value (e.g., "gzip", "deflate")
     * @return a WsByteBuffer containing all decompressed payload data
     * @throws DataFormatException if the decompression ratio tolerance is exceeded
     */
    public WsByteBuffer decompress(WsByteBuffer buffer, HttpChannelConfig config, String contentEncoding) throws DataFormatException{
        
        Objects.requireNonNull(config, "Http configuration must not be null");
        DecompressionHandler handler = chooseHandler(contentEncoding, config);

        return decompress(buffer, config, handler);
    }

    /**
     * Chooses a supported decompression handler based on the content encoding and auto-decompression
     * HttpOption setting.
     * 
     * @param contentEncoding the content encoding header value
     * @param config the Http configuration options
     * @return the corresponding DecompressionHandler instance
     */
    private DecompressionHandler chooseHandler(String contentEncoding, HttpChannelConfig config){
        if(!config.isAutoDecompressionEnabled()){
            return new IdentityInputHandler();
        }
        ContentEncodingValues encoding = ContentEncodingValues.find(contentEncoding);
        if(ContentEncodingValues.GZIP.equals(encoding) || ContentEncodingValues.XGZIP.equals(encoding)){
            return new GzipInputHandler();
        } else if(ContentEncodingValues.DEFLATE.equals(encoding)){
            return new DeflateInputHandler();
        } else {
            return new IdentityInputHandler();
        }
    }

    public WsByteBuffer decompress(WsByteBuffer buffer, HttpChannelConfig config, DecompressionHandler handler) throws DataFormatException{

        if(!handler.isEnabled() || buffer == null || buffer.remaining() == 0){
            return buffer;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Removing encoding...");
        }
        LinkedList<WsByteBuffer> tempBuffers = new LinkedList<>();
        tempBuffers.add(buffer);
        int cyclesAboveDecompressionRatio = 0;
        List<WsByteBuffer> storage = new ArrayList<>();

        try{
            while(!tempBuffers.isEmpty()){
                WsByteBuffer temp = tempBuffers.removeFirst();
                while(temp.hasRemaining()){
                    List<WsByteBuffer> decompressionChunks = handler.decompress(temp);
                    if(!decompressionChunks.isEmpty()){
                        if(handler.getBytesRead() > 0){
                            double ratio = (double) handler.getBytesWritten() / handler.getBytesRead();
                            if(ratio > config.getDecompressionRatioLimit()){
                                cyclesAboveDecompressionRatio++;
                                if(cyclesAboveDecompressionRatio > config.getDecompressionTolerance()){
                                    if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                                        Tr.debug(tc, "Decompression ratio tolerance reached. Cycles: "+ cyclesAboveDecompressionRatio);
                                    }
                                    throw new DataFormatException("Decompression tolerance reached");
                                }
                            }
                        }
                        storage.addAll(decompressionChunks);
                    }
                }
            temp.release();
            }
        }finally{
            while(!tempBuffers.isEmpty()){
                tempBuffers.removeFirst().release();
            }
        }

        if (storage.isEmpty()) {
            return ChannelFrameworkFactory.getBufferManager().allocate(0);
        }
        
        // Combine all decompressed chunks into one buffer.
        int totalSize = storage.stream().mapToInt(WsByteBuffer::remaining).sum();
        WsByteBuffer combinedBuffer = ChannelFrameworkFactory.getBufferManager().allocate(totalSize);
        
        for(Iterator<WsByteBuffer> it=storage.iterator(); it.hasNext();){
            WsByteBuffer buf = it.next();
            combinedBuffer.put(buf);
            buf.release();
            it.remove();
        }

        combinedBuffer.flip();
        return combinedBuffer;
        
    }

}
