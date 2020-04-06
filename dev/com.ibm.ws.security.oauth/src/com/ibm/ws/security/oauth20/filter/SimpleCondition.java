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

public abstract class SimpleCondition implements ICondition {
    private String key;
    private IValue value;

    protected SimpleCondition(String key, IValue value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public IValue getValue() {
        return value;
    }

    public String toString() {
        return getValue() + getOperand();
    }

    abstract public boolean checkCondition(IValue test) throws FilterException;

    abstract public String getOperand();
}
