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
package jakarta.data.expression;

import jakarta.data.constraint.EqualTo;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restriction;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface BooleanExpression<T> extends ComparableExpression<T, Boolean> {

    default Restriction<T> isFalse() {
        return BasicRestriction.of(this, EqualTo.value(false));
    }

    default Restriction<T> isTrue() {
        return BasicRestriction.of(this, EqualTo.value(true));
    }

    @Override
    default Class<Boolean> type() {
        return Boolean.class;
    }
}
