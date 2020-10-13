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
package io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.application.pojosingleton;

import java.util.HashSet;
import java.util.Set;

import io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.CDIFilter;
import io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.HelloResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
public class POJOSingletonApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HelloResource.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(new CDIFilter());
        return singletons;
    }
}
