/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.datamodel;

/**
 * A simple class that extends TestValues to test @FacesDataModel
 * ordering.
 *
 */
public class TestValuesChild extends TestValues {

    /**
     * @param testValue1
     * @param testValue2
     * @param testValue3
     * @param testValue4
     */
    public TestValuesChild(String testValue1, String testValue2, String testValue3, String testValue4) {
        super(testValue1, testValue2, testValue3, testValue4);
    }

}
