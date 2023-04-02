/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jbatch.rest.bridge;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;

public class BatchContainerAppNotFoundException extends BatchContainerRuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String appId;
    
    public BatchContainerAppNotFoundException(String appId, String msg, Throwable cause) {
        super(msg, cause);
        this.appId = appId;
    }
    
    public String getAppId() {
        return appId;
    }
}
