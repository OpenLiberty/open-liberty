/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi20;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.RequestContextController;
import javax.inject.Inject;

import com.ibm.ws.microprofile.faulttolerance.spi.AsyncRequestContextController;

@ApplicationScoped
public class AsyncRequestContextControllerImpl implements AsyncRequestContextController {

    private final RequestContextController requestContextController;

    public AsyncRequestContextControllerImpl() {
        // No-arg constructor required for bean
        requestContextController = null;
    }

    @Inject
    public AsyncRequestContextControllerImpl(RequestContextController requestContextController) {
        this.requestContextController = requestContextController;
    }

    @Override
    public void activateContext() {
        requestContextController.activate();
    }

    @Override
    public void deactivateContext() {
        requestContextController.deactivate();
    }

}
