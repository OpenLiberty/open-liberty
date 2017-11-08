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

import java.util.HashSet;
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

    @SuppressWarnings("static-access")
    @Reference
    protected void setCDIService(CDIService cdiService) {
        this.cdiService = cdiService;
    }

    @SuppressWarnings("static-access")
    protected void unsetCDIService(CDIService cdiService) {
        this.cdiService = null;
    }

    public static Object getBeanFromCurrentModule(Class<?> beanClass) {
        BeanManager beanManager = getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(beanClass);
        Bean<?> bean = beanManager.resolve(beans);
        return beanManager.getReference(bean, beanClass, beanManager.createCreationalContext(bean));
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> getBeansFromCurrentModule(Class<T> beanClass) {
        Set<T> beanInstances = new HashSet<T>();
        BeanManager beanManager = getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(beanClass);

        for (Bean<?> bean : beans) {
            beanInstances.add((T) beanManager.getReference(bean, beanClass, beanManager.createCreationalContext(bean)));
        }

        return beanInstances;
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