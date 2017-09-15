/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsf.cdi.flow.beans;

import java.io.Serializable;

import javax.faces.flow.FlowScoped;
import javax.inject.Named;

/**
 * A simple FlowScoped bean used to test basic functionality.
 */
@Named(value = "testBean")
@FlowScoped(value = "simpleBean")
public class TestBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String testValue;

    public TestBean() {
        this.testValue = "";
    }

    public String getTestValue() {
        return testValue;
    }

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }

    public String getReturnValue() {
        return "/JSF22Flows_return";
    }

    /* Returns a string for the sake of navigating to the second page in the flow. */
    public String simpleSubmit() {
        return "simpleBean-2";
    }
}
