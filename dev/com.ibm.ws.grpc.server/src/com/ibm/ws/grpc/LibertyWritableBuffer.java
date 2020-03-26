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

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.grpc.internal.WritableBuffer;

class LibertyWritableBuffer implements WritableBuffer {

	private final WsByteBuffer bytebuf;

	LibertyWritableBuffer(@Sensitive WsByteBuffer bytebuf) {
		this.bytebuf = bytebuf;
	}

	@Override
	public void write(@Sensitive byte[] src, int srcIndex, int length) {
		bytebuf.put(src, srcIndex, length);
	}

	@Override
	public void write(@Sensitive byte b) {
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
