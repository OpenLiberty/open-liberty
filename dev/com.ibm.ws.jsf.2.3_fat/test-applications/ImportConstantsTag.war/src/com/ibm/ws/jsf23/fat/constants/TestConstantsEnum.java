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
package com.ibm.ws.jsf23.fat.constants;

/**
 * A test enum that contains a number of constants to be tested using
 * the new JSF 2.3 <f:importConstants> tag.
 */
public enum TestConstantsEnum {

    TEST_CONSTANTS_1("Testing "),
    TEST_CONSTANTS_2("an "),
    TEST_CONSTANTS_3("enum!");

    private String value;

    private TestConstantsEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

}