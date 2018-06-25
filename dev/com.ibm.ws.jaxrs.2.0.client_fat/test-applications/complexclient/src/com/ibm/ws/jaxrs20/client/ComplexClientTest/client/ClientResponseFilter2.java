/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.ComplexClientTest.client;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

public class ClientResponseFilter2 implements ClientResponseFilter {

    @Override
    public void filter(final ClientRequestContext reqCtx,
                       final ClientResponseContext resCtx) throws IOException {
        if (resCtx.getStatusInfo() != null) {
            resCtx.setStatus(223);
        }
        System.out.println("ClientResponseFilter2: ");
        System.out.println("location: " + resCtx.getLocation());
        System.out.println("statusInfo: " + resCtx.getStatusInfo());
        System.out.println("reqCtx: " + reqCtx.getMethod());
    }
}
