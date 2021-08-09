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
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class LifeCycleContextRequestFilter implements ContainerRequestFilter {

    LifeCyclePerson person;

    @Inject
    public void setPerson(LifeCyclePerson person) {
        this.person = person;
        System.out.println("Filter Injection successful...");
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        System.out.println("RequestFilter Inject person: " + person.talk());
    }
}
