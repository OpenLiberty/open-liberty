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

import com.ibm.ws.microprofile.config.cdi.ConfigCDIExtension;
import com.ibm.ws.microprofile.config12.converters.Config12DefaultConverters;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/**
 * The Config12CDIExtension observes all the @ConfigProperty qualified InjectionPoints and ensures that a ConfigPropertyBean is created for each type.
 * It also registers the ConfigBean itself.
 */
public class Config12CDIExtension extends ConfigCDIExtension implements Extension, WebSphereCDIExtension {

    @Override
    protected Collection<? extends Type> getDefaultConverterTypes() {
        return Config12DefaultConverters.getDefaultConverters().getTypes();
    }

}
