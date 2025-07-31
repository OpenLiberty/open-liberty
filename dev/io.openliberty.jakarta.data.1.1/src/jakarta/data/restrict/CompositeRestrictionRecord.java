/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package jakarta.data.restrict;

import java.util.List;
import java.util.Objects;

/**
 * Method signatures are copied from Jakarta Data.
 */
record CompositeRestrictionRecord<T>(
                Type type,
                List<Restriction<? super T>> restrictions,
                boolean isNegated)
                implements CompositeRestriction<T> {

    private static final int SINGLE_RESTRICTION_LENGTH_ESTIMATE = 100;

    CompositeRestrictionRecord {

        Objects.requireNonNull(restrictions, "restrictions");

        if (restrictions.isEmpty())
            throw new IllegalArgumentException("restrictions");

        restrictions.forEach(r -> Objects.requireNonNull(r, "restriction"));
    }

    CompositeRestrictionRecord(Type type,
                               List<Restriction<? super T>> restrictions) {

        this(type, restrictions, false);
    }

    @Override
    public CompositeRestriction<T> negate() {

        return new CompositeRestrictionRecord<>(type, restrictions, !isNegated);
    }

    @Override
    public String toString() {

        String op = type.asQueryLanguage();
        int len = 6 + restrictions.size() * SINGLE_RESTRICTION_LENGTH_ESTIMATE;
        StringBuilder builder = new StringBuilder(len);
        if (isNegated)
            builder.append("NOT (");

        boolean first = true;
        for (Restriction<? super T> restriction : restrictions) {
            if (first)
                first = false;
            else
                builder.append(' ').append(op).append(' ');

            builder.append('(').append(restriction).append(')');
        }

        if (isNegated)
            builder.append(')');

        return builder.toString();
    }
}