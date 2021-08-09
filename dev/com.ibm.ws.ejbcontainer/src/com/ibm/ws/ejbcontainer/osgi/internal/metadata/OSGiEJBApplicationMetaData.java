/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.metadata;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;

/**
 * Extend EJBApplicationMetaData to gain access to the set of modules running
 * in this application.
 * 
 */
public class OSGiEJBApplicationMetaData extends EJBApplicationMetaData {

    public OSGiEJBApplicationMetaData(EJSContainer container, String name, String logicalName, boolean standaloneModule, ApplicationMetaData amd, boolean started,
                                      boolean blockWorkUntilStarted) {
        super(container, name, logicalName, standaloneModule, amd, started, blockWorkUntilStarted);
    }

    private final Map<String, EJBModuleMetaDataImpl> modulesMap = new HashMap<String, EJBModuleMetaDataImpl>();

    @Override
    public synchronized void startingModule(EJBModuleMetaDataImpl mmd, boolean blockWorkUntilStarted) {
        super.startingModule(mmd, blockWorkUntilStarted);
        modulesMap.put(mmd.getName(), mmd);
    }

    @Override
    public void stoppingModule(EJBModuleMetaDataImpl mmd) {
        modulesMap.remove(mmd.getName());
        super.stoppingModule(mmd);
    }

    public EJBModuleMetaDataImpl getModuleMetaData(String name) {
        return modulesMap.get(name);
    }
}
