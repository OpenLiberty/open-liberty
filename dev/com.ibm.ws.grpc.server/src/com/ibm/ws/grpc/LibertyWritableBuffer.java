/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.grpc;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.grpc.internal.WritableBuffer;

class LibertyWritableBuffer implements WritableBuffer {

	private final WsByteBuffer bytebuf;

	LibertyWritableBuffer(WsByteBuffer bytebuf) {
		this.bytebuf = bytebuf;
	}

	@Override
	public void write(byte[] src, int srcIndex, int length) {
		bytebuf.put(src, srcIndex, length);
	}

	@Override
	public void write(byte b) {
		bytebuf.put(b);
	}

	@Override
	public int writableBytes() {
		return bytebuf.remaining();
	}

	@Override
	public int readableBytes() {
		return bytebuf.position();
	}

	@Override
	public void release() {
		bytebuf.release();
	}

	WsByteBuffer bytebuf() {
		return bytebuf;
	}
}
