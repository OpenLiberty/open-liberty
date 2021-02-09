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
package com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.client;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

/**
 *
 */
public class JAXRS21ClientResponseFilter1 implements ClientResponseFilter {

    @Override
    public void filter(final ClientRequestContext reqCtx, final ClientResponseContext resCtx) throws IOException {
        if (resCtx.getDate() != null) {
            resCtx.setStatus(222);
        }
        System.out.println("JAXRS21ClientResponseFilter1: ");
        System.out.println("status: " + resCtx.getStatus());
        System.out.println("date: " + resCtx.getDate());
        System.out.println("last-modified: " + resCtx.getLastModified());
    }
}
