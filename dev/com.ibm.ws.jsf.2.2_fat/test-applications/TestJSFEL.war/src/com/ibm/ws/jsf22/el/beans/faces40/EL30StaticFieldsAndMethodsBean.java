/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.el.beans.faces40;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Simple bean, with a few fields and methods needed to test EL 3.0
 */
@Named("staticbean")
@ApplicationScoped
public class EL30StaticFieldsAndMethodsBean {

    /*
     * A static string
     */
    public static final String staticReference = "static reference";

    /*
     * A non-static string
     */
    private String nonStaticReference = "non-static reference";

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
