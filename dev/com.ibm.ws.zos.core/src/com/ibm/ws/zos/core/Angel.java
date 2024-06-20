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
package com.ibm.ws.zos.core;

/**
 * Marker interface that represents the angel process, if we're connected to it.
 */
public interface Angel {
    /**
     * Service property that indicates the version of the DRM when this service
     * was registered.
     */
    public final static String ANGEL_DRM_VERSION = "angel.drm.version";

    /**
     * Service property that indicates the name of the angel. If the default angel
     * is used then this property is omitted.
     */
    public final static String ANGEL_NAME = "angel.name";

    /**
     * Get the version of the DRM the angel was using when this service was registered.
     */
    public int getDRM_Version();

    /**
     * Get the name of the angel.
     *
     * @return The name of the angel, or null if the default angel.
     */
    public String getName();
}
