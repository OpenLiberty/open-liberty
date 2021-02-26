/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.wab1;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.wab.configure.WABConfiguration;

@WebServlet("/switch")
public class Switch extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private ServiceRegistration<WABConfiguration> configuration;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Bundle thisBundle = FrameworkUtil.getBundle(this.getClass());
        BundleContext bc = thisBundle.getBundleContext();

        try {
            if (configuration != null) {
                configuration.unregister();
            }
            String context = request.getParameter("context");
            Hashtable<String, String> props = new Hashtable<>();
            props.put(WABConfiguration.CONTEXT_NAME, context);
            props.put(WABConfiguration.CONTEXT_PATH, "/switchTarget");
            configuration = bc.registerService(WABConfiguration.class, new WABConfiguration() {
            }, props);
            response.getOutputStream().println("SUCCESS service: " + getClass().getName());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            response.getOutputStream().println("FAIL service: " + getClass().getName() + " " + e.getMessage());
        }
    }

}
