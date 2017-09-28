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
package beanvalidation10;

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

    /**
     * TODO This is needed here so that the application will compile against the 1.1 spec
     * level, even though it is "written" to the 1.0 spec level. This is a restriction
     * of compiling source folders in a single project in eclipse. Ideally we should
     * find a different solution so that all apps can be compiled against the correct
     * level and can then be run against the necessary features.
     */
    @Override
    public void releaseInstance(ConstraintValidator<?, ?> arg0) {}

}
