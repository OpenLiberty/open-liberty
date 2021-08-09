/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
import org.apache.myfaces.spi.impl.DefaultWebConfigProvider;
import org.apache.myfaces.spi.impl.ServletMappingImpl;

import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *
 */
public class LibertyWebConfigProvider extends WebConfigProvider {
    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf.ext.LibertyWebConfigProvider");

    DefaultWebConfigProvider provider = new DefaultWebConfigProvider();

    /**
     * return the faces servlet mappings from the Servlet Context.
     */
    @Override
    public List<ServletMapping> getFacesServletMappings(ExternalContext externalContext) {
        
        // In the jsf-2.0 feature, we discovered Faces Servlet mappings here by calling into 
        // ServletContext.getServletRegistrations().  Unfortunately Servlet 3.1 limited the scope of when 
        // programmatic configuration can be done, so calling getServletRegistrations here is illegal.
        // We now discover Faces Servlet mappings in the ServletContextInitializer and save that result 
        // on the Servlet Context attributes map - so finding mappings here is not needed.
        
        return provider.getFacesServletMappings(externalContext);
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
