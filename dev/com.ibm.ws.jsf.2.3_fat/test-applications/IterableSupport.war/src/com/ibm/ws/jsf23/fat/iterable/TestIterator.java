/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.iterable;

import java.util.Iterator;
import java.util.List;

/**
 * This is a simple Iterator. The idea here is that it will
 * iterate over the values in a list.
 */
public class TestIterator<E> implements Iterator<E> {
    private final List<E> testValues;
    private int currentIndex = 0;

    public TestIterator(List<E> testValues) {
        this.testValues = testValues;
    }

    @Override
    public boolean hasNext() {
        boolean retVal = false;

        // If there are still values to explore in the list
        // then return true.
        if (currentIndex < testValues.size()) {
            retVal = true;
        } else {
            retVal = false;
        }

        return retVal;
    }

    @Override
    public E next() {
        E retVal = testValues.get(currentIndex);
        currentIndex++;

        return retVal;
    }

}
