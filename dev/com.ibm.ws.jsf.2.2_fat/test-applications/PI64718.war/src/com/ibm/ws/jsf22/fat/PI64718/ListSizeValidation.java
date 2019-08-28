/*
 * Copyright (c)  2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.PI64718;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ListSizeValidation implements ConstraintValidator<ListSizeValidator, List<String>> {

	@Override
	public void initialize(ListSizeValidator constraintAnnotation) {
	}

	@Override
	public boolean isValid(List<String> value, ConstraintValidatorContext context) {
		return value != null && value.size() == 2;
	}

}
