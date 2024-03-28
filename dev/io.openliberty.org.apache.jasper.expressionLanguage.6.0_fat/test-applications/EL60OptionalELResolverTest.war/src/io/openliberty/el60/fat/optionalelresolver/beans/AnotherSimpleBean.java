/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.optionalelresolver.beans;

/**
 * This bean is used to test the Jakarta Expression Language 6.0 OptionalELResolver.
 *
 * There are multiple methods used to test method invocation with the OptionalELResolver.
 */
public class AnotherSimpleBean {
    private String testString;

    public AnotherSimpleBean(String testString) {
        this.testString = testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }

    public String getTestString() {
        return this.testString;
    }

    public void doSomething() {
        this.testString = "AnotherSimpleBean.doSomething() called!";
    }

    public String returnSomething() {
        return "AnotherSimpleBean.returnSomething called!";
    }
}
