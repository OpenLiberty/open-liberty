/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient10.handleresponses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ClientTestServlet")
public class ClientTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(ClientTestServlet.class.getName());

    private RestClientBuilder builder;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @Override
    public void init() throws ServletException {
        String baseUrlStr = "https://localhost:" + getSysProp("bvt.prop.HTTP_secondary.secure") + "/basicRemoteApp";
        LOG.info("baseUrl = " + baseUrlStr);
        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .register(UnknownWidgetExceptionMapper.class)
                        .property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig")
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);
    }

    @Test
    public void testEmpty202Response(HttpServletRequest req, HttpServletResponse res) throws Exception {

        Response r = builder.build(HandleResponsesClient.class).batchWidget(new Widget("Markers", 150, 0.2));
        assertEquals(202, r.getStatus());

        String entity = null;
        try {
            entity = r.readEntity(String.class);
            fail("Did not throw expected IllegalStateException");
        } catch (IllegalStateException expected) {
            entity = null;
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Caught unexpected exception: " + t);
        }
    }

    @Test
    public void testNonEmpty202Response(HttpServletRequest req, HttpServletResponse res) throws Exception {

        //pre condition:
        Widget w = new Widget("Whiteboard", 12, 120.50);
        Response r = builder.build(HandleResponsesClient.class).batchWidget(w);
        assertEquals(202, r.getStatus());

        r = builder.build(HandleResponsesClient.class).unbatchWidget(w);
        assertEquals(202, r.getStatus());
        Widget entity = r.readEntity(Widget.class);
        assertNotNull(entity);
        assertEquals("Whiteboard", entity.getName());
    }
}