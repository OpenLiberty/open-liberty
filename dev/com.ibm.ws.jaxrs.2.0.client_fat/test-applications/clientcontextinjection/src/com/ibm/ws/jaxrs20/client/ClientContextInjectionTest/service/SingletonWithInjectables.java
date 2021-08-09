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
package com.ibm.ws.jaxrs20.client.ClientContextInjectionTest.service;

public class SingletonWithInjectables {
    public SingletonWithInjectables(TSAppConfig config) {
        super();
        this.config = config;
    }

    private final TSAppConfig config;

    public String getInjectedContextValues() {
        return config.getInjectedContextValues();
    }
}
