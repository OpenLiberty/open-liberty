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

import javax.validation.constraints.Size;

public class JBVFATActSpecSuccessImpl extends JBVFATActSpecImpl {

    @Override
    public Double getAsProperty4() {
        return asProperty4;
    }

    @Override
    @Size(min = 2, max = 4)
    public String getAsProperty1() {
        return asProperty1;
    }

}
