/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.el30.fat.varargstest;

/**
 *  This bean is used to test which methods definitions are matched
 */
public class EL30VarargsMethodMatchingTestBean{

    private static final long serialVersionUID = 1L;
    
    public IEnum getEnum1() {
        return EnumBean.ENUM1;
    }

    public int getNumber() {
        return 1;
    }

    public String testMethod(IEnum enum1) {
        return "(IEnum enum1)";
    }

    public String testMethod(String param1) {
        return "(String param1)";
    }

    public String testMethod(String param1, String... param2) {
        return "(String param1, String... param2)";
    }

    public String testMethod(String param1, String param2) {
        return "(String param1, String param2)";
    }

    public String testMethod(String param1, IEnum... param2) {
        return "(String param1, IEnum... param2)";
    }

    public String testMethod(IEnum enum1, IEnum... enum2) {
        return "(IEnum enum1, IEnum... enum2)";
    }

    public String testMethod(int... param1) {
        return "(int... param1)";
    }

    public String chirp(Bird bird1) {
        return "(Bird bird1)";
    }

    public String chirp(Falcon bird1, String... param2) {
        return "(Falcon bird1, String... param2)";
    }

    public String chirp(String string1, Bird... bird2) {
        return "(String string1, Bird... bird2)";
    }

    // Used in testString_VarargsFalcon
    // public String chirp(String string1, Falcon... bird2) {
    //     return "chirp(String string1, Falcon... bird2)";
    // }

}
