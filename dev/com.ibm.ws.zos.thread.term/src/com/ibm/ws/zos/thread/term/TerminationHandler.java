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
 * Service which is notified when a Thread terminates. This allows the service
 * the opportunity to clean up any native resources held by that Thread.
 */
public interface TerminationHandler {
    /**
     * Called when a Thread terminates.
     *
     * @param thread The thread which has terminated.
     */
    public void threadTerminated(Thread thread);
}
