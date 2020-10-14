package io.openliberty.grpc.internal.client;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.openliberty.grpc.internal.client.config.GrpcClientConfigHolder;

/**
 * Provide support for propagating HTTP headers on outbound grpc client calls
 *
 */
public class LibertyHeaderPropagationSupport {

	private static final TraceComponent tc = Tr.register(LibertyHeaderPropagationSupport.class, GrpcClientMessages.GRPC_TRACE_NAME, GrpcClientMessages.GRPC_BUNDLE);

	/**
	 * Attempt to retrieve the headers configured via headersToPropagate and add
	 * them to the outbound grpc call
	 * 
	 * @param method
	 * @param headerMap
	 */
	@SuppressWarnings("rawtypes")
	public static void handleHeaderPropagation(String host, MethodDescriptor method, Metadata headerMap) {

		String headersToPropagate = GrpcClientConfigHolder.getHeaderPropagationSupport(host, method.getFullMethodName());
		if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
			Tr.debug(tc, "checking header propagation for " + host + "$" + method.getFullMethodName());
		}

		if (headersToPropagate == null || headersToPropagate.isEmpty()) {
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, "no header propagation configured for " + host + "/" + method.getFullMethodName());
			}
			return;
		} else {
			if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
				Tr.debug(tc, "propagating headers: " + headersToPropagate);
			}
			List<String> headerNames = Arrays.asList(headersToPropagate.split("\\s*,\\s*"));
			if (!headerNames.isEmpty()) {
				for (String headerName : headerNames) {
					Enumeration<String> headerValues = getThreadLocalRequestHeaders(headerName);
					if (headerValues != null) {
						while (headerValues.hasMoreElements()) {
							String headerValue = headerValues.nextElement();
							addHeader(headerName, headerValue, headerMap);
						}
					}
				}
			}
		}
	}

	/**
	 * Grab the request state ThreadLocal and return the requested values
	 * 
	 * @return the values of the requested header or null it could not be retrieved
	 */
	@SuppressWarnings("unchecked")
	private static Enumeration<String> getThreadLocalRequestHeaders(String headerName) {
		WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
		Enumeration<String> headerValues = null;
		if (reqState != null) {
			headerValues = reqState.getCurrentThreadsIExtendedRequest().getIRequest()
					.getHeaders(headerName.toLowerCase());
		}
		return headerValues;
	}

	/**
	 * Add a header to the outbound headers. Duplicate values for the same header name are allowed.
	 * 
	 * @param token
	 * @param headers
	 */
	private static void addHeader(String headerName, String headerValue, Metadata headers) {
		Metadata.Key<String> key = Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER);
		headers.put(key, headerValue);
		if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
			Tr.debug(tc, "addHeader " + headerName + " with value " + headerValue);
		}
	}
}
