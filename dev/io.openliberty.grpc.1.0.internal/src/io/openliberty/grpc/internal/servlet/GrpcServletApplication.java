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
package io.openliberty.grpc.internal.servlet;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.ws.managedobject.ManagedObjectContext;

import io.openliberty.grpc.internal.config.GrpcServiceConfigImpl;

/**
 * Keep track of gRPC service names and their class names
 */
class GrpcServletApplication {

	private Set<String> serviceNames = new HashSet<String>();
	private Set<String> serviceClassNames = new HashSet<String>();
	private Set<ManagedObjectContext> managedObectContexts = new HashSet<ManagedObjectContext>();
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
	 * @return the J2EE application name for this GrpcServletApplication
	 */
	String getAppName() {
		return j2eeAppName;
	}

	/**
	 * Keep track of the ManagedObject that was used to create a service instance
	 *
	 * @param ManagedObject
	 */
	void addManagedObjectContext(ManagedObjectContext mo) {
		if (mo != null) {
			managedObectContexts.add(mo);
		}
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
		for (ManagedObjectContext mo : managedObectContexts) {
			mo.release();
		}
		managedObectContexts = null;
		serviceNames = null;
		serviceClassNames = null;
	}
}
