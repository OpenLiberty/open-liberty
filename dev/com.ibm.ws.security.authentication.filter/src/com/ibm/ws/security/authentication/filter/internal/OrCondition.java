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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class OrCondition implements ICondition {
    List values = new LinkedList();
    String key;
    String operand;

    /**
     * create an OrCondition from an existing list of values.
     */
    public OrCondition(String key, List values) {
        this.values = values;
        this.key = key;
    }

    /**
     * Create an OrCondition from a single key. To make this work, you'll need to call addValue() later.
     * 
     * @param key
     */
    public OrCondition(String key, String operand) {
        this.key = key;
        this.operand = operand;
    }

    /**
     * Loop through all of the values and see if any of them pass the equality test
     */

    @Override
    public boolean checkCondition(IValue test) throws FilterException {
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            IValue value = (IValue) iter.next();
            if (value.containedBy(test)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getKey() {
        return key;
    }

    /**
     * helper method to add values to the existing condition.
     * 
     * @param value
     */
    public void addValue(IValue value) {
        values.add(value);
    }

    //expensive
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            IValue value = (IValue) iter.next();
            buf.append(value);
            buf.append('|');
        }
        int lastPipe = buf.lastIndexOf("|");
        if (lastPipe != -1) {
            buf.replace(lastPipe, lastPipe + 1, " ");
        }
        buf.append(operand);
        return buf.toString();
    }
}
