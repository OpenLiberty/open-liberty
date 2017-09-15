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

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;

/**
 * Service for firing deployed info events to listeners.
 */
public interface StateChangeService {

    void fireApplicationStarting(ApplicationInfo info) throws StateChangeException;

    void fireApplicationStarted(ApplicationInfo info) throws StateChangeException;

    void fireApplicationStopping(ApplicationInfo info);

    void fireApplicationStopped(ApplicationInfo info);

    void fireModuleStarting(ModuleInfo info) throws StateChangeException;

    void fireModuleStarted(ModuleInfo info) throws StateChangeException;

    void fireModuleStopping(ModuleInfo info);

    void fireModuleStopped(ModuleInfo info);
}
