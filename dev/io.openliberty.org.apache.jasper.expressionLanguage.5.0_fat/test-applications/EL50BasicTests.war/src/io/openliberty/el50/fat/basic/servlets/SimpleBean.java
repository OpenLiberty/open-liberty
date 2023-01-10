/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.el50.fat.basic.servlets;

import java.util.Arrays;

/**
 * Bean for testing basic changes in Expression Language 5.0
 */
public class SimpleBean {

    public static String staticString = "Static String for testGetType_returnsNull Test";

    private int simpleProperty = 10;

    public int getNumber() {
        return 25;
    }

    public int getSimpleProperty(){
        return simpleProperty;
    }

    public String getStringVarArgs(String... strings){
        return Arrays.toString(strings);
    }

}
