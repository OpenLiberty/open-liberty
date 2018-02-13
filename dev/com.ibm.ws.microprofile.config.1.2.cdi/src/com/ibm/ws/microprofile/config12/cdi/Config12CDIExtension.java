/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.cdi;

import java.lang.reflect.Type;
import java.util.Collection;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.config.cdi.ConfigCDIExtension;
import com.ibm.ws.microprofile.config12.converters.Config12DefaultConverters;

/**
 * The Config12CDIExtension observes all the @ConfigProperty qualified InjectionPoints and ensures that a ConfigPropertyBean is created for each type.
 * It also registers the ConfigBean itself.
 */
@Component(service = WebSphereCDIExtension.class, property = { "api.classes=org.eclipse.microprofile.config.inject.ConfigProperty;org.eclipse.microprofile.config.Config" }, immediate = true)
public class Config12CDIExtension extends ConfigCDIExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(Config12CDIExtension.class);

    @Override
    protected Collection<? extends Type> getDefaultConverterTypes() {
        return Config12DefaultConverters.getDefaultConverters().getTypes();
    }

}
