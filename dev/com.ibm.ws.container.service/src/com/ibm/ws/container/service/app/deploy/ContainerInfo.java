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
package com.ibm.ws.container.service.app.deploy;

import com.ibm.wsspi.adaptable.module.Container;

/**
 * Contains information about a container
 */
public interface ContainerInfo {
    public enum Type {
        MANIFEST_CLASSPATH,
        WEB_INF_CLASSES,
        WEB_INF_LIB,
        EAR_LIB,
        WEB_MODULE,
        EJB_MODULE,
        CLIENT_MODULE,
        RAR_MODULE,
        JAR_MODULE,
        SHARED_LIB
    }

    /**
     * Returns the container type
     */
    public Type getType();

    /**
     * Returns the container name
     */
    public String getName();

    /**
     * Returns the container object
     */
    public Container getContainer();
}
