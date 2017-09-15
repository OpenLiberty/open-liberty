/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.event;

import java.util.Map;

public interface EventHandle {

    // TODO: Rename this class.

    /**
     * Returns the Map of properties on the associated Event
     * 
     * @return properties on the associated Event
     */
    public Map<String, Object> getProperties();

    /**
     * Attempts to cancel execution of this event.
     * This attempt will fail if the event has already completed,
     * already been canceled, or could not be canceled for some other reason.
     * 
     * @param mayInterruptIfRunning
     *            true if this event should be interrupted
     * 
     * @return false if the event could not be canceled
     */
    public boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns true if this event was canceled before it completed normally.
     * 
     * @return true if event was canceled before it completed
     */
    public boolean isCancelled();

    /**
     * Returns true if this event has completed.
     * 
     * @return true if this event has completed.
     */
    public boolean isDone();

    /**
     * Waits to return until event processing has completed.
     */
    public void waitForCompletion();

}
