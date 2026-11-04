/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.thread;

/**
 * Interface definition to encapsulate JVMTI thread callback functions.
 */
public interface ThreadLifecycleEventListener {

    /**
     * Notification that the current thread is about to enter its run method.
     */
    public void threadStarted();

    /**
     * Notification that the current thread has returned from its run method.
     */
    public void threadTerminating();

}
