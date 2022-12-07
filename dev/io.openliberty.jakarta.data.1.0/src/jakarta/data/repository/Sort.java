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
package jakarta.data.repository;

/**
 * Method signatures copied from jakarta.data.repository.Sort from the Jakarta Data repo.
 */
public class Sort {
    private final boolean asc;
    private final boolean ignoreCase;
    private final String prop;

    private Sort(boolean ascending, String property, boolean ignoreCase) {
        asc = ascending;
        this.ignoreCase = ignoreCase;
        prop = property;
    }

    public static Sort asc(String property) {
        return new Sort(true, property, false);
    }

    public static Sort ascIgnoreCase(String property) {
        return new Sort(true, property, true);
    }

    public static Sort desc(String property) {
        return new Sort(false, property, false);
    }

    public static Sort descIgnoreCase(String property) {
        return new Sort(false, property, true);
    }

    public static Sort of(String property, Direction direction, boolean ignoreCase) {
        return new Sort(direction == Direction.ASC, property, ignoreCase);
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
        return "Sort by " + prop + (asc ? " ASC" : " DESC") + (ignoreCase ? " ignore case" : "");
    }
}
