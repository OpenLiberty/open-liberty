/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.classlevel.bval.beans;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * This class is the implementation of the ConstraintValidator. It is used to define the logic with which to validate.
 * 
 * Note that a PasswordHolder instance is passed to the isValid() method. This method will only be called if the individual properties of the PasswordHolder are valid.
 * This fact allows the isValid() method to inspect the properties and perform effective class-level validation.
 */
public class PasswordValidator implements ConstraintValidator<Password, PasswordHolder> {

    @Override
    public void initialize(Password constraintAnnotation) {
        // do nothing
    }

    @Override
    public boolean isValid(PasswordHolder value, ConstraintValidatorContext context) {
        System.out.println("PasswordHolder --> " + value);
        System.out.println("PasswordHolder getPassword1 --> " + value.getPassword1());
        System.out.println("PasswordHolder getPassword2 --> " + value.getPassword2());
        return value.getPassword1().equals(value.getPassword2());
    }

}
