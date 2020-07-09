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
package io.openliberty.grpc.internal.monitor;

import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;

/** Extracts information about a single gRPC method. */
class GrpcMethod {
	private final String serviceName;
	private final String methodName;
	private final MethodType type;

	static GrpcMethod of(MethodDescriptor<?, ?> method) {
		String serviceName = MethodDescriptor.extractFullServiceName(method.getFullMethodName());

		// full method names are of the form: "full.serviceName/MethodName". We extract
		// the last part.
		String methodName = method.getFullMethodName().substring(serviceName.length() + 1);
		return new GrpcMethod(serviceName, methodName, method.getType());
	}

	private GrpcMethod(String serviceName, String methodName, MethodType type) {
		this.serviceName = serviceName;
		this.methodName = methodName;
		this.type = type;
	}

	String serviceName() {
		return serviceName;
	}

	String methodName() {
		return methodName;
	}

	String type() {
		return type.toString();
	}

	boolean streamsRequests() {
		return type == MethodType.CLIENT_STREAMING || type == MethodType.BIDI_STREAMING;
	}

	boolean streamsResponses() {
		return type == MethodType.SERVER_STREAMING || type == MethodType.BIDI_STREAMING;
	}
}
