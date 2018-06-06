/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

public abstract class SimpleCondition implements ICondition {
    private final String key;
    private final IValue value;
    protected final String operand;

    protected SimpleCondition(String key, IValue value, String operand) {
        this.key = key;
        this.value = value;
        this.operand = operand;
    }

    @Override
    public String getKey() {
        return key;
    }

    public IValue getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue() + " " + getOperand();
    }

    @Override
    abstract public boolean checkCondition(IValue test) throws FilterException;

    abstract public String getOperand();
}
