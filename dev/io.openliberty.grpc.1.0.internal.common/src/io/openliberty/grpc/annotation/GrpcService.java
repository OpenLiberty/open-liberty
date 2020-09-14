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
package io.openliberty.grpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.grpc.ServerInterceptor;

/**
 * GrpcService is an optional annotation that specifies a class as a Liberty-managed gRPC service.
 * Classes with this annotation must implement {@link io.grpc.BindableService io.grpc.BindableService}. 
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcService {

	/**
	 * Specifies the {@link io.grpc.ServerInterceptor io.grpc.ServerInterceptor} classes to be registered with a service
	 */
    Class<? extends ServerInterceptor>[] interceptors() default {};

}
