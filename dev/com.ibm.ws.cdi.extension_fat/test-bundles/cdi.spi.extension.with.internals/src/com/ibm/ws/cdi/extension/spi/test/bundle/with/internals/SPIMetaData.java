/*******************************************************************************
 * Copyright (c) 2020. 2023 IBM Corporation and others.
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
package com.ibm.ws.cdi.extension.spi.test.bundle.with.internals;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.annotations.NewBDA;
import com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.annotations.NewBDATwo;
import com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.buildcompatible.BCExtension;
import com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.extension.MyExtension;
import com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.getclass.beaninjection.MyBeanInjectionString;
import com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.getclass.interceptor.ClassSPIInterceptor;
import com.ibm.ws.cdi.extension.spi.test.bundle.with.internals.getclass.producer.ClassSPIRegisteredProducer;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.cdi40.internal.extension.CDI40ExtensionMetadataInternal;

@Component(service = CDIExtensionMetadata.class, configurationPolicy = IGNORE)
public class SPIMetaData implements CDIExtensionMetadata, CDI40ExtensionMetadataInternal {

    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> beans = new HashSet<Class<?>>();
        //This will register a producer class and expose it's produced beans to applications
        beans.add(ClassSPIRegisteredProducer.class);

        //This will register a regular bean which can be injected.
        beans.add(MyBeanInjectionString.class);

        //This will register an intercepter that can be applied to other beans.
        beans.add(ClassSPIInterceptor.class);

        //Now repeat the whole thing with duplicate classes from another bundle.
        beans.add(com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.producer.ClassSPIRegisteredProducer.class);
        beans.add(com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.beaninjection.MyBeanInjectionString.class);
        beans.add(com.ibm.ws.cdi.misplaced.spi.test.bundle.getclass.interceptor.ClassSPIInterceptor.class);

        return beans;
    }

    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        Set<Class<? extends Annotation>> BDAs = new HashSet<Class<? extends Annotation>>();
        BDAs.add(NewBDA.class);
        BDAs.add(NewBDATwo.class);

        //Now repeat the whole thing with duplicate classes from another bundle.
        BDAs.add(com.ibm.ws.cdi.misplaced.spi.test.bundle.annotations.NewBDA.class);
        BDAs.add(com.ibm.ws.cdi.misplaced.spi.test.bundle.annotations.NewBDATwo.class);

        return BDAs;
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(MyExtension.class);

        //Now repeat the whole thing with duplicate classes from another bundle.
        extensions.add(com.ibm.ws.cdi.misplaced.spi.test.bundle.extension.MyExtension.class);

        return extensions;
    }

    @Override
    public Set<Class<? extends BuildCompatibleExtension>> getBuildCompatibleExtensions() {
        Set<Class<? extends BuildCompatibleExtension>> extensions = new HashSet<Class<? extends BuildCompatibleExtension>>();
        extensions.add(BCExtension.class);
        return extensions;
    }

}
