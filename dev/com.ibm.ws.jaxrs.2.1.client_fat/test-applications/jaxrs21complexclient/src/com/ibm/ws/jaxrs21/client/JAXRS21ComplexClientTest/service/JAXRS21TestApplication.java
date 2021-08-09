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
 package com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.client.JAXRS21MyReader;
import com.ibm.ws.jaxrs21.client.JAXRS21ComplexClientTest.client.JAXRS21MyWriter;

/**
 *
 */
public class JAXRS21TestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> list = new HashSet<Class<?>>();
        list.add(JAXRS21ComplexResource.class);
        list.add(JAXRS21MyResource.class);
        list.add(JAXRS21MyReader.class);
        list.add(JAXRS21MyWriter.class);
        return list;
    }

}
