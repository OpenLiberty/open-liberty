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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jose4j.json.JsonUtil;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.lang.JoseException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.acme.AcmeCaException;
import com.ibm.ws.security.acme.AcmeProvider;
import com.ibm.ws.security.acme.internal.AcmeClient.AcmeAccount;
import com.ibm.ws.security.acme.internal.exceptions.CertificateRenewRequestBlockedException;
import com.ibm.ws.security.acme.internal.exceptions.IllegalRevocationReasonException;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * A {@link RESTHandler} implementation that allows interaction with the
 * acmeCA-2.0 feature.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
		RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + AcmeCaRestHandler.PATH_ROOT,
		RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true" })
public class AcmeCaRestHandler implements RESTHandler {

	private static final TraceComponent tc = Tr.register(AcmeCaRestHandler.class);

	private final AtomicReference<AcmeProvider> acmeProviderRef = new AtomicReference<AcmeProvider>();
	private final AtomicReference<KeyStoreService> keyStoreServiceRef = new AtomicReference<KeyStoreService>();

	public static final String PATH_ROOT = "/acmeca";
	public static final String PATH_ACCOUNT = "/acmeca/account";
	public static final String PATH_CERTIFICATE = "/acmeca/certificate";

	private static final String HTTP_GET = "GET";
	private static final String HTTP_POST = "POST";

	public static final String OP_KEY = "operation";
	public static final String REASON_KEY = "reason";

	public static final String OP_RENEW_CERT = "renewCertificate";
	public static final String OP_REVOKE_CERT = "revokeCertificate";
	public static final String OP_RENEW_ACCT_KEY_PAIR = "renewAccountKeyPair";

	@Override
	public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {

		String path = request.getPath();

		if (PATH_ROOT.equals(path)) {
			handleRoot(request, response);
		} else if (PATH_ACCOUNT.equals(path)) {
			handleAccount(request, response);
		} else if (PATH_CERTIFICATE.equals(path)) {
			handleCertificate(request, response);
		} else {
			/*
			 * Invalid end point.
			 */
			response.sendError(404, "Not Found");
			return;
		}
	}

	/**
	 * Handle requests made to the {@value #PATH_ROOT} endpoint.
	 * 
	 * @param request
	 *            the incoming REST request
	 * @param response
	 *            the outgoing REST response
	 * @throws IOException
	 *             If there was an issue processing the request.
	 */
	private void handleRoot(RESTRequest request, RESTResponse response) throws IOException {
		String method = request.getMethod();
		if (!HTTP_GET.equalsIgnoreCase(method)) {
			commitJsonResponse(response, 405, Tr.formatMessage(tc, "REST_METHOD_NOT_SUPPORTED", method));
			return;
		}

		if (!hasReadRole(request, response)) {
			return;
		}

		/*
		 * Open the HTML document.
		 */
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");

		/*
		 * Add account and certificate text.
		 */
		appendAccountText(sb);
		appendCertificateText(sb);

		/*
		 * Close the HTML document.
		 */
		sb.append("</body></html>");

		/*
		 * Write the document to the response.
		 */
		response.setStatus(200);
		response.setContentType("text/html");
		response.setContentLength(sb.length());
		response.getOutputStream().write(sb.toString().getBytes());
	}

