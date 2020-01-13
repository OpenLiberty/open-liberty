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
package com.ibm.ws.wsat.service;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.ws.wsat.common.impl.WSATCoordinator;

/**
 * CoordinationContext information
 */
public class WSATContext {

    private final String globalId;
    private final EndpointReferenceType registration;
    private final long expires;

    public WSATContext(String id, WSATCoordinator registration, long expires) {
        this.globalId = id;
        this.registration = registration.getEndpointReference();
        this.expires = expires;
    }

    public String getId() {
        return globalId;
    }

    public EndpointReferenceType getRegistration() {
        return registration;
    }

    public long getExpires() {
        return expires;
    }
}
