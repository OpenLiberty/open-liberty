/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.ejs.j2c.HandleListInterface;
import com.ibm.ws.jca.cm.handle.HandleList;

// TODO still need to port the implementation to Liberty
class HandleListProxy implements HandleListInterface {
    public static final HandleListInterface INSTANCE = null;

    @Override
    public HandleList addHandle(Handle handle) {
        return null;
    }

    @Override
    public void parkHandle() {}

    @Override
    public void reAssociate() {}

    @Override
    public void removeHandle(Object handle) {
    }
}
