/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient10.multiClientCdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@ApplicationScoped
@WebServlet(urlPatterns = "/MultiClientCdiTestServlet")
public class MultiClientCdiTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(MultiClientCdiTestServlet.class.getName());

    @Inject
    @RestClient
    private ClientA clientA;

    @Inject
    @RestClient
    private ClientA clientA2;

    @Inject
    @RestClient
    private ClientB clientB;

    @Test
    public void testSameClientsGetSameResults(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        assertNotNull("Injection did not occur for ClientA", clientA);
        assertNotNull("Injection did not occur for ClientA2", clientA2);
        String result1 = clientA.getString();
        String result2 = clientA2.getString();
        assertEquals("ClientA did not get expected result", "ResultA", result1);
        assertEquals("ClientA2 did not get expected result", "ResultA", result2);
    }

    @Test
    public void testCanInvokeDifferentClients(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        assertNotNull("Injection did not occur for ClientA", clientA);
        assertNotNull("Injection did not occur for ClientB", clientB);
        String result1 = clientA.getString();
        String result2 = clientB.getString();
        assertEquals("ClientA did not get expected result", "ResultA", result1);
        assertEquals("ClientB did not get expected result", "ResultB", result2);

        // same thing - reverse order
        result2 = clientB.getString();
        result1 = clientA.getString();
        assertEquals("ClientA did not get expected result on second invocation", "ResultA", result1);
        assertEquals("ClientB did not get expected result on second invocation", "ResultB", result2);
    }
}