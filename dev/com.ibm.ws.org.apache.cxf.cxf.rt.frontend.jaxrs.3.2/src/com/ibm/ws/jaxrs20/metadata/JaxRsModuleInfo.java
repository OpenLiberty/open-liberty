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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class JaxRsModuleInfo implements Serializable {

    private static final long serialVersionUID = -8116043459266953308L;

    private final JaxRsModuleType moduleType;

    //private final Map<String, EndpointInfo> endpointInfoMap = new HashMap<String, EndpointInfo>();
    private final Map<String, List<EndpointInfo>> endpointInfoMap = new HashMap<String, List<EndpointInfo>>();

    private boolean isShareEJBJarWithJAXWS = false;

    /**
     * @return the isShareEJBJarWithJAXWS
     */
    public boolean isShareEJBJarWithJAXWS() {
        return isShareEJBJarWithJAXWS;
    }

    /**
     * @param isShareEJBJarWithJAXWS the isShareEJBJarWithJAXWS to set
     */
    public void setIsShareEJBJarWithJAXWS(boolean isShareEJBJarWithJAXWS) {
        this.isShareEJBJarWithJAXWS = isShareEJBJarWithJAXWS;
    }

    public JaxRsModuleInfo(JaxRsModuleType moduleType) {
        this.moduleType = moduleType;
    }

    /**
     * 
     * @param name - generally the application class name
     * @param endpointInfo
     */
    public void addEndpointInfo(String name, EndpointInfo endpointInfo) {
//        endpointInfo.setPortLink(name);
        List<EndpointInfo> eplist = endpointInfoMap.get(name);
        if (eplist == null) {
            eplist = new ArrayList<EndpointInfo>();
        }
        eplist.add(endpointInfo);
        //endpointInfoMap.put(name, endpointInfo);
        endpointInfoMap.put(name, eplist);
    }

    /*
     * // return the first endpointInfo for a given app class.
     * // note that this is not viable if a class has >1 url mapping.
     * public EndpointInfo getEndpointInfo(String name) {
     * List<EndpointInfo> list = endpointInfoMap.get(name);
     * if ( list == null ) return null;
     * if (list.isEmpty()) return null;
     * return list.get(0);
     * }
     */

    public Set<String> getEndpointNames() {
        return Collections.unmodifiableSet(endpointInfoMap.keySet());
    }

    /*
     * return all endpoints
     */
    public Collection<EndpointInfo> getEndpointInfos() {

        ArrayList<EndpointInfo> values = new ArrayList<EndpointInfo>();

        Iterator<String> it = getEndpointNames().iterator();
        // for each endpoint name, iterate over it's list of endpoints and add to values
        while (it.hasNext()) {
            List<EndpointInfo> list = endpointInfoMap.get(it.next());
            for (int i = 0; i < list.size(); i++) {
                values.add(list.get(i));
            }
        }
        return Collections.unmodifiableCollection(values);
        //return Collections.unmodifiableCollection(endpointInfoMap.values());
    }

    public boolean contains(String name) {
        return endpointInfoMap.containsKey(name);
    }

    public int endpointInfoSize() {
        return endpointInfoMap.size();
    }

    /*
     * public Map<String, EndpointInfo> getEndpointInfoMap() {
     * return Collections.unmodifiableMap(endpointInfoMap);
     * }
     */

    /*
     * public Set<String> getEndpointImplBeanClassNames() {
     * Set<String> serviceClassNames = new HashSet<String>();
     * for (EndpointInfo endpointInfo : endpointInfoMap.values()) {
     * serviceClassNames.add(endpointInfo.getAppClassName());
     * }
     * return serviceClassNames;
     * }
     */

    public JaxRsModuleType getModuleType() {
        return moduleType;
    }

}
