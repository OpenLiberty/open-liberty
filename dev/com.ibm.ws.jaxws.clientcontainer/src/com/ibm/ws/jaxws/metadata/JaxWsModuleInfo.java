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
package com.ibm.ws.jaxws.metadata;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class JaxWsModuleInfo implements Serializable {

    private static final long serialVersionUID = -8116043459266953308L;

    private final JaxWsModuleType moduleType;

    private final Map<String, EndpointInfo> endpointInfoMap = new HashMap<String, EndpointInfo>();

    //Only used by ejb based webservice in an ejb-jar
    private String contextRoot;

    private transient ServiceSecurityInfo serviceSecurityInfo;

    public JaxWsModuleInfo(JaxWsModuleType moduleType) {
        this.moduleType = moduleType;
    }

    public void addEndpointInfo(String name, EndpointInfo endpointInfo) {
        endpointInfo.setPortLink(name);
        endpointInfoMap.put(name, endpointInfo);
    }

    public EndpointInfo getEndpointInfo(String name) {
        return endpointInfoMap.get(name);
    }

    public Set<String> getEndpointNames() {
        return Collections.unmodifiableSet(endpointInfoMap.keySet());
    }

    public Collection<EndpointInfo> getEndpointInfos() {
        return Collections.unmodifiableCollection(endpointInfoMap.values());
    }

    public boolean contains(String name) {
        return endpointInfoMap.containsKey(name);
    }

    public int endpointInfoSize() {
        return endpointInfoMap.size();
    }

    public Map<String, EndpointInfo> getEndpointInfoMap() {
        return Collections.unmodifiableMap(endpointInfoMap);
    }

    public Set<String> getEndpointImplBeanClassNames() {
        Set<String> serviceClassNames = new HashSet<String>();
        for (EndpointInfo endpointInfo : endpointInfoMap.values()) {
            serviceClassNames.add(endpointInfo.getImplBeanClassName());
        }
        return serviceClassNames;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public ServiceSecurityInfo getServiceSecurityInfo() {
        return serviceSecurityInfo;
    }

    public void setServiceSecurityInfo(ServiceSecurityInfo serviceSecurityInfo) {
        this.serviceSecurityInfo = serviceSecurityInfo;
    }

    public JaxWsModuleType getModuleType() {
        return moduleType;
    }

}
