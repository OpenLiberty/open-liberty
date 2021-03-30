/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.wsspi.bytebuffer;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

/**
 *
 */
public class WsByteBufferPoolManagerNettyImpl implements WsByteBufferPoolManager {

    @Override
    public WsByteBuffer allocate(int entrySize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer allocateDirect(int entrySize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer allocateFileChannelBuffer(FileChannel fc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer wrap(byte[] array) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer wrap(byte[] array, int offset, int length) throws IndexOutOfBoundsException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer wrap(ByteBuffer buffer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer duplicate(WsByteBuffer oWsByteBuffer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer slice(WsByteBuffer oWsByteBuffer) {
        // TODO Auto-generated method stub
        return null;
    }

}
