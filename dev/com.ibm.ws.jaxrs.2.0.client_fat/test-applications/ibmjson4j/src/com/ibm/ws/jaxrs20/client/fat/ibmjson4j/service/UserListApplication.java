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
package com.ibm.ws.jaxrs20.client.fat.ibmjson4j.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.ibm.websphere.jaxrs.providers.json4j.JSON4JArrayProvider;
import com.ibm.websphere.jaxrs.providers.json4j.JSON4JJAXBProvider;
import com.ibm.websphere.jaxrs.providers.json4j.JSON4JObjectProvider;

public class UserListApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(UserListResource.class);
        classes.add(JSON4JArrayProvider.class);
        classes.add(JSON4JObjectProvider.class);
        classes.add(JSON4JJAXBProvider.class);
        return classes;
    }
}
