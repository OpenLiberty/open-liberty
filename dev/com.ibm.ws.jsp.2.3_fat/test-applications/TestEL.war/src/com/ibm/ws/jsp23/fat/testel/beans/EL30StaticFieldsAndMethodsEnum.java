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
package com.ibm.ws.jsp23.fat.testel.beans;

/**
 * A simple enumeration class needed for EL 3.0 testing
 */
public enum EL30StaticFieldsAndMethodsEnum {

    TEST_ONE(1),
    TEST_TWO(2),
    TEST_THREE(3);

    private final int testCode;

    EL30StaticFieldsAndMethodsEnum(int levelCode) {
        this.testCode = levelCode;
    }

    public int gettestCode() {
        return this.testCode;
    }
}
