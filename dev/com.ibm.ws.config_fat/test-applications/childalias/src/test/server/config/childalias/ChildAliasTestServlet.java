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
package test.server.config.childalias;

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

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 *
 */
public class ChildAliasTestServlet extends HttpServlet {

    private static final String CHILD_1_PID = "test.config.childalias.child.1";
    private static final String CHILD_2_PID = "test.config.childalias.child.2";
    private static final String CHILD_3_PID = "test.config.childalias.child.3";
    private static final String CHILD_4_PID = "test.config.childalias.child.4";
    private static final String CHILD_5_PID = "test.config.childalias.child.5";
    private static final String CHILD_6_PID = "test.config.childalias.child.6";
    private static final String PARENT_1_PID = "test.config.childalias.parent.1";
    private static final String PARENT_2_PID = "test.config.childalias.parent.2";
    private static final String PARENT_3_PID = "test.config.childalias.parent.3";
    private static final String PARENT_4_PID = "test.config.childalias.parent.4";
    private static final String PARENT_5_PID = "test.config.childalias.parent.5";
    private static final String PARENT_6_PID = "test.config.childalias.parent.6";
    private static final String TOP_LEVEL_PID = "test.config.childalias.toplevel";

    private final ArrayList<ServiceReference> references = new ArrayList<ServiceReference>();
    private BundleContext bundleContext;

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

