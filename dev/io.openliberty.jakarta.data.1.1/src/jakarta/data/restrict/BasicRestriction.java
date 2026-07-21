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

import jakarta.annotation.Nonnull;
import jakarta.data.constraint.Constraint;
import jakarta.data.expression.Expression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface BasicRestriction<T, V> extends Restriction<T> {

    @Nonnull
    Constraint<V> constraint();

    @Nonnull
    Expression<T, V> expression();

    @Nonnull
    static <T, V> Restriction<T> of(@Nonnull Expression<T, V> expression,
                                    @Nonnull Constraint<V> constraint) {

        return new BasicRestrictionRecord<>(expression, constraint);
    }
}
