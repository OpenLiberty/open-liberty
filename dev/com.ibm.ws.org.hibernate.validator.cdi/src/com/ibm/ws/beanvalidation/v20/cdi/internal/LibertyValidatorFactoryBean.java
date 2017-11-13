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
package com.ibm.ws.beanvalidation.v20.cdi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.cdi.HibernateValidator;
import org.hibernate.validator.cdi.internal.ValidationProviderHelper;
import org.hibernate.validator.cdi.internal.ValidatorFactoryBean;

/**
 * This class is used to extend the Apache ValidatorFactoryBean for the sole purpose
 * of overriding the create method. Instead of passing in the ValidatorFactory object
 * when the bean is initialized, we delay the creation of the ValidatorFactory until
 * create is called. The delay is needed since the server thread doesn't have its
 * metadata and context initialized to a point where creating the ValidatorFactory will succeed.
 *
 */
public class LibertyValidatorFactoryBean extends ValidatorFactoryBean {

    protected final String id = getClass().getName();

    static final Set<Annotation> qualifiers = new HashSet<Annotation>(Arrays.asList(new AnnotationLiteral<Default>() {},
                                                                                    new AnnotationLiteral<HibernateValidator>() {},
                                                                                    new AnnotationLiteral<Any>() {}));

    public LibertyValidatorFactoryBean() {
        super(null, ValidationProviderHelper.forHibernateValidator());
        System.out.println("@AGG LibertyValidatorFactoryBean ctor");
    }

    @Override
    public ValidatorFactory create(CreationalContext<ValidatorFactory> context) {
        System.out.println("@AGG creating VF!");
        Thread.dumpStack();
        return LibertyHibernateValidatorExtension.getDefaultValidatorFactory();
    }

    @Override
    public Class<?> getBeanClass() {
        return ValidatorFactory.class;
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> types = new HashSet<Type>();
        for (Class<?> c = ValidatorFactory.class; c != null; c = c.getSuperclass())
            types.add(c);
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /*
     * Override this method so that a LibertyValidatorFactoryBean is stored in the WELD
     * Bean Store keyed on its classname. This allows an injected ValidatorFactory Bean to
     * be retrieved in both local and server failover scenarios
     */
    @Override
    public String getId() {
        return id;
    }

}
