package io.openliberty.grpc.internal.servlet;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.http2.GrpcServletServices;

import io.openliberty.grpc.internal.config.GrpcServiceConfigImpl;

/**
 * Keep track of gRPC service names and their class names
 */
class GrpcServletApplication {

	private Set<String> serviceNames = new HashSet<String>();
	private Set<String> serviceClassNames = new HashSet<String>();
	private String j2eeAppName;

	/**
	 * Add a service name to context path mapping
	 * 
	 * @param String serviceName
	 * @param String ontextPath
	 */
	void addServiceName(String serviceName, String contextPath, Class<?> clazz) {
		serviceNames.add(serviceName);
		if (serviceName != null && contextPath != null && clazz != null) {
			GrpcServletServices.addServletGrpcService(serviceName, contextPath, clazz);
		}
	}

	/**
	 * Add the set of class names that contain gRPC services. These classes will be
	 * initialized by Liberty during startup.
	 * 
	 * @param Set<String> service class names
	 */
	void addServiceClassName(String name) {
		serviceClassNames.add(name);
	}

	/**
	 * @return Set<String> all service class names that have been registered
	 */
	Set<String> getServiceClassNames() {
		return serviceClassNames;
	}

	/**
	 * Set the current J2EE application name and register it with
	 * GrpcServiceConfigImpl
	 * 
	 * @param name
	 */
	void setAppName(String name) {
		j2eeAppName = name;
		GrpcServiceConfigImpl.addApplication(j2eeAppName);
	}

	/**
	 * Unregister and clean up any associated services and mappings
	 */
	void destroy() {
		for (String service : serviceNames) {
			GrpcServletServices.removeServletGrpcService(service);
		}
		if (j2eeAppName != null) {
			GrpcServiceConfigImpl.removeApplication(j2eeAppName);
		}
		serviceNames = null;
		serviceClassNames = null;
	}
}
