/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation and others.
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
package io.openliberty.grpc.internal.servlet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.ws.http2.GrpcServletServices.ServiceInformation;
import com.ibm.ws.managedobject.ManagedObjectException;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.servlet.ServletServerBuilder;
import io.openliberty.grpc.annotation.GrpcService;
import io.openliberty.grpc.internal.GrpcManagedObjectProvider;
import io.openliberty.grpc.internal.GrpcMessages;
import io.openliberty.grpc.internal.config.GrpcServiceConfigHolder;
import io.openliberty.grpc.server.monitor.GrpcMonitoringServerInterceptorService;

public class GrpcServletUtils {

    private static final TraceComponent tc = Tr.register(GrpcServletUtils.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

    private static final LibertyAuthorizationInterceptor authInterceptor = new LibertyAuthorizationInterceptor();

    /**
     * Removes the application context root from the front gRPC request path. For
     * example: "app_context_root/helloworld.Greeter/SayHello" ->
     * "helloworld.Greeter/SayHello"
     * 
     * @param String original request path
     * @return String request path without app context root
     */
    public static String translateLibertyPath(String requestPath) {
        int count = requestPath.length() - requestPath.replace("/", "").length();
        // if count < 2 there is no app context root to remove
        if (count == 2) {
            int index = requestPath.indexOf('/');
            requestPath = requestPath.substring(index + 1);
        }
        return requestPath;
    }

    /**
     * Given a request path use GrpcServletServices.getServletGrpcServices to map
     * the target service class, and using that Class map the target
     * java.lang.reflect.Method.
     *
     * Returns null if the service is not registered (unknown path). Throws
     * GrpcMethodNotFoundException if the service IS registered but the method cannot
     * be resolved. An unknown service is handled further up the stack, while an
     * unresolvable method on a known service should be rejected.
     *
     * @param String requestPath in the format of "service.Name/MethodName"
     * @return Method invoked by the request, or null if the service is not registered
     * @throws GrpcMethodNotFoundException if the service is registered but the method cannot be resolved
     */
    public static Method getTargetMethod(String requestPath) {
        int index = requestPath.indexOf('/');
        String service = requestPath.substring(0, index);

        Map<String, ServiceInformation> services = GrpcServletServices.getServletGrpcServices();
        if (services != null) {
            ServiceInformation info = services.get(service);
            if (info != null) {
                Class<?> clazz = info.getServiceClass();
                if (clazz != null) {
                    index = requestPath.indexOf('/');
                    String methodName = requestPath.substring(index + 1);
                    if (methodName.contains("_")) {
                        // Underscore-containing names are converted to
                        // camelCase by protoc-gen-grpc-java
                        methodName = convertToCamelCase(methodName);
                    } else if (!methodName.isEmpty()) {
                        // Standard PascalCase proto RPC names are lowercased only on the first character
                        // by protoc-gen-grpc-java. Need to normalize for a strict matching of method
                        // name to avoid ambiguity when requests make it to the server.
                        methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
                    }
                    // Match on both name and signature
                    Method candidate = null;
                    for (Method m : clazz.getMethods()) {
                        if (!m.getName().equals(methodName)) {
                            continue;
                        }
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 0 ||
                            !io.grpc.stub.StreamObserver.class.isAssignableFrom(params[params.length - 1])) {
                            continue;
                        }
                        if (candidate != null) {
                            // More than one same-named method, so it is ambiguous
                            // Deny this rather than guess
                            throw new GrpcMethodNotFoundException(requestPath);
                        }
                        candidate = m;
                    }
                    if (candidate != null) {
                        return candidate;
                    }
                    // Service is registered but the method name could not be resolved.
                    throw new GrpcMethodNotFoundException(requestPath);
                }
            }
        }
        return null;
    }

