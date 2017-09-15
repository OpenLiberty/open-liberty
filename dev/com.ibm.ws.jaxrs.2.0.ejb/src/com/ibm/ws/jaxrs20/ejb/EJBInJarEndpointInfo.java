/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.ejb;

import java.util.Set;

import com.ibm.ws.jaxrs20.metadata.EndpointInfo;

/**
 *
 */
public class EJBInJarEndpointInfo extends EndpointInfo {

    /**  */
    private static final long serialVersionUID = 7350696518624254292L;

    private String ejbModuleName = null;

    /**
     * @param servletName
     * @param servletClassName
     * @param servletMappingUrl
     * @param appClassName
     * @param appPath
     * @param providerAndPathClassNames
     */
    public EJBInJarEndpointInfo(String servletName, String servletClassName, String servletMappingUrl, String appClassName, String appPath, Set<String> providerAndPathClassNames) {
        super(servletName, servletClassName, servletMappingUrl, appClassName, appPath, providerAndPathClassNames);

    }

    public String getEJBModuleName() {
        return this.ejbModuleName;
    }

    public void setEJBModuleName(String eJBModuleName) {
        this.ejbModuleName = eJBModuleName;
    }

}
