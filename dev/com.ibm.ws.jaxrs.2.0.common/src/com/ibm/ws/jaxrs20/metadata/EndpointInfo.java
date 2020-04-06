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
package com.ibm.ws.jaxrs20.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

public class EndpointInfo implements Serializable {

    private static final long serialVersionUID = 97168420190792502L;

    private String servletName;
    private final String servletClassName;
    private final String servletMappingUrl;
    private final String appClassName;
    private final String appPath;
    private final Set<String> providerAndPathClassNames;
    private String address;
    private transient Application app;
    private Boolean customizedApp = false;

    public Boolean isCustomizedApp() {
        return customizedApp;
    }

    public void setCustomizedApp(Boolean customizedApp) {
        this.customizedApp = customizedApp;
    }

    public Application getApp() {
        return app;
    }

    public void setApp(Application app) {
        this.app = app;
    }

    /**
     * 2 sets for perRequest & singleton
     * 
     * @see JaxRsWebEndpointImpl.init()
     */
    private final Set<ProviderResourceInfo> perRequestProviderAndPathInfos = new HashSet<ProviderResourceInfo>();
    private final Set<ProviderResourceInfo> singletonProviderAndPathInfos = new HashSet<ProviderResourceInfo>();
    private final List<String> abstractClassInterfaceList = new ArrayList<String>();
    private boolean configuredInWebXml = false;

    private Map<String, String> endpointProperties;

    /**
     * 
     * @param servletName
     * @param servletClassName
     * @param servletMappingUrl
     * @param appClassName
     * @param appPath
     * @param endpointType
     * @param providerAndPathClassNames
     * @param instances
     */
    public EndpointInfo(String servletName, String servletClassName, String servletMappingUrl, String appClassName, String appPath,
                        Set<String> providerAndPathClassNames) {
        this.servletName = servletName;
        this.servletClassName = servletClassName;
        this.servletMappingUrl = servletMappingUrl;
        this.appClassName = appClassName;
        this.appPath = appPath;
        if (providerAndPathClassNames != null) {
            this.providerAndPathClassNames = providerAndPathClassNames;
        } else {
            this.providerAndPathClassNames = new HashSet<String>();
        }
    }

    /**
     * @return the providerAndPathClassNames
     */
    public Set<String> getProviderAndPathClassNames() {
        return providerAndPathClassNames;
    }

    /**
     * @return the servletName
     */
    public String getServletName() {
        return servletName;
    }

    /**
     * @param servletName the servletName to set
     */
    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    /**
     * @return the implBeanClassName
     */
    public String getServletClassName() {
        return servletClassName;
    }

    public String getServletMappingUrl() {
        return servletMappingUrl;
    }

    public String getAppClassName() {
        return appClassName;
    }

    public String getAppPath() {
        return appPath;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return the endpointProperties
     */
    public Map<String, String> getEndpointProperties() {
        return endpointProperties;
    }

    /**
     * @param endpointProperties the endpointProperties to set
     */
    public void setEndpointProperties(Map<String, String> endpointProperties) {
        this.endpointProperties = endpointProperties;
    }

    /**
     * ALEX: get perRequest & singleton ProviderAndPathInfos
     * 
     * @return
     */
    public Set<ProviderResourceInfo> getPerRequestProviderAndPathInfos() {
        return perRequestProviderAndPathInfos;
    }

    public Set<ProviderResourceInfo> getSingletonProviderAndPathInfos() {
        return singletonProviderAndPathInfos;
    }

    /**
     * @return the configuredInWebXml
     */
    public boolean isConfiguredInWebXml() {
        return configuredInWebXml;
    }

    /**
     * @param configuredInWebXml the configuredInWebXml to set
     */
    public void setConfiguredInWebXml(boolean configuredInWebXml) {
        this.configuredInWebXml = configuredInWebXml;
    }

    public List<String> getAbstractClassInterfaceList() {
        return abstractClassInterfaceList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" ").append(servletName).append(" ").append(appClassName).append(" ").append(appPath);
        sb.append(" ").append(configuredInWebXml);
        return sb.toString();
    }
}