	/**
	 * Handle requests made to the {@value #PATH_ACCOUNT} endpoint.
	 * 
	 * @param request
	 *            the incoming REST request
	 * @param response
	 *            the outgoing REST response
	 * @throws IOException
	 *             If there was an issue processing the request.
	 */
	private void handleAccount(RESTRequest request, RESTResponse response) throws IOException {

		String method = request.getMethod();
		if (HTTP_GET.equalsIgnoreCase(method)) {

			if (!hasReadRole(request, response)) {
				return;
			}

			/*
			 * Open the HTML document.
			 */
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");

			/*
			 * Add certificate text.
			 */
			appendAccountText(sb);

			/*
			 * Close the HTML document.
			 */
			sb.append("</body></html>");

			/*
			 * Write the document to the response.
			 */
			response.setStatus(200);
			response.setContentType("text/html");
			response.setContentLength(sb.length());
			response.getOutputStream().write(sb.toString().getBytes());
			return;
		} else if (HTTP_POST.equalsIgnoreCase(method)) {

			if (!hasAdminRole(request, response)) {
				return;
			}

			/*
			 * Expect JSON content type.
			 */
			String contentType = request.getContentType();
			if (contentType == null || !contentType.contains("application/json")) {
				commitJsonResponse(response, 415, Tr.formatMessage(tc, "REST_INVALID_CONTENT_TYPE"));
				return;
			}

			/*
			 * Get the content and the requested operation.
			 */
			Map<String, Object> jsonMap = getContentAsJsonMap(request);
			String operation = getOperation(jsonMap);
			if (operation == null) {
				commitJsonResponse(response, 400, Tr.formatMessage(tc, "REST_MISSING_OPERATION"));
				return;
			}

			/*
			 * Request to generate a new account key pair?
			 */
			if (OP_RENEW_ACCT_KEY_PAIR.equals(operation)) {

				/*
				 * Requires there be a AcmeProvider.
				 */
				AcmeProvider acmeProvider = getAcmeProvider(response);
				if (acmeProvider == null) {
					return;
				}

				try {
					acmeProvider.renewAccountKeyPair();
					commitJsonResponse(response, 200, null);
					return;
				} catch (AcmeCaException e) {
					commitJsonResponse(response, 500, e.getMessage());
					return;
				}
			} else {
				/*
				 * Do nothing.
				 */
				commitJsonResponse(response, 400, Tr.formatMessage(tc, "REST_OPERATION_NOT_SUPPORTED", operation));
				return;
			}

		} else {
			commitJsonResponse(response, 405, Tr.formatMessage(tc, "REST_METHOD_NOT_SUPPORTED", method));
			return;
		}
	}

	/**
	 * Handle requests made to the {@value #PATH_CERTIFICATE} endpoint.
	 * 
	 * @param request
	 *            the incoming REST request
	 * @param response
	 *            the outgoing REST response
	 * @throws IOException
	 *             If there was an issue processing the request.
	 */
	@FFDCIgnore({ AcmeCaException.class, IllegalRevocationReasonException.class, CertificateRenewRequestBlockedException.class })
	private void handleCertificate(RESTRequest request, RESTResponse response) throws IOException {
		String method = request.getMethod();
		if (HTTP_GET.equalsIgnoreCase(method)) {
			if (!hasReadRole(request, response)) {
				return;
			}

			/*
			 * Open the HTML document.
			 */
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body>");

			/*
			 * Add certificate text.
			 */
			appendCertificateText(sb);

			/*
			 * Close the HTML document.
			 */
			sb.append("</body></html>");

			/*
			 * Write the document to the response.
			 */
			response.setStatus(200);
			response.setContentType("text/html");
			response.setContentLength(sb.length());
			response.getOutputStream().write(sb.toString().getBytes());
			return;
		} else if (HTTP_POST.equalsIgnoreCase(method)) {
			if (!hasAdminRole(request, response)) {
				return;
			}

			/*
			 * Expect JSON content type.
			 */
			String contentType = request.getContentType();
			if (contentType == null || !contentType.contains("application/json")) {
				commitJsonResponse(response, 415, Tr.formatMessage(tc, "REST_INVALID_CONTENT_TYPE"));
				return;
			}

			/*
			 * Get the content and the requested operation.
			 */
			Map<String, Object> jsonMap = getContentAsJsonMap(request);
			String operation = getOperation(jsonMap);
			if (operation == null) {
				commitJsonResponse(response, 400, Tr.formatMessage(tc, "REST_MISSING_OPERATION"));
				return;
			}

			/*
			 * Request to generate a new account key pair?
			 */
			if (OP_RENEW_CERT.equalsIgnoreCase(operation)) {
				/*
				 * Requires there be a AcmeProvider.
				 */
				AcmeProvider acmeProvider = getAcmeProvider(response);
				if (acmeProvider == null) {
					return;
				}

				try {
					acmeProvider.checkCertificateRenewAllowed();
					acmeProvider.renewCertificate();
					commitJsonResponse(response, 200, null);
					return;
				} catch (CertificateRenewRequestBlockedException cr) {
					/*
					 * Use 429: Too Many Requests
					 */
					commitJsonResponse(response, 429, Tr.formatMessage(tc, "REST_TOO_MANY_REQUESTS", cr.getTimeLeftForBlackout() + "ms"));
				} catch (AcmeCaException e) {
					commitJsonResponse(response, 500, e.getMessage());
					return;
				}
			} else if (OP_REVOKE_CERT.equalsIgnoreCase(operation)) {
				/*
				 * Requires there be a AcmeProvider.
				 */
				AcmeProvider acmeProvider = getAcmeProvider(response);
				if (acmeProvider == null) {
					return;
				}

				String reason = getRevocationReason(jsonMap);

				try {
					acmeProvider.revokeCertificate(reason);
					commitJsonResponse(response, 200, null);
					return;
				} catch (IllegalRevocationReasonException e) {
					commitJsonResponse(response, 400, e.getMessage());
					return;
				} catch (AcmeCaException e) {
					commitJsonResponse(response, 500, e.getMessage());
					return;
				}
			} else {
				/*
				 * Do nothing.
				 */
				commitJsonResponse(response, 400, Tr.formatMessage(tc, "REST_OPERATION_NOT_SUPPORTED", operation));
				return;
			}
		} else {
			commitJsonResponse(response, 405, Tr.formatMessage(tc, "REST_METHOD_NOT_SUPPORTED"));
			return;
		}
	}

