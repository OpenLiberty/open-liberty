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
 * Simple bean, with a few fields and methods needed to test EL 3.0
 */
public class EL30StaticFieldsAndMethodsBean {

    /*
     * A static string
     */
    public static final String staticReference = "static reference";

    /*
     * A non-static string
     */
    public String nonStaticReference = "non-static reference";

    /*
     * A static method
     */
    public static String staticMethod() {
        return "static method";
    }

    /*
     * A static one-parameter method
     */
    public static String staticMethodParam(String s) {
        return s;
    }

    /*
     * A non-static method
     */
    public String nonStaticMethod() {
        return "non-static method";
    }
}
