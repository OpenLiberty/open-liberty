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

import javax.validation.constraints.NotNull;

public interface JBVFATActSpec {

    public String getAsProperty1();

    public Integer getAsProperty2();

    @NotNull(message = "This field should not be null")
    public Boolean getAsProperty3();

    public Double getAsProperty4();

}
