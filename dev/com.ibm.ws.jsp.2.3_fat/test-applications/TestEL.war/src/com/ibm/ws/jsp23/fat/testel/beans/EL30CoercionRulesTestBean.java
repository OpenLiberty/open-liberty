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
 * Basic bean to test the EL 3.0 Coercion Rules 1.23.1 which states:
 * If X is null and Y is not a primitive type and also not a String, return null
 */
public class EL30CoercionRulesTestBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Integer myNumber;

    public EL30CoercionRulesTestBean() {
        myNumber = null;
    }

    public Integer getNumber() {
        return myNumber;
    }

    public void setNumber(Integer n) {
        myNumber = n;
    }

}
