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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 *
 */
public class VarMergeTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 6784264217930725272L;
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

    private enum Behavior {
        MERGE, REPLACE, IGNORE
    }

    private void verify(Behavior behavior) throws Exception {

        ServiceReference<WsLocationAdmin> ref = bundleContext.getServiceReference(WsLocationAdmin.class);
        assertNotNull("No location service", ref);
        references.add(ref);
        WsLocationAdmin locationAdmin = bundleContext.getService(ref);

        String value = locationAdmin.resolveString("${var1}");
        switch (behavior) {
            case MERGE:
                assertEquals("fromInclude", value);
                break;
            case REPLACE:
                assertEquals("fromInclude", value);
                break;
            case IGNORE:
                assertEquals("fromServerXml", value);
                break;

        }

    }

    public void testMergedVariables() throws Exception {
        verify(Behavior.MERGE);
    }

    public void testMergedIncludesMerge() throws Exception {
        verify(Behavior.MERGE);
    }

    public void testMergedIncludesReplace() throws Exception {
        verify(Behavior.REPLACE);
    }

    public void testMergedIncludesIgnore() throws Exception {
        // Verify that the attribute in the included file gets ignored
        verify(Behavior.IGNORE);

    }

    public void testCommandLineVariables() throws Exception {
        ServiceReference<WsLocationAdmin> ref = bundleContext.getServiceReference(WsLocationAdmin.class);
        assertNotNull("No location service", ref);
        references.add(ref);
        WsLocationAdmin locationAdmin = bundleContext.getService(ref);

        assertEquals("CLV", locationAdmin.resolveString("${clvOnly}"));
        assertEquals("CLV", locationAdmin.resolveString("${clvOverrideBootstrap}"));
        assertEquals("CLV", locationAdmin.resolveString("${clvOverrideServerXML}"));
        assertEquals("CLV", locationAdmin.resolveString("${clvOverrideBoth}"));
        assertEquals("fromBootstrap", locationAdmin.resolveString("${bootstrapOnly}"));
        assertEquals("fromServerXML", locationAdmin.resolveString("${serverXMLOnly}"));
        assertEquals("", locationAdmin.resolveString("${clvEmpty}"));
        assertEquals("${clvInvalid}", locationAdmin.resolveString("${clvInvalid}"));
        assertEquals("${clvInvalid2}", locationAdmin.resolveString("${clvInvalid2}"));
    }
}
