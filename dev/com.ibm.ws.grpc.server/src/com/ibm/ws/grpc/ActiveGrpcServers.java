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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import io.grpc.Server;

/**
 * Keep track of active io.grpc.Server instances
 */
public class ActiveGrpcServers {
	
	private static ConcurrentHashMap<String, Server> serverList = new ConcurrentHashMap<String, Server>();
	
	protected static Collection<Server> getServerList() {
		return serverList.values();
	}
	
	protected static Server getServer(String name) {
		return serverList.get(name);
	}
	protected static void addServer(String name, Server s) {
		serverList.put(name, s);
	}
	
	protected static void removeServer(String name) {
		serverList.remove(name);
	}
	
	protected static void removeAllServers() {
		serverList.clear();
	}
}
