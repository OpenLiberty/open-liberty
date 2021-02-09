/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.EARApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.jpa.management.AbstractJPAComponent;
import com.ibm.ws.jpa.management.JPAApplInfo;
import com.ibm.ws.jpa.management.JPAPUnitInfo;
import com.ibm.ws.jpa.management.JPAPXml;
import com.ibm.ws.jpa.management.JPAScopeInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;

public class OSGiJPAApplInfo extends JPAApplInfo {
    private final ApplicationInfo appInfo;

    OSGiJPAApplInfo(AbstractJPAComponent jpaComponent, String name, ApplicationInfo appInfo) {
        super(jpaComponent, name);
        this.appInfo = appInfo;
    }

    @Override
    protected JPAPUnitInfo createJPAPUnitInfo(JPAPuId puId, JPAPXml pxml, JPAScopeInfo scopeInfo) {
        return new OSGiJPAPUnitInfo(this, puId, pxml.getClassLoader(), scopeInfo);
    }

    void introspect() {
        final Set<String> archivesSet = introspectionIdentifyApplicationModules();
        doIntrospect(archivesSet);
    }

    Container getContainer() {
        return appInfo.getContainer();
    }

    private Set<String> introspectionIdentifyApplicationModules() {
        final Set<String> archivesSet = new HashSet<String>();

        if (appInfo == null) {
            return archivesSet;
        }

        final String applName = appInfo.getDeploymentName();
        try {
            if (appInfo instanceof EARApplicationInfo) {
                final Container appContainer = ((EARApplicationInfo) appInfo).getContainer();
                final NonPersistentCache cache = appContainer.adapt(NonPersistentCache.class);
                if (cache != null) {
                    final ApplicationClassesContainerInfo acci = (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);
                    final List<ModuleClassesContainerInfo> mcci = (acci == null) ? null : acci.getModuleClassesContainerInfo();

                    if (mcci == null) {
                        return archivesSet;
                    }

                    for (ModuleClassesContainerInfo m : mcci) {
                        final List<ContainerInfo> moduleContainerInfos = m.getClassesContainerInfo();
                        if (moduleContainerInfos != null && !moduleContainerInfos.isEmpty()) {
                            final ContainerInfo moduleContainerInfo = moduleContainerInfos.get(0);
                            final String archiveName = moduleContainerInfo.getName();
                            archivesSet.add(applName + "/" + archiveName);

                            if (moduleContainerInfo.getType() == Type.WEB_MODULE) {
                                // Web modules can contain libraries in WEB-INF/lib
                                final Container warContainer = moduleContainerInfo.getContainer();
                                final Entry webInfLib = (warContainer == null) ? null : warContainer.getEntry("WEB-INF/lib/");
                                if (webInfLib != null) {
                                    try {
                                        final Container webInfLibContainer = webInfLib.adapt(Container.class);
                                        if (webInfLibContainer != null) {
                                            final String pathRoot = applName + "/" + archiveName + "/WEB-INF/lib/";
                                            for (Entry entry : webInfLibContainer) {
                                                archivesSet.add(pathRoot + entry.getName());
                                            }
                                        }
                                    } catch (Throwable t2) {
                                        // Swallow
                                    }
                                }
                            }
                        }
                    }
                }

                // Process EAR Library
                final Container appLibContainer = ((EARApplicationInfo) appInfo).getLibraryDirectoryContainer();
                if (appLibContainer != null) {
                    final String pathRoot = applName + "/" + appLibContainer.getName() + "/";
                    for (com.ibm.wsspi.adaptable.module.Entry entry : appLibContainer) {
                        archivesSet.add(pathRoot + entry.getName());
                    }
                }
            } else {
                // Standalone WAR deployed to appserver
                archivesSet.add(applName + " (WAR)");
                final Container warContainer = appInfo.getContainer();
                final Entry webInfLib = (warContainer == null) ? null : warContainer.getEntry("WEB-INF/lib/");
                if (webInfLib != null) {
                    try {
                        final Container webInfLibContainer = webInfLib.adapt(Container.class);
                        if (webInfLibContainer != null) {
                            final String pathRoot = applName + "/WEB-INF/lib/";
                            for (Entry entry : webInfLibContainer) {
                                archivesSet.add(pathRoot + entry.getName());
                            }
                        }
                    } catch (Throwable t2) {
                        // Swallow
                    }
                }
            }
        } catch (Throwable t) {
            // Swallow for dump introspection.
        }

        return archivesSet;
    }
}
