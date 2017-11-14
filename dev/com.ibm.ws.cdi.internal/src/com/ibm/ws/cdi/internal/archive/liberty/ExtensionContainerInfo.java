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
package com.ibm.ws.cdi.internal.archive.liberty;

import java.util.Set;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.wsspi.adaptable.module.Container;

public class ExtensionContainerInfo implements ContainerInfo {
    private final Container container;
    private final ClassLoader classLoader;
    private final String containerName;
    private final Set<String> extraClasses;
    private final Set<String> extraBeanDefiningAnnotations;
    private final boolean applicationBDAsVisible;
    private final boolean extClassesOnly;

    ExtensionContainerInfo(Container container, ClassLoader classLoader, String containerName,
                           Set<String> extraClasses, Set<String> extraBeanDefiningAnnotations,
                           boolean applicationBDAsVisible, boolean extClassesOnly) {
        this.container = container;
        this.containerName = containerName;
        this.classLoader = classLoader;
        this.extraClasses = extraClasses;
        this.extraBeanDefiningAnnotations = extraBeanDefiningAnnotations;
        this.applicationBDAsVisible = applicationBDAsVisible;
        this.extClassesOnly = extClassesOnly;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public String getName() {
        return containerName;
    }

    /**
     * Return the extra classes to be present in the bean archive
     *
     * @return the extraClasses
     */
    public Set<String> getExtraClasses() {
        return extraClasses;
    }

    /**
     * Return the set of names of the annotations which define beans.
     *
     * @return the extraBeanDefiningAnnotations
     */
    public Set<String> getExtraBeanDefiningAnnotations() {
        return extraBeanDefiningAnnotations;
    }

    public boolean applicationBDAsVisible() {
        return applicationBDAsVisible;
    }

    /** {@inheritDoc} */
    @Override
    public Type getType() {
        return ContainerInfo.Type.SHARED_LIB;
    }

    /** {@inheritDoc} */
    @Override
    public Container getContainer() {
        return container;
    }

    public boolean isExtClassesOnly() {
        return extClassesOnly;
    }

    @Override
    public String toString() {
        return "ExtensionContainerInfo: " + getName();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof ExtensionContainerInfo)) {
            return false;
        }
        ExtensionContainerInfo otherExtInfo = (ExtensionContainerInfo) other;
        return getName().equals(otherExtInfo.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
