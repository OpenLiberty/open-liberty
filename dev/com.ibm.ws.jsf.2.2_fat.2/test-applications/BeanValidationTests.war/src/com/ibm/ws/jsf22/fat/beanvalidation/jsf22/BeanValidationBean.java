/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.beanvalidation.jsf22;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.validator.LengthValidator;

/**
 * A Managed Bean that is being used with the
 * BeanValidationTests application to test
 * beanValidation capability with JSF 2.2
 *
 *
 * @author Christopher Meyer
 *
 */
@ManagedBean(name = "beanValidation")
@SessionScoped
public class BeanValidationBean {

    // used for binding test
    private String bindingValue;

    // Validator used with the bindingValue in the facelet.
    private LengthValidator lengthValidator;

    /**
     * Getter for bindingValue
     *
     * @return bindingValue
     */
    public String getBindingValue() {
        return bindingValue;
    }

    /**
     * Setter for bindingValue
     *
     * @param bindingValue
     */
    public void setBindingValue(String bindingValue) {
        this.bindingValue = bindingValue;
    }

    /**
     * Getter for lengthValidator
     *
     * @return lengthValidator
     */
    public LengthValidator getLengthValidator() {
        lengthValidator = new LengthValidator(2);
        return lengthValidator;
    }

    /**
     * Setter for lengthValidator
     *
     * @param lengthValidator
     */
    public void setLengthValidator(LengthValidator lengthValidator) {
        this.lengthValidator = lengthValidator;
    }
}
