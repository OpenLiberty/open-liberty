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
package com.ibm.ws.cdi.extension.spi.test.bundle;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.spi.test.bundle.annotations.NewBDA;
import com.ibm.ws.cdi.extension.spi.test.bundle.annotations.NewBDATwo;
import com.ibm.ws.cdi.extension.spi.test.bundle.extension.MyExtension;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.interceptor.ClassSPIInterceptor;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.producer.ClassSPIRegisteredProducer;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

@Component(service = CDIExtensionMetadata.class, configurationPolicy = IGNORE)
public class SPIMetaData implements CDIExtensionMetadata {

    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> beans = new HashSet<Class<?>>();
        //This will register a producer class and expose it's produced beans to applications
        beans.add(ClassSPIRegisteredProducer.class);

        //This will register a regular bean which can be injected.
        beans.add(MyBeanInjectionString.class);

        //This will register an intercepter that can be applied to other beans.
        beans.add(ClassSPIInterceptor.class);
        return beans;
    }

    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        Set<Class<? extends Annotation>> BDAs = new HashSet<Class<? extends Annotation>>();
        BDAs.add(NewBDA.class);
        BDAs.add(NewBDATwo.class);
        return BDAs;
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(MyExtension.class);
        return extensions;
    }

}
