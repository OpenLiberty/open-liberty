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
 * Simple RequestScoped bean
 * This bean will be used to test the statement in the VDLDoc for the validateWholeBean tag, which states:
 * "Invoke the newInstance() method on the bean's Class. If this throws any Exception, swallow it and continue."
 * To accomplish this, the constructor is private instead of public. This will throw an IllegalAccessException, which MyFaces will
 * log, if tracing is enabled, and continue on with the copying of the bean.
 */
@Named
@RequestScoped
@Password(groups = com.ibm.ws.jsf23.fat.classlevel.bval.beans.PasswordValidationGroup.class)
public class TestPasswordBeanIllegalAccess implements PasswordHolder, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String password1;
    @NotNull
    private String password2;

    //This private contructor will cause an IllegalAccessException, which is expected for this test.
    private TestPasswordBeanIllegalAccess() {}

    @Override
    protected Object clone() throws CloneNotSupportedException {
        TestPasswordBeanIllegalAccess other = (TestPasswordBeanIllegalAccess) super.clone();
        other.setPassword1(this.getPassword1());
        other.setPassword2(this.getPassword2());
        return other;
    }

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

}
