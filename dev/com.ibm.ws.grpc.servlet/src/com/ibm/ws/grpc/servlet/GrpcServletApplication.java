package com.ibm.ws.grpc.servlet;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.http2.GrpcServletServices;

/**
 * Keep track of gRPC service names and their class names
 */
class GrpcServletApplication {

	Set<String> serviceNames;
	Set<String> serviceClassNames;

	/**
	 * Add a service name to context path mapping
	 * @param String serviceName
	 * @param String ontextPath
	 */
	void addServiceName(String serviceName, String contextPath, Class<?> clazz) {
		if (serviceNames == null) {
			serviceNames = new HashSet<String>();
		}
		serviceNames.add(serviceName);
		if (serviceName != null && contextPath != null && clazz != null) {
			GrpcServletServices.addServletGrpcService(serviceName, contextPath, clazz);
		}
	}

	/**
	 * Add the set of class names that contain gRPC services.
	 * These classes will be initialized by Libery during startup.
	 * @param Set<String> service class names
	 */
	void addServiceClassName(String name) {
		if (serviceClassNames == null) {
			serviceClassNames = new HashSet<String>();
		}
		serviceClassNames.add(name);
	}

	/**
	 * @return Set<String> all service class names that have been registered
	 */
	Set<String> getServiceClassNames() {
		return serviceClassNames;
	}

	/**
	 * Unregister and clean up any associated services and mappings 
	 */
	void destroy() {
		for (String service : serviceNames) {
			GrpcServletServices.removeServletGrpcService(service);
		}
		serviceNames = null;
		serviceClassNames = null;
	}
}


