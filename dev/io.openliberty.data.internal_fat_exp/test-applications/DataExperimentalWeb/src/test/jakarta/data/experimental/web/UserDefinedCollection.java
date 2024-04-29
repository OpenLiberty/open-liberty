/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.experimental.web;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Used to test if a user-defined collection class can be used
 * for a repository method return type.
 */
public class UserDefinedCollection<T> extends AbstractCollection<T> {
    private final List<T> list = new ArrayList<T>();

    @Override
    public boolean add(T element) {
        return list.add(element);
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public int size() {
        return list.size();
    }
}
