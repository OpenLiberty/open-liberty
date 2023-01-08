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
package io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.application.pojosingleton;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.CDIFilter;
import io.openliberty.restfulWS30.cdi30.fat.applicationsingleton.HelloResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
public class POJOSingletonApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(HelloResource.class);
    }

    @Override
    public Set<Object> getSingletons() {
        return Collections.singleton(new CDIFilter());
    }
}
