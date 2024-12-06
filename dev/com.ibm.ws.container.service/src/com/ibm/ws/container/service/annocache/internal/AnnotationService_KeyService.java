/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
 package com.ibm.ws.container.service.annocache.internal;

import com.ibm.wsspi.anno.service.AppKey;

/**
 * This service provides keys with a 1:1 mapping to each application installed.
 * These keys can be used in a WeakHashMap to ensure the value is garbage collected
 * when the application shuts down
 */
public interface AnnotationService_KeyService {

    /**
     * Gets an AppKey for a given application
     *
     * @param appName the name of the application, this must be the deploymentName from com.ibm.ws.container.service.app.deploy.ApplicationInfo
     * @return An AppKey for the given appName
     */
    public AppKey getKeyForApp(String appName);

}
