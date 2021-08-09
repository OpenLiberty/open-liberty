/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.config.extensions;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 *
 */
@WebServlet(urlPatterns = { "test" })
public class TestResultsServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -4913608474531994493L;
    private static final String servicePIDName = "service.pid";

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
        // Get the ManagedServiceFactory identifier so we can look up the correct service.
        String servicePid = req.getParameter(servicePIDName);
        Collection<ServiceReference<ConfigPropertiesProvider>> refs = new HashSet<ServiceReference<ConfigPropertiesProvider>>();
        ServiceReference<ConfigPropertiesProvider> ref = null;

        try {

            System.out.println("Looking up services with filter of (" + servicePIDName + "=" + servicePid + ")");
            // Find all services that match the relevant service.pid attr. There should only be one of these.
            refs = ctxt.getServiceReferences(ConfigPropertiesProvider.class, "(" + servicePIDName + "=" + servicePid + ")");
            // If we don't find any then issue warning in the response.
            if (!refs.isEmpty()) {
                // There should only be one service, so pick the 1st one.
                ref = refs.iterator().next();
                ConfigPropertiesProvider propsProvider = ctxt.getService(ref);
                if (propsProvider != null) {
                    String id = req.getParameter("id");
                    Dictionary<String, ?> props = propsProvider.getPropertiesForId(id);
                    if (props != null) {
                        String configId = (String) props.get(XMLConfigConstants.CFG_CONFIG_INSTANCE_ID);
                        if (configId != null && configId.startsWith(servicePid)) {
                            resp.getWriter().print("PASSED: test bundle was called with properties for ID " + id);
                            String propName;
                            if ((propName = req.getParameter("prop")) != null) {
                                resp.getWriter().print("Prop value: " + props.get(propName));
                            }
                            resp.setStatus(HttpServletResponse.SC_OK);
                            return;

                        } else {
                            resp.getWriter().print("FAILED: incorrect config.id in properties for ID  " + id + ". Value: " + configId);
                            resp.setStatus(HttpServletResponse.SC_OK);
                            return;
                        }
                    } else {
                        resp.getWriter().print("FAILED: test bundle was not called with properties for PID starting with " + id);
                        resp.setStatus(HttpServletResponse.SC_OK);
                        return;
                    }
                }
            } else {
                System.out.println("No service");
                resp.getWriter().print("FAILED: unable to find ConfigPropertiesProvider service");
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (InvalidSyntaxException ise) {
            resp.getWriter().print("Exception thrown whilst getting Config Property Provider service references: " + ise);
        } finally {
            // If the reference isn't null, release the service.
            if (ref != null)
                ctxt.ungetService(ref);
        }
    }
}
