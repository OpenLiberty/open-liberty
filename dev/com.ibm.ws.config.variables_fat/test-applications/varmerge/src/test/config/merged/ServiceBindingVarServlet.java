/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.config.variables.ServerXMLVariables;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.LibertyVariable.Source;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 *
 */
public class ServiceBindingVarServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 7317679036855619912L;
    private final ArrayList<ServiceReference<?>> references = new ArrayList<ServiceReference<?>>();
    private BundleContext bundleContext;
    private PrintWriter writer;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        this.writer = response.getWriter();

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

    private Collection<LibertyVariable> getVariables() {
        ServiceReference<ServerXMLVariables> ref = bundleContext.getServiceReference(ServerXMLVariables.class);
        assertNotNull("No config variable component", ref);
        references.add(ref);
        ServerXMLVariables cvc = bundleContext.getService(ref);
        return cvc.getLibertyVariables();
    }

    private VariableRegistry getRegistry() {
        ServiceReference<VariableRegistry> ref = bundleContext.getServiceReference(VariableRegistry.class);
        assertNotNull("No variable registry found", ref);
        references.add(ref);
        return bundleContext.getService(ref);
    }

    public void debug() {
        Collection<LibertyVariable> vars = getVariables();
        for (LibertyVariable var : vars) {
            writer.println(var.toString());
        }
    }

    public void testNoVariables() {
        Collection<LibertyVariable> vars = getVariables();
        for (LibertyVariable var : vars) {
            assertFalse("No variables should come from the file system", var.getSource() == Source.SERVICE_BINDING);
        }

    }

    private static final String SIMPLE_VAR_NAME = "simple";
    private static final String SIMPLE_VAR_VALUE = "valueFromSimpleFile";

    public void testSimpleFileVariable() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                assertTrue("The only variable should be named 'simple'", SIMPLE_VAR_NAME.equals(var.getName()));
                assertTrue("The variable value should be valueFromSimpleFile", SIMPLE_VAR_VALUE.equals(var.getValue()));
            }
        }
        assertEquals("There should be exactly one service binding variable", 1, serviceBindingVars);

        VariableRegistry registry = getRegistry();
        assertEquals("${simple} should be resolved", SIMPLE_VAR_VALUE, registry.resolveRawString("${" + SIMPLE_VAR_NAME + "}"));

    }

    private static final String ACCOUNT_DB_USER = "account_db/username";
    private static final String ACCOUNT_DB_USER_VALUE = "wopr";
    private static final String ACCOUNT_DB_PASSWORD = "account_db/password";
    private static final String ACCOUNT_DB_PASSWORD_VALUE = "joshua";

    public void testSimpleVariablesWithDirectoryPrefix() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ACCOUNT_DB_USER_VALUE, ACCOUNT_DB_USER_VALUE, var.getValue());
                } else if (ACCOUNT_DB_PASSWORD.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_PASSWORD + " should be " + ACCOUNT_DB_PASSWORD_VALUE, ACCOUNT_DB_PASSWORD_VALUE, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly two service binding variables", 2, serviceBindingVars);
    }

    private static final String BINARY_FILE_NAME = "test.config.variables.jar";

    public void testBinaryFile() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (BINARY_FILE_NAME.equals(var.getName())) {
                    assertNull("The value for " + BINARY_FILE_NAME + " should be null", var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly one service binding variables", 1, serviceBindingVars);
    }

    public void testAlternateBindingsDirectory() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                assertTrue("The only variable should be named 'simple'", SIMPLE_VAR_NAME.equals(var.getName()));
                assertTrue("The variable value should be valueFromSimpleFile", SIMPLE_VAR_VALUE.equals(var.getValue()));
            }
        }
        assertEquals("There should be exactly one service binding variable", 1, serviceBindingVars);
    }

    private static final String VAR1 = "conflicts/var1";
    private static final String VAR1_SBV_VALUE = "fromServiceBinding";
    private static final String VAR1_XML_VALUE = "fromXML";
    private static final String VAR2 = "conflicts/var2";
    private static final String VAR2_SBV_VALUE = "fromServiceBinding";

    public void testServerXMLOverrides() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (VAR1.equals(var.getName())) {
                    assertEquals("The value for " + VAR1 + " should be " + VAR1_SBV_VALUE, VAR1_SBV_VALUE, var.getValue());
                } else if (VAR2.equals(var.getName())) {
                    assertEquals("The value for " + VAR2 + " should be " + VAR2_SBV_VALUE, VAR2_SBV_VALUE, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            } else if (var.getSource() == Source.XML_CONFIG) {
                if (VAR1.equals(var.getName())) {
                    assertEquals("The value for " + VAR1 + " should be " + VAR1_XML_VALUE, VAR1_XML_VALUE, var.getValue());
                }
            }
        }
        assertEquals("There should be two service binding variables", 2, serviceBindingVars);

        VariableRegistry registry = getRegistry();
        assertEquals("The value from server XML should be used", VAR1_XML_VALUE, registry.resolveRawString("${" + VAR1 + "}"));
        assertEquals("The value from service binding vars should be used", VAR2_SBV_VALUE, registry.resolveRawString("${" + VAR2 + "}"));
    }

    private static final String EMPTY_VAR = "empty";

    public void testEmptyFile() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (EMPTY_VAR.equals(var.getName())) {
                    assertNull("The value for " + EMPTY_VAR + " should be null", var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }
        }
        assertEquals("There should be one service binding variable", 1, serviceBindingVars);
    }

    private static final String ORIGINAL_VALUE = "original";

    public void testMbeanUpdate() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ORIGINAL_VALUE, ORIGINAL_VALUE, var.getValue());
                } else if (ACCOUNT_DB_PASSWORD.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_PASSWORD + " should be " + ORIGINAL_VALUE, ORIGINAL_VALUE, var.getValue());
                } else if (SIMPLE_VAR_NAME.equals(var.getName())) {
                    assertEquals("The value for " + SIMPLE_VAR_NAME + " should be " + ORIGINAL_VALUE, ORIGINAL_VALUE, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly three service binding variables", 3, serviceBindingVars);
    }

    public void testMBeanUpdateAfterModify() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ACCOUNT_DB_USER_VALUE, ACCOUNT_DB_USER_VALUE, var.getValue());
                } else if (ACCOUNT_DB_PASSWORD.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_PASSWORD + " should be " + ACCOUNT_DB_PASSWORD_VALUE, ACCOUNT_DB_PASSWORD_VALUE, var.getValue());
                } else if (SIMPLE_VAR_NAME.equals(var.getName())) {
                    assertEquals("The value for " + SIMPLE_VAR_NAME + " should be " + SIMPLE_VAR_VALUE, SIMPLE_VAR_VALUE, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly three service binding variables", 3, serviceBindingVars);
    }

    private static final String ADDED_NAME = "added";

    public void testMBeanUpdateAfterAdd() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ACCOUNT_DB_USER_VALUE, ACCOUNT_DB_USER_VALUE, var.getValue());
                } else if (ACCOUNT_DB_PASSWORD.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_PASSWORD + " should be " + ACCOUNT_DB_PASSWORD_VALUE, ACCOUNT_DB_PASSWORD_VALUE, var.getValue());
                } else if (SIMPLE_VAR_NAME.equals(var.getName())) {
                    assertEquals("The value for " + SIMPLE_VAR_NAME + " should be " + SIMPLE_VAR_VALUE, SIMPLE_VAR_VALUE, var.getValue());
                } else if (ADDED_NAME.equals(var.getName())) {
                    assertEquals("The value for " + ADDED_NAME + " should be " + ADDED_NAME, ADDED_NAME, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly four service binding variables", 4, serviceBindingVars);
    }

    public void testMBeanUpdateAfterDelete() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ACCOUNT_DB_USER_VALUE, ACCOUNT_DB_USER_VALUE, var.getValue());
                } else if (ADDED_NAME.equals(var.getName())) {
                    assertEquals("The value for " + ADDED_NAME + " should be " + ADDED_NAME, ADDED_NAME, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly two service binding variables", 2, serviceBindingVars);
    }

    public void testMBeanUpdateAfterDisabled() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ACCOUNT_DB_USER_VALUE, ACCOUNT_DB_USER_VALUE, var.getValue());
                } else if (ADDED_NAME.equals(var.getName())) {
                    assertEquals("The value for " + ADDED_NAME + " should be " + ADDED_NAME, ADDED_NAME, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly two service binding variables", 2, serviceBindingVars);

    }

    public void testMBeanUpdateAfterPolling() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ACCOUNT_DB_USER_VALUE, ACCOUNT_DB_USER_VALUE, var.getValue());
                } else if (ADDED_NAME.equals(var.getName())) {
                    assertEquals("The value for " + ADDED_NAME + " should be " + ADDED_NAME, ADDED_NAME, var.getValue());
                } else if (SIMPLE_VAR_NAME.equals(var.getName())) {
                    assertEquals("The value for " + SIMPLE_VAR_NAME + " should be " + SIMPLE_VAR_VALUE, SIMPLE_VAR_VALUE, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly one service binding variable", 3, serviceBindingVars);

    }

    public void testMBeanUpdateAfterReenabling() {
        Collection<LibertyVariable> vars = getVariables();
        int serviceBindingVars = 0;
        for (LibertyVariable var : vars) {
            if (var.getSource() == Source.SERVICE_BINDING) {
                serviceBindingVars++;
                if (ACCOUNT_DB_USER.equals(var.getName())) {
                    assertEquals("The value for " + ACCOUNT_DB_USER + " should be " + ACCOUNT_DB_USER_VALUE, ACCOUNT_DB_USER_VALUE, var.getValue());
                } else if (ADDED_NAME.equals(var.getName())) {
                    assertEquals("The value for " + ADDED_NAME + " should be " + ADDED_NAME, ADDED_NAME, var.getValue());
                } else {
                    fail("Unexpected variable encountered: " + var.toString());
                }
            }

        }
        assertEquals("There should be exactly one service binding variable", 2, serviceBindingVars);
    }
}
