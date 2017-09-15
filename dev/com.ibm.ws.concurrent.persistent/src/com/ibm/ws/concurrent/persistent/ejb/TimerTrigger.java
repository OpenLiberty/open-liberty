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
package com.ibm.ws.concurrent.persistent.ejb;

import java.io.Serializable;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.Trigger;

/**
 * Serializable Callable or Runnable that is also a ManagedTask and Trigger
 * for a persistent EJB timer.
 */
public interface TimerTrigger extends ManagedTask, Serializable, Trigger {
    /**
     * Returns the name of the application that schedules the timer.
     * Null may be returned after the task has been deserialized.
     * 
     * @return the name of the application that schedules the timer.
     */
    String getAppName();

    /**
     * Returns the class loader of the EJB that schedules the timer.
     * Null may be returned after the task has been deserialized.
     * 
     * @return the class loader of the EJB that schedules the timer.
     */
    ClassLoader getClassLoader();
}
