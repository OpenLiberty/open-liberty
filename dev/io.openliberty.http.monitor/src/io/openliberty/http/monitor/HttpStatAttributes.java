package io.openliberty.http.monitor;

import java.util.Optional;

public class HttpStatAttributes {
	
	Optional<String> httpRoute = Optional.ofNullable(null);
	Optional<Integer> responseStatus = Optional.ofNullable(null);
	Optional<Exception> exception = Optional.ofNullable(null);
	
	Optional<String> error = Optional.ofNullable(null);
	
	
	public Optional<String> getError() {
		return error;
	}

	public void setError(String error) {
		this.error = Optional.ofNullable(error);
	}

	String requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName;
	int serverPort;
	
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
	}

	@Override
	public String toString() {
		return String.format(
				" ------- \n" + "Request Method: %s \n" + "Scheme: %s \n" + "Network Protocol Name: %s \n"
						+ "Network Protocl Version: %s \n" + "Server Name: %s \n" + "Server Port: %d \n"
						+ "HTTP Route: %s \n" + "Response Status: %d",
				requestMethod, scheme, networkProtocolName, networkProtocolVersion, serverName, serverPort,
				httpRoute.orElse(""), responseStatus.orElse(-1));
	}
}
