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
package com.ibm.ws.concurrent.persistent.controller;

/**
 * Interface for a persistent executor to talk to a controller.
 */
public interface Controller {
    /**
     * Returns the id of a partition for a persistent executor instance that is active and able to run tasks.
     * 
     * @return a partition id.
     */
    Long getActivePartitionId();

    /**
     * Notifies the controller that another persistent executor instance has been assigned a task.
     */
    void notifyOfTaskAssignment(long partitionId, long newTaskId, long expectedExecTime, short binaryFlags, int transactionTimeout);
}
