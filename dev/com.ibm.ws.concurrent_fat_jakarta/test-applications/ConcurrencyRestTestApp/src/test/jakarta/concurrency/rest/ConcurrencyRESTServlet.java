/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency.rest;

import static org.junit.Assert.assertEquals;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ConcurrencyRESTServlet")
public class ConcurrencyRESTServlet extends FATServlet {
    /**
     * Invoke a REST application method that is annotated with Asynchronous.
     */
    @Test
    public void testInvokeRESTAsync(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = "http://" + request.getServerName() + ":" + request.getServerPort() + "/ConcurrencyRestTestApp/testapp/test/threadname";

        Client client = ClientBuilder.newBuilder().build();
        try {
            String threadName = client.target(uri).request("text/plain").buildGet().invoke(String.class);
            String currentThreadName = Thread.currentThread().getName();
            assertEquals("REST app thread: " + threadName + "; Current thread: " + currentThreadName,
                         false, threadName.equals(currentThreadName));
        } finally {
            client.close();
        }
    }
}
