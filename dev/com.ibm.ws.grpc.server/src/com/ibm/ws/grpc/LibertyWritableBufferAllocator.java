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

import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

import io.grpc.internal.WritableBuffer;
import io.grpc.internal.WritableBufferAllocator;

public class LibertyWritableBufferAllocator implements WritableBufferAllocator {

	WsByteBufferPoolManager bufManager;

	public LibertyWritableBufferAllocator() {
		bufManager = ChannelFrameworkFactory.getBufferManager();
	}

	@Override
	public WritableBuffer allocate(int size) {
		return new LibertyWritableBuffer(bufManager.allocate(size));
	}
}