/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

/**
 * Simple utility class to allow methods to return several values out as the
 * return codes.
 */
public class ReturnCodes {

    /** Boolean value stored */
    private boolean boolValue;
    /** Int value stored */
    private int intValue;

    /**
     * Constructor allowing the integer to be set immediately.
     * 
     * @param i
     */
    public ReturnCodes(int i) {
        setIntValue(i);
    }

    /**
     * Constructor allowing the boolean to be set immediately.
     * 
     * @param flag
     */
    public ReturnCodes(boolean flag) {
        setBooleanValue(flag);
    }

    /**
     * Constructor allowing both values to be set at once.
     * 
     * @param flag
     * @param i
     */
    public ReturnCodes(boolean flag, int i) {
        setBooleanValue(flag);
        setIntValue(i);
    }

    /**
     * Query the value of the boolean stored in this object.
     * 
     * @return boolean
     */
    public boolean getBooleanValue() {
        return this.boolValue;
    }

    /**
     * Set the boolean flag stored in this object to the input value.
     * 
     * @param value
     */
    public void setBooleanValue(boolean value) {
        this.boolValue = value;
    }

    /**
     * Query what the integer stored in this object is.
     * 
     * @return int
     */
    public int getIntValue() {
        return this.intValue;
    }

    /**
     * Set the integer stored in this object to the input value
     * 
     * @param value
     */
    public void setIntValue(int value) {
        this.intValue = value;
    }
}