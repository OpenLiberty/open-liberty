/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.wsat.service;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.annotation.Trivial;
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

    @Trivial
    public String getId() {
        return globalId;
    }

    @Trivial
    public EndpointReferenceType getRegistration() {
        return registration;
    }

    @Trivial
    public long getExpires() {
        return expires;
    }
}
