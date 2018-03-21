/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;

/**
 * Service interface to provide operation names for building spans in filters
 */
public interface OpentracingFilterHelper {

    String getBuildSpanName(ClientRequestContext clientRequestContext);

    String getBuildSpanName(ContainerRequestContext incomingRequestContext, ResourceInfo resourceInfo);
}
