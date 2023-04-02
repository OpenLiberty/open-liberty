/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.restfulWS30.fat.restfulwsprototype;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
public class RestfulWsPrototypeApp extends Application {


//  @Override
//  public Set<Class<?>> getClasses() {
//      Set<Class<?>> classes = new HashSet<Class<?>>();
//      classes.add(Resource.class);
//      return classes;
//      return Collections.singleton(Resource.class);
//  }
//
//  @Override
//  public Map<String, Object> getProperties() {
//      Map<String, Object> properties = new HashMap<String, Object>();
//      return properties;
//      return Collections.singleton(props);
//  }
//
//  @Override
//  public Set<Object> getSingletons() {
//      Set<Object> singletons = new HashSet<Object>();
//      singletons.add(new Resource());
//      return singletons;
//      return Collections.singleton(new Resource());
//  }
}
