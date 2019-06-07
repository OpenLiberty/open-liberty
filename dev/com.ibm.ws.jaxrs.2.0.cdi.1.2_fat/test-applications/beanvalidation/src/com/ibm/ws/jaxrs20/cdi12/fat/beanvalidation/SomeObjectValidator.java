/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.beanvalidation;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@RequestScoped
public class SomeObjectValidator implements ConstraintValidator<ValidSomeString, String> {

    public static AtomicInteger invocationCount = new AtomicInteger();
 
    @Override
    public boolean isValid(String someString, ConstraintValidatorContext pContext) {
        new Throwable("DEBUG: SomeObjectValidator.isValid(...)").printStackTrace();
        invocationCount.incrementAndGet();
        return someString.length() > 3;
    }
}