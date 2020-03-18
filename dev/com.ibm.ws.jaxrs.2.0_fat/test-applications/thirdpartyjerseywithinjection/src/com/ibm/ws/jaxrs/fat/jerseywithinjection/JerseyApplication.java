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
package com.ibm.ws.jaxrs.fat.jerseywithinjection;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("app5")
public class JerseyApplication extends Application {

    /* will be specified as an init-param in web.xml */

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(MyResource.class);
        classes.add(MyResource2.class);
        classes.add(MyFilter.class);
        classes.add(MyFilter2.class);
        classes.add(TestEntity.class);
        classes.add(TestEntity2.class);
        classes.add(TestEntityMessageBodyReader.class);
        classes.add(TestEntityMessageBodyWriter.class);
        classes.add(TestEntityMessageBodyReader2.class);
        classes.add(TestEntityMessageBodyWriter2.class);
        return classes;
    }
}
