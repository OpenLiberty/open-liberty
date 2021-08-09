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
package com.ibm.ws.webcontainer.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo.Type;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class MetaInfResourceFinder {

    private final List<Container> jarResourceContainers;

    private final Container container;

    public MetaInfResourceFinder(Container container) {
        this.container = container;
        jarResourceContainers = new ArrayList<Container>();

        try {
            WebModuleClassesInfo classesInfo = this.container.adapt(WebModuleClassesInfo.class);
            if (classesInfo != null) {
                List<ContainerInfo> containerInfos = classesInfo.getClassesContainers();
                for (ContainerInfo containerInfo : containerInfos) {
                    if (containerInfo.getType() == Type.WEB_INF_LIB) {
                        jarResourceContainers.add(containerInfo.getContainer());
                    }
                }
            }
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }
    }

    public Entry findResourceInModule(String path, boolean searchLibJars) {
        return findResourceInModule(path, searchLibJars, false);
    }

    public Entry findResourceInModule(String path, boolean searchLibJars, boolean searchLibJarsOnly) {

        if (!searchLibJarsOnly) {
            //Search the META-INF resource dir of the war module
            Entry resource = container.getEntry(path);

            //If we find the resource in the root, return it without further search.
            if (resource != null) {
                return resource;
            }
        }

        if (searchLibJars) {
            //Search any lib jars
            for (Container jarContainer : jarResourceContainers) {
                if (jarContainer != null) {
                    Entry jarResource = jarContainer.getEntry("META-INF/resources" + path);
                    if (jarResource != null) {
                        return jarResource;
                    }
                }
            }
        }

        return null;
    }

    public Entry findResourceInModule(String path) {
        return findResourceInModule(path, true);
    }

    public List<Container> getJarResourceContainers() {
        return jarResourceContainers;
    }

    public List<URL> getJarResourceURLs() {
        List<URL> urls = new ArrayList<URL>();
        for (Container c : jarResourceContainers) {
            try {
                Entry entry = c.adapt(Entry.class);
                if (entry != null) {
                    Container container = entry.adapt(Container.class);
                    if (container != null) {
                        Collection<URL> cUrls = container.getURLs();
                        urls.addAll(cUrls);
                    }
                }
            } catch (UnableToAdaptException e) {
                throw new IllegalStateException(e);
            }
        }
        return urls;
    }
}
