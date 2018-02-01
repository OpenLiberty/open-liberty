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
package com.ibm.ws.cdi12.test.rootClassLoader.extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

public class RandomBean implements Bean<Random>, PassivationCapable {

    private final HashSet<Type> types;
    private final HashSet<Annotation> qualifiers;

    RandomBean() {
        types = new HashSet<Type>();
        types.add(Random.class);
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
    public Random create(CreationalContext<Random> creationalContext) {
        java.util.Random rnd = new java.util.Random();
        return rnd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.context.spi.Contextual#destroy(java.lang.Object, javax.enterprise.context.spi.CreationalContext)
     */
    @Override
    public void destroy(Random instance, CreationalContext<Random> creationalContext) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getTypes()
     */
    @Override
    public Set<Type> getTypes() {
        return types;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getQualifiers()
     */
    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getScope()
     */
    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getName()
     */
    @Override
    public String getName() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.BeanAttributes#getStereotypes()
     */
    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
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
    public Class<?> getBeanClass() {
        return Random.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#getInjectionPoints()
     */
    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.enterprise.inject.spi.Bean#isNullable()
     */
    @Override
    public boolean isNullable() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.PassivationCapable#getId()
     */
    @Override
    public String getId() {
        return "Random: " + hashCode();
    }

}
