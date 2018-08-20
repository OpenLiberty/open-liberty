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

    public ContainerAnnotationsImpl(
    	AnnotationsAdapterImpl annotationsAdapter,
        Container rootContainer, OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
        String appName, String modName, String modCatName) {

    	super(annotationsAdapter,
    		  rootContainer, rootOverlayContainer,
    		  rootArtifactContainer, rootAdaptableContainer,
    		  appName, modName, modCatName);
    }

	@Override
	protected void addInternalToClassSource() {
		ClassSource_Factory classSourceFactory = getClassSourceFactory();
		if ( classSourceFactory == null ) {
			return;
		}

		// Most often, the container is NOT a WEB-INF/classes container, and
		// is used directly.

		Container container = getContainer();
		String containerName = null;
		String containerPrefix = null;

		// When the container IS a WEB-INF/classes container, adjust the container
		// to the module root container and access the container indirectly.
		// Shifting to the module root container enabled the annotations code to
		// locate the JANDEX index of the web module without internally adjusting
		// the location.

		if ( !container.isRoot() ) {
			String containerPath = container.getPath();
			if ( containerPath.equals("/WEB-INF/classes") ) {
				container = container.getEnclosingContainer().getEnclosingContainer();
				containerName = "WEB-INF/classes";
				containerPrefix = "WEB-INF/classes";
			}
		}

		ClassSource_MappedContainer containerClassSource;
		try {
			if ( containerPrefix == null ) {
				containerClassSource = classSourceFactory.createImmediateClassSource(
					classSource, container);
			} else {
				containerClassSource = classSourceFactory.createContainerClassSource(
					classSource, containerName, container, containerPrefix);
			}
			// Both 'createContainerClassSource' throw ClassSource_Exception
		} catch ( ClassSource_Exception e ) {
			return; // FFDC
		}
		classSource.addClassSource(containerClassSource);
	}
}