/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.io.Serializable;

/**
 * Recreate from Jakarta Data TCK
 */
@jakarta.persistence.Entity
public class AsciiCharacter implements Serializable {
    private static final long serialVersionUID = 1L;

    @jakarta.persistence.Id
    private long id;

    private int numericValue;

    private String hexadecimal;

    private char thisCharacter;

    private boolean isControl;

    public static AsciiCharacter of(int numericValue) {
        AsciiCharacter inst = new AsciiCharacter();
        inst.id = numericValue;
        inst.numericValue = numericValue;
        inst.hexadecimal = Integer.toHexString(numericValue);
        inst.thisCharacter = (char) numericValue;
        inst.isControl = Character.isISOControl(inst.thisCharacter);

        return inst;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(int numericValue) {
        this.numericValue = numericValue;
    }

    public String getHexadecimal() {
        return hexadecimal;
    }

    public void setHexadecimal(String hexadecimal) {
        this.hexadecimal = hexadecimal;
    }

    public char getThisCharacter() {
        return thisCharacter;
    }

    public void setThisCharacter(char thisCharacter) {
        this.thisCharacter = thisCharacter;
    }

    public boolean isControl() {
        return isControl;
    }

    public void setControl(boolean isControl) {
        this.isControl = isControl;
    }

}