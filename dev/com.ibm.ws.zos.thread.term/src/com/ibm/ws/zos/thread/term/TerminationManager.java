/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.thread.term;

/**
 * Service which marks threads as eligible for termination notification, and
 * which notifies termination handlers of thread termination.
 */
public interface TerminationManager {
    /**
     * Marks the input thread as eligible for termination notification. All
     * registered listeners will be notified when this Thread terminates.
     */
    public void registerCurrentThread();
}
