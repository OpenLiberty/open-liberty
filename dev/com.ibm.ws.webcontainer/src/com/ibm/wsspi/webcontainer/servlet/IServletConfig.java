/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;

import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;

/**
 *
 * @ibm-private-in-use
 */
public interface IServletConfig extends javax.servlet.ServletConfig, ServletRegistration.Dynamic
{
    
        /**
         * Set the classname of the servlet that is represented by this config object
         * @param string
         */
        public void setClassName(String string);
        
        /**
         * Returns the file name that may be associated with this config. The value will be 
         * non-null only in the case where the target represented by this config is a
         * JSP.
         * 
         * @return
         */
        public String getFileName();
        
        /**
         * Returns whether or not the servlet represented by this config should
         * be loaded at startup. For servlets added and configured dynamically 
         * (as is the case with JSPs), it means that the target will be initialized
         * through the init() method at the time of creation, as opposed to when 
         * the first request for thie resource arrives.
         * 
         * @return
         */
        public boolean isLoadOnStartup();
        
        /**
         * Sets the fileName associated with this config
         * @param jspUri
         */
        public void setFileName(String jspUri);
        
        /**
         * Sets the Map of initialization parameters for the servlet associated
         * with this config instance.
         * @param map
         */
        @SuppressWarnings("rawtypes")
        public void setInitParams(Map map);
        
        /**     
         * Sets whether or not this config represents a JSP file.
         * @param b
         */
        public void setIsJsp(boolean b);
        
        /**
         * Associates the given context with this Servlet's config.
         * @param context
         */
        public void setServletContext(ServletContext context);

        
        /**
         * Set the servletName for this config
         * @param jspRegisteredName
         */
        public void setServletName(String jspRegisteredName);
        
        /**
         * Set the display name for this config
         * @param jspRegisteredName
         */
        public void setDisplayName(String jspRegisteredName);
        
        /**
         * Set the classname for the servlet represented by this config
         * @return
         */
        public String getClassName();
        
        /**
         * Set the startup weight for the servlet represented by this config.
         * Setting a value >= 0 tells the container not to wait until the first
         * request for this servlet to initialize it.
         * 
         * @param integer
         */
        public void setStartUpWeight(Integer integer);
        
        /**
         * Add an attribute for this config
         * @param key
         * @param value
         */
        public void addAttribute(Object key, Object value);
        

        // Begin f269714, LI3477 - ServletConfig creation for Security
        /**
         * Set the current attribute map to the passed in parameter
         * @param map
         */
        @SuppressWarnings("rawtypes")
        public void setAttributes(Map map);
     // End f269714, LI3477 - ServletConfig creation for Security
 
        /**
         * Remove an attribute for this config
         * @param key
         * @return
         */
        public Object removeAttribute(Object key); 
        
        /**
         * Returns the component metadata associated with this configuration
         * @return
         */
        public WebComponentMetaData getMetaData();
        
        /**
         * Sets the component metadata associated with this configuration
         * @return
         */
        public void setMetaData(WebComponentMetaData metaData);
        
        //       Begin LIDB3477-17, cache setting for dynamically added servlets
        
        /**
         * Checks if caching is enabled for the servlet (requires dynacache to be enabled to have desired effect).
         * @return
         */
        public boolean isCachingEnabled();
        
        /**
         * Set whether caching is enabled for a servlet (requires dynacache to have desired effect).
         * @return
         */
        public void setIsCachingEnabled(boolean isEnabled);
        
        //      End LIDB3477-17
        /**
         * Set whether performance monitoring statistics are enabled.
         * @param value
         */
        public void setStatisticsEnabled(boolean value);  //304662, 304662.1

        /**
         * Check if performance monitoring statistics are enabled.
         * @return
         */
        public boolean isStatisticsEnabled();  //304662, 304662.1
        
        public boolean isJsp();
        
        public int getStartUpWeight();
        
        public void setDescription(String description);
        
        /**
         * Set whether resource should be considered internal. 
         *       * @return
         */
        public void setInternal(boolean isInternal);
        
        /**
         * Checks if resource should be considered internal.
         * @return
         */
        public boolean isInternal();
        
        public boolean isSingleThreadModelServlet();

                public void setSingleThreadModelServlet(boolean isSTM);

                public void setServletWrapper(IServletWrapper wrapper);

                public IServletWrapper getServletWrapper();

                public void setServletClass(Class<? extends Servlet> servletClass);

                public void setServlet(Servlet servlet);
                
                public Class<? extends Servlet> getServletClass();

                public Servlet getServlet();

        public void setMappings(List<String> mappings);

        public List<String> getMappings();
        
        public boolean isAddedToLoadOnStartup();
        
        public boolean isAsyncSupported();
        
        public void setAddedToLoadOnStartup(boolean addedToLoadOnStartup);

        public boolean isWeightChanged();
        
        public MultipartConfigElement getMultipartConfig();
        
        public File getMultipartBaseLocation();
        
        public void setMultipartBaseLocation(File location);

                public Set<String> addMapping(CheckContextInitialized checkContextInitialized, String... mappingURI);
                
                public enum CheckContextInitialized {
                        TRUE, FALSE
                }
                
                public ServletSecurityElement getServletSecurity();

                public boolean isClassDefined();

                public boolean isEnabled();

}


