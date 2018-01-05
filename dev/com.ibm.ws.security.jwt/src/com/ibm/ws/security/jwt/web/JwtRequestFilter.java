/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.web;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.web.JwtRequest.EndpointType;

public class JwtRequestFilter implements Filter {
	private static TraceComponent tc = Tr.register(JwtRequestFilter.class);

	public static final String REGEX_COMPONENT_ID = "/([\\w-]+)";

	private static final Pattern PATH_JWK_REGEX = Pattern.compile("^" + REGEX_COMPONENT_ID + "/(jwk)$");
	private static final Pattern PATH_TOKEN_REGEX = Pattern.compile("^/(token)" + REGEX_COMPONENT_ID + "$");;

	/**
	 * Default constructor.
	 */
	public JwtRequestFilter() {
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	@Override
	public void init(FilterConfig fConfig) throws ServletException {
	}

	/**
	 * @see Filter#destroy()
	 */
	@Override
	public void destroy() {
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		if (response.isCommitted()) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Response has already been committed; will do nothing");
			}
			// Some component has already committed the servlet response; do
			// nothing
			return;
		}
		// We need jwk endpoint to be unsecured, unauthenticated.
		// We need token endpoint to be authenticated & secured.
		// We cannot change existing jwk endpoint.
		// Servlet url mapping limitations require
		// slightly inconsistent endpoints to achieve this.
		// ibm/api/<configid>/jwk, and ibm/api/token/<configid>

		// try to match jwk request
		Matcher matcher = matchEndpointRequest(request, PATH_JWK_REGEX);
		if (matcher != null) {
			invokeEndpointRequest(request, response, chain, matcher, false);
			return;
		}
		// try to match token request
		matcher = matchEndpointRequest(request, PATH_TOKEN_REGEX);
		if (matcher != null) {
			invokeEndpointRequest(request, response, chain, matcher, true);
			return;
		}

		String message = Tr.formatMessage(tc, "JWT_ENDPOINT_FILTER_MATCH_NOT_FOUND",
				new Object[] { request.getPathInfo() });
		Tr.warning(tc, message); // CWWKS6004W
		response.sendError(HttpServletResponse.SC_NOT_FOUND, message);

	}

	/**
	 * Creates a JwtRequest object based on the provided Matcher, sets it as the
	 * value of the {@value WebConstants#JWT_REQUEST_ATTR} request attribute,
	 * and applies the filter to the request.
	 *
	 * @param request
	 * @param response
	 * @param chain
	 * @param matcher
	 * @throws IOException
	 * @throws ServletException
	 */
	protected void invokeEndpointRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Matcher matcher, boolean isTokenRequest) throws IOException, ServletException {
		JwtRequest jwtRequest = null;
		if (!isTokenRequest) {
			jwtRequest = new JwtRequest(matcher.group(1), getType(matcher.group(2)), request);
		} else {
			jwtRequest = new JwtRequest(matcher.group(2), getType(matcher.group(1)), request);
		}
		request.setAttribute(WebConstants.JWT_REQUEST_ATTR, jwtRequest);
		chain.doFilter(request, response);
	}

	private Matcher matchEndpointRequest(HttpServletRequest request, Pattern pattern) {
		String path = request.getPathInfo();
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Path info: [" + path + "]");
		}
		Matcher m = pattern.matcher(path);
		if (m.matches()) {
			return m;
		}
		return null;
	}

	/**
	 * Method to determine type of request based on known path values (ex: jwk,
	 * others yet to be defined)
	 *
	 * @param pathType
	 *            Known endpoint path value
	 * @return enum type characterizing the request
	 */
	protected EndpointType getType(String pathType) {
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Path type is " + pathType);
		}
		return EndpointType.valueOf(pathType);
	}

}
