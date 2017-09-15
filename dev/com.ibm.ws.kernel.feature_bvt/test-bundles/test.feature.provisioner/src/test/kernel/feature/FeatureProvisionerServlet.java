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
package test.kernel.feature;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.kernel.feature.FeatureProvisioner;

/**
 *
 */
@WebServlet(urlPatterns = { "provisioner" })
public class FeatureProvisionerServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 8280749374546298397L;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Writer responseWriter = resp.getWriter();
        FeatureProvisioner provisionerService = null;
        try {
            BundleContext ctxt = (BundleContext) req.getServletContext().getAttribute("osgi-bundlecontext");
            ServiceReference<FeatureProvisioner> featureProvisionerRef = ctxt.getServiceReference(FeatureProvisioner.class);
            if (featureProvisionerRef != null) {
                provisionerService = ctxt.getService(featureProvisionerRef);
            }
            if (provisionerService != null) {
                provisionerService.refreshFeatures();
                responseWriter.write("FeatureProvisioner: features refreshed.");
            } else {
                responseWriter.write("FeatureProvisioner: No service found.");
            }
        } finally {
            responseWriter.flush();
            responseWriter.close();
        }
    }
}
