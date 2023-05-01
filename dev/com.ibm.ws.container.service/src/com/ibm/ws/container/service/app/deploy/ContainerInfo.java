/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.container.service.app.deploy;

import com.ibm.wsspi.adaptable.module.Container;

/**
 * Class path element types. These are the types of elements
 * which may be placed on a class path.
 *
 * Except for {@link ContainerInfo#SHARED_LIB}, the locations are
 * according to usual JavaEE packaging rules.
 *
 * Shared libraries are libraries external to the JavaEE packaging
 * which are added either to the application class path or to module
 * class paths.
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
     * The particular type of this class path element.
     *
     * @return The particular type of this class path element.
     */
    Type getType();

    /**
     * The name of this class path element. Usually, the relative URI
     * of the element.
     *
     * @return
     */
    String getName();

    Container getContainer();
}
