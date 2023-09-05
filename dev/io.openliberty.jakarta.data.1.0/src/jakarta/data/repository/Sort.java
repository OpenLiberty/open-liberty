/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package jakarta.data.repository;

import java.util.Objects;

/**
 * Method signatures copied from jakarta.data.repository.Sort from the Jakarta Data repo.
 */
public final class Sort {
    private final boolean asc;
    private final boolean ignoreCase;
    private final String prop;
    private final int hash;

    private Sort(Direction direction, String property, boolean ignoreCase) {
        asc = direction == Direction.ASC;
        this.ignoreCase = ignoreCase;
        prop = property;
        hash = Objects.hash(property, direction, ignoreCase);

        if (property == null)
            throw new NullPointerException("property is required");
    }

    public static Sort asc(String property) {
        return new Sort(Direction.ASC, property, false);
    }

    public static Sort ascIgnoreCase(String property) {
        return new Sort(Direction.ASC, property, true);
    }

    public static Sort desc(String property) {
        return new Sort(Direction.DESC, property, false);
    }

    public static Sort descIgnoreCase(String property) {
        return new Sort(Direction.DESC, property, true);
    }

    public static Sort of(String property, Direction direction, boolean ignoreCase) {
        if (direction == null)
            throw new NullPointerException("direction is required");

        return new Sort(direction, property, ignoreCase);
    }

    @Override
    public boolean equals(Object s) {
        Sort sort;
        return this == s
               || s != null
                  && s.getClass() == getClass()
                  && (sort = (Sort) s).hash == hash
                  && sort.asc == asc
                  && sort.ignoreCase == ignoreCase
                  && sort.prop.equals(prop);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public boolean ignoreCase() {
        return ignoreCase;
    }

    public boolean isAscending() {
        return asc;
    }

    public boolean isDescending() {
        return !asc;
    }

    public String property() {
        return prop;
    }

    @Override
    public String toString() {
        return "Sort{property='" + prop +
               "', direction=" + (asc ? "ASC" : "DESC") +
               (ignoreCase ? ", ignore case}" : "}");
    }
}
