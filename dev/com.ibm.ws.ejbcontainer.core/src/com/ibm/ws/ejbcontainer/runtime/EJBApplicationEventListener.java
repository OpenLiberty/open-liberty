/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.runtime;

/**
 * This interface is used internally by the EJB runtime to notify listeners
 * for a module when their containing application has started or stopped.
 */
public interface EJBApplicationEventListener
{
    /**
     * This method is invoked when the EJB runtime indicates an application has
     * finished starting. If the module for this listener was started as part
     * of a fine-grained application update, then this callback will be called
     * at the end of module start.
     */
    void applicationStarted(String appName);

    /**
     * This method is invoked when the EJB runtime indicates an application is
     * about to begin stopping. This event occurs prior to any modules in the
     * application actually being stopped.
     * 
     * <p>If the module for this listener is being stopped as part of a
     * fine-grained application update, then this callback will be called at the
     * beginning of module stop rather than application stop.
     */
    void applicationStopping(String appName);
}
