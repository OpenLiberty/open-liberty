/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy.internal;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.app.deploy.extended.ManifestClassPathHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class WebModuleClassesInfoAdapter implements ContainerAdapter<WebModuleClassesInfo> {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(WebModuleClassesInfoAdapter.class);

    /*
     * This adapter processes JEE classpath locations..
     *
     * OSGi classpath locations are managed by the WAB installer,
     * and pre-cached to the overlay, to be returned by this adapter
     */

    @Override
    public WebModuleClassesInfo adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        WebModuleClassesInfo classesInfo = (WebModuleClassesInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), WebModuleClassesInfo.class);
        if (classesInfo != null) {
            //either we created this already, or OSGi prepopulated it.
            return classesInfo;
        }

        ArrayList<String> resolved = new ArrayList<String>();
        final List<ContainerInfo> containerInfos = new ArrayList<ContainerInfo>();

        //This should possibly be processing manifest classpath locations, like the old classloader did..
        Entry moduleEntry = containerToAdapt.adapt(Entry.class);
        if (moduleEntry != null && !moduleEntry.getPath().isEmpty()) {
            ManifestClassPathHelper.processMFClasspath(moduleEntry, containerToAdapt, containerInfos, resolved, true);
        }

        Entry classesEntry = containerToAdapt.getEntry("WEB-INF/classes");
        if (classesEntry != null) {
            final Container classesContainer = classesEntry.adapt(Container.class);
            if (classesContainer != null) {
                ContainerInfo containerInfo = new ContainerInfo() {
                    @Override
                    public Type getType() {
                        return Type.WEB_INF_CLASSES;
                    }

                    @Override
                    public String getName() {
                        return "WEB-INF/classes";
                    }

                    @Override
                    public Container getContainer() {
                        return classesContainer;
                    }
                };
                containerInfos.add(containerInfo);
            }
        }

        Entry libEntry = containerToAdapt.getEntry("WEB-INF/lib");
        if (libEntry != null) {
            Container libContainer = libEntry.adapt(Container.class);
            if (libContainer != null) {
                StringBuilder infoNameBuilder = new StringBuilder("WEB-INF/lib/");
                int prefixLength = infoNameBuilder.length();
                for (Entry entry : libContainer) {
                    if (entry.getName().toLowerCase().endsWith(".jar")) {
                        final String jarEntryName = entry.getName();
                        final Container jarContainer = entry.adapt(Container.class);
                        if (jarContainer != null) {
                            infoNameBuilder.setLength(prefixLength);
                            final String infoName = infoNameBuilder.append(jarEntryName).toString();
                            ContainerInfo containerInfo = new ContainerInfo() {
                                @Override
                                public Type getType() {
                                    return Type.WEB_INF_LIB;
                                }

                                @Override
                                public String getName() {
                                    return infoName;
                                }

                                @Override
                                public Container getContainer() {
                                    return jarContainer;
                                }
                            };
                            containerInfos.add(containerInfo);

                            ManifestClassPathHelper.addCompleteJarEntryUrls(containerInfos, entry, jarContainer, resolved);
                        }
                    }
                }
            }
        }

        classesInfo = new WebModuleClassesInfo() {
            @Override
            public List<ContainerInfo> getClassesContainers() {
                return containerInfos;
            }
        };

        rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), WebModuleClassesInfo.class, classesInfo);
        return classesInfo;
    }

}
