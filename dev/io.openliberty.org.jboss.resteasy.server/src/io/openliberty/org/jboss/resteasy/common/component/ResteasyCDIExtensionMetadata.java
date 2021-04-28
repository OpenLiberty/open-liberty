/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.component;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIService;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.org.jboss.resteasy.common.cdi.LibertyCdiInjectorFactory;

@Component(service = CDIExtensionMetadata.class,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true,
    property = { "service.vendor=IBM" })
public class ResteasyCDIExtensionMetadata implements CDIExtensionMetadata {

    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(javax.ws.rs.core.Application.class);
        return classes;
    }

    @Override
    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        Set<Class<? extends Annotation>> classes = new HashSet<Class<? extends Annotation>>();
        classes.add(javax.ws.rs.ApplicationPath.class);
        classes.add(javax.ws.rs.Path.class);
        classes.add(javax.ws.rs.ext.Provider.class);
        classes.add(javax.annotation.ManagedBean.class);
        return classes;
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(LibertyResteasyCdiExtension.class);
        return extensions;
    }

    @Reference
    protected void setCdiService(CDIService cdiService) {
        LibertyCdiInjectorFactory.cdiService = cdiService;
    }

    protected void unsetCdiService(CDIService cdiService) {
        LibertyCdiInjectorFactory.cdiService = null;
    }
}
