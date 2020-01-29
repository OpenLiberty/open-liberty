/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.filter;

public class GreaterCondition extends SimpleCondition {

    public GreaterCondition(String key, IValue value) {
        super(key, value);
    }

    public boolean checkCondition(IValue test) throws FilterException {

        // is the input string greater than the value
        return getValue().greaterThan(test);
    }

    public String getOperand() {
        return ">";
    }
}
