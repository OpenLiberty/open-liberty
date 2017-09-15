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

import java.io.IOException;

import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Snapshot of status for a persistent EJB timer.
 * 
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public interface TimerStatus<T> extends TaskStatus<T> {
    /**
     * Returns the serializable timer task/trigger for the EJB timer task.
     * The state of the timer task/trigger is a snapshot of the point in time
     * when the <code>TimerStatus</code> instance was captured.
     * Each invocation of this method causes a new copy to be deserialized.
     * 
     * @return the task/trigger (if any) for the EJB timer task with the specified id.
     * @throws ClassNotFoundException if the class of a serialized object cannot be found.
     * @throws IOException if an error occurs during deserialization of the task/trigger.
     */
    TimerTrigger getTimer() throws ClassNotFoundException, IOException;
}
