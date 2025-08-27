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
package org.apache.xml.security.stax.impl.util;

import java.io.IOException;
import java.io.InputStream;

/**
 */
public class MultiInputStream extends InputStream {

    private final InputStream[] inputStreams;
    private final int inputStreamCount;
    private int inputStreamIndex;

    public MultiInputStream(InputStream... inputStreams) {
        this.inputStreams = inputStreams;
        this.inputStreamCount = inputStreams.length;
    }

    @Override
    public int read() throws IOException {
        for (int i = inputStreamIndex; i < inputStreamCount; i++) {
            int b = inputStreams[i].read();
            if (b >= 0) {
                return b;
            }
            inputStreamIndex++;
        }
        return -1;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        for (int i = inputStreamIndex; i < inputStreamCount; i++) {
            int read = inputStreams[i].read(b, off, len);
            if (read >= 0) {
                return read;
            }
            inputStreamIndex++;
        }
        return -1;
    }

    @Override
    public long skip(long n) throws IOException {
        throw new IOException("skip() not supported");
    }

    @Override
    public int available() throws IOException {
        if (inputStreamIndex < inputStreamCount) {
            return inputStreams[inputStreamIndex].available();
        }
        return 0;
    }

    @Override
    public void close() throws IOException {
        IOException e = new IOException("Failed to close all streams!");
        for (int i = 0; i < inputStreamCount; i++) {
            try {
                inputStreams[i].close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
        }
        if (e.getSuppressed().length > 0) {
            throw e;
        }
    }
}
