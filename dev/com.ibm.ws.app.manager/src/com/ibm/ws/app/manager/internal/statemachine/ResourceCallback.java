/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.statemachine;

import java.io.File;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.location.WsResource;

interface ResourceCallback {
    /**
     * An indication that an attempt was made to access the resource, and it
     * is not currently available, but it will be monitored for availability.
     * This method may be called multiple times.
     */
    void pending();

    void successfulCompletion(Container c, WsResource r);

    void failedCompletion(Throwable t);

    Container setupContainer(String _servicePid, File downloadedFile);
}
