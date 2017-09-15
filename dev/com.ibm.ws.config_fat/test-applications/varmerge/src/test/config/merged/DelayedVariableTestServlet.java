/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.config.merged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 *
 */
public class DelayedVariableTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 6784264217930725272L;
    private static final String DELAYED_VAR_PID = "com.ibm.ws.config.variable.delay";

    private static final String DELAYED_VAR_ATTR = "delayedVariable";
    private static final String DELAYED_IBM_VAR_ATTR = "delayedIBMVar";
    private static final String IMMEDIATE_VAR_ATTR = "immediateVar";
    private static final String IMMEDIATE_VAR_TWO = "immediateVarTwo";

    private static final String VARIABLE_STRING = "${variableDelayTest}";
    private static final String EVALUATED_VARIABLE = "evaluated";

    private final ArrayList<ServiceReference<?>> references = new ArrayList<ServiceReference<?>>();
    private BundleContext bundleContext;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();

        String testName = request.getParameter("testName");
        assertNotNull("No testName parameter specified", testName);

        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class);

        this.bundleContext = bundle.getBundleContext();
        try {

            log("Begin test: " + testName);
            invokeTest(testName);
            writer.println("OK");

        } catch (NoSuchMethodException e) {
            writer.println("FAILED - Invalid test name: " + testName);
        } catch (InvocationTargetException e) {
            writer.println("FAILED");
            e.getTargetException().printStackTrace(writer);
        } catch (Throwable e) {
            writer.println("FAILED");
            e.printStackTrace(writer);
        } finally {
            log("End test: " + testName);
            for (ServiceReference<?> ref : references) {
                bundleContext.ungetService(ref);
            }
            references.clear();
        }

        writer.flush();
        writer.close();
    }

    private void invokeTest(String testName) throws Exception {
        Method method = getClass().getDeclaredMethod(testName);
        method.invoke(this);
    }

    private ConfigurationAdmin getConfigurationAdmin(BundleContext ctx, List<ServiceReference<?>> references) throws Exception {
        ServiceReference<ConfigurationAdmin> ref = ctx.getServiceReference(ConfigurationAdmin.class);
        assertNotNull("No ConfigurationAdmin service", ref);
        references.add(ref);
        return ctx.getService(ref);
    }

    private String getFilter(String pid, boolean isFactory) {
        if (isFactory) {
            return "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + pid + ")";
        } else {
            return "(" + Constants.SERVICE_PID + "=" + pid + ")";
        }
    }

    public void testVariableDelay() throws Exception {

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(DELAYED_VAR_PID, true);
        Configuration[] varDelay = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + DELAYED_VAR_PID + " should exist", varDelay);

        assertEquals("There should be one instance of PID " + DELAYED_VAR_PID, 1, varDelay.length);

        Dictionary<String, Object> properties = varDelay[0].getProperties();

        assertEquals(VARIABLE_STRING, properties.get(DELAYED_VAR_ATTR));
        assertEquals(VARIABLE_STRING, properties.get(DELAYED_IBM_VAR_ATTR));
        assertEquals(EVALUATED_VARIABLE, properties.get(IMMEDIATE_VAR_ATTR));
        assertEquals(EVALUATED_VARIABLE, properties.get(IMMEDIATE_VAR_TWO));

    }

}
