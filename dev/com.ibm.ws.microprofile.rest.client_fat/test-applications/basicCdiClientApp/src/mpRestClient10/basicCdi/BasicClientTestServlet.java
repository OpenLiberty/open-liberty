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
package mpRestClient10.basicCdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.logging.Level;
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
@WebServlet(urlPatterns = "/BasicClientTestServlet")
public class BasicClientTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(BasicClientTestServlet.class.getName());

    @Inject
    private MyCdiManagedObject obj;

    @Inject
    @RestClient
    private BasicServiceClient client;

    @Test
    public void testSimplePostGetDelete_injectedIntoFieldOnServlet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        testSimplePostGetDelete(req, resp, this.client);
    }

    @Test
    public void testSimplePostGetDelete_injectedIntoCtorOnCdiObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        testSimplePostGetDelete(req, resp, this.obj.getClientFromCtor());
    }

    @Test
    public void testSimplePostGetDelete_injectedIntoFieldOnCdiObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        testSimplePostGetDelete(req, resp, this.obj.getClientFromField());
    }

    @Test
    public void testSimplePostGetDelete_injectedIntoMethodOnCdiObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        testSimplePostGetDelete(req, resp, this.obj.getClientFromMethod());
    }

    private void testSimplePostGetDelete(HttpServletRequest req, HttpServletResponse resp, BasicServiceClient client) throws Exception {
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
    public void testFiltersInvoked(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            MyFilter.requestFilterInvocationCount = 0;
            MyFilter.responseFilterInvocationCount = 0;

            client.createNewWidget(new Widget("Erasers", 10, 0.8));
            assertEquals("Request filter was not invoked or invoked more than once", 1, MyFilter.requestFilterInvocationCount);
            assertEquals("Response filter was not invoked or invoked more than once", 1, MyFilter.responseFilterInvocationCount);
            assertTrue("POSTed widget does not show up in query", client.getWidgetNames().contains("Erasers"));

        } finally {
            //ensure we delete so as to not throw off other tests
            client.removeWidget("Erasers");
        }
    }

    @Test
    public void testSqlDateTypeAndFiltersInvokedCorrectly(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // this test is not applicable to MP Rest Client 1.0 or 1.1 - if we are running these older versions, just abort the test
        try {
            Class.forName("org.eclipse.microprofile.rest.client.annotation.ClientHeadersParam");
        } catch (Throwable t) {
            LOG.log(Level.INFO, "Could not load MP Rest Client 1.2+ class - skipping test - expected in MP Rest Client 1.0 or 1.1", t);
            return;
        }
        MyFilter.requestFilterInvocationCount = 0;
        MyFilter.responseFilterInvocationCount = 0;
        
        Date currentDate = client.getCurrentDate();
        LOG.info("Client received from getCurrentDate(): " + currentDate);
        assertNotNull("Date returned was null", currentDate);
        assertEquals("Request filter was not invoked or invoked more than once", 1, MyFilter.requestFilterInvocationCount);
        assertEquals("Response filter was not invoked or invoked more than once", 1, MyFilter.responseFilterInvocationCount);

        Date echoedDate = client.echoDate(currentDate);
        LOG.info("Client received from echoDate(" + currentDate + "): " + echoedDate);
        assertNotNull("Date returned was null", echoedDate);
        assertEquals("Echoed date does not match passed-in date", currentDate.getTime(), echoedDate.getTime());
        assertEquals("Request filter was not invoked or invoked more than once", 2, MyFilter.requestFilterInvocationCount);
        assertEquals("Response filter was not invoked or invoked more than once", 2, MyFilter.responseFilterInvocationCount);
    }
}