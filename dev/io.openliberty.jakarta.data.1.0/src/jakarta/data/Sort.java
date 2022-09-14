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
package jakarta.data;

import java.util.function.BiFunction;

/**
 * Copied from jakarta.nosql.Sort to investigate how well the
 * JNoSQL repository-related classes work for relational database access.
 * Unable to copy the of method because it depends on a JNoSQL class, ServiceLoaderProvider.
 */
public interface Sort {
    String getName();

    SortType getType();

    static Sort of(String name, SortType type) {
        // We don't have the JNoSQL ServiceLoaderProvider here.
        return new SortImpl.Provider().apply(name, type);
    }

    static Sort asc(String name) {
        return of(name, SortType.ASC);
    }

    static Sort desc(String name) {
        return of(name, SortType.DESC);
    }

    interface SortProvider extends BiFunction<String, SortType, Sort> {
    }
}

/**
 * Fake implementation:
 */
class SortImpl implements Sort {
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