    public void testChildAlias1() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(CHILD_1_PID, true);
        Configuration[] children1 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + CHILD_1_PID + " should exist", children1);
        assertEquals("There should only be one instance of PID " + CHILD_1_PID, 1, children1.length);

        Configuration child1 = children1[0];
        Dictionary<String, Object> childProperties = child1.getProperties();
        assertEquals("The attribute value should be the correct metatype translated value",
                     "defaultValue", childProperties.get("defaultAttribute"));

        filter = getFilter(PARENT_1_PID, true);
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PARENT_1_PID + " should exist", parents);
        assertEquals("There should be only one instance of PID " + PARENT_1_PID, 1, parents.length);
        Configuration parent = parents[0];
        Dictionary<String, Object> parentProperties = parent.getProperties();
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parentProperties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     childProperties.get(XMLConfigConstants.CFG_PARENT_PID));
    }

    public void testChildAlias2() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(CHILD_2_PID, true);
        Configuration[] children2 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + CHILD_2_PID + " should exist", children2);
        assertEquals("There should only be one instance of PID " + CHILD_2_PID, 1, children2.length);

        Configuration child2 = children2[0];
        Dictionary<String, Object> child2Properties = child2.getProperties();
        assertEquals("The attribute value should be the correct metatype translated value",
                     "square", child2Properties.get("shape"));
        assertEquals("The attribute value should be the correct value from the config",
                     "orange", child2Properties.get("color"));

        filter = getFilter(PARENT_2_PID, true);
        Configuration[] parents2 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PARENT_2_PID + " should exist", parents2);
        assertEquals("There should be only one instance of PID " + PARENT_2_PID, 1, parents2.length);
        Configuration parent2 = parents2[0];
        Dictionary<String, Object> parent2Properties = parent2.getProperties();
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parent2Properties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     child2Properties.get(XMLConfigConstants.CFG_PARENT_PID));
    }

    public void testChildAliasSingleton1() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = "(" + Constants.SERVICE_PID + "=" + CHILD_3_PID + ")";
        Configuration[] children3 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + CHILD_3_PID + " should exist", children3);
        assertEquals("There should only be one instance of PID " + CHILD_3_PID, 1, children3.length);

        Configuration child3 = children3[0];
        Dictionary<String, Object> childProperties = child3.getProperties();
        assertEquals("The attribute value should be the correct metatype translated value",
                     "coconut", childProperties.get("defaultAttribute"));
        assertNull("There should be no contamination from other singletons", childProperties.get("state"));

        filter = "(" + Constants.SERVICE_PID + "=" + PARENT_3_PID + ")";
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PARENT_3_PID + " should exist", parents);
        assertEquals("There should be only one instance of PID " + PARENT_3_PID, 1, parents.length);
        Configuration parent = parents[0];
        Dictionary<String, Object> parentProperties = parent.getProperties();
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parentProperties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     childProperties.get(XMLConfigConstants.CFG_PARENT_PID));
    }

    public void testChildAliasSingleton2() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = "(" + Constants.SERVICE_PID + "=" + CHILD_4_PID + ")";
        Configuration[] children4 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + CHILD_4_PID + " should exist", children4);
        assertEquals("There should only be one instance of PID " + CHILD_4_PID, 1, children4.length);

        Configuration child4 = children4[0];
        Dictionary<String, Object> childProperties = child4.getProperties();
        assertEquals("The attribute value should be the correct metatype translated value",
                     "washington", childProperties.get("state"));
        assertNull("There should be no contamination from other singletons", childProperties.get("defaultAttribute"));

        filter = "(" + Constants.SERVICE_PID + "=" + PARENT_4_PID + ")";
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PARENT_4_PID + " should exist", parents);
        assertEquals("There should be only one instance of PID " + PARENT_4_PID, 1, parents.length);
        Configuration parent = parents[0];
        Dictionary<String, Object> parentProperties = parent.getProperties();
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parentProperties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     childProperties.get(XMLConfigConstants.CFG_PARENT_PID));
    }

    /**
     * Simulate bundle ordering issues by loading a new feature that forces a new bundle to load.
     * 
     * Bundle B has a parent with an unloaded child and a child with an unloaded parent. At this point,
     * nothing much will be resolved, but we can verify that there is no contamination with other child
     * elements with the same childAlias.
     * 
     * @throws Exception
     */
    public void testBundleOrdering1() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(CHILD_5_PID, true);
        Configuration[] children5 = ca.listConfigurations(filter);
        assertNull("The configuration for " + CHILD_5_PID + " should not exist", children5);

        filter = getFilter(PARENT_5_PID, true);
        Configuration[] parents = ca.listConfigurations(filter);
        assertNull("The configuration for " + PARENT_5_PID + " should not exist", parents);

        Configuration[] children6 = ca.listConfigurations(getFilter(CHILD_6_PID, true));
        assertNull("The configuration for " + CHILD_6_PID + " should not exist", children6);

        dump(new PrintWriter(System.out));

        Configuration[] parents6 = ca.listConfigurations(getFilter(PARENT_6_PID, true));
        assertNotNull("The configuration for " + PARENT_6_PID + " should exist", parents6);
        assertEquals("There should be only one instance of PID " + PARENT_6_PID, 1, parents6.length);
        Configuration parent = parents6[0];
        Dictionary<String, Object> parentProperties = parent.getProperties();
        String[] child6Pids = (String[]) parentProperties.get("testCAChild");
        assertNotNull(PARENT_6_PID + " should have a child element testCAChild", child6Pids);
        assertEquals("There should be 1 child pid", 1, child6Pids.length);
        Configuration child6Config = ca.getConfiguration(child6Pids[0]);
        assertNotNull("The child for " + PARENT_6_PID + " should be available in the configuration", child6Config);
        Dictionary<String, Object> child6Props = child6Config.getProperties();
        assertEquals("The child should not be metatype processed", "Not Default", child6Props.get("testAttribute6"));
        assertNull("The child should not be metatype processed", child6Props.get("defaultSix"));
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parentProperties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     child6Props.get(XMLConfigConstants.CFG_PARENT_PID));
    }

    /**
     * Simulate bundle ordering issues by loading a new feature that forces a new bundle to load.
     * 
     * Bundle C has a parent with a child in Bundle B and a child with a parent in Bundle B. All
     * elements should be resolved correctly at this point.
     * 
     * @throws Exception
     */
    public void testBundleOrdering2() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(CHILD_5_PID, true);
        Configuration[] children5 = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + CHILD_5_PID + " should exist", children5);

        filter = getFilter(PARENT_5_PID, true);
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PARENT_5_PID + " should exist", parents);

        Configuration[] children6 = ca.listConfigurations(getFilter(CHILD_6_PID, true));
        assertNotNull("The configuration for " + CHILD_6_PID + " should exist", children6);

        Configuration[] parents6 = ca.listConfigurations(getFilter(PARENT_6_PID, true));
        assertNotNull("The configuration for " + PARENT_6_PID + " should exist", parents6);
        assertEquals("There should be only one instance of PID " + PARENT_6_PID, 1, parents6.length);
        Configuration parent = parents6[0];
        Dictionary<String, Object> parentProperties = parent.getProperties();
        String[] child6Pids = (String[]) parentProperties.get("testCAChild");
        assertNotNull(PARENT_6_PID + " should have a child element testCAChild", child6Pids);
        assertEquals("There should be only one child pid", 1, child6Pids.length);
        Configuration child6Config = ca.getConfiguration(child6Pids[0]);
        assertNotNull("The child for " + PARENT_6_PID + " should be available in the configuration", child6Config);
        Dictionary<String, Object> child6Props = child6Config.getProperties();
        assertEquals("The child should have correct properties", "Not Default", child6Props.get("testAttribute6"));
        assertEquals("The child should be metatype processed", "defaultValueSix", child6Props.get("defaultSix"));
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parentProperties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     child6Props.get(XMLConfigConstants.CFG_PARENT_PID));
    }

    /**
     * Same scenario as testBundleOrdering2, but asserting that there are no contamination issues between the
     * top level element and the child alias elements.
     * 
     * @throws Exception
     */
    public void testBundleOrderingAliasConflict() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(TOP_LEVEL_PID, true);
        Configuration[] topLevel = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + TOP_LEVEL_PID + " should exist", topLevel);
        assertEquals("There should be only one instance of PID " + TOP_LEVEL_PID, 1, topLevel.length);
        Configuration topLevelConfig = topLevel[0];
        Dictionary<String, Object> topLevelProperties = topLevelConfig.getProperties();

        assertEquals("The element should have correct properties", "topLevel", topLevelProperties.get("location"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("testAttribute6"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("defaultSix"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("country"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("state"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("defaultAttribute"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("testAttribute3"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("color"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("shape"));
        assertNull("The element should not have properties from different OCDs", topLevelProperties.get("testAttribute1"));

        assertNull("The parent pid should be null",
                   topLevelProperties.get(XMLConfigConstants.CFG_PARENT_PID));
    }

    public void testRemoveChild() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] parents6 = ca.listConfigurations(getFilter(PARENT_6_PID, true));
        assertNull(PARENT_6_PID + " should be removed", parents6);

        Configuration[] children6 = ca.listConfigurations(getFilter(CHILD_6_PID, true));
        assertNull(CHILD_6_PID + " should be removed", children6);
    }

    public void testAddNewChild() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] parents6 = ca.listConfigurations(getFilter(PARENT_6_PID, true));
        assertNotNull(PARENT_6_PID + " should be added", parents6);
        assertEquals("There should be one parent", 1, parents6.length);
        Configuration parent = parents6[0];
        Dictionary<String, Object> parentProperties = parent.getProperties();
        String[] child6Pids = (String[]) parentProperties.get("testCAChild");
        assertNotNull(PARENT_6_PID + " should have a child element testCAChild", child6Pids);
        assertEquals("There should be only one child pid", 1, child6Pids.length);
        Configuration child6Config = ca.getConfiguration(child6Pids[0]);
        assertNotNull("The child for " + PARENT_6_PID + " should be available in the configuration", child6Config);
        Dictionary<String, Object> child6Props = child6Config.getProperties();
        assertEquals("The child should have correct properties", "New Child", child6Props.get("testAttribute6"));
        assertEquals("The child should be metatype processed", "defaultValueSix", child6Props.get("defaultSix"));
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parentProperties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     child6Props.get(XMLConfigConstants.CFG_PARENT_PID));

    }

    public void testUpdateChild() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] children6 = ca.listConfigurations(getFilter(CHILD_6_PID, true));
        assertNotNull("The child for " + PARENT_6_PID + " should be available in the configuration", children6);
        assertEquals("There should be only one child pid", 1, children6.length);
        Configuration child6Config = children6[0];
        Dictionary<String, Object> child6Props = child6Config.getProperties();
        assertEquals("The child should have correct properties", "Updated Child", child6Props.get("testAttribute6"));
        assertEquals("The child should be metatype processed", "defaultValueSix", child6Props.get("defaultSix"));
    }

    public void testRemoveSingletonChild() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] parents3 = ca.listConfigurations(getFilter(PARENT_3_PID, false));
        assertNotNull(PARENT_3_PID + " should exist", parents3);
        assertEquals("There should be one parent", 1, parents3.length);

        Configuration[] children3 = ca.listConfigurations(getFilter(CHILD_3_PID, false));
        // CHILD_3 has defaults. Instead of a remove taking place, the defaults will be used
        assertNotNull(CHILD_3_PID + " should not be removed", children3);
        assertEquals("There should be one child", 1, children3.length);
        Configuration child3 = children3[0];
        Dictionary<String, Object> properties = child3.getProperties();
        assertEquals("Default values should be used", "coconut", properties.get("defaultAttribute"));
        assertEquals("Default values should be used", "Attribute 3", properties.get("testAttribute3"));
    }

    public void testAddNewSingletonChild() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] parents3 = ca.listConfigurations(getFilter(PARENT_3_PID, false));
        assertNotNull(PARENT_3_PID + " should exist", parents3);
        assertEquals("There should be one parent", 1, parents3.length);
        Configuration parent = parents3[0];
        Dictionary<String, Object> parentProperties = parent.getProperties();
        String[] child3Pids = (String[]) parentProperties.get("testCAChild");
        assertNotNull(PARENT_3_PID + " should have a child element testCAChild", child3Pids);
        assertEquals("There should be only one child pid", 1, child3Pids.length);
        Configuration child3Config = ca.getConfiguration(child3Pids[0]);
        assertNotNull("The child for " + PARENT_3_PID + " should be available in the configuration", child3Config);
        Dictionary<String, Object> child3Props = child3Config.getProperties();
        assertEquals("The child should have correct properties", "New Singleton Child", child3Props.get("testAttribute3"));
        assertEquals("The child should be metatype processed", "coconut", child3Props.get("defaultAttribute"));
        assertEquals("The child's parent PID should be equal to the parent's service pid",
                     parentProperties.get(XMLConfigConstants.CFG_SERVICE_PID),
                     child3Props.get(XMLConfigConstants.CFG_PARENT_PID));

    }

    public void testUpdateSingletonChild() throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        Configuration[] children3 = ca.listConfigurations(getFilter(CHILD_3_PID, false));
        assertNotNull("The child for " + PARENT_3_PID + " should be available in the configuration", children3);
        assertEquals("There should be only one child pid", 1, children3.length);
        Configuration child3Config = children3[0];
        Dictionary<String, Object> child3Props = child3Config.getProperties();
        assertEquals("The child should have correct properties", "Updated Singleton Child", child3Props.get("testAttribute3"));
        assertNull("The child should not be contaminated with other singletons", child3Props.get("testAttribute1"));
        assertEquals("The child should be metatype processed", "coconut", child3Props.get("defaultAttribute"));
    }
}
