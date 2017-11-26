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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import com.ibm.ws.beanvalidation.service.ValidationReleasable;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.managedobject.ManagedObject;

/**
 * A custom implementation of the {@link ConstraintValidatorFactory} that will
 * create it's {@link ConstraintValidator} instances as CDI managed beans.
 */
public class ReleasableConstraintValidatorFactory implements ConstraintValidatorFactory, Closeable, ValidationReleasable<ConstraintValidatorFactory> {

    private final ValidationReleasableFactory releasableFactory;
    private Map<ConstraintValidator<?, ?>, ValidationReleasable<?>> releasables;

    public ReleasableConstraintValidatorFactory(ValidationReleasableFactory releasableFactory) {
        this.releasableFactory = releasableFactory;
    }

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> clazz) {
        ManagedObject<T> mo = releasableFactory.createValidationReleasable(clazz);

        return mo == null ? null : mo.getObject();
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> validator) {
        if (releasables != null) {
            ValidationReleasable<?> releasable = releasables.get(validator);
            if (releasable != null) {
                releasable.release();
            }
            releasables.remove(validator);
        }
    }

    @Override
    public void release() {
        if (releasables != null) {
            for (ValidationReleasable<?> releasable : releasables.values()) {
                releasable.release();
            }
            releasables.clear();
        }
    }

    @Override
    public ConstraintValidatorFactory getInstance() {
        return this;
    }

    @Override
    public void close() throws IOException {
        release();
    }

}
