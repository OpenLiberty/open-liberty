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
package mpRestClient10.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
@WebServlet(urlPatterns = "/BasicClientTestServlet")
public class BasicClientTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(BasicClientTestServlet.class.getName());

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
                        .register(DuplicateWidgetExceptionMapper.class)
                        .register(UnknownWidgetExceptionMapper.class)
                        .property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig")
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUrl(baseUrl);
    }

    @Test
    public void testSimplePostGetDelete(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        BasicServiceClient client = builder.build(BasicServiceClient.class);
        try {
            client.createNewWidget(new Widget("Pencils", 100, 0.2));
            assertTrue("POSTed widget does not show up in query", client.getWidgetNames().contains("Pencils"));
            Widget w = client.getWidget("Pencils");
            assertEquals("Widget returned from GET does not match widget POSTed", "Pencils", w.getName());
            assertEquals("Widget returned from GET does not match widget POSTed", 100, w.getQuantity());
            assertEquals("Widget returned from GET does not match widget POSTed", 0.2, w.getWeight(), 0.0);
        } finally {
            //ensure we delete so as to not throw off other tests
            client.removeWidget("Pencils");
        }
    }

    @Test
    public void testMaps404Exception(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        BasicServiceClient client = builder.build(BasicServiceClient.class);
        try {
            client.getWidget("NO_SUCH_WIDGET");
            fail("Did not throw expected UnknownWidgetException");
        } catch (UnknownWidgetException ex) {
            // expected
        }

        try {
            client.removeWidget("NO_SUCH_WIDGET");
            fail("Did not throw expected UnknownWidgetException");
        } catch (UnknownWidgetException ex) {
            // expected
        }
    }

    @Test
    public void testMaps409Exception(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        BasicServiceClient client = builder.build(BasicServiceClient.class);

        // create the initial widget
        client.createNewWidget(new Widget("Widget1", 5, 25.06));

        // now try to create another widget with the same name
        try {
            client.createNewWidget(new Widget("Widget1", 17, 1.005));

            fail("Did not throw expected DuplicateWidgetException");
        } catch (DuplicateWidgetException ex) {
            // expected
        }
    }

    @Test
    public void testReadTimeout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        builder.property("com.ibm.ws.jaxrs.client.receive.timeout", "5");
        long startTime = System.nanoTime();
        WaitServiceClient client = builder.build(WaitServiceClient.class);
        Response r = null;
        try {
            r = client.waitFor(20);
            fail("Did not throw expected ProcessingException");
        } catch (ProcessingException expected) {
            LOG.info("Caught expected ProcessingException");
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Caught unexpected exception", t);
            fail("Caught unexpected exception: " + t);
        }
        long elapsedTime = System.nanoTime() - startTime;
        long elapsedTimeSecs = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        LOG.info("waited >=" + elapsedTimeSecs + " seconds");
        assertTrue("Did not time out when expected (5 secs) - instead waited at least 20 seconds.",
                   elapsedTimeSecs < 20);
    }
}