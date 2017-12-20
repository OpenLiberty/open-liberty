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
import javax.validation.Validator;

import org.hibernate.validator.cdi.HibernateValidator;
import org.hibernate.validator.cdi.internal.ValidationProviderHelper;
import org.hibernate.validator.cdi.internal.ValidatorBean;

/**
 * This class is used to extend the Hibernate ValidatorBean for the sole purpose
 * of overriding the create method. Instead of passing in the Validator object
 * when the bean is initialized, we delay the creation of the Validator until
 * create is called. The delay is needed since the server thread doesn't have its
 * metadata and context initialized to a point where creating the Validator will succeed.
 *
 */
public class LibertyValidatorBean extends ValidatorBean {

    protected final String id = getClass().getName();

    static final Set<Annotation> qualifiers = new HashSet<Annotation>(Arrays.asList(new AnnotationLiteral<Default>() {},
                                                                                    new AnnotationLiteral<HibernateValidator>() {},
                                                                                    new AnnotationLiteral<Any>() {}));

    public LibertyValidatorBean() {
        super(null, null, ValidationProviderHelper.forHibernateValidator());
    }

    @Override
    public Validator create(CreationalContext<Validator> context) {
        return new LibertyValidatorProxy();
    }

    @Override
    public Class<?> getBeanClass() {
        return Validator.class;
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> types = new HashSet<Type>();
        for (Class<?> c = Validator.class; c != null; c = c.getSuperclass())
            types.add(c);
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /*
     * Override this method so that a LibertyValidatorBean is stored in the WELD
     * Bean Store keyed on its classname. This allows an injected Validator Bean to
     * be retrieved in both local and server failover scenarios
     */
    @Override
    public String getId() {
        return id;
    }
}
