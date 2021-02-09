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
import javax.ws.rs.client.ClientRequestFilter;

/**
 * Example: http://www.hascode.com/2013/12/jax-rs-2-0-rest-client-features-by-example/
 */
public class JAXRS21ClientRequestFilter1 implements ClientRequestFilter {

    @Override
    public void filter(final ClientRequestContext rc) throws IOException {
        rc.getClient().property("filter1", rc.getMethod());
    }

}
