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

import io.openliberty.data.Sort;
import io.openliberty.data.SortType;

/**
 */
public class SortImpl implements Sort {
    private final String name;
    private final SortType type;

    private SortImpl(String name, SortType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SortType getType() {
        return type;
    }

    public static class Provider implements Sort.SortProvider {
        @Override
        public Sort apply(String name, SortType type) {
            return new SortImpl(name, type);
        }
    }
}
