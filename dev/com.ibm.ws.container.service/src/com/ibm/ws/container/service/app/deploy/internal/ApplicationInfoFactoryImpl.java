/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy.internal;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.AppClassLoaderFactory;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoFactory;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedEARApplicationInfo;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class ApplicationInfoFactoryImpl implements ApplicationInfoFactory {

    private final AtomicServiceReference<J2EENameFactory> j2eeNameFactory = new AtomicServiceReference<J2EENameFactory>("j2eeNameFactory");

    private final Set<String> names = new HashSet<String>();

    public void setJ2eeNameFactory(ServiceReference<J2EENameFactory> ref) {
        j2eeNameFactory.setReference(ref);
    }

    public void unsetJ2eeNameFactory(ServiceReference<J2EENameFactory> ref) {
        j2eeNameFactory.unsetReference(ref);
    }

    protected void activate(ComponentContext cc) {
        j2eeNameFactory.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        j2eeNameFactory.deactivate(cc);
    }

    private String reserveName(String preferredName) {
        String name = preferredName;

        synchronized (names) {
            if (!names.add(name)) {
                int number = 2;
                do {
                    name = preferredName + '_' + number;
                    number++;
                } while (!names.add(name));
            }
        }

        return name;
    }

    private void unreserveName(String name) {
        synchronized (names) {
            names.remove(name);
        }
    }

    @Override
    public ExtendedApplicationInfo createApplicationInfo(String appMgrName, String preferredName, Container container,
                                                         ApplicationClassesContainerInfo appClassesContainerInfo,
                                                         NestedConfigHelper configHelper) {
        J2EEName j2eeName = j2eeNameFactory.getService().create(appMgrName, null, null);
        String name = reserveName(preferredName);

        ExtendedApplicationInfo appInfo = new ApplicationInfoImpl(name, j2eeName, container, configHelper);
        try {
            NonPersistentCache cache = container.adapt(NonPersistentCache.class);
            cache.addToCache(ApplicationInfo.class, appInfo);
            cache.addToCache(ApplicationClassesContainerInfo.class, appClassesContainerInfo);
        } catch (UnableToAdaptException ex) {
            FFDCFilter.processException(ex, getClass().getName(), "ApplicationInfoFactoryImpl_createApplicationInfo");
        }
        return appInfo;
    }

    @Override
    public ExtendedEARApplicationInfo createEARApplicationInfo(String appMgrName, String preferredName, Container container,
                                                               ApplicationClassesContainerInfo appClassesContainerInfo,
                                                               NestedConfigHelper configHelper,
                                                               Container libDirContainer, AppClassLoaderFactory classLoaderFactory) {
        J2EEName j2eeName = j2eeNameFactory.getService().create(appMgrName, null, null);
        ExtendedEARApplicationInfo appInfo = null;
        try {
            String name = reserveName(preferredName);
            appInfo = new EARApplicationInfoImpl(name, j2eeName, container, configHelper,
                            libDirContainer, classLoaderFactory);

            NonPersistentCache cache = container.adapt(NonPersistentCache.class);
            cache.addToCache(ApplicationInfo.class, appInfo);
            cache.addToCache(ApplicationClassesContainerInfo.class, appClassesContainerInfo);
        } catch (UnableToAdaptException ex) {
            FFDCFilter.processException(ex, getClass().getName(), "ApplicationInfoFactoryImpl_createEARApplicationInfo");
        }
        return appInfo;
    }

    @Override
    public void destroyApplicationInfo(ApplicationInfo appInfo) {
        unreserveName(appInfo.getName());
    }
}
