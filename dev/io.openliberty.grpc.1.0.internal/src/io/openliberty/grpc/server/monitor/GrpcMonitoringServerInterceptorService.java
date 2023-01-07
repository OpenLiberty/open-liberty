/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.grpc.server.monitor;

import io.grpc.ServerInterceptor;

/**
 * A service which provides Liberty monitoring ServerInterceptor instances
 */
public interface GrpcMonitoringServerInterceptorService {

    public ServerInterceptor createInterceptor(String serviceName, String appName);

}
