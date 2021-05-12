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

public class JBVFATActSpecEmbeddedImpl extends JBVFATActSpecImpl {

    private Integer asProperty5;

    @Override
    @Min(20)
    public Integer getAsProperty2() {
        return asProperty2;
    }

    public Integer getAsProperty5() {
        return asProperty5;
    }

    public void setAsProperty5(Integer asProperty5) {
        this.asProperty5 = asProperty5;
    }

}
