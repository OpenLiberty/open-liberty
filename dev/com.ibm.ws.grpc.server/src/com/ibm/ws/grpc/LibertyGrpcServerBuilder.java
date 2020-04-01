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

import io.grpc.ServerStreamTracer.Factory;
import io.grpc.internal.AbstractServerImplBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A builder for Liberty-based gRPC servers.
 */
public final class LibertyGrpcServerBuilder extends AbstractServerImplBuilder<LibertyGrpcServerBuilder> {

	public LibertyGrpcServerBuilder() {
	}

	/**
	 * Currently returns a single LibertyServer
	 */
	@Override
	protected List<? extends LibertyGrpcServer> buildTransportServers(List<? extends Factory> streamTracerFactories) {
		List<LibertyGrpcServer> transportServers = new ArrayList<LibertyGrpcServer>(1);
		transportServers.add(new LibertyGrpcServer());
		return Collections.unmodifiableList(transportServers);
	}

	@Override
	public LibertyGrpcServerBuilder useTransportSecurity(File certChain, File privateKey) {
		return null;
	}
}
