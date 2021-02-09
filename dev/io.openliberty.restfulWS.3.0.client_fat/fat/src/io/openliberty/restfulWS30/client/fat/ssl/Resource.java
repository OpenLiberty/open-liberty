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
package io.openliberty.restfulWS30.client.fat.ssl;

import java.util.Collections;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/hello")
@Path("/secure")
@Produces("text/plain")
@ApplicationScoped
public class Resource extends Application {

    @PostConstruct
    public void init() {
        System.out.println("Resource.init()");
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(this.getClass());
    }

    @GET
    public String get() {
        return "Hello secure world!";
    }
}
