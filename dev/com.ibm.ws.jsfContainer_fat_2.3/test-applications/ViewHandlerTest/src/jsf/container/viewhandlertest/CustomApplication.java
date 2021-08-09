/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsf.container.viewhandlertest;

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
