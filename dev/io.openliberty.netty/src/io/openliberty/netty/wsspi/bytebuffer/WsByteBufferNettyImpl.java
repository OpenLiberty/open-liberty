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
import java.nio.ByteOrder;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.buffer.ByteBuf;

/**
 *
 */
public class WsByteBufferNettyImpl implements WsByteBuffer {

    /**  */
    private static final long serialVersionUID = 1L;
    private final ByteBuf buf;

    /**
     * @param directBuffer
     */
    public WsByteBufferNettyImpl(ByteBuf bb) {
        buf = bb;
    }

    @Override
    public boolean setBufferAction(int value) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public byte[] array() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int arrayOffset() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer compact() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int compareTo(Object obj) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public char getChar() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public char getChar(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer putChar(char value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putChar(int index, char value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putChar(char[] values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putChar(char[] values, int off, int len) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getDouble() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getDouble(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer putDouble(double value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putDouble(int index, double value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getFloat() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getFloat(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer putFloat(float value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putFloat(int index, float value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getInt() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getInt(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer putInt(int value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putInt(int index, int value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLong() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getLong(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer putLong(long value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putLong(int index, long value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public short getShort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getShort(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer putShort(short value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putShort(int index, short value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer putString(String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasArray() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ByteOrder order() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer order(ByteOrder bo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer clear() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int capacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer flip() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte get() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int position() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer position(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer limit(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int limit() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int remaining() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public WsByteBuffer mark() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer reset() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer rewind() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasRemaining() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public WsByteBuffer duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer slice() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer get(byte[] dst) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer get(byte[] dst, int offset, int length) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte get(int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isDirect() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public WsByteBuffer put(byte value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer put(byte[] value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer put(byte[] src, int offset, int length) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer put(int index, byte value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer put(ByteBuffer src) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer put(WsByteBuffer src) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WsByteBuffer put(WsByteBuffer[] src) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ByteBuffer getWrappedByteBuffer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ByteBuffer getWrappedByteBufferNonSafe() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setReadOnly(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getReadOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeFromLeakDetection() {
        // TODO Auto-generated method stub

    }

    @Override
    public void release() {
        // TODO Auto-generated method stub

    }

    @Override
    public int getType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getStatus() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setStatus(int value) {
        // TODO Auto-generated method stub

    }

}
