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
package com.ibm.ws.container.service.state;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;

public interface ModuleStateListener {

    /**
     * Notification that a module is starting.
     * 
     * @param moduleInfo The ModuleInfo of the module
     */
    void moduleStarting(ModuleInfo moduleInfo) throws StateChangeException;

    /**
     * Notification that a module has started.
     * 
     * @param moduleInfo The ModuleInfo of the module
     */
    void moduleStarted(ModuleInfo moduleInfo) throws StateChangeException;

    /**
     * Notification that a module is stopping.
     * 
     * @param moduleInfo The ModuleInfo of the module
     */
    void moduleStopping(ModuleInfo moduleInfo);

    /**
     * Notification that a module has stopped.
     * 
     * @param moduleInfo The ModuleInfo of the module
     */
    void moduleStopped(ModuleInfo moduleInfo);
}
