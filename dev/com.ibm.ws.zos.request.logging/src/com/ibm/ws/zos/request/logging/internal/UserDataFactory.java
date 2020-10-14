/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging.internal;

import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 *
 */
public class UserDataFactory implements ResourceFactory {
    /**
     * Reference to related WorkManagerService
     */
    private final UserDataImpl userDataImpl;

    /**
     * Constructor
     */
    UserDataFactory(UserDataImpl udi) {
        userDataImpl = udi;
    }

    /** {@inheritDoc} */
    @Override
    public Object createResource(ResourceInfo ref) throws Exception {
        return userDataImpl;
    }

}
