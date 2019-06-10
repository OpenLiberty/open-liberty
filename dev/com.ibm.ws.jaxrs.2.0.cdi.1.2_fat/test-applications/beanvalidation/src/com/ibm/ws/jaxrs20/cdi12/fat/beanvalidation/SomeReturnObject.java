/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.beanvalidation;

/**
 * Arbitrary object returned by the JAX-RS resource used to validate - 
 * a valid object has a string less than 3 characters
 */
public class SomeReturnObject {
    private int number;
    @ValidSomeReturnString
    private String string;

    public SomeReturnObject() {
    }
    public SomeReturnObject(int number, String string) {
        this.number = number;
        this.string = string;
    }
    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }
    public String getString() {
        return string;
    }
    public void setString(String string) {
        this.string = string;
    }
}
