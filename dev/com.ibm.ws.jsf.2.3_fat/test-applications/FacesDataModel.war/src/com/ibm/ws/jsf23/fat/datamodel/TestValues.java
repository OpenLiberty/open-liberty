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

/**
 * A class the represents a set of String values.
 */
public class TestValues {

    private String testValue1;
    private String testValue2;
    private String testValue3;
    private String testValue4;

    public TestValues(String testValue1, String testValue2, String testValue3, String testValue4) {
        this.testValue1 = testValue1;
        this.testValue2 = testValue2;
        this.testValue3 = testValue3;
        this.testValue4 = testValue4;
    }

    public String getTestValue1() {
        return testValue1;
    }

    public void setTestValue1(String testValue1) {
        this.testValue1 = testValue1;
    }

    public String getTestValue2() {
        return testValue2;
    }

    public void setTestValue2(String testValue2) {
        this.testValue2 = testValue2;
    }

    public String getTestValue3() {
        return testValue3;
    }

    public void setTestValue3(String testValue3) {
        this.testValue3 = testValue3;
    }

    public String getTestValue4() {
        return testValue4;
    }

    public void setTestValue4(String testValue4) {
        this.testValue4 = testValue4;
    }

}
