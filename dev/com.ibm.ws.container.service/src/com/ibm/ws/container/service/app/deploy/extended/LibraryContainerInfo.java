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
package com.ibm.ws.container.service.app.deploy.extended;

import com.ibm.ws.container.service.app.deploy.ContainerInfo;

/**
 *
 */
public interface LibraryContainerInfo extends ContainerInfo {

    public enum LibraryType {
        PRIVATE_LIB, COMMON_LIB, GLOBAL_LIB
    }

    /**
     * @return
     */
    public LibraryType getLibraryType();

    /**
     * @return
     */
    public ClassLoader getClassLoader();

}
