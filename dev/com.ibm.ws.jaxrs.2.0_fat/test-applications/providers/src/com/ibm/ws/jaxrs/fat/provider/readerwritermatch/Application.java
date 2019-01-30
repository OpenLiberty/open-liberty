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
package com.ibm.ws.jaxrs.fat.provider.readerwritermatch;

import java.util.HashSet;
import java.util.Set;

public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> clazzes = new HashSet<Class<?>>();
        clazzes.add(Resource.class);
        return clazzes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(new ApplicationProvider());
        singletons.add(new ApplicationCharProvider());
        singletons.add(new ApplicationJaxbProvider());
        singletons.add(new JaxbContextProvider());
        return singletons;

    }

}
