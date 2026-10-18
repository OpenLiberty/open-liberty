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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    if (!methodName.isEmpty()) {
                        // Normalize method names the same way protoc-gen-grpc-java does
                        methodName = toJavaMethodName(methodName);
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
     * Java reserved keywords — mirrors the java_keywords set in protoc-gen-grpc-java's
     * java_generator.cpp (Java SE 8 list, which is what the code generator targets).
     * When MixedLower produces one of these names it appends "_"; we must do the same
     * so that our reflection-based lookup matches the generated ImplBase method name.
     */
    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
        "abstract", "assert",   "boolean",  "break",        "byte",
        "case",     "catch",    "char",     "class",        "const",
        "continue", "default",  "do",       "double",       "else",
        "enum",     "extends",  "final",    "finally",      "float",
        "for",      "goto",     "if",       "implements",   "import",
        "instanceof", "int",    "interface","long",         "native",
        "new",      "package",  "private",  "protected",    "public",
        "return",   "short",    "static",   "strictfp",     "super",
        "switch",   "synchronized", "this", "throw",        "throws",
        "transient","try",      "void",     "volatile",     "while",
        // reserved literals also treated as keywords by the generator
        "true",     "false"
    ));

    /**
     * Converts a proto RPC name to the Java method name that protoc-gen-grpc-java
     * generates on the service ImplBase class, matching the MixedLower function in
     * java_generator.cpp exactly:
     *  - The first character is lowercased.
     *  - Each underscore is consumed and the immediately following character is
     *    uppercased (matching MixedLower's after_underscore flag).
     *  - All other characters are copied verbatim.
     *  - If the resulting identifier is a Java reserved keyword, a trailing "_" is
     *    appended (e.g. proto RPC "New" -> Java method "new_").
     * Examples:
     *    "SayHello"         -> "sayHello"
     *    "some_method_name" -> "someMethodName"
     *    "New"              -> "new_"
     *    "Return"           -> "return_"
     *
     * @param name the proto RPC method name extracted from the request path
     * @return the corresponding Java method name on the generated ImplBase class
     */
    private static String toJavaMethodName(String name) {
        final StringBuilder builder = new StringBuilder(name.length() + 1);
        builder.append(Character.toLowerCase(name.charAt(0)));
        boolean afterUnderscore = false;
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                afterUnderscore = true;
            } else {
                builder.append(afterUnderscore ? Character.toUpperCase(c) : c);
                afterUnderscore = false;
            }
        }
        String result = builder.toString();
        return JAVA_KEYWORDS.contains(result) ? result + "_" : result;
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
