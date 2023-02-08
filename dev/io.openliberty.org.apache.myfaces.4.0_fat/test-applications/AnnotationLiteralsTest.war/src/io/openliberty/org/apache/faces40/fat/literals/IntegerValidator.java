/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.literals;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/**
 * This class forces CDI to create a @FacesValidator bean for testing
 */
@FacesValidator(value = "integerValidator", isDefault = false, managed = true)
public class IntegerValidator implements Validator<Integer> {

    @Override
    public void validate(FacesContext context, UIComponent component, Integer value) throws ValidatorException {
        if (value.intValue() > 30) {
            throw new ValidatorException(new FacesMessage("TO_HIGH"));
        }
    }

}