    /**
     * @param interceptorClassNames
     * @param interceptors
     */
    private static void getServerXmlInterceptors(List<String> interceptorClassNames, List<ServerInterceptor> interceptors) {
        for (String className : interceptorClassNames) {
            try {
                ServerInterceptor interceptor = (ServerInterceptor) GrpcManagedObjectProvider.createObjectFromClassName(className);
                if (interceptor != null) {
                    interceptors.add(interceptor);
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException | ManagedObjectException e) {
                Tr.warning(tc, "invalid.serverinterceptor", e.getMessage());
            }
        }
    }

    /**
     * @param annotationInterceptors
     * @param interceptors
     */
    private static void getAnnotationInterceptors(Class<? extends ServerInterceptor>[] annotationInterceptors, List<ServerInterceptor> interceptors) {
        for (Class<? extends ServerInterceptor> interceptorClass : annotationInterceptors) {
            ServerInterceptor interceptor;
            try {
                interceptor = (ServerInterceptor) GrpcManagedObjectProvider.createObjectFromClass(interceptorClass);
                interceptors.add(interceptor);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException
                    | ManagedObjectException e) {
                Tr.warning(tc, "invalid.serverinterceptor", e.getMessage());
            }
        }
    }

    /**
     * @param String service name
     * @param BindableService service instance
     * 
     * @return the (merged) list of server interceptors registered in server.xml and via @GrpcService
     */
    private static List<ServerInterceptor> getUserInterceptors(String serviceName, BindableService service) {

        List<ServerInterceptor> interceptors = new LinkedList<ServerInterceptor>();

        // get the string containing any class names of server interceptors defined in server.xml via <gprc serviceInterceptors="..."/>
        String interceptorListString = GrpcServiceConfigHolder.getServiceInterceptors(serviceName);

        // get the set of classes registered to this class via @GrpcService(interceptors="...")
        Class<? extends ServerInterceptor>[] annotationInterceptors = null;
        if (service.getClass().getAnnotation(GrpcService.class) != null) {
            annotationInterceptors = service.getClass().getAnnotation(GrpcService.class).interceptors();
        }

        if (interceptorListString != null) {
            List<String> items = new ArrayList<String>(Arrays.asList(interceptorListString.split("\\s*,\\s*")));
            if (items != null && !items.isEmpty()) {
                // filter out interceptors that are also defined via @GrpcService
                if (annotationInterceptors != null) {
                    for (Class<? extends ServerInterceptor> clazz : annotationInterceptors) {
                        String className = clazz.getName();
                        if (items.contains(className)) {
                            items.remove(className);
                        }
                    }
                }
                getServerXmlInterceptors(items, interceptors);
            }
        }
        if (annotationInterceptors != null && annotationInterceptors.length > 0) {
            getAnnotationInterceptors(annotationInterceptors, interceptors);
        }
        // flip the interceptor list so that @GrpcService interceptors will run after <grpc/> interceptors
        if (!interceptors.isEmpty()) {
            Collections.reverse(interceptors);
        }
        return interceptors;

    }


    /**
     * Register grpc services with a ServletServerBuilder and apply liberty-specific
     * configurations
     * 
     * @param bindableServices
     * @param serverBuilder
     */
    public static void addServices(List<? extends BindableService> bindableServices,
            ServletServerBuilder serverBuilder, String appName) {
        for (BindableService service : bindableServices) {
            String serviceName = service.bindService().getServiceDescriptor().getName();

            // set any user-defined server interceptors and add the service
            List<ServerInterceptor> interceptors = getUserInterceptors(serviceName, service);

            // add Liberty auth interceptor to every service
            interceptors.add(authInterceptor);
            // add monitoring interceptor to every service
            ServerInterceptor monitoringInterceptor = createMonitoringServerInterceptor(serviceName, appName);
            if (monitoringInterceptor != null) {
                interceptors.add(monitoringInterceptor);
            }
            serverBuilder.addService(ServerInterceptors.intercept(service, interceptors));

            // set the max inbound msg size, if it's configured
            int maxInboundMsgSize = GrpcServiceConfigHolder.getMaxInboundMessageSize(serviceName);
            if (maxInboundMsgSize != -1) {
                serverBuilder.maxInboundMessageSize(maxInboundMsgSize);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "gRPC service {0} has been registered", serviceName);
            }
        }
    }
    
    
    private static ServerInterceptor createMonitoringServerInterceptor(String serviceName, String appName) {
        // create the monitoring interceptor only if the monitor feature is enabled 
        ServerInterceptor interceptor = GrpcServerComponent.getMonitoringServerInterceptor(serviceName, appName);
        if (interceptor != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "monitoring interceptor has been added to service {0}", serviceName);
            }
        }
        return interceptor;
    }

    /**
     * Given a String, remove any underscores and convert to camel case
     * Examples:
     *    "some_method_name" -> "someMethodName",
     *    "some_MeTHod_NAME" -> "someMethodName"
     *
     * @param String name
     * @return String
     */
    private static String convertToCamelCase(String name) {
        final StringBuilder builder = new StringBuilder(name.length());
        boolean firstSection = true;
        for (String section : name.split("_")) {
            if (!section.isEmpty()) {
                if (firstSection) {
                    firstSection = false;
                    builder.append(Character.toLowerCase(section.charAt(0)));
                } else {
                    builder.append(Character.toUpperCase(section.charAt(0)));
                }
                if (section.length() > 1) {
                    builder.append(section.substring(1));
                }
            }
        }
        return builder.toString();
    }

    /**
     * Thrown by getTargetMethod() when the target gRPC service is registered but
     * the requested method name cannot be resolved to a Java method. This is
     * distinct from a null return, which means the service itself is not registered.
     */
    public static class GrpcMethodNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public GrpcMethodNotFoundException(String requestPath) {
            super();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "gRPC method not found for registered service: " + requestPath);
            }
        }
    }
}
