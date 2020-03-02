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
public final class LibertyServerBuilder extends AbstractServerImplBuilder<LibertyServerBuilder> {

	public LibertyServerBuilder() {
	}

	/**
	 * Currently returns a single LibertyServer
	 */
	@Override
	protected List<? extends LibertyServer> buildTransportServers(List<? extends Factory> streamTracerFactories) {
		List<LibertyServer> transportServers = new ArrayList<LibertyServer>(1);
		transportServers.add(new LibertyServer());
		return Collections.unmodifiableList(transportServers);
	}

	@Override
	public LibertyServerBuilder useTransportSecurity(File certChain, File privateKey) {
		return null;
	}
}
