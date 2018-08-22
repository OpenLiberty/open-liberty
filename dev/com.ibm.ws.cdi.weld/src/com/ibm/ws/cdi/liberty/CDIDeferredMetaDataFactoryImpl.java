/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.liberty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;

@Component(service = {DeferredMetaDataFactory.class},
           property = { "deferredMetaData=CDI" })
public class CDIDeferredMetaDataFactoryImpl implements DeferredMetaDataFactory {

    //Entries are added in applicationStarting() -> beginContext() and removed in the finally block inside applicationStopped. 
    //If you add annother codepath that adds to this map, be sure to clean it up to prevent memory leaks. 
    private static Map<String,ComponentMetaData> metaDataMap = new ConcurrentHashMap<String,ComponentMetaData>();

    public void registerComponentMetaData(CDIArchive archive, ComponentMetaData cmd) throws CDIException  {
        metaDataMap.put(getPersistentIdentifier(archive.getMetaData()), cmd);
    }

    public void removeComponentMetaData(CDIArchive archive) throws CDIException  {
        metaDataMap.remove(getPersistentIdentifier(archive.getMetaData()));
    }

    //Naming convention: Container name#
    //e.g. WEB#, EJB#
    public static String getPersistentIdentifier(MetaData md) {
        return "CDI#" + md.getName();
    }

    @Override
    public ComponentMetaData createComponentMetaData(String identifier) {
        if (! metaDataMap.containsKey(identifier)) {
            throw new IllegalStateException("Could not find ComponentMetaData");
        }

        return metaDataMap.get(identifier);
    }

    @Override  
    public void initialize(ComponentMetaData metadata) throws IllegalStateException {
        return;
    }

    @Override
    public String getMetaDataIdentifier(String appName, String moduleName, String componentName) {
        return appName + "#" + moduleName + "#" + componentName;
    }

    @Override
    public ClassLoader getClassLoader(ComponentMetaData metadata) {
        return null;
    }

}
