/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import java.util.Optional;

public class HttpStatAttributes {
	

	/*
	 * Mandatory fields - Technically networkProtocolName is optional as per http semantics
	 * It is conditionally required if scheme is NOT http and protocol version is set.
	 * But we'll make it mandatory anyways
	 */
	String requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName;
	int serverPort;
	
	/*
	 * Exception related fields are optional.
	 * We are unable to facilitate capturing Exceptions
	 * But we will leave it here.
	 * Additional Context : We can capture  exceptions thrown by servlets
	 * by surrounding the the chainFilter with try catch. But we have no way
	 * of capturing application exception of Jaxrs/restfulws exceptions
	 */
	Optional<Exception> exception = Optional.ofNullable(null);
	Optional<String> errorType = Optional.ofNullable(null);
	
	/*
	 * Conditionally required as per HTTP Semantics Convention
	 */ 
	Optional<String> httpRoute = Optional.ofNullable(null); 
	Optional<Integer> responseStatus = Optional.ofNullable(null); 
	
	
	public boolean validate() {
		return (requestMethod != null) &&
				(scheme != null) &&
				(networkProtocolName != null) &&
				(networkProtocolVersion != null) &&
				(serverName != null) &&
				(serverPort != 0);
	}
	
	public Optional<String> getErrorType() {
		return errorType;
	}

	public void setErrorType(String error) {
		this.errorType = Optional.ofNullable(error);
	}


	
	public Optional<Exception> getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = Optional.ofNullable(exception);
	}

	
	
	public String getRequestMethod() {
		return requestMethod;
	}

	public String getScheme() {
		return scheme;
	}

	public String getNetworkProtocolName() {
		return networkProtocolName;
	}

	public String getNetworkProtocolVersion() {
		return networkProtocolVersion;
	}

	public String getServerName() {
		return serverName;
	}

	public Optional<String> getHttpRoute() {
		return httpRoute;
	}

	public int getServerPort() {
		return serverPort;
	}

	public Optional<Integer> getResponseStatus() {
		return responseStatus;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public void setHttpRoute(String httpRoute) {
		this.httpRoute = Optional.ofNullable(httpRoute);
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setNetworkProtocolName(String networkProtocolName) {
		this.networkProtocolName = networkProtocolName;
	}

	public void setNetworkProtocolVersion(String networkProtocolVersion) {
		this.networkProtocolVersion = networkProtocolVersion;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = Optional.ofNullable(responseStatus);
		if (responseStatus >= 500) {
			this.setErrorType(String.valueOf(responseStatus));
		}
	}

	@Override
	public String toString() {
		return String.format(
				" ------- \n" 
		+ "Request Method (mandatory): [%s] \n" 
		+ "Scheme (mandatory): [%s] \n"
		+ "Network Protocol Name (optional): [%s] \n" 
		+ "Network Protocol Version (mandatory): [%s] \n" 
		+ "Server Name (mandatory): [%s] \n" 
		+ "Server Port (mandatory): [%d] \n"
		+ "HTTP Route (Optional: can be empty): [%s] \n" 
		+ "Response Status (Optional: can be -1): [%d] \n" 
		+ "Error Type(Optional - can be empty): [%s]",
				requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName, serverPort,
				httpRoute.orElse(""), responseStatus.orElse(-1), errorType.orElse(""));
	}
}
