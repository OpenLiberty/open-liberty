/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.service;

import java.io.IOException;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

public interface ServerContent {

    public final static String REQUEST_SERVER_CONTENT_PROPERTY = BootstrapConstants.REQUEST_SERVER_CONTENT_PROPERTY;

    /**
     * Get an array of platform local absolute paths that represent the entire on-disk content of the app server.
     * osRequest can contain requested platform filtering information
     */
    String[] getServerContentPaths(String osRequest) throws IOException;
}
