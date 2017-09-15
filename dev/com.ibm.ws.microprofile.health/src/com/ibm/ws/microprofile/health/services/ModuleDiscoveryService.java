package com.ibm.ws.microprofile.health.services;

import java.util.List;

public interface ModuleDiscoveryService {

	/**
	 * Returns list of strings like app#war that are registered
	 */
	public List<String> getDiscoveredModuleNames();
	
}
