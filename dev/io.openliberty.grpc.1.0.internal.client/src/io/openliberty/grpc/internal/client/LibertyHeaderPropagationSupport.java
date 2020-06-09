package io.openliberty.grpc.internal.client;

import java.util.Arrays;
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

	private static final TraceComponent tc = Tr.register(LibertyHeaderPropagationSupport.class);

	/**
	 * Attempt to retrieve the headers configured via headersToPropagate and add
	 * them to the outbound grpc call
	 * 
	 * @param method
	 * @param headerMap
	 */
	@SuppressWarnings("rawtypes")
	public static void handleHeaderPropagation(MethodDescriptor method, Metadata headerMap) {

		String headersToPropagate = GrpcClientConfigHolder.getHeaderPropagationSupport(method.getFullMethodName());

		if (headersToPropagate == null || headersToPropagate.isEmpty()) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "no header propagation configured");
			}
			return;
		} else {
			List<String> headerNames = Arrays.asList(headersToPropagate.split("\\s*,\\s*"));
			if (!headerNames.isEmpty()) {
				for (String headerName : headerNames) {
					String headerValue = getThreadLocalRequestHeader(headerName);
					if (headerValue != null) {
						addHeader(headerName, headerValue, headerMap);
					}
				}
			}
		}
	}

	/**
	 * Grab the request state ThreadLocal and return
	 * 
	 * @return the value of the requested header or null it could not be retrieved
	 */
	private static String getThreadLocalRequestHeader(String headerName) {
		WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
		String headerValue = null;
		if (reqState != null) {
			headerValue = reqState.getCurrentThreadsIExtendedRequest().getIRequest()
					.getHeader(headerName.toLowerCase());
		}
		return headerValue;
	}

	/**
	 * Add a header to the outbound headers
	 * 
	 * @param token
	 * @param headers
	 */
	private static void addHeader(String headerName, String headerValue, Metadata headers) {
		Metadata.Key<String> key = Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER);
		headers.put(key, headerValue);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Authorization header with Bearer token is added successfully");
		}
	}
}
