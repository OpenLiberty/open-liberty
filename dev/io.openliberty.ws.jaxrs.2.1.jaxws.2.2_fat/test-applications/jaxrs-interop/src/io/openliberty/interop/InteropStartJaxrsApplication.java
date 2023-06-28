/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.interop;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/")
public class InteropStartJaxrsApplication extends Application {

//    @Override
//    public Set<Class<?>> getClasses() {
//        Set<Class<?>> classes = new HashSet<Class<?>>();
//        classes.add(PrototypeResource.class);
//        return classes;
//        return Collections.singleton(PrototypeResource.class);
//    }
//
//    @Override
//    public Map<String, Object> getProperties() {
//        Map<String, Object> properties = new HashMap<String, Object>();
//        return properties;
//        return Collections.singleton(props);
//    }
//
//    @Override
//    public Set<Object> getSingletons() {
//        Set<Object> singletons = new HashSet<Object>();
//        singletons.add(new PrototypeResource());
//        return singletons;
//        return Collections.singleton(new PrototypeResource());
//    }
}