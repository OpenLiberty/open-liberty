/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.application.lifecycle;

import java.util.concurrent.Future;

/**
 *
 */
public interface ApplicationRecycleContext {
    /**
     * The name of the application which owns this context
     */
    public String getAppName();

    /**
     * Request a Future that will be completed by the application manager after
     * all of the applications using the components of this context have stopped.
     */
    public Future<Boolean> getAppsStoppedFuture();
}
