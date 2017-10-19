/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec;

import java.util.Set;

import javax.el.ELProcessor;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIService;

@Component(service = { CDIHelper.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class CDIHelper {

    private static CDIService cdiService;

    @Reference
    protected void setCDIService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    protected void unsetCDIService(CDIService cdiService) {
        this.cdiService = null;
    }

    public static Object getBeanFromCurrentModule(Class<?> beanClass) {
        BeanManager beanManager = getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(beanClass);
        Bean<?> bean = beanManager.resolve(beans);
        return beanManager.getReference(bean, beanClass, beanManager.createCreationalContext(bean));
    }

    public static BeanManager getBeanManager() {
        return cdiService.getCurrentModuleBeanManager();
    }

    public static ELProcessor getELProcessor() {
        ELProcessor elProcessor = new ELProcessor();
        elProcessor.getELManager().addELResolver(getBeanManager().getELResolver());
        return elProcessor;
    }

}