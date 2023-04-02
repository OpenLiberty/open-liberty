/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * A gRPC client for connecting to the Jaeger trace retrieval API - https://www.jaegertracing.io/docs/1.23/apis/#trace-retrieval-apis
 * <p>
 * Generated from https://github.com/jaegertracing/jaeger-idl/tree/main/proto/api_v2
 * <p>
 * Generating the client using the Makefile was straightforward (it starts a docker container which has all the dependencies), but I needed to manually remove references to
 * "GoGoProtos" from the generated code.
 */
package io.jaegertracing.api_v2;