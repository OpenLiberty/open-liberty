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

/**
 * Method signatures copied from jakarta.data.repository.Sort
 */
public class Sort {
    private final boolean asc;
    private final String prop;

    private Sort(boolean ascending, String property) {
        asc = ascending;
        prop = property;
    }

    public static Sort asc(String property) {
        return new Sort(true, property);
    }

    public static Sort desc(String property) {
        return new Sort(false, property);
    }

    public static Sort of(String property, Direction direction) {
        return new Sort(direction == Direction.ASC, property);
    }

    public boolean isAscending() {
        return asc;
    }

    public boolean isDescending() {
        return !asc;
    }

    public String getProperty() {
        return prop;
    }

    @Override
    public String toString() {
        return "Sort by " + prop + (asc ? " ASC" : " DESC");
    }
}
