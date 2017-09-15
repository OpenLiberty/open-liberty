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
package com.ibm.ws.ejbcontainer.osgi;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;

/**
 * The deployment configuration for a stateless EJB in a system module.
 */
public class EJBSystemBeanConfig {
    private final String name;
    private final Class<?> ejbClass;
    private final Class<? extends EJBHome> remoteHomeInterface;
    private final Class<? extends EJBObject> remoteInterface;
    private final String remoteHomeBindingName;

    /**
     * @param name the valid EJB name (e.g., "MyBean"), which is part of the
     *            persistent identity of EJB references and must not be changed
     *            or else serialized proxies will no longer work
     * @param ejbClass the implementation class
     * @param remoteHomeBindingName the CosNaming binding name (e.g., "ejb/test/MyBean")
     * @param remoteHomeInterface the remote home interface
     * @param remoteInterface the remote component interface
     */
    public EJBSystemBeanConfig(String name,
                               Class<?> ejbClass,
                               String remoteHomeBindingName,
                               Class<? extends EJBHome> remoteHomeInterface,
                               Class<? extends EJBObject> remoteInterface) {
        this.name = name;
        this.ejbClass = ejbClass;
        this.remoteHomeInterface = remoteHomeInterface;
        this.remoteInterface = remoteInterface;
        this.remoteHomeBindingName = remoteHomeBindingName;
    }

    @Override
    public String toString() {
        return super.toString() +
               "[name=" + name +
               ", class=" + ejbClass +
               ", remoteHome=" + remoteHomeInterface +
               ", remote=" + remoteInterface +
               ']';
    }

    public String getName() {
        return name;
    }

    public Class<?> getEJBClass() {
        return ejbClass;
    }

    public Class<? extends EJBHome> getRemoteHomeInterface() {
        return remoteHomeInterface;
    }

    public Class<? extends EJBObject> getRemoteInterface() {
        return remoteInterface;
    }

    public String getRemoteHomeBindingName() {
        return remoteHomeBindingName;
    }
}
