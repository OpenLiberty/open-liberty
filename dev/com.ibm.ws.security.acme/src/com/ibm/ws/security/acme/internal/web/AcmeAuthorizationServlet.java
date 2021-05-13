/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.internal.web;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeProvider;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;

/**
 * As part of the ACME HTTP-01 domain authorization process, this servlet will
 * return the authorization that corresponds to the challenge token passed in as
 * part of this request's URI.
 */
@WebServlet("*")
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
public class AcmeAuthorizationServlet extends HttpServlet {

	private static final long serialVersionUID = -8515248242091988849L;

	/** The WAB's application name when running EE8-. */
	public static final String APP_NAME_EE8 = "com.ibm.ws.security.acme";
	
	/** The WAB's application name when running EE9+. */
	public static final String APP_NAME_EE9 = "io.openliberty.security.acme.internal";

	private static final TraceComponent tc = Tr.register(AcmeAuthorizationServlet.class);

	/** Reference to the AcmeProvider service. */
	private static final AtomicReference<AcmeProvider> acmeProviderRef = new AtomicReference<AcmeProvider>();

	private static final String NOT_FOUND = "NOT FOUND";
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Processing challenge token request '" + request.getRequestURI() + "' from '"
					+ request.getRemoteAddr() + "'");
		}

		/*
		 * Get the challenge token from the request path and ensure it is not
		 * empty.
		 */
		String token = request.getRequestURI().replace(AcmeConstants.ACME_CONTEXT_ROOT + "/", "");
		if (token == null || token.trim().isEmpty()) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No challenge token found in URI.");
			}
			response.sendError(404, NOT_FOUND); // TODO 400 Bad Request?
			return;
		}

		/*
		 * Retrieve the AcmeProvider service so we can look up the authorization
		 * via the provided challenge token.
		 */
		AcmeProvider acmeProvider = acmeProviderRef.get();
		if (acmeProvider == null) {
			/*
			 * This should not happen, but in the case it does, return a 404 and
			 * let the ACME server can try again later.
			 */
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "The servlet has no AcmeProvider.");
			}
			response.sendError(404, NOT_FOUND);
			return;
		}

		try {
			/*
			 * Get the authorization.
			 */
			String authorization = acmeProvider.getHttp01Authorization(token);
			if (authorization == null || authorization.trim().isEmpty()) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc,
							"The AcmeProvider did not find an authorization for the challange token '" + token + "'.");
				}
				response.sendError(404, NOT_FOUND);
				return;
			}

			response.resetBuffer();
			response.getWriter().write(authorization);
			response.getWriter().close();
			response.getWriter().flush();
		} catch (AcmeCaException e) {
			/*
			 * This could happen if for some reason the AcmeClient was not
			 * available (perhaps only b/c of concurrent configuration updates
			 * at the time of retrieval).
			 * 
			 * The ACME server can try again later.
			 */
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Error encountered from AcmeProvider: ", e);
			}
			response.sendError(404, NOT_FOUND);
			return;
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/*
		 * Do POST as GET.
		 */
		doGet(request, response);
	}

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected void setAcmeProvider(AcmeProvider acmeProvider) {
		acmeProviderRef.set(acmeProvider);
	}

	protected void unsetAcmeProvider(AcmeProvider acmeProvider) {
		acmeProviderRef.compareAndSet(acmeProvider, null);
	}
}
