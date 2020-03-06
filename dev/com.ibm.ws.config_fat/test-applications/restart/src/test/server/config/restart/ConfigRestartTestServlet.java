/*******************************************************************************
 * Copyright (c) 2017-2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config.restart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
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
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.jmx.PlatformMBeanService;

import test.server.config.ServerConfigTest;

@SuppressWarnings("serial")
public class ConfigRestartTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();

        String testName = request.getParameter("testName");
        assertNotNull("No testName parameter specified", testName);

        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class);

        List<ServiceReference> references = new ArrayList<ServiceReference>();
        BundleContext bundleContext = bundle.getBundleContext();
        try {
            if ("before".equals(testName)) {
                testConfigurationBefore(bundleContext, references);
            } else if ("after".equals(testName)) {
                testConfigurationAfter(bundleContext, references);
            } else if ("beforeVariable".equals(testName)) {
                testBeforeVariable(bundleContext, references);
            } else if ("afterVariable".equals(testName)) {
                testAfterVariable(bundleContext, references);
            } else if ("checkImport".equals(testName)) {
                testCheckImport(bundleContext, references);
            } else if (ServerConfigTest.CHECK_VARIABLE_IMPORT.equals(testName)) {
                testCheckVariableImport(bundleContext, references);
            } else if (ServerConfigTest.CHECK_VARIABLE_IMPORT_UPDATE.equals(testName)) {
                testCheckVariableImportAfterUpdate(bundleContext, references);
            } else if ("refresh".equals(testName)) {
                testRefresh(bundleContext, references);
            } else {
                fail("Invalid test name: " + testName);
            }
            writer.println("Test Passed");
        } catch (Throwable e) {
            e.printStackTrace(writer);
        } finally {
            for (ServiceReference ref : references) {
                bundleContext.ungetService(ref);
            }
        }

        writer.flush();
        writer.close();
    }

    private void testCheckVariableImport(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(ctx, references);

        Configuration[] configs = null;
        String filter = null;

        filter = "(" + Constants.SERVICE_PID + "=" + "person" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration firstName property", "Joe", configs[0].getProperties().get("firstName"));
        assertEquals("Configuration lastName property", "Doe", configs[0].getProperties().get("lastName"));

    }

    private void testCheckVariableImportAfterUpdate(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(ctx, references);

        Configuration[] configs = null;
        String filter = null;

        filter = "(" + Constants.SERVICE_PID + "=" + "person" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration firstName property", "Jill", configs[0].getProperties().get("firstName"));
        assertEquals("Configuration lastName property", "Doe", configs[0].getProperties().get("lastName"));

    }

    private void testConfigurationBefore(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(ctx, references);
        testSingletonBefore(ca);
        testFactoryBefore(ca);
    }

    private void testConfigurationAfter(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(ctx, references);
        testSingletonAfter(ca);
        testFactoryAfter(ca);
    }

    private void testSingletonBefore(ConfigurationAdmin ca) {
        Configuration[] configs = null;
        String filter = null;

        filter = "(" + Constants.SERVICE_PID + "=" + "singleton-one" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration value property", "abc", configs[0].getProperties().get("value"));

        filter = "(" + Constants.SERVICE_PID + "=" + "singleton-two" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration value property", "def", configs[0].getProperties().get("value"));
    }

    private void testSingletonAfter(ConfigurationAdmin ca) {
        Configuration[] configs = null;
        String filter = null;

        // deleted after restart
        filter = "(" + Constants.SERVICE_PID + "=" + "singleton-one" + ")";
        configs = getConfiguration(ca, filter);
        assertNull("There should not be a configuration found for filter " + filter, configs);

        // unchanged after restart
        filter = "(" + Constants.SERVICE_PID + "=" + "singleton-two" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration value property", "def", configs[0].getProperties().get("value"));
    }

    private void testFactoryBefore(ConfigurationAdmin ca) {
        Configuration[] configs = null;
        String filter = null;

        filter = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + "factory" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 2 configurations for filter " + filter, 2, configs.length);

        filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + "factory" + ")(id=1))";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration name property", "one", configs[0].getProperties().get("name"));

        filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + "factory" + ")(id=2))";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration name property", "two", configs[0].getProperties().get("name"));
    }

    private void testFactoryAfter(ConfigurationAdmin ca) {
        Configuration[] configs = null;
        String filter = null;

        filter = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + "factory" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 2 configurations for filter " + filter, 2, configs.length);

        filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + "factory" + ")(id=1))";
        configs = getConfiguration(ca, filter);
        assertNull("There should not be a configuration found for filter " + filter, configs);

        filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + "factory" + ")(id=2))";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration name property", "two", configs[0].getProperties().get("name"));

        filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + "factory" + ")(id=3))";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration name property", "three", configs[0].getProperties().get("name"));

    }

    private void testBeforeVariable(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(ctx, references);

        Configuration[] configs = null;
        String filter = null;

        filter = "(" + Constants.SERVICE_PID + "=" + "myPort" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration type property", "http", configs[0].getProperties().get("type"));
        assertEquals("Configuration value property", "1234", configs[0].getProperties().get("value"));
    }

    private void testAfterVariable(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(ctx, references);

        Configuration[] configs = null;
        String filter = null;

        filter = "(" + Constants.SERVICE_PID + "=" + "myPort" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration type property", "http", configs[0].getProperties().get("type"));
        assertEquals("Configuration value property", "5678", configs[0].getProperties().get("value"));
    }

    private void testCheckImport(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(ctx, references);

        Configuration[] configs = null;
        String filter = null;

        filter = "(" + Constants.SERVICE_PID + "=" + "person" + ")";
        configs = getConfiguration(ca, filter);
        assertNotNull("There should be a configuration found for filter " + filter, configs);
        assertEquals("There should be 1 configuration for filter " + filter, 1, configs.length);
        assertEquals("Configuration firstName property", "Joe", configs[0].getProperties().get("firstName"));
        assertEquals("Configuration lastName property", "Doe", configs[0].getProperties().get("lastName"));
    }

    private ConfigurationAdmin getConfigurationAdmin(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ServiceReference<ConfigurationAdmin> ref = ctx.getServiceReference(ConfigurationAdmin.class);
        assertNotNull("No ConfigurationAdmin service", ref);
        references.add(ref);
        return ctx.getService(ref);
    }

    private Configuration[] getConfiguration(ConfigurationAdmin ca, String filter) {
        try {
            return ca.listConfigurations(filter);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Invalid filter: " + filter, e);
        } catch (IOException e) {
            throw new RuntimeException("Error listing configurations", e);
        }
    }

    private void testRefresh(BundleContext ctx, List<ServiceReference> references) throws Exception {
        ServiceTracker tracker = new ServiceTracker(ctx, PlatformMBeanService.class.getName(), null);
        tracker.open();
        try {
            PlatformMBeanService service = (PlatformMBeanService) tracker.waitForService(60 * 1000);
            assertNotNull("No Platform MBean service", service);

            List<String> serverXML = new ArrayList<String>();
            serverXML.add(new File("server.xml").getAbsolutePath());

            MBeanServer server = service.getMBeanServer();
            ObjectName configServices = new ObjectName("WebSphere", "service", "com.ibm.ws.kernel.filemonitor.FileNotificationMBean");
            server.invoke(configServices, "notifyFileChanges", new Object[] { Collections.emptyList(), serverXML, Collections.EMPTY_LIST },
                          new String[] { Collection.class.getName(), Collection.class.getName(), Collection.class.getName() });
        } finally {
            tracker.close();
        }
    }
}
