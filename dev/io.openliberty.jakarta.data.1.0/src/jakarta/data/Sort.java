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
package jakarta.data;

/**
 * Method signatures copied from jakarta.data.repository.Sort from the Jakarta Data repo.
 */
public record Sort<T>(String property,
                boolean isAscending,
                boolean ignoreCase) {

    public Sort {
        if (property == null)
            throw new NullPointerException("property is required");
    }

    public static <T> Sort<T> asc(String property) {
        return new Sort<>(property, true, false);
    }

    public static <T> Sort<T> ascIgnoreCase(String property) {
        return new Sort<>(property, true, true);
    }

    public static <T> Sort<T> desc(String property) {
        return new Sort<>(property, false, false);
    }

    public static <T> Sort<T> descIgnoreCase(String property) {
        return new Sort<>(property, false, true);
    }

    public static <T> Sort<T> of(String property, Direction direction, boolean ignoreCase) {
        if (direction == null)
            throw new NullPointerException("direction is required");

        return new Sort<>(property, direction.equals(Direction.ASC), ignoreCase);
    }

    public boolean isDescending() {
        return !isAscending;
    }
}
