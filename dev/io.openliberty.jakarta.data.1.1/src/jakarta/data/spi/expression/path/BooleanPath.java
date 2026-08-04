/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package jakarta.data.spi.expression.path;

import jakarta.annotation.Nonnull;
import jakarta.data.expression.BooleanExpression;
import jakarta.data.expression.NavigableExpression;
import jakarta.data.metamodel.BooleanAttribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface BooleanPath<T, U> extends //
                Path<T, U>, //
                BooleanExpression<T> {

    @Nonnull
    static <T, U> BooleanPath<T, U> of(@Nonnull NavigableExpression<T, U> expression,
                                       @Nonnull BooleanAttribute<U> attribute) {

        return new BooleanPathRecord<>(expression, attribute);
    }
}
