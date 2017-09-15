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
package test.metatype.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 *
 */
public class MetatypeProviderTestServlet extends HttpServlet {

    /**  */
    private static final String LIFESPAN_ATTRIBUTE = "lifespan";

    /**  */
    private static final String NAME_ATTRIBUTE = "name";

    /**  */
    private static final long serialVersionUID = -1977276153574893730L;

    private final ArrayList<ServiceReference> references = new ArrayList<ServiceReference>();
    private BundleContext bundleContext;

    public static final String PLANT_PID = "test.metatype.provider.plant";
    public static final String ANIMAL_PID = "test.metatype.provider.animal";

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
            if (testName.equals("dump")) {
                // Just for debugging
                dump(writer);
            } else {
                log("Begin test: " + testName);
                invokeTest(testName);
                writer.println("OK");
            }
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
            for (ServiceReference ref : references) {
                bundleContext.ungetService(ref);
            }
            references.clear();
        }

        writer.flush();
        writer.close();
    }

    /**
     * Just for manual debugging
     * 
     * @param writer
     * @throws Exception
     */
    private void dump(PrintWriter writer) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configurations;
        try {
            configurations = ca.listConfigurations(null);
            for (Configuration c : configurations) {
                writer.print("<P>");
                String pid = c.getFactoryPid() == null ? c.getPid() : c.getFactoryPid();
                if (pid.startsWith("test")) {
                    writer.println(pid);
                }
            }
        } catch (InvalidSyntaxException e1) {
            throw new IOException(e1);
        }

    }

    private void invokeTest(String testName) throws Exception {
        Method method = getClass().getDeclaredMethod(testName);
        method.invoke(this);
    }

    private ConfigurationAdmin getConfigurationAdmin(BundleContext ctx, List<ServiceReference> references) throws Exception {
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

    public void testMetatypeProvider1() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(PLANT_PID, true);
        Configuration[] children1 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PLANT_PID + " should exist", children1);
        assertEquals("There should only be one instance of PID " + PLANT_PID, 1, children1.length);

    }

    public void testMetatypeProvider2() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(ANIMAL_PID, true);
        Configuration[] children1 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + ANIMAL_PID + " should exist", children1);
        assertEquals("There should only be one instance of PID " + ANIMAL_PID, 1, children1.length);

        Configuration child = children1[0];
        Dictionary<String, Object> dictionary = child.getProperties();
        assertNotNull("The dictionary should exist", dictionary);

        Object lifespan = dictionary.get(LIFESPAN_ATTRIBUTE);
        assertNotNull("The lifespan attribute should exist", lifespan);

        assertEquals("The lifespan attribute should be 1000 days", 86400000000L, lifespan);

        Object name = dictionary.get(NAME_ATTRIBUTE);
        assertNotNull("The name attribute should exist", name);
        assertEquals("The name should be marmot", "marmot", name);
    }

    /**
     * Test that metatype converted config is removed when its corresponding metatype is removed
     * 
     * PLANT_PID should not exist in config admin even though it was specified using the full pid.
     * We don't recreate the pre-metatype config when the metatype is removed.
     * 
     * ANIMAL_PID should not exist.
     * 
     * @throws Exception
     */
    public void testMetatypeProvider3() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(PLANT_PID, true);
        Configuration[] children1 = ca.listConfigurations(filter);
        assertNull("The configuration for " + PLANT_PID + " should not exist", children1);

        filter = getFilter(ANIMAL_PID, true);
        children1 = ca.listConfigurations(filter);
        assertNull("The configuration for " + ANIMAL_PID + " should not exist", children1);
    }
}
