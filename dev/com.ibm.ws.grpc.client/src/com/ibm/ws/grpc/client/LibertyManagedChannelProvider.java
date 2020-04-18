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
package com.ibm.ws.grpc.client;

import io.grpc.ManagedChannelProvider;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

/**
 * io.grpc.ManagedChannelProvider that adds a LibertyClientInterceptor to all
 * calls and delegates to the NettyChannelBuilder
 */
public class LibertyManagedChannelProvider extends ManagedChannelProvider {

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public int priority() {
		return 10;
	}

	@Override
	public NettyChannelBuilder builderForAddress(String name, int port) {
		return NettyChannelBuilder.forAddress(name, port).intercept(new LibertyClientInterceptor());
	}

	@Override
	public NettyChannelBuilder builderForTarget(String target) {
		return NettyChannelBuilder.forTarget(target).intercept(new LibertyClientInterceptor());
	}
}
