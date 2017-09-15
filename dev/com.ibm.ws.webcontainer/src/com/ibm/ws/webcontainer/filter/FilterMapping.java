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
package com.ibm.ws.webcontainer.filter;

import javax.servlet.DispatcherType;

import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.filter.IFilterMapping;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

public class FilterMapping implements IFilterMapping {
    private String urlPattern;
    private DispatcherType[] dispatchMode = { DispatcherType.REQUEST }; // Default is
                                                                   // request
    private IFilterConfig filterConfig;
    private IServletConfig sconfig;
    private int mappingType;

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

}
