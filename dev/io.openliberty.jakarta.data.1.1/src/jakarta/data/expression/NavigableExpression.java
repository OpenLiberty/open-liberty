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
package jakarta.data.expression;

import java.time.temporal.Temporal;

import jakarta.data.metamodel.ComparableAttribute;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.TemporalAttribute;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.spi.expression.path.ComparablePath;
import jakarta.data.spi.expression.path.NavigablePath;
import jakarta.data.spi.expression.path.NumericPath;
import jakarta.data.spi.expression.path.TemporalPath;
import jakarta.data.spi.expression.path.TextPath;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NavigableExpression<T, U> {

    default <C extends Comparable<C>> ComparableExpression<T, C> //
                    navigate(ComparableAttribute<U, C> attribute) {
        return ComparablePath.of(this, attribute);
    }

    default <V> NavigableExpression<T, V> //
                    navigate(NavigableAttribute<U, V> attribute) {
        return NavigablePath.of(this, attribute);
    }

    default <N extends Number & Comparable<N>> NumericExpression<T, N> //
                    navigate(NumericAttribute<U, N> attribute) {
        return NumericPath.of(this, attribute);
    }

    default <V extends Temporal & Comparable<? extends Temporal>> //
                    TemporalExpression<T, V> //
                    navigate(TemporalAttribute<U, V> attribute) {
        return TemporalPath.of(this, attribute);
    }

    default TextExpression<T> navigate(TextAttribute<U> attribute) {
        return TextPath.of(this, attribute);
    }
}
