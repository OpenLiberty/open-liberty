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

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.spi.test.bundle.extension.MyExtension;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.ClassSPIRegisteredProducer;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.interceptor.ClassSPIInterceptor;
import com.ibm.ws.cdi.extension.spi.test.bundle.getclass.interceptor.Intercept;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtensionMetaData;

@Component(service = WebSphereCDIExtensionMetaData.class)
public class SPIMetaData implements WebSphereCDIExtensionMetaData {

    @Override
    public Set<Class<?>> getCDIBeans() {
        Set<Class<?>> beans = new HashSet<Class<?>>();
        beans.add(ClassSPIRegisteredProducer.class);
        beans.add(ClassSPIInterceptor.class);
        beans.add(Intercept.class);
        return beans;
    }

    @Override
    public Set<Class<? extends Extension>> getCDIExtensions() {
        Set<Class<? extends Extension>> extensions = new HashSet<Class<? extends Extension>>();
        extensions.add(MyExtension.class);
        return extensions;
    }

}
