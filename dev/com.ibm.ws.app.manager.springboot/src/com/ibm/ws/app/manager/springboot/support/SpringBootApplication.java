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
package com.ibm.ws.app.manager.springboot.support;

import java.io.IOException;

import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory.Instance;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * A Spring Boot Application object. This object provides functions for
 * {@link ContainerInstanceFactory}s to use when creating new {@link Instance} of an
 * embedded container.
 */
public interface SpringBootApplication {
    /**
     * Creates a new container that is bound to the giving virtual
     * host id.
     *
     * @param id the virtual host id to bind to.
     * @return a container that is bound to a virtual host
     * @throws IOException
     * @throws UnableToAdaptException
     */
    Container createContainerFor(String id) throws IOException, UnableToAdaptException;

    /**
     * Returns the ModuleClassesContainerInfo for the applications class loader.
     *
     * @return the ModuleClassesContainerInfo
     */
    ModuleClassesContainerInfo getSpringClassesContainerInfo();

    /**
     * The class loader for the application
     *
     * @return the class loader
     */
    ClassLoader getClassLoader();

    /**
     * Creates an application info for the specified id and container
     *
     * @param id           the id for the application
     * @param appContainer the container for the application
     * @return
     */
    ExtendedApplicationInfo createApplicationInfo(String id, Container appContainer);

    /**
     * Destroys the application info.
     *
     * @param appInfo the application info to destroy
     */
    void destroyApplicationInfo(ExtendedApplicationInfo appInfo);
}
