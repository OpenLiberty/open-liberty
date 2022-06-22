/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal;

import java.util.ArrayList;
import java.util.List;

import io.openliberty.data.Sort;
import io.openliberty.data.Sorts;

/**
 */
public class SortsImpl implements Sorts {
    private final List<Sort> sorts = new ArrayList<>();

    private SortsImpl() {
    }

    @Override
    public Sorts asc(String name) {
        sorts.add(Sort.asc(name));
        return this;
    }

    @Override
    public Sorts desc(String name) {
        sorts.add(Sort.desc(name));
        return this;
    }

    @Override
    public Sorts add(Sort sort) {
        sorts.add(sort);
        return this;
    }

    @Override
    public Sorts remove(Sort sort) {
        sorts.remove(sort);
        return this;
    }

    @Override
    public List<Sort> getSorts() {
        return new ArrayList<>(sorts);
    }

    public static class Provider implements Sorts.SortsProvider {
        @Override
        public Sorts get() {
            return new SortsImpl();
        }
    }
}
