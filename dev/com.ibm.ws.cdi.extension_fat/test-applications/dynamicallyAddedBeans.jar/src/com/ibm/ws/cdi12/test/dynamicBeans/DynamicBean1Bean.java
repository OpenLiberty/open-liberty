/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.dynamicBeans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 *
 */
public class DynamicBean1Bean implements Bean, PassivationCapable {

    private final HashSet<Type> types;
    private final HashSet<Annotation> qualifiers;

    public DynamicBean1Bean() {
        types = new HashSet<Type>();
        types.add(DynamicBean1.class);
        types.add(Object.class);

        qualifiers = new HashSet<Annotation>();
        qualifiers.add(DefaultLiteral.INSTANCE);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.context.spi.Contextual#create(javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public Object create(CreationalContext creationalContext) {
        DynamicBean1 bean1 = new DynamicBean1();
        return bean1;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.context.spi.Contextual#destroy(java.lang.Object, javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public void destroy(Object instance, CreationalContext creationalContext) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.BeanAttributes#getTypes()
     */
    @Override
    public Set getTypes() {
        // TODO Auto-generated method stub
        return types;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.BeanAttributes#getQualifiers()
     */
    @Override
    public Set getQualifiers() {
        // TODO Auto-generated method stub
        return qualifiers;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.BeanAttributes#getScope()
     */
    @Override
    public Class getScope() {
        return SessionScoped.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.BeanAttributes#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.BeanAttributes#getStereotypes()
     */
    @Override
    public Set getStereotypes() {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.BeanAttributes#isAlternative()
     */
    @Override
    public boolean isAlternative() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.Bean#getBeanClass()
     */
    @Override
    public Class getBeanClass() {
        // TODO Auto-generated method stub
        return DynamicBean1.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.Bean#getInjectionPoints()
     */
    @Override
    public Set getInjectionPoints() {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.Bean#isNullable()
     */
    @Override
    public boolean isNullable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getId() {
        return "DynamicBean1: " + hashCode();
    }

}
