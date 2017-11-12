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
import javax.validation.Validator;

import org.hibernate.validator.cdi.HibernateValidator;
import org.hibernate.validator.cdi.internal.ValidationProviderHelper;
import org.hibernate.validator.cdi.internal.ValidatorBean;

/**
 * This class is used to extend the Apache ValidatorBean for the sole purpose
 * of overriding the create method. Instead of passing in the Validator object
 * when the bean is initialized, we delay the creation of the Validator until
 * create is called. The delay is needed since the server thread doesn't have its
 * metadata and context initialized to a point where creating the Validator will succeed.
 *
 */
public class LibertyValidatorBean extends ValidatorBean {

    protected String id = null;

    static final Set<Annotation> qualifiers = new HashSet<Annotation>(Arrays.asList(new AnnotationLiteral<Default>() {},
                                                                                    new AnnotationLiteral<HibernateValidator>() {},
                                                                                    new AnnotationLiteral<Any>() {}));

    public LibertyValidatorBean() {
        super(null, null, ValidationProviderHelper.forHibernateValidator());
    }

    @Override
    public Validator create(CreationalContext<Validator> context) {
        Validator validator = LibertyHibernateValidatorExtension.instance().getDefaultValidator();
        return validator;
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
     * be retrieved in both local and server failover scenarios as per defect 774504.
     */
    @Override
    public String getId() {
        if (id == null) {
            // Set id to the class name
            id = this.getClass().getName();
        }
        return id;
    }
}
