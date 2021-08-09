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
 * Contains information about an enterprise application
 */
public interface EARApplicationInfo extends ApplicationInfo {

    /**
     * Returns the container for the library directory
     * 
     * @return
     */
    Container getLibraryDirectoryContainer();

    /**
     * Returns the application classloader
     * 
     * @return
     */
    ClassLoader getApplicationClassLoader();
}
