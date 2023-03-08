/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.zos.core.utils.internal.DirectBufferHelperImpl;

/** Test implementation of DirectBufferHelper. */
class LoggingTestBufferHelper extends DirectBufferHelperImpl {

    Map<Long, ByteBuffer> buffers = new HashMap<Long, ByteBuffer>();

    public LoggingTestBufferHelper() {
        super();
    }

    public LoggingTestBufferHelper(Map<Long, ByteBuffer> buffers) {
        super();
        this.buffers = buffers;
    }

    public void addBuffer(long address, ByteBuffer testBuffer) {
        buffers.put(address, testBuffer);
    }

    @Override
    protected ByteBuffer mapDirectByteBuffer(long address, int size) {
        return buffers.get(address);
    }

    public Map<BufferKey, ByteBuffer> getSegments() {
        return segments.get();
    }

    public BufferHolder getRecentBuffer() {
        return recentBuffer.get();
    }
}