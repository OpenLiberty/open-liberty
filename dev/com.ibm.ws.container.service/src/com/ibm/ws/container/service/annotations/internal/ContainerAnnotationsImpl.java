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
package com.ibm.ws.container.service.annotations.internal;

import com.ibm.ws.container.service.annotations.ContainerAnnotations;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_MappedContainer;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class ContainerAnnotationsImpl extends AnnotationsImpl implements ContainerAnnotations {
    // private final String CLASS_NAME = "ContainerAnnotationsImpl";

    public ContainerAnnotationsImpl(
        AnnotationsAdapterImpl annotationsAdapter,
        Container rootContainer, OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
        String appName, String modName, String modCatName) {

        super(annotationsAdapter,
              rootContainer, rootOverlayContainer,
              rootArtifactContainer, rootAdaptableContainer,
              appName, modName, modCatName);

        this.entryPrefix = null;
    }

    //

    private String entryPrefix;

    @Override
    public String getEntryPrefix() {
        return entryPrefix;
    }

    @Override
    public void setEntryPrefix(String entryPrefix) {
        this.entryPrefix = entryPrefix;
    }

    //

    @Override
    protected void addInternalToClassSource() {
        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if ( classSourceFactory == null ) {
            return;
        }

        // When the target is WEB-INF/classes, the container must be
        // set to the root container and the entry prefix must be set to
        // "WEB-INF/classes".

        Container useContainer = getContainer();

        String useContainerName = getModName();
        if ( useContainerName == null ) {
            useContainerName = getAppName();
            if ( useContainerName == null ) {
                useContainerName = "unused";
            }
        }

        String useContainerPrefix = getEntryPrefix();

        ClassSource_MappedContainer containerClassSource;
        try {
            containerClassSource = classSourceFactory.createContainerClassSource(
                rootClassSource, useContainerName, useContainer, useContainerPrefix);
            // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            return; // FFDC
        }

        rootClassSource.addClassSource(containerClassSource);
    }
}
