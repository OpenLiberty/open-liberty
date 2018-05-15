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
 * An implementation of Iterable. Returns a custom Iterator for a List.
 * The TestIterator is just a simple Iterator for a List.
 */
public class TestIterable<E> implements Iterable<E> {
    List<E> values;

    public TestIterable(List<E> values) {
        this.values = values;
    }

    @Override
    public Iterator<E> iterator() {
        return new TestIterator<E>(values);
    }

}
