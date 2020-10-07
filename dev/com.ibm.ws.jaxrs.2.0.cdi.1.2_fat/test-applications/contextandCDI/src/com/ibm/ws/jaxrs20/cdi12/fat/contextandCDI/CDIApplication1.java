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
package com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


@ApplicationPath("contextandCDI1")
public class CDIApplication1 extends Application {

    @Override
    public Set<Class<?>> getClasses() {

        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(TestResource.class);
        classes.add(TestResource2.class);
        classes.add(TestResource3.class);
        classes.add(TestResource4.class);
        return classes;
        
    }
    
    @Override
    public Set<Object> getSingletons() {
       
        LinkedHashSet<Object> classes = new LinkedHashSet<>();
        CDIFilter filter = CDIUtils.getBean(CDIFilter.class);
        classes.add(filter);
        CDIFilter2 filter2 = CDIUtils.getBean(CDIFilter2.class);
        classes.add(filter2);
        // @Dependent scope providers call @PostConstruct method to be called twice.  https://github.com/OpenLiberty/open-liberty/issues/10633 
        // CDIFilter3 filter3 = getBean(CDIFilter3.class);
        // classes.add(filter3);
        CDIFilter4 filter4 = CDIUtils.getBean(CDIFilter4.class);
        classes.add(filter4);
        return classes;
    }
}

