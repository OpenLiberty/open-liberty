/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.config.merged;

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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.config.variables.ServerXMLVariables;

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

    private static final String EVALUATED = "evaluated";
    /**  */
    private static final String VARIABLE_DELAY_TEST = "variableDelayTest";
    /**  */
    private static final long serialVersionUID = 6784264217930725272L;
    private static final String DELAYED_VAR_PID = "com.ibm.ws.config.variable.delay";

    private static final String DELAYED_VAR_ATTR = "delayedVariable";
    private static final String DELAYED_IBM_VAR_ATTR = "delayedIBMVar";
    private static final String IMMEDIATE_VAR_ATTR = "immediateVar";
    private static final String IMMEDIATE_VAR_TWO = "immediateVarTwo";

    private static final String VARIABLE_STRING = "${variableDelayTest}";
    private static final String EVALUATED_VARIABLE = EVALUATED;

    private static final String ENV_VAR_ATTR = "envVar";
    private static final String ALL_CAPS_ENV_VAR_ATTR = "allCapsEnvVar";
    private static final String MANGLED_ENV_VAR_ATTR = "mangledEnvVar";
    private static final String MANGLED_CAPS_ENV_VAR_ATTR = "mangledCapsEnvVar";
    private static final String DEFAULT_VAR_ATTR = "defaultVar";
    private static final String META_REF_VAR_ATTR = "metaRefVar";
    private static final String METATYPE_VAR_ATTR = "shouldBeMetatype";

    private static final String DEFAULT_VALUE = "this is the default";
    private static final String MANGLED_VALUE = "mangled";
    private static final String MANGLED_CAPS_VALUE = "mangledCaps";
    private static final String CAP_VALUE = "caps";
    private static final String ENV_VALUE = "envValue";
    private static final String METATYPE_VALUE = "fromConfig";

    private static final String DEFAULT_INSTANCE_VALUE = "default instance value";

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

        assertEquals(ENV_VALUE, properties.get(ENV_VAR_ATTR));
        assertEquals(CAP_VALUE, properties.get(ALL_CAPS_ENV_VAR_ATTR));
        assertEquals(MANGLED_VALUE, properties.get(MANGLED_ENV_VAR_ATTR));
        assertEquals(MANGLED_CAPS_VALUE, properties.get(MANGLED_CAPS_ENV_VAR_ATTR));
        assertEquals(DEFAULT_VALUE, properties.get(DEFAULT_VAR_ATTR));
        assertEquals(DEFAULT_VALUE, properties.get(META_REF_VAR_ATTR));

        // Check that values from config are used before values from environment variables
        assertEquals(METATYPE_VALUE, properties.get(METATYPE_VAR_ATTR));

    }

    // Tests method to return variables defined in server.xml and defaultInstances.xml
    public void testConfigVariables() throws Exception {
        ServiceReference<ServerXMLVariables> ref = bundleContext.getServiceReference(ServerXMLVariables.class);
        assertNotNull("No config variable component", ref);
        references.add(ref);
        ServerXMLVariables cvc = bundleContext.getService(ref);
        Map<String, String> vars = cvc.getServerXMLVariables();
        // Check server.xml variable is present
        assertEquals(vars.get(VARIABLE_DELAY_TEST), EVALUATED);
        // Check that bootstrap var is not
        assertNull(vars.get("com.ibm.ws.logging.trace.specification"));

        // Check that variable with default value is not
        assertNull(vars.get("variableDefaultTest"));

        // Check that defaultInstance variable is available
        assertEquals(vars.get("defaultInstanceWithValue"), "value");

        Map<String, String> defaultVars = cvc.getServerXMLVariableDefaultValues();
        assertEquals("this is the default", defaultVars.get("variableDefaultTest"));
        assertEquals("default", defaultVars.get("defaultInstanceNoValue"));

        // Check server.xml variable is not present
        assertNull(defaultVars.get(VARIABLE_DELAY_TEST));
        // Check that bootstrap var is not present
        assertNull(defaultVars.get("com.ibm.ws.logging.trace.specification"));
        // Check that defaultInstance value is not present
        assertNull(defaultVars.get("defaultInstanceWithValue"));
    }


    // Tests that variable values are correct after removing the definitions from server.xml
    public void testUpdateConfigVariables() throws Exception {
        ServiceReference<ServerXMLVariables> ref = bundleContext.getServiceReference(ServerXMLVariables.class);
        assertNotNull("No config variable component", ref);
        references.add(ref);       
       
        ServerXMLVariables cvc = bundleContext.getService(ref);
        Map<String, String> vars = cvc.getServerXMLVariables();
        // Check server.xml variable is present
        assertEquals(DEFAULT_INSTANCE_VALUE, vars.get(VARIABLE_DELAY_TEST));

        // Check that bootstrap var is not
        assertNull(vars.get("com.ibm.ws.logging.trace.specification"));

        // Check that variable with default value is not
        assertNull(vars.get("variableDefaultTest"));

        // Check that defaultInstance variable is available
        assertEquals("value", vars.get("defaultInstanceWithValue"));

        Map<String, String> defaultVars = cvc.getServerXMLVariableDefaultValues();
        assertEquals(DEFAULT_INSTANCE_VALUE, defaultVars.get("variableDefaultTest"));

        assertEquals("default", defaultVars.get("defaultInstanceNoValue"));

        // Check server.xml variable is not present
        assertNull(defaultVars.get(VARIABLE_DELAY_TEST));
        // Check that bootstrap var is not present
        assertNull(defaultVars.get("com.ibm.ws.logging.trace.specification"));
        // Check that defaultInstance value is not present
        assertNull(defaultVars.get("defaultInstanceWithValue"));


        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(DELAYED_VAR_PID, true);
        Configuration[] varDelay = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + DELAYED_VAR_PID + " should exist", varDelay);

        assertEquals("There should be one instance of PID " + DELAYED_VAR_PID, 1, varDelay.length);

        Dictionary<String, Object> properties = varDelay[0].getProperties();
        // Should be taken from the defaultValue in defaultInstances because it was removed from server.xml
        assertEquals(DEFAULT_INSTANCE_VALUE, properties.get("defaultTest"));
        // Should resolve to the value from defaultInstances
        assertEquals("value", properties.get("defaultInstanceWithValueTest"));
        // Should be taken from the value in defaultInstances because it was removed from server.xml
        assertEquals(DEFAULT_INSTANCE_VALUE, properties.get("immediateVar"));

        assertEquals("default", properties.get("defaultInstanceNoValueTest"));
    }

      // Tests that variable values are correct after re-adding the definitions from server.xml
      public void testUpdateConfigVariablesTwo() throws Exception {
        ServiceReference<ServerXMLVariables> ref = bundleContext.getServiceReference(ServerXMLVariables.class);
        assertNotNull("No config variable component", ref);
        references.add(ref);       
       
        ServerXMLVariables cvc = bundleContext.getService(ref);
        Map<String, String> vars = cvc.getServerXMLVariables();
        // Check server.xml variable is present
        assertEquals(EVALUATED, vars.get(VARIABLE_DELAY_TEST));

        // Check that bootstrap var is not
        assertNull(vars.get("com.ibm.ws.logging.trace.specification"));

        // Check that variable with default value is not
        assertNull(vars.get("variableDefaultTest"));

        // Check that defaultInstance variable is available
        assertEquals("value", vars.get("defaultInstanceWithValue"));

        Map<String, String> defaultVars = cvc.getServerXMLVariableDefaultValues();
        assertEquals(DEFAULT_VALUE, defaultVars.get("variableDefaultTest"));

        assertEquals("default", defaultVars.get("defaultInstanceNoValue"));

        // Check server.xml variable is not present
        assertNull(defaultVars.get(VARIABLE_DELAY_TEST));
        // Check that bootstrap var is not present
        assertNull(defaultVars.get("com.ibm.ws.logging.trace.specification"));
        // Check that defaultInstance value is not present
        assertNull(defaultVars.get("defaultInstanceWithValue"));


        ConfigurationAdmin ca = getConfigurationAdmin(bundleContext, references);
        String filter = getFilter(DELAYED_VAR_PID, true);
        Configuration[] varDelay = ca.listConfigurations(filter);
        assertNotNull("The configuration for " + DELAYED_VAR_PID + " should exist", varDelay);

        assertEquals("There should be one instance of PID " + DELAYED_VAR_PID, 1, varDelay.length);

        Dictionary<String, Object> properties = varDelay[0].getProperties();
        // Should be taken from the defaultValue in server.xml because it was re-added 
        assertEquals(DEFAULT_VALUE, properties.get("defaultTest"));
        // Should resolve to the value from defaultInstances
        assertEquals("value", properties.get("defaultInstanceWithValueTest"));
        // Should be taken from the value in server.xml because it was re-added
        assertEquals("evaluated", properties.get("immediateVar"));

        assertEquals("default", properties.get("defaultInstanceNoValueTest"));
    }

}
