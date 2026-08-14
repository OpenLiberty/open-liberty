/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.websocket.app;

import javax.enterprise.context.ApplicationScoped;

/**
 * A simple CDI bean to be injected into WebSocket endpoint
 */
@ApplicationScoped
public class InjectedBean {

    private boolean injected = false;

    public InjectedBean() {
        // Default constructor
    }

    public String getMessage() {
        injected = true;
        return "CDI Bean successfully injected!";
    }

    public boolean wasInjected() {
        return injected;
    }
}

// Made with Bob
