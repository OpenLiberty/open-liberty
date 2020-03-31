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
package com.ibm.ws.security.acme.internal.web;

import static com.ibm.ws.security.acme.internal.util.AcmeConstants.DEFAULT_ALIAS;
import static com.ibm.ws.security.acme.internal.util.AcmeConstants.DEFAULT_KEY_STORE;
import static com.ibm.ws.security.acme.internal.util.AcmeConstants.KEY_KEYSTORE_SERVICE;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeProvider;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerForbiddenError;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerInternalError;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerMethodNotAllowedError;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerOSGiError;

/**
 * A {@link RESTHandler} implementation that allows interaction with the
 * acmeCA-2.0 feature.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
		RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/acmeca",
		RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true" })
public class AcmeCaRestHandler implements RESTHandler {

	private static final TraceComponent tc = Tr.register(AcmeCaRestHandler.class);

	private final AtomicReference<AcmeProvider> acmeProviderRef = new AtomicReference<AcmeProvider>();
	private final AtomicReference<KeyStoreService> keyStoreServiceRef = new AtomicReference<KeyStoreService>();

	private static final Set<String> REQUIRED_ROLES_GET = new HashSet<String>();
	private static final Set<String> REQUIRED_ROLES_PUT = new HashSet<String>();

	static {
		REQUIRED_ROLES_GET.add(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME);
		REQUIRED_ROLES_GET.add(ManagementSecurityConstants.READER_ROLE_NAME);

		REQUIRED_ROLES_PUT.add(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME);
	}

	@Override
	public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			handleGet(request, response);
		} else if ("PUT".equalsIgnoreCase(request.getMethod())) {
			handlePut(request, response);
		} else {
			throw new RESTHandlerMethodNotAllowedError("GET,PUT");
		}
	}

	/**
	 * Handle GET requests.
	 * 
	 * @param request
	 *            The request.
	 * @param response
	 *            The response.
	 * @throws IOException
	 *             if there was an error processing the request.
	 */
	private void handleGet(RESTRequest request, RESTResponse response) throws IOException {

		/*
		 * Readers and administrator can read the certificate.
		 */
		if (!request.isUserInRole(ManagementSecurityConstants.READER_ROLE_NAME)
				&& !request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME)) {
			throw new RESTHandlerForbiddenError(REQUIRED_ROLES_PUT,
					"Only users with the administrator or reader role can view the TLS certificate.");
		}

		/*
		 * Requires there be a KeyStoreService.
		 */
		if (keyStoreServiceRef.get() == null) {
			throw new RESTHandlerOSGiError("KeyStoreService");
		}

		try {
			/*
			 * Get the certificate chain and then generate an HTML document.
			 */
			Certificate[] chain = keyStoreServiceRef.get().getCertificateChainFromKeyStore(DEFAULT_KEY_STORE,
					DEFAULT_ALIAS);
			String html = getCertificateAsHTML(chain);

			/*
			 * Write the document to the response.
			 */
			response.setStatus(200);
			response.setContentType("text/html");
			response.setContentLength(html.length());
			response.getOutputStream().write(html.getBytes());

		} catch (KeyStoreException | CertificateException e) {
			response.sendError(404, "Not Found");
			return;
		}
	}

	/**
	 * Handle PUT requests.
	 * 
	 * @param request
	 *            The request.
	 * @param response
	 *            The response.
	 * @throws IOException
	 *             if there was an error processing the request.
	 */
	private void handlePut(RESTRequest request, RESTResponse response) throws IOException {

		/*
		 * Only allow administrators to refresh the certificate.
		 */
		if (!request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME)) {
			throw new RESTHandlerForbiddenError(REQUIRED_ROLES_PUT,
					"Only users with the administrator role can refresh the TLS certificate.");
		}

		/*
		 * Requires there be a AcmeProvider.
		 */
		if (acmeProviderRef.get() == null) {
			throw new RESTHandlerOSGiError("AcmeProvider");
		}

		try {
			acmeProviderRef.get().refreshCertificate();
			response.setStatus(204);
		} catch (AcmeCaException e) {
			throw new RESTHandlerInternalError(e);
		}
	}

	/**
	 * Wrap the certificate in HTML tags.
	 * 
	 * @param chain
	 *            The certificate chain to wrap.
	 * @return The HTML text.
	 */
	private static String getCertificateAsHTML(Certificate[] chain) {
		return "<html><body><pre>" + Arrays.asList(chain) + "</pre></body></html>";
	}

	/**
	 * Method for declarative services to set the {@link AcmeProvider} service.
	 * 
	 * @param acmeProvider
	 *            The {@link AcmeProvider} service to set.
	 */
	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected void setAcmeProvider(AcmeProvider acmeProvider) {
		acmeProviderRef.set(acmeProvider);
	}

	/**
	 * Method for declarative services to unset the {@link AcmeProvider}
	 * service.
	 * 
	 * @param acmeProvider
	 *            The {@link AcmeProvider} service to unset.
	 */
	protected void unsetAcmeProvider(AcmeProvider acmeProvider) {
		acmeProviderRef.compareAndSet(acmeProvider, null);
	}

	/**
	 * Method for declarative services to set the {@link KeyStoreService}
	 * service.
	 * 
	 * @param keyStoreService
	 *            The {@link KeyStoreService} service to set.
	 */
	@Reference(name = KEY_KEYSTORE_SERVICE, service = KeyStoreService.class, cardinality = ReferenceCardinality.MANDATORY)
	protected void setKeyStoreService(KeyStoreService keyStoreService) {
		final String methodName = "setKeyStoreService(KeyStoreService)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, keyStoreService);
		}

		keyStoreServiceRef.set(keyStoreService);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}

	/**
	 * Method for declarative services to unset the {@link KeyStoreService}
	 * service.
	 * 
	 * @param keyStoreService
	 *            The {@link KeyStoreService} service to unset.
	 */
	protected void unsetKeyStoreService(KeyStoreService keyStoreService) {
		final String methodName = "unsetKeyStoreService(KeyStoreService)";
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.entry(tc, methodName, keyStoreService);
		}

		keyStoreServiceRef.compareAndSet(keyStoreService, null);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
			Tr.exit(tc, methodName);
		}
	}
}
