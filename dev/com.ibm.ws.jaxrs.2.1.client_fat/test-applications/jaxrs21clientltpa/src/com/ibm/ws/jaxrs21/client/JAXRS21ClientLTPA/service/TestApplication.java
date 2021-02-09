/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.JAXRS21ClientLTPA.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 *
 */
public class TestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> list = new HashSet<Class<?>>();
        list.add(BasicResource.class);
        return list;
    }

}
