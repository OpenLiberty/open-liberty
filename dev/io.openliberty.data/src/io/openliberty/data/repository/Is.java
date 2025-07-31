/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.data.repository;

import static io.openliberty.data.repository.Constants.NOT;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Is {
    Op value() default Op.Equal;

    public static enum Op {
        Equal,
        GreaterThan,
        GreaterThanEqual,
        In,
        LessThan,
        LessThanEqual,
        Like,
        Prefixed,
        Substringed,
        Suffixed,
        Not(NOT, Equal),
        NotIn(NOT, In),
        NotLike(NOT, Like),
        NotPrefixed(NOT, Prefixed),
        NotSubstringed(NOT, Substringed),
        NotSuffixed(NOT, Suffixed);

        // The remaining part of this class is here to help Jakarta Data providers
        // interpret the enumerated constants. It can be removed if it doesn't seem
        // helpful.

        private final Op base;

        private final boolean isNegative;

        private Op() {
            this.base = this;
            this.isNegative = false;
        }

        private Op(int not, Op baseOp) {
            this.base = baseOp;
            this.isNegative = not == NOT;
        }

        /**
         * The base comparison operation without negation or ignoring of case.
         * For example, the base operation for {@link Op#NotLike NotLike}
         * is {@link Op#Like Like}.
         *
         * @return the base comparison operation.
         */
        public Op base() {
            return base;
        }

        /**
         * Whether this comparison is a negative comparison.
         * For example, the {@link Op#NotLike NotLike} comparison operation
         * is a negation of the {@link Op#Like Like} comparison operation.
         *
         * @return whether this comparison is a negative comparison.
         */
        public boolean isNegative() {
            return isNegative;
        }
    }
}

//Internal constants that make constructors for the enumerated values more readable
class Constants {
    static final int NOT = -1;
}
