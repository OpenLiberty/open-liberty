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
package com.ibm.ws.jaxrs.fat.getCgetS.server;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class TestApplication1 extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<Class<?>>();
        set.add(TestResource1.class);
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> set = new LinkedHashSet<Object>();
        set.add(new TestResource1(1580149));
        set.add(new TestResource1(1348869));
        return set;
    }
}
