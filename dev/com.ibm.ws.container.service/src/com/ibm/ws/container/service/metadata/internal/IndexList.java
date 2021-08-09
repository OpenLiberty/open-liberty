/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.internal;

import java.util.Arrays;

public class IndexList {
    private int nextValue;
    private int[] freeValues = new int[8];
    private int nextFreeIndex;

    public int reserve() {
        if (nextFreeIndex == 0) {
            return nextValue++;
        }

        return freeValues[--nextFreeIndex];
    }

    public void unreserve(int value) {
        if (nextFreeIndex == freeValues.length) {
            freeValues = Arrays.copyOf(freeValues, freeValues.length + freeValues.length);
        }
        freeValues[nextFreeIndex++] = value;
    }
}
