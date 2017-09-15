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

package test.config.dropins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
public class ConfigDropinsServlet extends HttpServlet {

    private static final String LIBRARY_PID = "com.ibm.ws.classloading.sharedlibrary";
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
            for (ServiceReference<?> ref : references) {
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

    public void testNonXmlFile() throws Exception {
        // Make sure that we don't load files with extensions other than .xml
        checkLibraryValue(TEST1, SERVER);
    }

    public void testBrokenDropin() throws Exception {
        // We should continue to parse server.xml and dropins after encountering a broken file
        checkLibraryValue(TEST1, SIMPLE);
    }

    public void testSimpleDefaults() throws Exception {
        // Value specified for library description in dropin defaults is overridden 
        checkLibraryValue(TEST1, SERVER);
    }

    public void testSimpleOverrides() throws Exception {
        // Value specified for library description in dropin overrides is used instead of server.xml 
        checkLibraryValue(TEST1, SIMPLE);
    }

    public void testSimpleOverrides2() throws Exception {
        // Value specified for library description in dropin overrides is used instead of server.xml or default dropins
        checkLibraryValue(TEST1, SIMPLE2);
    }

    public void testDefaultsOrdering() throws Exception {
        // Value from server.xml is used. Not testing much here, just sanity. 
        checkLibraryValue(TEST1, SERVER);
    }

    public void testOverridesOrdering() throws Exception {
        // Value from simple2.xml in overrides is used rather than simple.xml from overrides or server.xml.
        checkLibraryValue(TEST1, SIMPLE2);
    }

    public void testNoServerValue1() throws Exception {
        // File in defaults, no value in server.xml, use default value
        checkLibraryValue(TEST2, A);
    }

    public void testNoServerValue2() throws Exception {
        // File in overrides, no value in server.xml, use overrides value
        checkLibraryValue(TEST2, A);
    }

    public void testNoServerValue3() throws Exception {
        // Files in defaults and overrides, no value in server.xml, use overrides value
        checkLibraryValue(TEST2, B);
    }

    private void checkLibraryValue(String libraryID, String description) throws Exception {

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(LIBRARY_PID, true));
        if (configs == null)
            fail("Could not find library configuration");

        boolean found = false;
        for (Configuration config : configs) {
            Dictionary<String, Object> props = config.getProperties();
            String id = (String) props.get("id");
            if (libraryID.equals(id)) {
                assertEquals("The description should be from simple.xml", description, props.get("description"));
                found = true;
            }
        }

        assertTrue("The configuration for the library with ID testLibrary should exist", found);

    }

    private static final String TEST1 = "testLibrary";
    private static final String TEST2 = "testLibrary2";

    private static final String SIMPLE = "Library From Simple.xml";
    private static final String SIMPLE2 = "Library from simple2.xml";
    private static final String SERVER = "Library from server.xml";
    private static final String A = "Library A";
    private static final String B = "Library B";
}
