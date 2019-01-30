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
package com.ibm.ws.jaxrs.fat.param.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Application extends javax.ws.rs.core.Application {

    Set<Class<?>> classes = new HashSet<Class<?>>();

    public Application() {
        classes = new HashSet<Class<?>>();
        classes.add(MultipleEntityParamsResource.class);
        classes = Collections.unmodifiableSet(classes);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

}
