/*******************************************************************************
 * Copyright (c) 1997, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.filter;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.DispatcherType;

import com.ibm.ws.webcontainer.filter.extended.IFilterMappingExtended;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

public class FilterMapping implements IFilterMappingExtended {
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.filter");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.filter.FilterMapping";
    private String urlPattern;
    private DispatcherType[] dispatchMode = { DispatcherType.REQUEST }; // Default is
                                                                   // request
    private IFilterConfig filterConfig;
    private IServletConfig sconfig;
    private int mappingType;
    private String servletName;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.IFilterMapping#getMappingType()
     */
    public int getMappingType() {
        return mappingType;
    }

    public FilterMapping(String urlPattern, IFilterConfig fconfig, IServletConfig sconfig) {
        filterConfig = fconfig;
        if (urlPattern != null)
            setUrlPattern(urlPattern);
        this.sconfig = sconfig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.IFilterMapping#getFilterConfig()
     */
    public IFilterConfig getFilterConfig() {
        return filterConfig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.IFilterMapping#getUrlPattern()
     */
    public String getUrlPattern() {
        return urlPattern;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.ws.webcontainer.filter.IFilterMapping#setFilterConfig(com.ibm
     * .wsspi.webcontainer.filter.IFilterConfig)
     */
    public void setFilterConfig(IFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.ws.webcontainer.filter.IFilterMapping#setUrlPattern(java.lang
     * .String)
     */
    public void setUrlPattern(String filterURI) {
        // determine what type of filter uri we have and set the mapping type
        if (filterURI.equals("/"))
            mappingType = WebAppFilterManager.FMI_MAPPING_SINGLE_SLASH;
        else if (filterURI.startsWith("/") && filterURI.endsWith("/*")) {
            mappingType = WebAppFilterManager.FMI_MAPPING_PATH_MATCH;

            // go ahead and strip the /* for later matching
            filterURI = filterURI.substring(0, filterURI.length() - 2);
        } else if (filterURI.startsWith("*."))
            mappingType = WebAppFilterManager.FMI_MAPPING_EXTENSION_MATCH;
        // PK57083 START
        else if (WCCustomProperties.MAP_FILTERS_TO_ASTERICK && filterURI.equals("*")) {
            mappingType = WebAppFilterManager.FMI_MAPPING_PATH_MATCH;
            filterURI = "";
        }
        // PK57083 END
        else
            mappingType = WebAppFilterManager.FMI_MAPPING_EXACT_MATCH;

        this.urlPattern = filterURI;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.IFilterMapping#getServletConfig()
     */
    public IServletConfig getServletConfig() {
        return sconfig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.IFilterMapping#getDispatchMode()
     */
    public DispatcherType[] getDispatchMode() {
        return dispatchMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.IFilterMapping#setDispatchMode(int[])
     */
    public void setDispatchMode(DispatcherType[] dispatchMode) {
        this.dispatchMode = dispatchMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.extended.IFilterMappingExtended#getServletFilterMappingName()
     */
    public String getServletFilterMappingName() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "getServletFilterMappingName", "servlet name " + servletName + " , filter mapping -> "+ this);
        return servletName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.extended.IFilterMappingExtended#saveServletFilterMappingName(String)
     */
    public void saveServletFilterMappingName(String sName) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "saveServletFilterMappingName", "servlet name " + sName + " , filter mapping -> "+ this);
        servletName = sName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.filter.extended.IFilterMappingExtended#setServletConfig(IServletConfig)
     */
    public void setServletConfig(IServletConfig sconfig) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME, "setServletConfig", "set ServletConfig to {"+ sconfig + "} , filter mapping -> "+ this);
        this.sconfig = sconfig;   
    }
}
