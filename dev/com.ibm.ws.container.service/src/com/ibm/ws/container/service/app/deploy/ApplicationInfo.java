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
 * Contains information about a JEE application
 */
public interface ApplicationInfo {

    /**
     * Returns the unique application name. This will normally be the name
     * specified in the deployment descriptor (or the URI base name) unless
     * that value would conflict with another application, in which case a
     * unique name will have been generated and will be returned.
     */
    String getName();

    /**
     * Returns the Container object associated with this application
     *
     */
    Container getContainer();

    /**
     * Returns the unique deployment name for an application.
     */
    String getDeploymentName();

    /**
     * Returns an instance of NestedConfigHelper that can be used to obtain
     * application properties
     *
     */
    NestedConfigHelper getConfigHelper();

    /**
     * This indicates whether Jandex annotation indexes supplied in the application are to be used.
     *
     * @return
     */
    boolean getUseJandex();

}
