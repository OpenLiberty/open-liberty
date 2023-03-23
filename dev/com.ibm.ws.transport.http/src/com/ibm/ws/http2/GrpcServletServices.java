/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    private static final Map<String, ServiceInformation> servletGrpcServices = new HashMap<String, ServiceInformation>();

    public static boolean grpcInUse = false;

    /**
     * Register an a gRPC service with its application
     *
     * @param String gRPC service name
     * @param String contextRoot for the app
     */
    public static synchronized void addServletGrpcService(String service, String contextRoot, Class<?> clazz, String j2eeName) {

        grpcInUse = true;

        if (servletGrpcServices.containsKey(service)) {
            throw new RuntimeException("duplicate gRPC service added: " + service);
        } else {
            servletGrpcServices.put(service, new ServiceInformation(contextRoot, clazz, j2eeName));
        }
    }

    /**
     * Remove a service mapping
     *
     * @param String service
     */
    public static synchronized void removeServletGrpcService(String service, String j2eeName) {
        ServiceInformation info = servletGrpcServices.get(service);
        if (info != null) {
            if (j2eeName == null) {
                if (info.j2eeName != null) {
                    return;
                }
                servletGrpcServices.remove(service);
            } else if (j2eeName.equals(info.j2eeName)) {
                servletGrpcServices.remove(service);
            }

        }

    }

    /**
     * Return the set of servlet gRPC Services that have been registered.
     *
     * @return HashMap<String service, String contextRoot> or null if the set is empty
     */
    public static synchronized Map<String, ServiceInformation> getServletGrpcServices() {
        if (servletGrpcServices.isEmpty()) {
            return null;
        }
        return servletGrpcServices;
    }

    public static void destroy() {
        servletGrpcServices.clear();
    }

    public static class ServiceInformation {
        String contextRoot;
        Class<?> clazz;
        String j2eeName;

        ServiceInformation(String root, Class<?> c, String name) {
            contextRoot = root;
            clazz = c;
            j2eeName = name;
        }

        public String getContextRoot() {
            return contextRoot;
        }

        public Class<?> getServiceClass() {
            return clazz;
        }
    }
}
