/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.application;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

import com.ibm.ws.jsf.container.JSFContainer;

public class JSFContainerApplicationFactory extends ApplicationFactory {

    static final String MOJARRA_APP_FACTORY = "com.sun.faces.application.ApplicationFactoryImpl";
    static final String MYFACES_APP_FACTORY = "org.apache.myfaces.application.ApplicationFactoryImpl";

    private ApplicationFactory delegate;
    private volatile boolean initialized = false;

    public JSFContainerApplicationFactory() {
        // TODO: Find a more elegant way to detect which provider is available.
        // Perhaps checking manifest at the time integration jar is applied to app classpath
        try {
            delegate = (ApplicationFactory) Class.forName(MOJARRA_APP_FACTORY).newInstance();
            return;
        } catch (ReflectiveOperationException ignore) {
        }
        try {
            delegate = (ApplicationFactory) Class.forName(MYFACES_APP_FACTORY).newInstance();
        } catch (ReflectiveOperationException ignore) {
        }
        if (delegate == null)
            throw new IllegalStateException("No JSF implementations found.  One of the following ApplicationFactory implementations must be available"
                                            + MOJARRA_APP_FACTORY + " or " + MYFACES_APP_FACTORY);
    }

    @Override
    public Application getApplication() {
        Application a = delegate.getApplication();
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    if (JSFContainer.isCDIEnabled())
                        JSFContainer.initializeCDI(a);
                    if (JSFContainer.isBeanValidationEnabled())
                        JSFContainer.initializeBeanValidation();
                    initialized = true;
                }
            }
        }
        return a;
    }

    @Override
    public void setApplication(Application application) {
        delegate.setApplication(application);
    }

    @Override
    public ApplicationFactory getWrapped() {
        return delegate.getWrapped();
    }
}