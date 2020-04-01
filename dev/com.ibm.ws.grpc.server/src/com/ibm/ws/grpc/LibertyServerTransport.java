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

import com.google.common.util.concurrent.ListenableFuture;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
//import com.ibm.ws.http.channel.h2internal.H2InboundLink;

import io.grpc.InternalChannelz.SocketStats;
import io.grpc.InternalLogId;
import io.grpc.Status;
import io.grpc.internal.ServerTransport;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The Liberty-based server transport.
 */
class LibertyServerTransport implements ServerTransport {

	private final InternalLogId logId;

	public LibertyServerTransport() {
		logId = InternalLogId.allocate(getClass().getName(), "LibertyServer");
	}

	@Override
	public ScheduledExecutorService getScheduledExecutorService() {
		return CHFWBundle.getScheduledExecutorService();
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void shutdownNow(Status reason) {
	}

	@Override
	public InternalLogId getLogId() {
		return logId;
	}

	@Override
	public ListenableFuture<SocketStats> getStats() {
		return null;
	}

}
