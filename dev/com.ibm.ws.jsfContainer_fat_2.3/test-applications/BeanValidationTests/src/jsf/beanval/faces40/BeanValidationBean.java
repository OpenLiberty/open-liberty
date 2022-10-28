/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsf.beanval.faces40;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.faces.validator.LengthValidator;

/**
 * A CDI Bean that is being used with the
 * BeanValidationTests application to test
 * beanValidation capability with Faces 4.0
 */
@Named("beanValidation")
@SessionScoped
public class BeanValidationBean implements Serializable {

    // used for binding test
    private String bindingValue;

    // Validator used with the bindingValue in the facelet.
    private LengthValidator lengthValidator;

    /**
     * Getter for bindingValue
     */
    public String getBindingValue() {
        return bindingValue;
    }

    /**
     * Setter for bindingValue
     */
    public void setBindingValue(String bindingValue) {
        this.bindingValue = bindingValue;
    }

    /**
     * Getter for lengthValidator
     */
    public LengthValidator getLengthValidator() {
        lengthValidator = new LengthValidator(2);
        return lengthValidator;
    }

    /**
     * Setter for lenghtValidator
     */
    public void setLengthValidator(LengthValidator lengthValidator) {
        this.lengthValidator = lengthValidator;
    }
}
