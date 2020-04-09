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
package com.ibm.ws.grpc.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.ws.http2.GrpcServletServices.ServiceInformation;
import com.ibm.ws.security.authorization.util.RoleMethodAuthUtil;
import com.ibm.ws.security.authorization.util.UnauthenticatedException;

import io.grpc.Metadata;

public class GrpcServletUtils {

	public final static String LIBERTY_AUTH_KEY_STRING = "libertyAuthCheck";
	public final static Map<String, Boolean> authMap = new ConcurrentHashMap<String, Boolean>();
	public static final Metadata.Key<String> LIBERTY_AUTH_KEY = Metadata.Key.of(LIBERTY_AUTH_KEY_STRING,
			Metadata.ASCII_STRING_MARSHALLER);

	/**
	 * Helper method to add the "authorized" flag to the byte arrays that will get built into Metadata
	 * 
	 * @param byteArrays
	 * @param req
	 * @param authorized
	 */
	public static void addLibertyAuthHeader(List<byte[]> byteArrays, HttpServletRequest req, boolean authorized) {
		byteArrays.add(GrpcServletUtils.LIBERTY_AUTH_KEY.name().getBytes(StandardCharsets.US_ASCII));
		byteArrays.add((String.valueOf(req.hashCode())).getBytes(StandardCharsets.US_ASCII));
		authMap.put(String.valueOf(req.hashCode()), authorized);
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
	 * Checks if a given request is authorized to access the requested method, by scanning the requested method 
	 * for @DenyAll, @RolesAllowed, or @AllowAll and validating the request's Subject
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
			}

		} catch (AccessDeniedException e) {
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

	private static void handleMessage(HttpServletRequest req, String path) throws UnauthenticatedException, AccessDeniedException {

		Method method = GrpcServletUtils.getTargetMethod(path);
		if (RoleMethodAuthUtil.parseMethodSecurity(method, req.getUserPrincipal(), s -> req.isUserInRole(s))) {
			return;
		}
		throw new AccessDeniedException("Unauthorized");
	}
}
