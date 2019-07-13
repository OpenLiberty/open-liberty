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
package com.ibm.ws.security.oauth20.filter;

public class ContainsCondition extends SimpleCondition {

    public ContainsCondition(String key, IValue value) {
        super(key, value);
    }

    public boolean checkCondition(IValue test) throws FilterException {

        // does the input string contain the value
        return getValue().containedBy(test);
    }

    public String getOperand() {
        return "%=";
    }
}
