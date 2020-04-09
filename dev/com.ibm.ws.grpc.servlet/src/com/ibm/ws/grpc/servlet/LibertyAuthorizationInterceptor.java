package com.ibm.ws.grpc.servlet;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

public class LibertyAuthorizationInterceptor implements ServerInterceptor {

	@Override
	public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {

		String key = headers.get(GrpcServletUtils.LIBERTY_AUTH_KEY);
		boolean isAuthorized = GrpcServletUtils.authMap.get(key);
		if (!isAuthorized) {
			call.close(Status.UNAUTHENTICATED.withDescription("Unauthorized"), headers);
			// return no-op listener
			return new ServerCall.Listener<ReqT>() {};
		}
		return next.startCall(call, headers);
	}
}
