/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jackson.internal;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.ibm.ws.jaxrs.fat.jackson.JacksonPOJOResource;
import com.ibm.ws.jaxrs.fat.jackson.Manager;
import com.ibm.ws.jaxrs.fat.jackson.Person;
import com.ibm.ws.jaxrs.fat.jackson.SimplePOJOResource;

/**
 * <code>JacksonPOJOApplication</code> is a {@link javax.ws.rs.core.Application} subclass which contains configuration information for the JAX-RS application.
 * Any JAX-RS resources or providers that are to be used must be returned in
 * either the {@link #getClasses()} or {@link #getSingletons()} methods. Note
 * that providers are always singletons according to the JavaDoc.
 */
public class JacksonPOJOApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(JacksonPOJOResource.class);
        classes.add(SimplePOJOResource.class);
        classes.add(Person.class);
        classes.add(Manager.class);
        return classes;
    }

}
