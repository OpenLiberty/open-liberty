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
package jakarta.data.metamodel;

import jakarta.annotation.Nonnull;
import java.time.temporal.Temporal;

import jakarta.data.expression.TemporalExpression;
import jakarta.data.messages.Messages;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TemporalAttribute<T, V extends Temporal & Comparable<? extends Temporal>> //
                extends //
                ComparableAttribute<T, V>, //
                TemporalExpression<T, V> {

    @Nonnull
    static <T, V extends Temporal & Comparable<? extends Temporal>> //
    TemporalAttribute<T, V> of(@Nonnull Class<T> entityClass,
                               @Nonnull String name,
                               @Nonnull Class<V> attributeType) {

        Messages.requireNonNull(entityClass, "entityClass");
        Messages.requireNonNull(name, "name");
        Messages.requireNonNull(attributeType, "attributeType");

        return new TemporalAttributeRecord<>(entityClass, name, attributeType);
    }
}
