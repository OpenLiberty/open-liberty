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
package mpRestClient10.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.KeyStore.Entry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CollectionsTestServlet")
public class CollectionsTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(CollectionsTestServlet.class.getName());

    private RestClientBuilder builder;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @Override
    public void init() throws ServletException {
        String baseUrlStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_secondary") + "/basicRemoteApp/basic";
        LOG.info("baseUrl = " + baseUrlStr);
        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .register(DuplicateWidgetExceptionMapper.class)
                        .register(UnknownWidgetExceptionMapper.class)
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);
    }

    @Test
    public void testGetSet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        CollectionsClient client = builder.build(CollectionsClient.class);
        try {
            // PRE: create some widgets
            int created = client.createNewWidgets(Arrays.asList(new Widget("Eraser", 300, 0.5),
                                                                new Widget("Blue Pen", 200, 0.3),
                                                                new Widget("Red Pen", 100, 0.3)));
            assertEquals(3, created);

            Set<Widget> set = client.getWidgets();
            assertEquals(3, set.size());
            for (Widget w : set) {
                String name = w.getName();
                assertTrue("Eraser".equals(name) || "Blue Pen".equals(name) || "Red Pen".equals(name)); 
            }
        } finally {
            //ensure we delete so as to not throw off other tests
            Set<String> widgetsToRemove = new HashSet<>();
            widgetsToRemove.add("Eraser");
            widgetsToRemove.add("Blue Pen");
            widgetsToRemove.add("Red Pen");
            try {
                client.removeWidgets(widgetsToRemove);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Caught exception cleaning after test", t);
            }
        }
    }

    @Test
    public void testGetMap(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        CollectionsClient client = builder.build(CollectionsClient.class);
        try {
            // PRE: create some widgets
            int created = client.createNewWidgets(Arrays.asList(new Widget("Eraser", 300, 0.5),
                                                                new Widget("Blue Pen", 200, 0.3),
                                                                new Widget("Red Pen", 100, 0.3)));
            assertEquals(3, created);

            Map<String, Widget> map = client.getWidgetsByName("Pen");
            assertEquals(2, map.size());
            for (Map.Entry<String, Widget> entry : map.entrySet()) {
                String name = entry.getKey();
                Widget widget = entry.getValue();
                assertTrue("Blue Pen".equals(name) || "Red Pen".equals(name)); 
                assertNotNull(widget);
                assertEquals(name, widget.getName());
            }
        } finally {
            //ensure we delete so as to not throw off other tests
            Set<String> widgetsToRemove = new HashSet<>();
            widgetsToRemove.add("Eraser");
            widgetsToRemove.add("Blue Pen");
            widgetsToRemove.add("Red Pen");
            try {
                client.removeWidgets(widgetsToRemove);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Caught exception cleaning after test", t);
            }
        }
    }

    @Test
    public void testGetSetAsync(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (!isMPRestClient12OrAbove()) {
            //assumeX doesn't work in FAT servlets, so just return here
            return;
        }
        CollectionsClient client = builder.build(CollectionsClient.class);
        try {
            // PRE: create some widgets
            int created = client.createNewWidgets(Arrays.asList(new Widget("Eraser", 300, 0.5),
                                                                new Widget("Blue Pen", 200, 0.3),
                                                                new Widget("Red Pen", 100, 0.3)));
            assertEquals(3, created);

            Set<Widget> set = client.getWidgetsAsync().toCompletableFuture().get();
            assertEquals(3, set.size());
            for (Widget w : set) {
                String name = w.getName();
                assertTrue("Eraser".equals(name) || "Blue Pen".equals(name) || "Red Pen".equals(name)); 
            }
        } finally {
            //ensure we delete so as to not throw off other tests
            Set<String> widgetsToRemove = new HashSet<>();
            widgetsToRemove.add("Eraser");
            widgetsToRemove.add("Blue Pen");
            widgetsToRemove.add("Red Pen");
            try {
                client.removeWidgets(widgetsToRemove);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Caught exception cleaning after test", t);
            }
        }
    }

    @Test
    public void testGetMapAsync(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (!isMPRestClient12OrAbove()) {
            //assumeX doesn't work in FAT servlets, so just return here
            return;
        }
        CollectionsClient client = builder.build(CollectionsClient.class);
        try {
            // PRE: create some widgets
            int created = client.createNewWidgets(Arrays.asList(new Widget("Eraser", 300, 0.5),
                                                                new Widget("Blue Pen", 200, 0.3),
                                                                new Widget("Red Pen", 100, 0.3)));
            assertEquals(3, created);

            Map<String, Widget> map = client.getWidgetsByNameAsync("Pen").toCompletableFuture().get();
            assertEquals(2, map.size());
            for (Map.Entry<String, Widget> entry : map.entrySet()) {
                String name = entry.getKey();
                Widget widget = entry.getValue();
                assertTrue("Blue Pen".equals(name) || "Red Pen".equals(name)); 
                assertNotNull(widget);
                assertEquals(name, widget.getName());
            }
        } finally {
            //ensure we delete so as to not throw off other tests
            Set<String> widgetsToRemove = new HashSet<>();
            widgetsToRemove.add("Eraser");
            widgetsToRemove.add("Blue Pen");
            widgetsToRemove.add("Red Pen");
            try {
                client.removeWidgets(widgetsToRemove);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Caught exception cleaning after test", t);
            }
        }
    }
    private static boolean isMPRestClient12OrAbove() {
        try {
            Class.forName("org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory"); //new interface in 1.2
            return true;
        } catch (ClassNotFoundException ex) {
            return false; // MP Rest Client 1.0 or 1.1
        }
    }
}