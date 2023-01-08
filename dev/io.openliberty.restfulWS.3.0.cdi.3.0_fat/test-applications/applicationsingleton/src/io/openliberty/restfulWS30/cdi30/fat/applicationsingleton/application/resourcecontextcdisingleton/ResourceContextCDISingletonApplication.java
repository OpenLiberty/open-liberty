/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.application.resourcecontextcdisingleton;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.CDIFilter;
import io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.HelloResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;

@ApplicationPath("/")
public class ResourceContextCDISingletonApplication extends Application {

    @Context
    ResourceContext resourceContext;
    
    @Inject
    private CDIFilter cdiFilter;
    
    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(HelloResource.class);
    }

    @Override
    public Set<Object> getSingletons() {
        return Collections.singleton(resourceContext.initResource(cdiFilter));
    }
}