	/**
	 * Append account HTML text to the {@link StringBuilder}.
	 * 
	 * @param sb
	 *            the {@link StringBuilder} to append to.
	 */
	@FFDCIgnore(AcmeCaException.class)
	private void appendAccountText(StringBuilder sb) {
		sb.append("<br/><hr>");
		sb.append("<h1>ACME CA Account Details</h1>");
		sb.append("<hr>");

		/*
		 * Print the account information.
		 */
		try {

			AcmeAccount account = acmeProviderRef.get().getAccount();
			sb.append("<pre>");
			sb.append("Location:                  ").append(account.getLocation()).append("\n");
			sb.append("Status:                    ").append(account.getStatus()).append("\n");
			sb.append("Terms of Service Agreed:   ").append(account.getTermsOfServiceAgreed()).append("\n");
			sb.append("Contacts:                  ").append("\n");
			if (account.getContacts() != null && !account.getContacts().isEmpty()) {
				for (URI contact : account.getContacts()) {
					sb.append("    ").append(contact).append("\n");
				}
			} else {
				sb.append("    ").append("NONE");
			}
			sb.append("\n");
			sb.append("Orders:                    ").append("\n");
			if (account.getOrders() != null && !account.getOrders().isEmpty()) {
				for (String order : account.getOrders()) {
					sb.append("    ").append(order).append("\n");
				}
			} else {
				sb.append("    ").append("NONE");
			}
			sb.append("\n");
			sb.append("</pre>");
		} catch (AcmeCaException e) {
			sb.append("<pre>Unable to load certificate chain: ").append(e.getMessage()).append("</pre>");
		}
	}

	/**
	 * Append certificate HTML text to the {@link StringBuilder}.
	 * 
	 * @param sb
	 *            the {@link StringBuilder} to append to.
	 */
	@FFDCIgnore(KeyStoreException.class)
	private void appendCertificateText(StringBuilder sb) {

		sb.append("<br/><hr>");
		sb.append("<h1>Active Certificate Chain</h1>");
		sb.append("<hr>");

		/*
		 * Get the certificate chain and then generate an HTML document.
		 */
		try {
			Certificate[] chain = keyStoreServiceRef.get().getCertificateChainFromKeyStore(DEFAULT_KEY_STORE,
					DEFAULT_ALIAS);
			sb.append("<pre>");
			sb.append(Arrays.asList(chain));
			sb.append("</pre>");
		} catch (CertificateException | KeyStoreException e) {
			sb.append("<br/><h1>Unable to load certificate chain: ").append(e.getMessage()).append("</h1>");
		}
	}

