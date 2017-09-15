/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletRegistration;

import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared_impl.webapp.webxml.DelegatedFacesServlet;
import org.apache.myfaces.spi.ServletMapping;
import org.apache.myfaces.spi.WebConfigProvider;
import org.apache.myfaces.spi.impl.ServletMappingImpl;

import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *
 */
public class LibertyWebConfigProvider extends WebConfigProvider {
    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf.ext.LibertyWebConfigProvider");

    /**
     * return the faces servlet mappings from the Servlet Context.
     */
    @Override
    public List<ServletMapping> getFacesServletMappings(ExternalContext externalContext) {
        
        IServletContext context = (IServletContext) externalContext.getContext();
        MyfacesConfig mfconfig = MyfacesConfig.getCurrentInstance(externalContext);
        String delegateFacesServlet = mfconfig.getDelegateFacesServlet();
        List<ServletMapping> facesServletMappings = new ArrayList<ServletMapping>();

        Map servletRegistrations = context.getServletRegistrations();
        if (null != servletRegistrations) {
            Iterator iterator = servletRegistrations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();

                ServletRegistration servletRegistration = (ServletRegistration) entry.getValue();
                String servletName = servletRegistration.getClassName();

                // Call the new simpleClasSforName method and pass false to ensure we don't log
                // any errors if we can't load the class.  For instance we won't be able to load
                // the WebSockets servlet if it was added programmatically.
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("Trying to load the following Servlet that was registered: " + servletName);
                }

                Class servletClass = ClassUtils.simpleClassForName(servletName, false);

                // The servletClass will be null if ClassUtils.simpleClassForName can't
                // load the specificed class.
                if (servletClass == null)
                {
                    if (log.isLoggable(Level.FINE))
                    {
                        log.fine("servletClass is null for: " + servletName);
                        log.fine("Continuing on to the next Servlet that was registered.");
                    }
                    continue;
                }

                //make sure the servlet class is extended from FacesServlet or from deletegated faces servlet.
                if (FacesServlet.class.isAssignableFrom(servletClass) ||
                                DelegatedFacesServlet.class.isAssignableFrom(servletClass) ||
                                servletClass.getName().equals(delegateFacesServlet)) {

                    Collection<String> mappings = servletRegistration.getMappings();

                    for (String url : mappings) {
                        org.apache.myfaces.shared_impl.webapp.webxml.ServletMapping delegateMapping = new org.apache.myfaces.shared_impl.webapp.webxml.ServletMapping(servletName, servletClass, url);
                        facesServletMappings.add(new ServletMappingImpl(delegateMapping));
                    }
                }
            }
        }

        return facesServletMappings;
    }

    /**
     * check if the web config defined the error pages
     */
    @Override
    public boolean isErrorPagePresent(ExternalContext externalContext) {
        
        IServletContext context = (IServletContext) externalContext.getContext();
        WebAppConfig webAppConfig = context.getWebAppConfig();
        return webAppConfig.isErrorPagePresent();
    }

}
