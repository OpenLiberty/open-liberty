/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package mpRestClient12.jsonbContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JsonbContextTestServlet")
public class JsonbContextTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(JsonbContextTestServlet.class.getName());

    private RestClientBuilder builder;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @Override
    public void init() throws ServletException {
        String baseUrlStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_secondary") + "/basicRemoteApp";
        LOG.info("baseUrl = " + baseUrlStr);
        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .register(JsonbContextResolver.class)
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);
    }

    @Test
    public void testSimplePostGetDeleteUsingUserSpecifiedJsonbContext(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        BasicServiceClient client = builder.build(BasicServiceClient.class);
        client.createNewWidget(new Widget("Pencils", 100, 0.2));
        assertTrue("POSTed widget does not show up in query", client.getWidgetNames().contains("Pencils"));
        Widget w = client.getWidget("Pencils");
        assertEquals("Widget returned from GET does not match widget POSTed", "Pencils;100;0.2", w.toString());
    }
}