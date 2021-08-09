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
package beanvalidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

/**
 * Simple implementation of a ConstraintValidatorFactory that tolerates a null
 * parameter for testing purposes.
 */
public class CustomConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> arg0) {
        if (arg0 != null) {
            try {
                return arg0.newInstance();
            } catch (IllegalAccessException e) {

            } catch (InstantiationException e) {

            }
        }
        return null;
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> arg0) {}

}
