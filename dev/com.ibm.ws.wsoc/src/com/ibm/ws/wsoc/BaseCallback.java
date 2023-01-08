/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.wsoc;

import com.ibm.ws.wsoc.injection.InjectionThings;

/**
 * common methods both the Read and Write Callback can use
 */
public class BaseCallback {

    WsocConnLink connLink = null;

    InjectionThings it = null;

    public void setConnLinkCallback(WsocConnLink _link) {
        connLink = _link;
    }

    protected ClassLoader pushContexts() {

        it = connLink.pushContexts();

        return it.getOriginalCL();
    }

    protected void popContexts(ClassLoader originalCL) {

        connLink.popContexts(it);
    }
}
