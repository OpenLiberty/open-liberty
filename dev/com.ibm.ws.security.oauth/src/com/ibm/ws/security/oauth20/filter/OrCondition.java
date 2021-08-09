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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class OrCondition implements ICondition {
    List<IValue> values = new LinkedList<IValue>();
    String key;

    /**
     * create an OrCondition from an existing list of values. 
     */
    public OrCondition(String key, List<IValue> values) {
        this.values = values;
        this.key = key;
    }

    /**
     * Create an OrCondition from a single key. To make this work, you'll need to call addValue() later.
     * @param key
     */
    public OrCondition(String key) {
        this.key = key;
    }

    /**
     * Loop through all of the values and see if any of them pass the equality test
     */

    public boolean checkCondition(IValue test) throws FilterException {
        Iterator<IValue> iter = values.iterator();
        while (iter.hasNext()) {
            IValue value = (IValue) iter.next();
            if (value.containedBy(test)) {
                return true;
            }
        }
        return false;
    }

    public String getKey() {
        return key;
    }

    /**
     * helper method to add values to the existing condition.
     * @param value
     */
    public void addValue(IValue value) {
        values.add(value);
    }

    // expensive
    public String toString() {
        StringBuffer buf = new StringBuffer();
        Iterator<IValue> iter = values.iterator();
        while (iter.hasNext()) {
            IValue value = (IValue) iter.next();
            buf.append(value);
            buf.append('|');
        }
        buf.append("^=");
        return buf.toString();
    }
}
