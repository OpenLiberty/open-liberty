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
 * Contains information about a JEE Module
 */
public interface ModuleInfo {

    /**
     * Returns the unique module name. This will normally be the module name
     * specified in the deployment descriptor (or the URI base name) unless that
     * value would conflict with another module in the same application, in
     * which case a unique name will have been generated and will be returned.
     */
    String getName();

    /**
     * @return the module URI (e.g., "test.jar") as specified in the application
     *         deployment descriptor, or the application basename for a
     *         standalone module
     */
    String getURI();

    /**
     * Returns the container object for this module
     * 
     * @return
     */
    Container getContainer();

    /**
     * Returns the ApplicationInfo for the application that contains this module
     * 
     * @return
     */
    ApplicationInfo getApplicationInfo();

    /**
     * Returns the classloader associated with the module
     * 
     * @return
     */
    ClassLoader getClassLoader();

}
