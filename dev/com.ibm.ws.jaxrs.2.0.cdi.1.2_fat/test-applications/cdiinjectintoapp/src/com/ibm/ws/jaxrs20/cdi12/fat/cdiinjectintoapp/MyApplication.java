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
package com.ibm.ws.jaxrs20.cdi12.fat.cdiinjectintoapp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

//Test that CDI injects without the @ApplicationPath annotation, use web.xml instead
//@ApplicationPath("/app")
public class MyApplication extends Application {

    @Inject
    InvocationCounter counter;

    @Override
    public Map<String, Object> getProperties() {
        System.out.println("counter=" + counter);
        return Collections.singletonMap("counter", counter);
    }

    InvocationCounter getCounter() {
        return counter;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(MyResource.class);
        return classes;
        
    }
}
