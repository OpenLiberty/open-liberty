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
package io.openliberty.restfulWS30.cdi30.fat.basic;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

public class ContextRequestFilter implements ContainerRequestFilter {

    Person person;

    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println("Filter Injection successful...");
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        System.out.println("RequestFilter Inject person: " + person.talk());
    }
}
