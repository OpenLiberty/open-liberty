/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.converter.validator.behavior.injection.beans;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;

/**
 * Validator that supports Injection
 */
@FacesValidator(value = "testValidator", managed = true)
public class TestValidator implements Validator<String> {

    @Inject
    private TestCDIBean testBean;

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.validator.Validator#validate(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
     */
    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        if (!value.contains(testBean.getEarth())) {
            FacesMessage msg = new FacesMessage("Text validation failed. Text does not contain '" + testBean.getWorld() + "' or '" + testBean.getEarth() + "'.");
            throw new ValidatorException(msg);
        }
    }

}
