/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.component;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

/**
 * OSGi services registered with this interface will receive a callback when a builder is created for a Rest Client which is to be injected as a bean.
 * <p>
 * This is necessary because CXF doesn't call {@link RestClientBuildListener}s when creating clients for injection, only when they're created via the API.
 */
public interface CxfRestClientBeanBuilderListener {

    public void onNewBuilder(RestClientBuilder builder);
    
}
