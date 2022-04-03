/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import org.jboss.weld.bootstrap.BeanDeploymentModule;
import org.jboss.weld.bootstrap.api.Environment;

/**
 *
 */
public interface CDIContainerEventManager {

    public void fireStartupEvent(BeanDeploymentModule module);

    public void fireShutdownEvent(BeanDeploymentModule module);

    /**
     * Not sure if this is the correct place for this method but it is only
     * ever needed if this service is also being used to fire the events.
     * Don't really want to create yet another separate services.
     *
     * @return the Weld Environment to use
     */
    public Environment getEnvironment();
}
