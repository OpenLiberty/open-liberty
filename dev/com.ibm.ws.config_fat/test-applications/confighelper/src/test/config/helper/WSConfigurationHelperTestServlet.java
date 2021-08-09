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

package test.config.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
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

import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 *
 */
public class WSConfigurationHelperTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;
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

    private WSConfigurationHelper getHelper() {
        ServiceReference<WSConfigurationHelper> ref = bundleContext.getServiceReference(WSConfigurationHelper.class);
        if (ref == null) {
            fail("Reference to WSConfigurationHelper not found");
            return null;
        } else {
            return bundleContext.getService(ref);
        }
    }

    public void testGetDefaultProperties() throws Exception {

        String factoryPid = "com.ibm.ws.config";

        Dictionary<String, Object> dictionary = getHelper().getMetaTypeDefaultProperties(factoryPid);
        assertEquals(new Long(500), dictionary.get("monitorInterval"));
        assertEquals("polled", dictionary.get("updateTrigger"));
        assertEquals(OnError.WARN, dictionary.get("onError"));

    }

    public void testGetDefaultPropertiesWithRequired() throws Exception {
        String factoryPid = "com.ibm.ws.config.test.helper";

        Dictionary<String, Object> dictionary = getHelper().getMetaTypeDefaultProperties(factoryPid);
        assertEquals("hello", dictionary.get("defaultVal"));
        assertNull(dictionary.get("requiredVal"));

    }

    /**
     * Add a default singleton, replacing a "default" singleton (not specified in server.xml, but has all required metatype defaults)
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration1() throws Exception {
        String pid = "com.ibm.ws.config";

        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("monitorInterval", "700");

        getHelper().addDefaultConfiguration(pid, dictionary);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration c = ca.getConfiguration(pid);
        assertEquals(new Long(700), c.getProperties().get("monitorInterval"));

        // Remove the default config we added, see that the singleton has returned to the original
        getHelper().removeDefaultConfiguration(pid);
        c = ca.getConfiguration(pid);
        assertEquals(new Long(500), c.getProperties().get("monitorInterval"));
    }

    /**
     * Add a default factory config for a pid that didn't previously exist
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration2() throws Exception {
        String factoryPid = "com.ibm.ws.config.test.helper";
        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("requiredVal", "goodbye");

        getHelper().addDefaultConfiguration(factoryPid, dictionary);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertEquals(1, configs.length);
        Dictionary<String, Object> props = configs[0].getProperties();
        assertEquals("goodbye", props.get("requiredVal"));
        assertEquals("hello", props.get("defaultVal"));

        // Remove the default configuration, verify that the config goes away
        getHelper().removeDefaultConfiguration(factoryPid);
        configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertNull(configs);

    }

    /**
     * Add a default factory config for a pid that did exist. Values should be merged
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration3() throws Exception {
        String factoryPid = "com.ibm.example.topLevelElement";

        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("id", "top");
        dictionary.put("drink", "coffee");

        getHelper().addDefaultConfiguration(factoryPid, dictionary);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertEquals(1, configs.length);
        Dictionary<String, Object> props = configs[0].getProperties();
        assertEquals("top", props.get("id"));
        // From new default config
        assertEquals("coffee", props.get("drink"));
        // From server.xml
        assertEquals("scone", props.get("food"));
        // From metatype defaults, both instances
        assertEquals("value", props.get("attribute"));

        // Remove the default configuration, see that the config has returned to the values in server.xml
        getHelper().removeDefaultConfiguration(factoryPid);

        configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertEquals(1, configs.length);
        props = configs[0].getProperties();
        assertEquals("top", props.get("id"));
        // From new default config
        assertNull(props.get("drink"));
        // From server.xml
        assertEquals("scone", props.get("food"));
        // From metatype defaults, both instances
        assertEquals("value", props.get("attribute"));
    }

    /**
     * Add two default configurations, same pid, distinct ids
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration4() throws Exception {
        String factoryPid = "com.ibm.example.topLevelElement";

        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("id", "top");
        dictionary.put("drink", "coffee");

        WSConfigurationHelper helper = getHelper();
        helper.addDefaultConfiguration(factoryPid, dictionary);
        dictionary.put("id", "top2");
        dictionary.put("drink", "tea");
        helper.addDefaultConfiguration(factoryPid, dictionary);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertEquals(2, configs.length);

        for (Configuration config : configs) {
            Dictionary<String, Object> props = config.getProperties();
            String id = (String) props.get("id");
            if ("top".equals(id)) {
                // From new default config
                assertEquals("coffee", props.get("drink"));
                // From server.xml
                assertEquals("scone", props.get("food"));
                // From metatype defaults, both instances
                assertEquals("value", props.get("attribute"));
            } else if ("top2".equals(id)) {
                assertEquals("tea", props.get("drink"));
                assertNull(props.get("food"));
                assertEquals("value", props.get("attribute"));
            } else {
                fail("Unrecognized id: " + id);
            }
        }

        // Remove the default configuration, see that the config has returned to the values in server.xml
        getHelper().removeDefaultConfiguration(factoryPid);

        configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertEquals(1, configs.length);
        Dictionary<String, Object> props = configs[0].getProperties();
        assertEquals("top", props.get("id"));
        // From new default config
        assertNull(props.get("drink"));
        // From server.xml
        assertEquals("scone", props.get("food"));
        // From metatype defaults, both instances
        assertEquals("value", props.get("attribute"));
    }

    /**
     * Add factory pid with default id and ibm:extends
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration5() throws Exception {
        String factoryPid = "com.ibm.example.child.a";
        String superPid = "com.ibm.example.supertype";
        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("attrA1", "goodbye");

        getHelper().addDefaultConfiguration(factoryPid, dictionary);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertEquals(1, configs.length);
        Dictionary<String, Object> props = configs[0].getProperties();
        assertEquals("goodbye", props.get("attrA1"));
        assertEquals("value2", props.get("attrA2"));

        configs = ca.listConfigurations(getFilter(superPid, true));
        assertEquals(1, configs.length);
        props = configs[0].getProperties();
        assertEquals("goodbye", props.get("attrA1"));
        assertEquals("value2", props.get("attrA2"));

        // Remove the default configuration, verify that the config goes away
        getHelper().removeDefaultConfiguration(factoryPid);
        configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertNull(configs);
        configs = ca.listConfigurations(getFilter(superPid, true));
        assertNull(configs);

    }

    /**
     * Add default configuration for a pid that doesn't have metatype. It shouldn't exist in
     * config admin. Then remove the default configuration and verify that it still isn't there.
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration6() throws Exception {
        String factoryPid = "nonMetatype";
        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        dictionary.put("attrA1", "goodbye");

        getHelper().addDefaultConfiguration(factoryPid, dictionary);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertNull("The non-metatype configuration should not exist", configs);

        // Remove the default configuration, verify that the config still doesn't exist
        getHelper().removeDefaultConfiguration(factoryPid);
        configs = ca.listConfigurations(getFilter(factoryPid, true));
        assertNull(configs);
    }

    /**
     * Add a default factory nested config for a parent that did exist. Values should be merged
     * 
     * Using metatype from metatype-nested-merge.xml
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration7() throws Exception {
        String parentPid = "test.nestedmerge.parent.ONE";
        String childPid = "test.nestedmerge.child";

        String xml = "<server>" +
                     "<test.nestedmerge.parent.ONE id=\"one\"> " +
                     "<child id=\"child\" someAttr=\"coffee\"/>" +
                     "</test.nestedmerge.parent.ONE>" +
                     "</server>";

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        getHelper().addDefaultConfiguration(bais);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(childPid, true));
        assertNotNull("The child configuration should exist", configs);
        assertEquals(1, configs.length);
        Dictionary<String, Object> props = configs[0].getProperties();
        assertEquals("child", props.get("id"));
        // From new default config
        assertEquals("coffee", props.get("someAttr"));

        // Remove the default configuration, see that the config has returned to the values in server.xml

        getHelper().removeDefaultConfiguration(parentPid);

        configs = ca.listConfigurations(getFilter(parentPid, true));
        assertEquals(1, configs.length);
        props = configs[0].getProperties();
        assertEquals("one", props.get("id"));

        assertNull("The child element should not exist as a property on the parent", props.get("child"));

        configs = ca.listConfigurations(getFilter(childPid, true));
        assertNull("The child element should not exist", configs);
    }

    /**
     * Add a default factory nested config for a parent that doesn't exist with the same id. An instance of the
     * parent and child should be added by addDefaultConfiguration, and both should be removed by remove
     * 
     * Using metatype from metatype-nested-merge.xml
     * 
     * @throws Exception
     */
    public void testAddDefaultConfiguration8() throws Exception {
        String parentPid = "test.nestedmerge.parent.ONE";
        String childPid = "test.nestedmerge.child";

        String xml = "<server>" +
                     "<test.nestedmerge.parent.ONE id=\"two\"> " +
                     "<child id=\"child\" someAttr=\"coffee\"/>" +
                     "</test.nestedmerge.parent.ONE>" +
                     "</server>";

        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());

        getHelper().addDefaultConfiguration(bais);

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] configs = ca.listConfigurations(getFilter(parentPid, true));
        assertNotNull("the parent configurations should exist", configs);
        assertEquals(2, configs.length);

        // Get the properties for the parent with id "two"
        Dictionary<String, Object> props = configs[0].getProperties();
        if ("one".equals(props.get("id")))
            props = configs[1].getProperties();
        assertEquals("two", props.get("id"));
        String childServicePid = (String) props.get("child");
        assertNotNull("The child should exist as a property on the parent", childServicePid);

        // Get the child configuration
        configs = ca.listConfigurations(getFilter(childPid, true));
        assertNotNull("The child configuration should exist", configs);
        assertEquals(1, configs.length);
        props = configs[0].getProperties();
        assertEquals("child", props.get("id"));
        // From new default config
        assertEquals("coffee", props.get("someAttr"));
        assertEquals(childServicePid, props.get("service.pid"));

        // Remove the default configuration, see that the config has returned to the values in server.xml

        getHelper().removeDefaultConfiguration(parentPid);

        configs = ca.listConfigurations(getFilter(parentPid, true));
        assertEquals(1, configs.length);
        props = configs[0].getProperties();
        assertEquals("one", props.get("id"));

        assertNull("The child element should not exist as a property on the parent", props.get("child"));

        configs = ca.listConfigurations(getFilter(childPid, true));
        assertNull("The child element should not exist", configs);
    }

    private String getFilter(String pid, boolean isFactory) {
        if (isFactory) {
            return "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + pid + ")";
        } else {
            return "(" + Constants.SERVICE_PID + "=" + pid + ")";
        }
    }
}
