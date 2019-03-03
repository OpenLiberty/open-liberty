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

package com.ibm.ws.serialfilter.fat.object.allowed;

import java.io.Serializable;

public class Test2 implements Serializable {

    private int _intValue;
    private String _stringValue;

    // Default constructor
    public Test2(int intValue, String stringValue) {
        _intValue = intValue;
        _stringValue = stringValue;
    }

    public int getInt() {
        return _intValue;
    }

    public String getString() {
        return _stringValue;
    }
}