	/**
	 * Get the incoming request content as a JSON map.
	 * 
	 * @param request
	 *            the incoming request
	 * @return The JSON map.
	 * @throws IOException
	 *             if there was an error parsing the request contents to a JSON
	 *             map.
	 */
	@FFDCIgnore(JoseException.class)
	private static Map<String, Object> getContentAsJsonMap(RESTRequest request) throws IOException {
		/*
		 * Get the content.
		 */
		try {
			InputStream inputStream = request.getInputStream();
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}

			/*
			 * Hard-coded to UTF_8 charset at this time. We use Jose4j simply
			 * b/c we already have a dependency on it.
			 */
			String jsonString = result.toString(StandardCharsets.UTF_8.name());
			return JsonUtil.parseJson(jsonString);
		} catch (UnsupportedEncodingException e) {
			/*
			 * Not possible if only UTF_8.
			 */
			throw new IOException("Unsupported content payload encoding: " + e.getMessage(), e);
		} catch (JoseException e) {
			return Collections.emptyMap();
		} catch (IOException e) {
			throw new IOException("Error loading request payload: " + e.getMessage(), e);
		}
	}

	/**
	 * Get the requested operation from the JSON {@link Map}.
	 * 
	 * @param jsonMap
	 *            The JSON map.
	 * @return the requested operation.
	 */
	@FFDCIgnore(ClassCastException.class)
	private static String getOperation(Map<String, Object> jsonMap) {
		try {
			return (String) jsonMap.get(OP_KEY);
		} catch (ClassCastException e) {
			return null;
		}
	}

	/**
	 * Get the reason for the revocation.
	 * 
	 * @param jsonMap
	 *            The JSON map.
	 * @return the requested operation.
	 * 
	 */
	@FFDCIgnore(ClassCastException.class)
	private static String getRevocationReason(Map<String, Object> jsonMap) {
		try {
			return (String) jsonMap.get(REASON_KEY);
		} catch (ClassCastException e) {
			return null;
		}
	}

	/**
	 * Commit the JSON response to the {@link RESTResponse}.
	 * 
	 * @param response
	 *            The {@link RESTResponse} to add the JSON response to.
	 * @param code
	 *            The http status code.
	 * @param errorMessage
	 *            The error message, or null if none.
	 */
	private static void commitJsonResponse(RESTResponse response, int statusCode, String errorMessage)
			throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"httpCode\":").append(String.valueOf(statusCode));

		/*
		 * Add error message if present.
		 */
		if (errorMessage != null) {
			sb.append(", \"message\":\"").append(JSONObject.escape(errorMessage)).append("\"");
		}
		sb.append("}");

		response.setStatus(statusCode);
		response.setContentType("application/json");
		response.setContentLength(sb.length());
		response.getOutputStream().write(sb.toString().getBytes());
	}

	/**
	 * Get the {@link AcmeProvider} instance. Commits an error response if one
	 * is not registered.
	 * 
	 * @param response
	 *            The response to commit the response to if the instance is not
	 *            registered.
	 * @return The {@link AcmeProvider} instance.
	 * @throws IOException
	 *             If the response could not be committed.
	 */
	private AcmeProvider getAcmeProvider(RESTResponse response) throws IOException {
		AcmeProvider acmeProvider = acmeProviderRef.get();
		if (acmeProvider == null) {
			commitJsonResponse(response, 500, Tr.formatMessage(tc, "REST_NO_ACME_SERVICE"));
			return null;
		}
		return acmeProvider;
	}

	/**
	 * Check if the user making the request has the administrator role. If not,
	 * the {@link RESTResponse} will be updated with a JSON response.
	 * 
	 * @param request
	 *            The {@link RESTRequest}
	 * @param response
	 *            the {@link RESTResponse} to update.
	 * @return true if the user has the administrator role, false otherwise.
	 * @throws IOException
	 *             if there was an issue committing the response.
	 */
	private static boolean hasAdminRole(RESTRequest request, RESTResponse response) throws IOException {
		boolean hasAdminRole = request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME);

		if (!hasAdminRole) {
			commitJsonResponse(response, 403, Tr.formatMessage(tc, "REST_FORBIDDEN"));
		}

		return hasAdminRole;
	}

	/**
	 * Check if the user making the request has the administrator or reader
	 * role. If not, the {@link RESTResponse} will be updated with a JSON
	 * response.
	 * 
	 * @param request
	 *            The {@link RESTRequest}
	 * @param response
	 *            the {@link RESTResponse} to update.
	 * @return true if the user has the administrator or reader role, false
	 *         otherwise.
	 * @throws IOException
	 *             if there was an issue committing the response.
	 */
	private static boolean hasReadRole(RESTRequest request, RESTResponse response) throws IOException {
		boolean hasReadRole = request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME)
				|| request.isUserInRole(ManagementSecurityConstants.READER_ROLE_NAME);

		if (!hasReadRole) {
			commitJsonResponse(response, 403, Tr.formatMessage(tc, "REST_FORBIDDEN"));
		}

		return hasReadRole;
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
		keyStoreServiceRef.set(keyStoreService);
	}

	/**
	 * Method for declarative services to unset the {@link KeyStoreService}
	 * service.
	 * 
	 * @param keyStoreService
	 *            The {@link KeyStoreService} service to unset.
	 */
	protected void unsetKeyStoreService(KeyStoreService keyStoreService) {
		keyStoreServiceRef.compareAndSet(keyStoreService, null);
	}
}
