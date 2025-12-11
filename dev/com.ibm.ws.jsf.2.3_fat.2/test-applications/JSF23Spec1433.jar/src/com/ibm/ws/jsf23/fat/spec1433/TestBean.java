/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.spec1433;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A simple RequestScoped bean.
 *
 */
@Named
@RequestScoped
public class TestBean {
    private String valueOne;
    private String valueTwo;

    public void setValueOne(String valueOne) {
        this.valueOne = valueOne;
    }

    public String getValueOne() {
        return this.valueOne;
    }

    public void setValueTwo(String valueTwo) {
        this.valueTwo = valueTwo;
    }

    public String getValueTwo() {
        return this.valueTwo;
    }
}
