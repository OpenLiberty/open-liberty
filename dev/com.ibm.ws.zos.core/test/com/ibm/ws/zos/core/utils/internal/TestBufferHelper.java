/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.utils.internal;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/** Test implementation of DirectBufferHelper. */
class TestBufferHelper extends DirectBufferHelperImpl {

    Map<Long, ByteBuffer> buffers = new HashMap<Long, ByteBuffer>();

    public TestBufferHelper() {
        super();
    }

    public TestBufferHelper(Map<Long, ByteBuffer> buffers) {
        super();
        this.buffers = buffers;
    }

    public void addBuffer(long address, ByteBuffer testBuffer) {
        buffers.put(address, testBuffer);
    }

    @Override
    protected ByteBuffer mapDirectByteBuffer(long address, int size) {
        return buffers.get(address).asReadOnlyBuffer();
    }

    public Map<BufferKey, ByteBuffer> getSegments() {
        return segments.get();
    }

    public BufferHolder getRecentBuffer() {
        return recentBuffer.get();
    }
}