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
package com.ibm.ws.kernel.launch.service;

public interface FrameworkReady {
    /**
     * Waits for a framework service to finish starting. After initial bundle
     * provisioning, the kernel will call this method for all registered
     * services before it considers the framework to be "ready".
     * 
     * @throws InterruptedException
     */
    void waitForFrameworkReady() throws InterruptedException;
}
