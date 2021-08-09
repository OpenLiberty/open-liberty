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

import javax.validation.constraints.Min;

public class JBVFATActSpecFailureImpl extends JBVFATActSpecImpl {

    @Override
    @Min(value = 20, message = "The value should be > 20")
    public Integer getAsProperty2() {
        return asProperty2;
    }

    @Override
    public String getAsProperty1() {
        return asProperty1;
    }

    // @NotNull
    @Override
    public Boolean getAsProperty3() {
        return asProperty3;
    }

}
