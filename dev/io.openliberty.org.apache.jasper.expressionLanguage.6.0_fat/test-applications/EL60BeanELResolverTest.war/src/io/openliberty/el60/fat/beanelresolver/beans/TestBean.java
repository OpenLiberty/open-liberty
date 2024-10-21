/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.beanelresolver.beans;

/**
 * A simple test bean with one String property. The writeTest and returnTest
 * methods are non standard bean names for getTest/setTest. In order to make this
 * work a TestBeanBeanInfo is defined.
 */
public class TestBean {

    private String test;

    public TestBean() {
        test = "Hi from TestBean!";
    }

    public void writeTest(String test) {
        this.test = test;
    }

    public String returnTest() {
        return test;
    }
}
