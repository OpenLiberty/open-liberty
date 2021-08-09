/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.fat.injectAppViaContext;


import java.util.Collections;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Providers;

@ApplicationScoped
public class MyApp extends Application {

    private static int _counter = 0;
    int instanceId = _counter++;

    private MyAppAccessor accessor = new MyAppAccessor(this);

    @Context
    private Providers providers;

    boolean providersInjectedBeforePostConstruct;
    
    @PostConstruct
    protected void postConstruct() {
        providersInjectedBeforePostConstruct = providers != null;
    }

    @Override
    public Set<Object> getSingletons() {
        return Collections.singleton(accessor);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(MyResource.class);
    }
}
