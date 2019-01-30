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
package com.ibm.ws.jaxrs.fat.servletcoexist4;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/servletcoexist4")
public class DemoApplication extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(UserRestService4.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> objs = new HashSet<Object>();
        objs.add(new GroupRestService4());
        return objs;
    }
}
