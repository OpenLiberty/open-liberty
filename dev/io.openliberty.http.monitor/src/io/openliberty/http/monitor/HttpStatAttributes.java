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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class HttpStatAttributes {
	
	private final String httpStat_ID;
	
	private static final TraceComponent tc = Tr.register(HttpStatAttributes.class);
	
	/*
	 * Mandatory fields - Technically networkProtocolName is optional as per http semantics
	 * It is conditionally required if scheme is NOT http and protocol version is set.
	 * But we'll make it mandatory anyways
	 */
	private final String requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName;
	private final int serverPort;
	
	/*
	 * Exception related fields are optional.
	 * We are unable to facilitate capturing Exceptions
	 * But we will leave it here.
	 * Additional Context : We can capture  exceptions thrown by servlets
	 * by surrounding the the chainFilter with try catch. But we have no way
	 * of capturing application exception of Jaxrs/restfulws exceptions
	 */
	private final Exception exception;
	private final String errorType;
	
	/*
	 * Conditionally required as per HTTP Semantics Convention
	 */ 
	private final String httpRoute;
	private final int responseStatus;
	
	
	/**
	 * Constructor for HttpStatsAttributes. This should not be called directly, but
	 * should be instantiated through {@link Builder#build()}
	 * 
	 * @param builder see {@link Builder}
	 * @throws IllegalStateException if the builder's validation fails
	 */
	HttpStatAttributes(Builder builder) throws IllegalStateException {
		
		if (!builder.validate()) {
			throw new IllegalStateException("Invalid HTTP Stats attributes");
		}
		
		this.requestMethod = builder.requestMethod;
		this.scheme = builder.scheme;
		this.networkProtocolName = builder.networkProtocolName;
		this.networkProtocolVersion = builder.networkProtocolVersion;
		this.serverName = builder.serverName;
		
		this.serverPort = builder.serverPort;
		
		this.exception =  (builder.exception.isPresent() ? builder.exception.get() : null);
		this.errorType = (builder.errorType.isPresent() ? builder.errorType.get() : null);
		this.httpRoute = (builder.httpRoute.isPresent() ? builder.httpRoute.get() : null);
		this.responseStatus = (builder.responseStatus.isPresent() ? builder.responseStatus.get() : -1);
		
		httpStat_ID = resolveKeyID();
	}
	
	/**
	 * Resolve the object name (specifically the name property)
	 * <code> domain:type=type,name="this"</code>
	 * 
	 * This is also used by {@link HttpServerStatsMonitor} when registering MBean
	 * into the {@link MeterCollection}
	 *
	 * @return
	 */
	private String resolveKeyID() {

		StringBuilder sb = new StringBuilder();
		sb.append("\""); // starting quote
		sb.append("method:").append(requestMethod);
		
		/*
		 * Status, Route and errorType may be null.
		 * In which cas we will not append it to the name property
		 */
		if (responseStatus != -1) {
			sb.append(";status:").append(responseStatus);
		}

		if (httpRoute != null) {
			sb.append(";httpRoute:").append(httpRoute.replace("*", "\\*"));
		}
		
		if (errorType != null) {
			sb.append(";errorType:").append(errorType);
		}

		sb.append("\""); // ending quote
		return sb.toString();
	}
	
	/**
	 * The error type if it exists, null otherwise
	 * 
	 * @return a String that represents the error type or null if it does not exist
	 */
	public String getErrorType() {
		return errorType;
	}

	/**
	 * The Exception if it exists, null otherwise
	 * 
	 * @return an Exception or null if it does not exist
	 */
	public Exception getException() {
		return exception;
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

	/**
	 * The http route if it exists, null otherwise
	 * 
	 * @return a String that represents the http route or null if it does not exist
	 */
	public String getHttpRoute() {
		return httpRoute;
	}

	public int getServerPort() {
		return serverPort;
	}


	/**
	 * The response status if it exists, null otherwise
	 * @return An Integer representing the  response status or null if it does not exist
	 */
	public int getResponseStatus() {
		return responseStatus;
	}
	
	public String getHttpStatID() {
		return httpStat_ID;
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
				httpRoute, responseStatus, errorType);
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	
	public static class Builder {

		private String requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName;
		private int serverPort;

		/*
		 * Exception related fields are optional. We are unable to facilitate capturing
		 * Exceptions But we will leave it here. Additional Context : We can capture
		 * exceptions thrown by servlets by surrounding the the chainFilter with try
		 * catch. But we have no way of capturing application exception of
		 * Jaxrs/restfulws exceptions
		 */
		private Optional<Exception> exception = Optional.ofNullable(null);
		private Optional<String> errorType = Optional.ofNullable(null);

		/*
		 * Conditionally required as per HTTP Semantics Convention
		 */
		private Optional<String> httpRoute = Optional.ofNullable(null);
		private Optional<Integer> responseStatus = Optional.ofNullable(null);

		/*
		 * Define a constructor with default protection so others do not call it directly and instead
		 * call the builder() method above.
		 */
		Builder() {
		}

		/**
		 * Builds an instance of {@link HttpStatAttributes} with values from this
		 * builder. Will validate and throw an {@link IllegalStateException} if the
		 * required fields are not filled.
		 * 
		 * @return Instance of {@link HttpStatAttributes}
		 * @throws IllegalStateException
		 */
		@FFDCIgnore(value = { IllegalStateException.class })
		public HttpStatAttributes build() {
			try {
				return new HttpStatAttributes(this);
			} catch (IllegalStateException ise) {
				//do nothing
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, String.format("Invalid HTTP Stats attributes : \n %s", toString()));
				}
			}
			return null;
		}

		public Builder withRequestMethod(String requestMethod) {
			this.requestMethod = requestMethod;
			return this;
		}

		public Builder withErrorType(String error) {
			this.errorType = Optional.ofNullable(error);
			return this;
		}

		public Builder withException(Exception exception) {
			this.exception = Optional.ofNullable(exception);
			return this;
		}

		public Builder withHttpRoute(String httpRoute) {
			this.httpRoute = Optional.ofNullable(httpRoute);
			return this;
		}

		public Builder withScheme(String scheme) {
			this.scheme = scheme;
			return this;
		}

		public Builder withNetworkProtocolName(String networkProtocolName) {
			this.networkProtocolName = networkProtocolName;
			return this;
		}

		public Builder withNetworkProtocolVersion(String networkProtocolVersion) {
			this.networkProtocolVersion = networkProtocolVersion;
			return this;
		}

		public Builder withServerName(String serverName) {
			this.serverName = serverName;
			return this;
		}

		public Builder withServerPort(int serverPort) {
			this.serverPort = serverPort;
			return this;
		}

		public Builder withResponseStatus(int responseStatus) {
			this.responseStatus = Optional.ofNullable(responseStatus);
			if (responseStatus >= 500) {
				this.withErrorType(Integer.toString(responseStatus));
			}
			return this;
		}

		public boolean validate() {
			return (requestMethod != null) && (scheme != null) && (networkProtocolName != null)
					&& (networkProtocolVersion != null) && (serverName != null) && (serverPort != 0);
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
					httpRoute.orElse(null), responseStatus.orElse(null), errorType.orElse(null));
		}

	}
}
