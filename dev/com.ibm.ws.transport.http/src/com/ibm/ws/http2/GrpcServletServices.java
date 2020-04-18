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
package com.ibm.ws.http2;

import java.util.HashMap;
import java.util.Map;

/**
 * Keep track of gRPC service to application mappings
 */
public class GrpcServletServices {

    private static final Map<String, String> servletGrpcServices = new HashMap<String, String>();

    /**
     * Register an a gRPC service with its application
     *
     * @param String gRPC service name
     * @param String contextRoot for the app
     */
    public static synchronized void addServletGrpcService(String service, String contextRoot) {
        if (servletGrpcServices.containsKey(service)) {
            throw new RuntimeException("duplicate gRPC service added: " + service);
        } else {
            servletGrpcServices.put(service, contextRoot);
        }
    }

    /**
     * Remove a service mapping
     *
     * @param String service
     */
    public static synchronized void removeServletGrpcService(String service) {
        servletGrpcServices.remove(service);
    }

    /**
     * Return the set of servlet gRPC Services that have been registered.
     *
     * @return HashMap<String service, String contextRoot> or null if the set is empty
     */
    public static synchronized Map<String, String> getServletGrpcServices() {
        if (servletGrpcServices.isEmpty()) {
            return null;
        }
        return servletGrpcServices;
    }

    public static synchronized void destroy() {
        servletGrpcServices.clear();
    }
}
