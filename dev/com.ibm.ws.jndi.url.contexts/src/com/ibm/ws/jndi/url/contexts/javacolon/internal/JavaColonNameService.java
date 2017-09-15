/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import java.util.Collection;
import java.util.Collections;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.jndi.url.contexts.javacolon.JavaJNDIComponentMetaDataAccessor;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class JavaColonNameService implements ApplicationMetaDataListener, ModuleMetaDataListener, JavaColonNamingHelper {
    /**
     * Name in java namespace for "java:app/AppName"
     */
    private static final String APP_NAME = "AppName";

    /**
     * Name in java namespace for "java:module/ModuleName"
     */
    private static final String MODULE_NAME = "ModuleName";

    private MetaDataSlot appSlot;
    private MetaDataSlot moduleSlot;

    public void setMetaDataSlotService(MetaDataSlotService service) {
        appSlot = service.reserveMetaDataSlot(ApplicationMetaData.class);
        moduleSlot = service.reserveMetaDataSlot(ModuleMetaData.class);
    }

    public void unsetMetaDataSlotService(MetaDataSlotService service) {}

    @Override
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) {
        try {
            Container container = event.getContainer();
            NonPersistentCache cache = container.adapt(NonPersistentCache.class);
            ApplicationInfo appInfo = (ApplicationInfo) cache.getFromCache(ApplicationInfo.class);
            event.getMetaData().setMetaData(appSlot, appInfo.getName());
        } catch (UnableToAdaptException e) {
            e.getClass(); // findbugs
        }
    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        // Nothing.
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {
        try {
            Container container = event.getContainer();
            NonPersistentCache cache = container.adapt(NonPersistentCache.class);
            ModuleInfo moduleInfo = (ModuleInfo) cache.getFromCache(ModuleInfo.class);
            event.getMetaData().setMetaData(moduleSlot, moduleInfo.getName());
        } catch (UnableToAdaptException e) {
            e.getClass(); // findbugs
        }
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        // Nothing.
    }

    private ComponentMetaData getComponentMetaData(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        return JavaJNDIComponentMetaDataAccessor.getComponentMetaData(namespace, name);
    }

    protected String getModuleName(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        ComponentMetaData cmd = getComponentMetaData(namespace, name);
        return (String) cmd.getModuleMetaData().getMetaData(moduleSlot);
    }

    protected String getAppName(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        ComponentMetaData cmd = getComponentMetaData(namespace, name);
        ModuleMetaData mmd = cmd.getModuleMetaData();
        ApplicationMetaData amd = mmd.getApplicationMetaData();
        return (String) amd.getMetaData(appSlot);
    }

    @Override
    public Object getObjectInstance(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        if (namespace == JavaColonNamespace.MODULE) {
            if (name.equals(MODULE_NAME)) {
                return getModuleName(namespace, name);
            }
        } else if (namespace == JavaColonNamespace.APP) {
            if (name.equals(APP_NAME)) {
                return getAppName(namespace, name);
            }
        }

        return null;
    }

    @Override
    public boolean hasObjectWithPrefix(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        if (namespace == JavaColonNamespace.MODULE) {
            if (name.equals("")) {
                return getModuleName(namespace, name) != null;
            }
        } else if (namespace == JavaColonNamespace.APP) {
            if (name.equals("")) {
                return getAppName(namespace, name) != null;
            }
        }

        return false;
    }

    @Override
    public Collection<? extends NameClassPair> listInstances(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        if (namespace == JavaColonNamespace.MODULE) {
            if (name.equals("")) {
                if (getModuleName(namespace, name) != null) {
                    return Collections.singletonList(new NameClassPair(MODULE_NAME, String.class.getName()));
                }
            }
        } else if (namespace == JavaColonNamespace.APP) {
            if (name.equals("")) {
                if (getAppName(namespace, name) != null) {
                    return Collections.singletonList(new NameClassPair(APP_NAME, String.class.getName()));
                }
            }
        }

        return Collections.emptyList();
    }
}
