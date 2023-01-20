/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

public enum Compare {
    Between(null),
    Contains(null),
    Empty(null),
    EndsWith(null),
    Equal(null),
    False(null),
    GreaterThan(null),
    GreaterThanEqual(null),
    In(null),
    LessThan(null),
    LessThanEqual(null),
    Like(null),
    Null(null),
    StartsWith(null),
    True(null),
    Not(Equal),
    NotContains(Contains),
    NotEmpty(Empty),
    NotEndsWith(EndsWith),
    NotIn(In),
    NotLike(Like),
    NotNull(Null),
    NotStartsWith(StartsWith);

    private Compare negatedFrom;

    private Compare(Compare negatedFrom) {
        this.negatedFrom = negatedFrom;
    }

    /**
     * For conditions that begin with {@code Not}, returns the condition
     * that was negated to form this condition.
     *
     * @return the condition that was negated to form this condition.
     *         Null if this condition begins with something other than {@code Not}.
     */
    public final Compare negatedFrom() {
        return negatedFrom;
    }
}
