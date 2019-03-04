/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.cdi;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.config.cdi.ConfigPropertyBean;
import com.ibm.ws.microprofile.config12.cdi.Config12CDIExtension;

/**
 * The Config12CDIExtension observes all the @ConfigProperty qualified InjectionPoints and ensures that a ConfigPropertyBean is created for each type.
 * It also registers the ConfigBean itself.
 */
public class Config14CDIExtension extends Config12CDIExtension implements Extension, WebSphereCDIExtension {

    @Override
    protected <T> void addConfigPropertyBean(AfterBeanDiscovery abd, BeanManager beanManager, Type beanType, Class<T> clazz) {
        ConfigPropertyBean<T> converterBean = new ConfigPropertyBean<T>(beanManager, beanType, clazz, Config14PropertyLiteral.INSTANCE);
        abd.addBean(converterBean);
    }

}
