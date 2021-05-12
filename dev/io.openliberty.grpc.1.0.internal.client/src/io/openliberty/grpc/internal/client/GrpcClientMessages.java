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
package io.openliberty.grpc.internal.client;

public interface GrpcClientMessages {
	
    /** RAS trace bundle for the GRPC client channel */
    String GRPC_BUNDLE = "io.openliberty.grpc.internal.client.resources.grpcclientmessages";
    /** RAS trace bundle for the GRPC client security channel */
    String GRPC_CLIENT_SECURITY_BUNDLE = "io.openliberty.grpc.internal.client.security.resources.grpcclientsecuritymessages";
    /** RAS trace name for the GRPC channel */
    String GRPC_TRACE_NAME = "GRPC";

}
