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

import jakarta.data.constraint.Constraint;
import jakarta.data.expression.Expression;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface BasicRestriction<T, V> extends Restriction<T> {

    Constraint<V> constraint();

    Expression<T, V> expression();

    static <T, V> Restriction<T> of(Expression<T, V> expression,
                                    Constraint<V> constraint) {

        return new BasicRestrictionRecord<>(expression, constraint);
    }
}
