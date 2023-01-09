/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.internal.interfaces;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.weld.bootstrap.BeanDeploymentModule;
import org.jboss.weld.bootstrap.api.Environment;

/**
 *
 */
public interface CDIContainerEventManager {

    /**
     * Fire a CDI Startup Event for the given module
     *
     * @param module The module to fire the event for
     */
    public void fireStartupEvent(BeanDeploymentModule module);

    /**
     * Fire a CDI Shutdown Event for the given module
     *
     * @param module The module to fire the event for
     */
    public void fireShutdownEvent(BeanDeploymentModule module);

    /**
     * Not sure if this is the correct place for this method but it is only
     * ever needed if this service is also being used to fire the events.
     * Don't really want to create yet another separate service.
     *
     * Get the Weld Environment to use. The environment should be based on Environments.EE.
     *
     * @return the Weld Environment to use
     */
    public Environment getEnvironment();

    /**
     * Not sure if this is the correct place for this method but don't really want to create yet another separate service.
     *
     * This extension point allows us to perform additional operations when a DeploymentException is caught. In most cases
     * it is expected that the implementation will do something and then just return that same exception... but it could be
     * a modified instance of the exception
     */
    public DeploymentException processDeploymentException(WebSphereCDIDeployment deployment, DeploymentException e);
}
