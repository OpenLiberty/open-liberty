/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package io.openliberty.org.jboss.resteasy.common.component;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;
import javax.annotation.ManagedBean;
import javax.ws.rs.core.Application;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIService;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import io.openliberty.org.jboss.resteasy.common.cdi.LibertyCdiInjectorFactory;

@Component(service = CDIExtensionMetadata.class,
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true)
public class ResteasyCDIExtensionMetadata implements CDIExtensionMetadata {
    
    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(LibertyResteasyCdiExtension.class);
        return extensions;
    }
    
    @Override
    public Set<Class<? extends Annotation>> getBeanDefiningAnnotationClasses() {
        Set<Class<? extends Annotation>> BDAs = new HashSet<Class<? extends Annotation>>();
        BDAs.add(Path.class);
        BDAs.add(Provider.class);
        BDAs.add(ManagedBean.class);
        BDAs.add(ApplicationPath.class);
        return BDAs;
    }
    
    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> BDAs = new HashSet<Class<?>>();
        BDAs.add(Application.class);
        return BDAs;
    }
    
    @Reference
    protected void setCdiService(CDIService cdiService) {
        LibertyCdiInjectorFactory.cdiService = cdiService;
    }

    protected void unsetCdiService(CDIService cdiService) {
        LibertyCdiInjectorFactory.cdiService = null;
    }
}
