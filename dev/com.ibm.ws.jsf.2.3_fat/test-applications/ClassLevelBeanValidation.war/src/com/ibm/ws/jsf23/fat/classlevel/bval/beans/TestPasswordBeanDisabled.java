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

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * This bean is valid to perform a copy for the class-level bean validation. However, the purpose of this bean is to use it for setting
 * the disabled attribute to true on the validateWholeBean tag.
 */
@Named
@RequestScoped
@Password(groups = com.ibm.ws.jsf23.fat.classlevel.bval.beans.PasswordValidationGroup.class)
public class TestPasswordBeanDisabled implements PasswordHolder, Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    @NotNull
    private String password1;
    @NotNull
    private String password2;

    private Boolean disabled = Boolean.TRUE;

    @NotNull(groups = com.ibm.ws.jsf23.fat.classlevel.bval.beans.PasswordValidationGroup.class)
    @Size(max = 16, min = 8, message = "Password must be between 8 and 16 characters long",
          groups = com.ibm.ws.jsf23.fat.classlevel.bval.beans.PasswordValidationGroup.class)
    @Override
    public String getPassword1() {
        return password1;
    }

    public void setPassword1(String password1) {
        this.password1 = password1;
    }

    @NotNull(groups = com.ibm.ws.jsf23.fat.classlevel.bval.beans.PasswordValidationGroup.class)
    @Size(max = 16, min = 8, message = "Password must be between 8 and 16 characters long",
          groups = com.ibm.ws.jsf23.fat.classlevel.bval.beans.PasswordValidationGroup.class)
    @Override
    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password12) {
        this.password2 = password12;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = Boolean.valueOf(disabled);
    }
}
