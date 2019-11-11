/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache;

import com.ibm.ws.container.service.annocache.ContainerAnnotations;

/**
 * Annotations data for a non-module CDI container.
 */

// Used by:
//
// com.ibm.ws.cdi.internal/src/com/ibm/ws/cdi/internal/archive/liberty/CDIArchiveImpl.java
// -- used to detect annotations on a specified container
// -- the container is either the immediate container of the CDI archive,
//    or is the WEB-INF/classes folder when the archive is a web module

/**
 * Annotations data for a single container.
 *
 * CDI container annotations
 *
 * Used for general annotations discovery on CDI archives. 
 */
public interface CDIContainerAnnotations extends ContainerAnnotations {
    // EMPTY
}
