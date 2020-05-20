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
package com.ibm.ws.cdi.extension.spi.test.constructor.exception;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

@Component(service = CDIExtensionMetadata.class, configurationPolicy = IGNORE)
public class SPIMetaData implements CDIExtensionMetadata {

    @Override
    public Set<Class<?>> getBeanClasses() {
        Set<Class<?>> beans = new HashSet<Class<?>>();
        beans.add(InterfaceRegisteredBean.class);
        return beans;
    }

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(MyExtension.class);
        return extensions;
    }

}
