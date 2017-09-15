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
import static org.junit.Assert.assertNull;
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
public class MergedConfigTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 4084916808353306371L;
    private final ArrayList<ServiceReference<?>> references = new ArrayList<ServiceReference<?>>();
    private BundleContext bundleContext;

    public static final String PARENT_PID = "com.ibm.example.topLevelElement";
    public static final String CHILD_A_PID = "com.ibm.example.child.a";
    public static final String CHILD_B_PID = "com.ibm.example.child.b";
    public static final String CHILD_C_PID = "com.ibm.example.child.c";
    public static final String PARENT_D_PID = "com.ibm.example.top.d";
    private static final String DEFAULT_PID = "com.ibm.example.default";
    private static final String DEFAULT_MISSING_PID = "com.ibm.example.default.missing";

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

    private enum Behavior {
        MERGE, REPLACE, IGNORE
    }

    private void verify(Behavior behavior) throws Exception {
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(PARENT_PID, true);
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PARENT_PID + " should exist", parents);

        assertEquals("There should be four instances of PID " + PARENT_PID, 4, parents.length);

        for (Configuration parent : parents) {
            Dictionary<String, Object> properties = parent.getProperties();
            String id = (String) properties.get("id");
            if ("a".equals(id)) {
                String[] pids = (String[]) properties.get("child.a");
                assertNotNull("The child pids should exist", pids);
                assertEquals("There should be one child pid", 1, pids.length);
                Configuration child = ca.getConfiguration(pids[0]);
                assertNotNull("The child should exist", child);
                Dictionary<String, Object> childProps = child.getProperties();
                if (behavior == Behavior.MERGE || behavior == Behavior.REPLACE) {
                    assertEquals("a1", childProps.get("attrA1"));
                } else {
                    assertNull(childProps.get("attrA1"));
                }
                if (behavior == Behavior.MERGE || behavior == Behavior.IGNORE) {
                    assertEquals("a2", childProps.get("attrA2"));
                } else {
                    // We replaced the top level (attrA2=a2) with the include (attr1=a1), but attrA2 has a default value.
                    assertEquals("value2", childProps.get("attrA2"));
                }
            } else if ("b".equals(id)) {
                String[] pids = (String[]) properties.get("child.b");
                assertNotNull("the child pids should exist", pids);
                assertEquals("there should be one child pid", 1, pids.length);
                Configuration child = ca.getConfiguration(pids[0]);
                assertNotNull("The child should exist", child);
                Dictionary<String, Object> childProps = child.getProperties();
                assertEquals("b1", childProps.get("attrB1"));
                assertEquals("b2", childProps.get("attrB2"));
            } else if ("c".equals(id) || "common".equals(id)) {
            } else {
                fail("Something went horribly wrong.");
            }
        }
    }

    public void testMergedConfig() throws Exception {
        // All config in one file, verify it's merged correctly. If this isn't working, fix this first, because
        // whatever is broken here would break the other tests. 
        verify(Behavior.MERGE);
    }

    public void testMergedIncludesMerge() throws Exception {
        // Same set of assumptions as the all in one case
        verify(Behavior.MERGE);
    }

    public void testMergedIncludesBreak() throws Exception {
        // The config doesn't get updated, so verify we're still using the all in one config
        verify(Behavior.MERGE);
    }

    public void testMergedIncludesBreak2() throws Exception {
        // Test is arranged so that the assumptions here are the same as for merge.. What would normally be
        // merged in is specified in the top level file. The included file has the non conflicting elements, which
        // should all get added despite onConflict="break"
        verify(Behavior.MERGE);
    }

    public void testMergedIncludesReplace() throws Exception {
        // Verify that the attribute in the top level file gets replaced by the include
        verify(Behavior.REPLACE);
    }

    public void testMergedIncludesIgnore() throws Exception {
        // Verify that the attribute in the included file gets ignored
        verify(Behavior.IGNORE);

    }

    public void testMergedIncludesIgnoreReplace() throws Exception {
        // Verify that none of the level 3 (REPLACE) or level 2 (IGNORE) elements are used
        verify(Behavior.IGNORE);
    }

    public void testMergedIncludesFourLevelReplace() throws Exception {
        // Conflict is between level 2 (fourLevelB.xml) and level 4 (conflict.xml). Verify that the "replace" behavior from
        // level 2 is used instead of the "ignore" behavior from level 3. 
        verify(Behavior.REPLACE);
    }

    public void testMergedIncludesFourLevelIgnore() throws Exception {
        // Conflict is between level 2 (fourLevelB2.xml) and level 4 (conflict.xml). Verify that the "ignore" behavior from 
        // level 2 is used instead of the "replace" behavior from level 3. 
        verify(Behavior.IGNORE);
    }

    public void testDefaultInstances1() throws Exception {
        // Check to make sure a default instance marked requiresExisting=true gets merged into an existing
        // configuration
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = "(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + PARENT_PID + ")(id=common))";
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + PARENT_PID + "with the ID 'common' should exist", parents);
        assertEquals("There should be one instance " + PARENT_PID, 1, parents.length);

        Dictionary<String, Object> properties = parents[0].getProperties();
        String[] pids = (String[]) properties.get("child.a");
        assertNotNull("the child pid should exist", pids);
        assertEquals("there should be one child pid", 1, pids.length);

    }

    public void testDefaultInstances2() throws Exception {
        // Check to make sure that a default instance without a requiresExisting attribute gets merged in and that one
        // with requiresExisting=true does not.

        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(DEFAULT_PID, true);
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + DEFAULT_PID + " should exist", parents);
        assertEquals("There should be one instance of PID " + DEFAULT_PID, 1, parents.length);

        Dictionary<String, Object> properties = parents[0].getProperties();
        String id = (String) properties.get("id");
        assertEquals("The ID should be 'two'", "two", id);

    }

    public void testDefaultInstances3() throws Exception {
        // Check to make sure a default instance marked addIfMissing=true gets added when an existing
        // configuration is not specified. Also checks that an instance does not get added when there is an existing configuration.
        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(DEFAULT_MISSING_PID, true);
        Configuration[] parents = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + DEFAULT_MISSING_PID + " should exist", parents);
        assertEquals("There should be two instances of PID " + DEFAULT_MISSING_PID, 2, parents.length);

        for (Configuration config : parents) {
            Dictionary<String, Object> properties = config.getProperties();
            String id = (String) properties.get("id");
            String someProperty = (String) properties.get("someProperty");
            if ("one".equals(id)) {
                assertNull("someProperty should be null because the default instance should not be merged", someProperty);
            } else if ("two".equals(id)) {
                assertEquals("someProperty should be defined because the default instance should be merged", someProperty, "from default instances");
            } else {
                fail("Something went horribly wrong -- default instance with ID " + id + " found when 'one' or 'two' was expected");
            }
        }

    }

}
