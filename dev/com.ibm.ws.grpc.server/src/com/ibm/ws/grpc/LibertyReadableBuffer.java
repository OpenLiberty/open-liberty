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

package com.ibm.ws.grpc;

import com.google.common.base.Preconditions;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.grpc.internal.AbstractReadableBuffer;
import io.grpc.internal.ReadableBuffer;
import io.grpc.internal.ReadableBuffers;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class LibertyReadableBuffer extends AbstractReadableBuffer {
	
	private final WsByteBuffer buffer;
	private boolean closed;

	LibertyReadableBuffer(WsByteBuffer buffer) {
		this.buffer = Preconditions.checkNotNull(buffer, "buffer");
	}

	WsByteBuffer buffer() {
		return buffer;
	}

	@Override
	public int readableBytes() {
		return buffer.remaining();
	}

	@Override
	public void skipBytes(int length) {
		buffer.position(buffer.position() + length);
	}

	@Override
	public int readUnsignedByte() {
		return buffer.get() & 0xFF;
	}

	@Override
	public void readBytes(byte[] dest, int index, int length) {
		buffer.get(dest, index, length);
	}

	public void readBytes(ByteBuffer dest) {
		while (dest.hasRemaining() && buffer.hasRemaining()) {
			dest.put(buffer.get());
		}
	}

	@Override
	public void readBytes(OutputStream dest, int length) {
		try {
			for (int i = 0; i < length; ++i) {
				dest.write(buffer.get());
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public ReadableBuffer readBytes(int length) {
		byte[] data = new byte[length];
		buffer.get(data);
		return ReadableBuffers.wrap(data);
	}

	@Override
	public boolean hasArray() {
		return buffer.hasArray();
	}

	@Override
	public byte[] array() {
		return buffer.array();
	}

	@Override
	public int arrayOffset() {
		return buffer.arrayOffset();
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			buffer.release();
		}
	}
}
