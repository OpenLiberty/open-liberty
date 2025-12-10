/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.client.threadleak.server;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 * Wrapper around JAX-RS Client that creates WebTarget instances.
 * This is the key class that triggers Bus creation in CXF.
 */
public class RestClient {
    private final Client client;

    public RestClient(Client client) {
        this.client = client;
    }

    /**
     * Creates a new WebTarget for the given URI.
     * In CXF, this can trigger Bus creation depending on the URL.
     */
    public WebTarget resource(String uri, MediaType type) {
        WebTarget result = this.client.target(uri);
        result.request(type);
        return result;
    }
}

