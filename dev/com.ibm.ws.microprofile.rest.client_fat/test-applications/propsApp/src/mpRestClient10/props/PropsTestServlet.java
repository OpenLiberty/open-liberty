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
package mpRestClient10.props;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PropsTestServlet")
public class PropsTestServlet extends FATServlet {
    private final static Logger _log = Logger.getLogger(PropsTestServlet.class.getName());

    @Test
    public void testKeepAliveProp(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testKeepAliveProp";
        int port = req.getServerPort();
        PropChecker client = RestClientBuilder.newBuilder()
                                              .baseUrl(new URL("http://localhost:" + port + "/propsApp"))
                                              .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                                              .build(PropChecker.class);
        
        _log.info(m + " invoking rest client");
        Response r = client.checkKeepAliveProp();
        assertEquals(200, r.getStatus());
        String entity = r.readEntity(String.class);
        _log.info(m + " entity: " + entity);
        assertNotNull(entity);
        assertEquals("close", entity.toLowerCase());
    }
}
