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
package jakarta.data.constraint;

import jakarta.data.expression.ComparableExpression;
import jakarta.data.spi.expression.literal.ComparableLiteral;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface AtLeast<V extends Comparable<?>> extends Constraint<V> {

    ComparableExpression<?, V> bound();

    static <V extends Comparable<?>> AtLeast<V> min(ComparableExpression<?, V> minimum) {
        return new AtLeastRecord<>(minimum);
    }

    static <V extends Comparable<?>> AtLeast<V> min(V minimum) {
        return new AtLeastRecord<>(ComparableLiteral.of(minimum));
    }

}
