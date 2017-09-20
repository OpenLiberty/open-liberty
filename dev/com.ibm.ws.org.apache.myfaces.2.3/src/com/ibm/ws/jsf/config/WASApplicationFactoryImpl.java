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
package com.ibm.ws.jsf.config;

import javax.faces.application.Application;

import org.apache.myfaces.application.ApplicationFactoryImpl;

import com.ibm.ws.jsf.extprocessor.JSFExtensionFactory;

/**
 * WAS custom application factory that initializes CDI per application
 */
public class WASApplicationFactoryImpl extends ApplicationFactoryImpl {
    
    private volatile boolean initialized = false;

    @Override
    public Application getApplication() {
        Application app = super.getApplication();
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    JSFExtensionFactory.initializeCDI(app);
                    initialized = true;
                }
            }
        }
        return app;
    }

}
