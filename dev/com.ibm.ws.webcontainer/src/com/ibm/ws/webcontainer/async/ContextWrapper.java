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
package com.ibm.ws.webcontainer.async;

/**
 * Added for PM90834.
 * Wrap a runnable in the context data from the ServiceWrapper.
 */
public class ContextWrapper implements Runnable {

    private ServiceWrapper serviceWrapper;
    private Runnable runnable;

    ContextWrapper(Runnable runnable, ServiceWrapper serviceWrapper) {
        this.serviceWrapper = serviceWrapper;
        this.runnable = runnable;
    }

    public void run() {
        serviceWrapper.wrapAndRun(this.runnable);
    }

}
