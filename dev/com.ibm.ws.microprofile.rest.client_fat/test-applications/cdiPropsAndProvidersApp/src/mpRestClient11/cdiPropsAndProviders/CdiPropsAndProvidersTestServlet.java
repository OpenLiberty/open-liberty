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
package mpRestClient11.cdiPropsAndProviders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.Test;

import componenttest.app.FATServlet;
import mpRestClient10.basicCdi.BasicClientTestServlet;
import mpRestClient10.basicCdi.BasicServiceClient;
import mpRestClient10.basicCdi.DuplicateWidgetException;
import mpRestClient10.basicCdi.MyFilter;
import mpRestClient10.basicCdi.UnknownWidgetException;
import mpRestClient10.basicCdi.Widget;

@SuppressWarnings("serial")
@ApplicationScoped
@WebServlet(urlPatterns = "/CdiPropsAndProvidersTestServlet")
public class CdiPropsAndProvidersTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(CdiPropsAndProvidersTestServlet.class.getName());

    @Inject
    @RestClient
    private CdiPropsAndProvidersClient client;

    @Test
    public void testProvidersAndPropertiesFromMPConfig(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            client.get();
            fail("Did not throw expected MyException from MyExceptionMapper specified in MP Config in server.xml");
        } catch (MyException ex) {
            // expected
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            if (r != null && r.getStatus() == 409) {
                // processed Filter4, but failed to go through MyExceptionMapper
                fail("MyExceptionMapper provider (registered via MP Config in server.xml) was not invoked");
            }
            ex.printStackTrace();
            fail("Unexpected web application exception - response code: " + r.getStatus());
        }

        Bag bag = Bag.getBag();
        // 3 filters specified in MP Config + 1 specified on client interface
        assertEquals(4, bag.filtersInvoked.size());

        // priority order specified in same place as filters themselves
        assertEquals("Filter2", bag.filtersInvoked.get(0).getSimpleName());
        assertEquals("Filter1", bag.filtersInvoked.get(1).getSimpleName());
        assertEquals("Filter3", bag.filtersInvoked.get(2).getSimpleName());
        assertEquals("Filter4", bag.filtersInvoked.get(3).getSimpleName());
    }
}