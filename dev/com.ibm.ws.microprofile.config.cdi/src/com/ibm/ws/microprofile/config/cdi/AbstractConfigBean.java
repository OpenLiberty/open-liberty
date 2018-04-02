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
package com.ibm.ws.microprofile.config.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This CDI Bean controls the creation and destruction of Config instances injected by CDI.
 * They are all Dependent scope.
 */
@Trivial
public abstract class AbstractConfigBean<T> implements Bean<T>, PassivationCapable {

    private final HashSet<Type> types;
    private final HashSet<Annotation> qualifiers;
    private final String id;
    private final String name;

    /**
     * @param clazz
     */
    public AbstractConfigBean(BeanManager beanManager, Type type, Annotation qualifier) {
        this.types = new HashSet<Type>();
        types.add(type);

        this.qualifiers = new HashSet<Annotation>();
        this.qualifiers.add(qualifier);

        this.name = this.getClass().getName() + "[" + type + "]";
        this.id = beanManager.hashCode() + "#" + this.name;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Type> getTypes() {
        return types;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAlternative() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Bean for " + this.types + " with Qualifiers " + this.qualifiers;
    }
}
