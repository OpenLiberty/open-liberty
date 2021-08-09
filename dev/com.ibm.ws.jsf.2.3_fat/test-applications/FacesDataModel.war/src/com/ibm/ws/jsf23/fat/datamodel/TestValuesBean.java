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
package com.ibm.ws.jsf23.fat.datamodel;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A RequestScoped bean that contains an instance of TestValues.
 *
 */
@Named
@RequestScoped
public class TestValuesBean {

    private TestValues values = new TestValues("test1", "test2", "test3", "test4");
    private TestValuesChild childValues = new TestValuesChild("test1", "test2", "test3", "test4");

    public TestValues getValues() {
        return this.values;
    }

    public void setValues(TestValues values) {
        this.values = values;
    }

    public TestValuesChild getChildValues() {
        return this.childValues;
    }

    public void setChildValues(TestValuesChild childValues) {
        this.childValues = childValues;
    }

}
