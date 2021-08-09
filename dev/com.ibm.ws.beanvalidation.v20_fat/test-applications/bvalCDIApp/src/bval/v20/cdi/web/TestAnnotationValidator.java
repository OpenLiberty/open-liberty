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
package bval.v20.cdi.web;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TestAnnotationValidator implements ConstraintValidator<TestAnnotation, Object> {

    private static final String c = TestAnnotationValidator.class.getSimpleName();

    public static int isValidCounter = 0;

    @Inject
    BeanValCDIBean bean;

    @Override
    public void initialize(TestAnnotation arg0) {
        System.out.println(c + " initialize with " + arg0);
    }

    @Override
    public boolean isValid(Object arg0, ConstraintValidatorContext arg1) {
        isValidCounter += 1;
        if (this.bean != null) {
            return true;
        }
        return false;
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println(c + " is getting destroyed.");
    }
}
