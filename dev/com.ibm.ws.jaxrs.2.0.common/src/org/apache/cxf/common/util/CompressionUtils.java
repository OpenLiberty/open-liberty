/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// CXF v3.1.18 - https://github.com/apache/cxf/blob/91278f9fdcc4fd1d5037d30cb0ba93cea10104b3/core/src/main/java/org/apache/cxf/common/util/CompressionUtils.java

package org.apache.cxf.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class CompressionUtils {
    
    // Limits the size of the inflated data
    public static final long DEFAULT_MAX_INFLATED_SIZE =
        SystemPropertyAction.getInteger("org.apache.cxf.compression-max-inflated-size", 10 * 1024 * 1024); // Liberty Change - CXF #3394

    private CompressionUtils() {
        
    }
    public static InputStream inflate(byte[] deflatedToken) 
        throws DataFormatException {
        return inflate(deflatedToken, true);
    }
    public static InputStream inflate(byte[] deflatedToken, boolean nowrap) 
        throws DataFormatException {
        // Liberty Change start - CXF #3394
        return inflate(deflatedToken, nowrap, DEFAULT_MAX_INFLATED_SIZE);
    }
    public static InputStream inflate(byte[] deflatedToken, boolean nowrap, long maxInflatedSize)
        throws DataFormatException {
        // Liberty Change end - CXF #3394
        Inflater inflater = new Inflater(true);
        inflater.setInput(deflatedToken);

        byte[] buffer = new byte[deflatedToken.length];
        int inflateLen;
        long totalInflated = 0; // Liberty Change - CXF #3394
        ByteArrayOutputStream inflatedToken = new ByteArrayOutputStream();
        while (!inflater.finished()) {
            inflateLen = inflater.inflate(buffer, 0, deflatedToken.length);
            if (inflateLen == 0 && !inflater.finished()) {
                if (inflater.needsInput()) {
                    throw new DataFormatException("Inflater can not inflate all the token bytes");
                } else {
                    break;
                }
            }
            
            // Liberty Change start - CXF #3394
            totalInflated += inflateLen;
            if (totalInflated > maxInflatedSize) {
                throw new DataFormatException("Inflated data exceeds the maximum allowed size of "
                    + maxInflatedSize + " bytes");
            }
            // Liberty Change end - CXF #3394

            inflatedToken.write(buffer, 0, inflateLen);
        }

        return new ByteArrayInputStream(inflatedToken.toByteArray());
    }
    
    public static byte[] deflate(byte[] tokenBytes) {
        return deflate(tokenBytes, true);
    }
    
    public static byte[] deflate(byte[] tokenBytes, boolean nowrap) {
        return deflate(tokenBytes, Deflater.DEFLATED, nowrap);
    }
    
    public static byte[] deflate(byte[] tokenBytes, int level, boolean nowrap) {
        Deflater compresser = new Deflater(level, nowrap);
        
        compresser.setInput(tokenBytes);
        compresser.finish();
        
        byte[] output = new byte[tokenBytes.length * 2];
        
        int compressedDataLength = compresser.deflate(output);
        
        byte[] result = new byte[compressedDataLength];
        System.arraycopy(output, 0, result, 0, compressedDataLength);
        return result;
    }
}