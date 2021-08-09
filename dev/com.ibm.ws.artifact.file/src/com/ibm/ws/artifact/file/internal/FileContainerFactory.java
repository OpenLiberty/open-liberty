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
package com.ibm.ws.artifact.file.internal;

import java.io.File;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.artifact.contributor.ArtifactContainerFactoryHelper;
import com.ibm.ws.artifact.file.ContainerFactoryHolder;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

public class FileContainerFactory implements ArtifactContainerFactoryHelper, ContainerFactoryHolder {

    @Override
    public ArtifactContainer createContainer(File cacheDir, Object o) {
        //if it's a File and the File's a dir.. use it =)
        if (o instanceof File && FileUtils.fileIsDirectory((File) o)) {
            return new FileContainer(cacheDir, (File) o, this);
        }
        return null;
    }

    @Override
    public ArtifactContainer createContainer(File cacheDir, ArtifactContainer parent, ArtifactEntry e, Object o) {
        //if it's a File and the File's a dir.. use it =)
        if (o instanceof File && FileUtils.fileIsDirectory((File) o)) {
            return new FileContainer(cacheDir, parent, e, (File) o, this);
        }
        return null;
    }

    private ArtifactContainerFactory containerFactory = null;
    private BundleContext ctx = null;

    protected synchronized void activate(ComponentContext ctx) {
        //need to get this into containers for the notifier.. 
        this.ctx = ctx.getBundleContext();
    }

    protected synchronized void deactivate(ComponentContext ctx) {
        this.containerFactory = null;
        this.ctx = null;
    }

    protected synchronized void setContainerFactory(ArtifactContainerFactory cf) {
        this.containerFactory = cf;
    }

    protected synchronized void unsetContainerFactory(ArtifactContainerFactory cf) {
        if (this.containerFactory == cf) {
            this.containerFactory = null;
        }
    }

    @Override
    public synchronized ArtifactContainerFactory getContainerFactory() {
        if (containerFactory == null) {
            throw new IllegalStateException();
        }
        return containerFactory;
    }

    @Override
    public synchronized BundleContext getBundleContext() {
        if (ctx == null) {
            throw new IllegalStateException();
        }
        return ctx;
    }

}
