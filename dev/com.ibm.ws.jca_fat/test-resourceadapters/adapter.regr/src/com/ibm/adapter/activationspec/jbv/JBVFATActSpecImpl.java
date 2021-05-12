/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.activationspec.jbv;

import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

import com.ibm.adapter.ActivationSpecImpl;

public class JBVFATActSpecImpl extends ActivationSpecImpl implements JBVFATActSpec {

    public String asProperty1;
    public Integer asProperty2;
    public Boolean asProperty3;
    public Double asProperty4;

    @Override
    @Size(min = 2, max = 4, message = "Size should be between 2 and 4")
    public String getAsProperty1() {
        return asProperty1;
    }

    public void setAsProperty1(String asProperty1) {
        this.asProperty1 = asProperty1;
    }

    @Override
    @Max(value = 30, message = "Should be < 30")
    public Integer getAsProperty2() {
        return asProperty2;
    }

    public void setAsProperty2(Integer asProperty2) {
        this.asProperty2 = asProperty2;
    }

    @Override
    public Boolean getAsProperty3() {
        return asProperty3;
    }

    public void setAsProperty3(Boolean asProperty3) {
        this.asProperty3 = asProperty3;
    }

    @Override
    public Double getAsProperty4() {
        return asProperty4;
    }

    public void setAsProperty4(Double asProperty4) {
        this.asProperty4 = asProperty4;
    }

}
