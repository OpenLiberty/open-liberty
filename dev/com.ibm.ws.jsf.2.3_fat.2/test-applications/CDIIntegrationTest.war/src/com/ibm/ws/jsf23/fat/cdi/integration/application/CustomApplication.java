/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.cdi.integration.application;

import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.application.ViewHandler;

/**
 * Custom application
 */
public class CustomApplication extends ApplicationWrapper {

    private final Application delegate;

    /**
     * Constructor that wraps an {@link Application} instance.
     *
     * @param wrapped The {@link Appplication} to be wrapped.
     */
    public CustomApplication(Application delegate) {
        this.delegate = delegate;
        System.out.println("CustomApplication was invoked!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Application getWrapped() {
        return delegate;
    }

    @Override
    public void setViewHandler(ViewHandler viewHandler) {
        System.out.println("CustomApplication setViewHandler: " + viewHandler);
        delegate.setViewHandler(viewHandler);
    }
}
