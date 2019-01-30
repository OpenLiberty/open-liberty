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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;

/**
 *
 */
public class JaxWsServerMetaData {

    private final LibertyApplicationBus applicationBus;

    private final JaxWsModuleMetaData moduleMetaData;

    private final Map<String, J2EEName> endpointNameJ2EENameMap = new HashMap<String, J2EEName>();

    public JaxWsServerMetaData(JaxWsModuleMetaData moduleMetaData) {
        this.moduleMetaData = moduleMetaData;
        this.applicationBus = LibertyApplicationBusFactory.getInstance().createServerScopedBus(moduleMetaData);
    }

    public void destroy() {
        if (applicationBus != null)
            applicationBus.shutdown(false);
    }

    /**
     * @return the applicationBus
     */
    public LibertyApplicationBus getServerBus() {
        return applicationBus;
    }

    /**
     * @return the moduleMetaData
     */
    public JaxWsModuleMetaData getModuleMetaData() {
        return moduleMetaData;
    }

    /**
     * Add the Endpoint portLink and J2EEName pair
     * 
     * @param endpointName
     * @param j2eeName
     */
    public void putEndpointNameAndJ2EENameEntry(String endpointName, J2EEName j2eeName) {
        endpointNameJ2EENameMap.put(endpointName, j2eeName);
    }

    /**
     * Get the J2EEName by endpointName
     * 
     * @param endpointName
     * @return
     */
    public J2EEName getEndpointJ2EEName(String endpointName) {
        return endpointNameJ2EENameMap.get(endpointName);
    }

    /**
     * Get the endpoint name by j2eeName
     * 
     * @param j2eeName
     * @return
     */
    public String retrieveEndpointName(J2EEName j2eeName) {
        for (Entry<String, J2EEName> entry : endpointNameJ2EENameMap.entrySet()) {
            if (entry.getValue().equals(j2eeName)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
