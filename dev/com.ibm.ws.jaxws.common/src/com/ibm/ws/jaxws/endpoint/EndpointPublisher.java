/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.endpoint;

import com.ibm.ws.jaxws.metadata.EndpointInfo;

/**
 * EndpointPublisher provides the abstraction for publishing the target endpoint into the target container.
 * e.g. Web Container, JMS Container and etc.
 */
public interface EndpointPublisher {

    /**
     * Publish the endpointInfo to the supported container,
     * 
     * @param endpointInfo endpoint meta data
     * @param context
     */
    public void publish(EndpointInfo endpointInfo, JaxWsPublisherContext context);

    /**
     * Return the endpoint publisher type, e.g. WEB and etc.
     * 
     * @return
     */
    public String getType();
}
