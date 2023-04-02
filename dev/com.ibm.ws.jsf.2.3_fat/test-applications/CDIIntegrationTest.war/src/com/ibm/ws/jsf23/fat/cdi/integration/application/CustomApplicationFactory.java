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
import javax.faces.application.ApplicationFactory;

/**
 * Custom Application Factory
 */
public class CustomApplicationFactory extends ApplicationFactory {

    private final ApplicationFactory factory;
    private Application application;

    public CustomApplicationFactory(ApplicationFactory factory) {
        this.factory = factory;
        System.out.println("CustomApplicationFactory was invoked!");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.application.ApplicationFactory#getApplication()
     */
    @Override
    public Application getApplication() {
        if (application == null) {
            application = new CustomApplication(factory.getApplication());
        }
        return application;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.application.ApplicationFactory#setApplication(javax.faces.application.Application)
     */
    @Override
    public void setApplication(Application application) {
        factory.setApplication(application);
    }

}
