/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy;

import java.util.List;

/**
 *
 */
public interface ApplicationClassesContainerInfo {
    /**
     * Get the ContainerInfo for all of the classes directly or indirectly available
     * through the application library directory jars.
     * 
     * Note that this does not include the container infos for any of the EJB modules
     * in the application. That information is in the ModuleClassesContainerInfo
     * for those individual modules and is used when creating the Classloader for
     * the application.
     * 
     * @return The application library directory classes container infos
     */
    public List<ContainerInfo> getLibraryClassesContainerInfo();

    /**
     * Get the classes container info for each of the modules defined in this application.
     * 
     * @return
     */
    public List<ModuleClassesContainerInfo> getModuleClassesContainerInfo();
}
