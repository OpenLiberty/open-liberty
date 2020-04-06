/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi20;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ibm.ws.microprofile.faulttolerance.spi.AsyncRequestContextController;

@Dependent
public class AsyncRequestContextControllerImpl implements AsyncRequestContextController {

    @Inject
    private Instance<RequestContextController> requestContextControllerInstance;

    @Override
    public ActivatedContext activateContext() {
        RequestContextController requestContextController = requestContextControllerInstance.get();
        requestContextController.activate();
        return new ActivatedContextImpl(requestContextController);
    }

    private static class ActivatedContextImpl implements ActivatedContext {

        private final RequestContextController requestContextController;

        public ActivatedContextImpl(RequestContextController requestContextController) {
            this.requestContextController = requestContextController;
        }

        @Override
        public void deactivate() {
            try {
                requestContextController.deactivate();
            } catch (ContextNotActiveException e) {
                // If the application is shut down during the execution, the context may have already been deactivated
            }
        }

    }

}
