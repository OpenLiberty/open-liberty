/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.data;

import java.util.ArrayList;

/**
 *
 */
public class GDArrayList extends ArrayList<KeyValuePair> {

    /**
    *
    */
    public GDArrayList() {
        super();
    }

    /**
     * @param size
     */
    public GDArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * If the specified position is within range, replaces the KeyValuePair at the specified position in this list with the specified KeyValuePair.
     * If the specified position is out of range, expands list to size <code>index</code> and adds KeyValuePair to the end of this list.
     *
     * @param index index of the element to replace
     * @param kvp KeyValuePair to be stored at the specified position
     */
    public void setAt(int index, KeyValuePair kvp) {
        if (index >= this.size()) {
            while (index > this.size()) {
                this.add(null);
            }
            this.add(kvp);
        } else if (index < this.size()) {
            this.set(index, kvp);
        }
    }
}
