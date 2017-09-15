/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa;

import javax.persistence.Embeddable;

@Embeddable
public class JobParameter {

    private String name = "";
    private String value = "";

    public String getParameterName() {
        return name;
    }

    public void setParameterName(String newName) {
        name = newName;
    }

    public String getParameterValue() {
        return value;
    }

    public void setParameterValue(String newValue) {
        value = newValue;
    }

}
