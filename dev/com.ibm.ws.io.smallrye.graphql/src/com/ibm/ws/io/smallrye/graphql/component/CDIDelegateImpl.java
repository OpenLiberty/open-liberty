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
package com.ibm.ws.io.smallrye.graphql.component;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.context.spi.CreationalContext;

import io.smallrye.graphql.lookup.LookupService;


public class CDIDelegateImpl implements LookupService {
    private static Logger LOG = Logger.getLogger(CDIDelegateImpl.class.getName());

    @Override
    public String getName() {
        return "Liberty CDIDelegateImpl";
    }

    @Override
    public Class<?> getClass(Class<?> declaringClass) {
        return getInstance(declaringClass).getClass();
    }

    @Override
    public Object getInstance(Class<?> declaringClass) {
        BeanManager manager = GraphQLExtension.getBeanManager();
        Bean bean = getBeanFromCDI(declaringClass);
        CreationalContext creationalContext = manager.createCreationalContext(bean);
        Object obj = null;
        if (bean != null && manager != null) {
            obj = manager.getReference(bean, declaringClass, creationalContext);
        }
        
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("instance, " + obj + " returned from class, " + declaringClass);
        }
        return obj;
    }

    private Bean<?> getBeanFromCDI(Class<?> clazz) {

        Set<Bean<?>> beans = GraphQLExtension.graphQLApiBeans.get(GraphQLExtension.getContextClassLoader());
        for (Bean<?> bean : beans) {
            if (clazz.equals(bean.getBeanClass())) {
                return bean;
            }
        }
        return null;
    }
}
