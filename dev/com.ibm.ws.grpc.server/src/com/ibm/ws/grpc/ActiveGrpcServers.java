package com.ibm.ws.grpc;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import io.grpc.Server;

/**
 * 
 */
public class ActiveGrpcServers {
	
	private static ConcurrentHashMap<String, Server> serverList = new ConcurrentHashMap<String, Server>();
	
	protected static Collection<Server> getServerList() {
		return serverList.values();
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
