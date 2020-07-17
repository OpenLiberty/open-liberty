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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.ws.http2.GrpcServletServices.ServiceInformation;
import com.ibm.ws.security.authorization.util.RoleMethodAuthUtil;
import com.ibm.ws.security.authorization.util.UnauthenticatedException;

import io.grpc.BindableService;
import io.grpc.Metadata;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.servlet.ServletServerBuilder;
import io.openliberty.grpc.internal.GrpcMessages;
import io.openliberty.grpc.internal.config.GrpcServiceConfigHolder;

public class GrpcServletUtils {

	private static final TraceComponent tc = Tr.register(GrpcServletUtils.class, GrpcMessages.GRPC_TRACE_NAME, GrpcMessages.GRPC_BUNDLE);

	public final static String LIBERTY_AUTH_KEY_STRING = "libertyAuthCheck";
	private final static Map<String, Boolean> authMap = new ConcurrentHashMap<String, Boolean>();
	public static final Metadata.Key<String> LIBERTY_AUTH_KEY = Metadata.Key.of(LIBERTY_AUTH_KEY_STRING,
			Metadata.ASCII_STRING_MARSHALLER);

	private static final LibertyAuthorizationInterceptor authInterceptor = new LibertyAuthorizationInterceptor();

	/**
	 * Helper method to add the "authorized" flag to the byte arrays that will get
	 * built into Metadata
	 * 
	 * @param byteArrays
	 * @param req
	 * @param authorized
	 */
	public static void addLibertyAuthHeader(List<byte[]> byteArrays, HttpServletRequest req, boolean authorized) {
		byteArrays.add(GrpcServletUtils.LIBERTY_AUTH_KEY.name().getBytes(StandardCharsets.US_ASCII));
		byteArrays.add((String.valueOf(req.hashCode())).getBytes(StandardCharsets.US_ASCII));
		authMap.put(String.valueOf(req.hashCode()), authorized);
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "adding {0} to authMap with value {1}", req.hashCode(), authorized);
		}
	}

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
	 * java.lang.reflect.Method
	 * 
	 * @param String requestPath in the format of "service.Name/MethodName"
	 * @return Method invoked by the request
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
					char c[] = methodName.toCharArray();
					c[0] = Character.toLowerCase(c[0]);
					methodName = new String(c);
					Method[] methods = clazz.getMethods();
					for (Method m : methods) {
						if (m.getName().equals(methodName)) {
							// TODO: we're only checking the method name here; should we check signature?
							return m;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Checks if a given request is authorized to access the requested method, by
	 * scanning the requested method for @DenyAll, @RolesAllowed, or @AllowAll and
	 * validating the request's Subject
	 * 
	 * @param req
	 * @param res
	 * @param requestPath
	 * @return
	 */
	@FFDCIgnore({ UnauthenticatedException.class, UnauthenticatedException.class, AccessDeniedException.class })
	public static boolean doServletAuth(HttpServletRequest req, HttpServletResponse res, String requestPath) {
		try {
			handleMessage(req, requestPath);
			return true;
		} catch (UnauthenticatedException ex) {
			try {
				if (authenticate(req, res)) {
					// try again with authenticated user
					handleMessage(req, requestPath);
					return true;
				}
			} catch (UnauthenticatedException | AccessDeniedException ex2) {
				// TODO: catch other exceptions
			}

		} catch (AccessDeniedException e) {
			// TODO: catch other exceptions
		}
		return false;
	}

	private static boolean authenticate(HttpServletRequest req, HttpServletResponse res) {
		try {
			return req.authenticate(res);
		} catch (IOException | ServletException e) {
			// AutoFFDC
		}
		return false;
	}

	private static void handleMessage(HttpServletRequest req, String path)
			throws UnauthenticatedException, AccessDeniedException {

		Method method = GrpcServletUtils.getTargetMethod(path);
		if (method == null) {
			// the requested service doesn't exist - we'll handle this further up
			return;
		}
		if (RoleMethodAuthUtil.parseMethodSecurity(method, req.getUserPrincipal(), s -> req.isUserInRole(s))) {
			return;
		}
		throw new AccessDeniedException("Unauthorized");
	}

	/**
	 * 
	 * @param key the LIBERTY_AUTH_KEY to check
	 * @return the authorization value for the key in GrpcServletUtils.authMap, or
	 *         false if the key is null
	 */
	public static boolean isAuthorized(String key) {
		if (key == null) {
			return false;
		}
		else return Boolean.TRUE.equals(authMap.remove(key));
	}

	/**
	 * @param service name
	 * @return the list of server interceptors registered for a given service, or an
	 *         empty list if none are registered
	 */
	public static List<ServerInterceptor> getUserInterceptors(String service) {
		List<ServerInterceptor> interceptors = new LinkedList<ServerInterceptor>();
		String interceptorListString = GrpcServiceConfigHolder.getServiceInterceptors(service);

		if (interceptorListString != null) {
			// TODO: wildcard support
			List<String> items = Arrays.asList(interceptorListString.split("\\s*,\\s*"));
			if (!items.isEmpty()) {
				for (String className : items) {
					try {
						// use the app classloader to load the interceptor
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
						Class<?> clazz = Class.forName(className, true, cl);
						ServerInterceptor interceptor = (ServerInterceptor) clazz.getDeclaredConstructor()
								.newInstance();
						interceptors.add(interceptor);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
							| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
							| SecurityException e) {
						Tr.warning(tc, "invalid.serverinterceptor", e.getMessage());
					}
				}
			}
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
			List<ServerInterceptor> interceptors = GrpcServletUtils.getUserInterceptors(serviceName);
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
		ServerInterceptor interceptor = null;
		// monitoring interceptor 
		final String className = "io.openliberty.grpc.internal.monitor.GrpcMonitoringServerInterceptor";
		try {
			Class<?> clazz = Class.forName(className);
			interceptor = (ServerInterceptor) clazz.getDeclaredConstructor(String.class, String.class)
					.newInstance(serviceName, appName);
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "monitoring interceptor has been added to service {0}", serviceName);
			}
		} catch (Exception e) {
			// an exception can happen if the monitoring package is not loaded 
			FFDCFilter.processException(e, GrpcServletUtils.class.getName(), "GrpcServletUtils");
        }

		return interceptor;
	}
}
