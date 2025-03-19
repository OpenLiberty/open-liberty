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

import static io.openliberty.data.repository.Constants.IGNORE_CASE;
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
        IgnoreCase(Equal, IGNORE_CASE),
        In,
        LessThan,
        LessThanEqual,
        Like,
        LikeIgnoreCase(Like, IGNORE_CASE),
        Prefixed,
        PrefixedIgnoreCase(Prefixed, IGNORE_CASE),
        Substringed,
        SubstringedIgnoreCase(Substringed, IGNORE_CASE),
        Suffixed,
        SuffixedIgnoreCase(Suffixed, IGNORE_CASE),
        Not(NOT, Equal),
        NotIgnoreCase(NOT, Equal, IGNORE_CASE),
        NotIn(NOT, In),
        NotLike(NOT, Like),
        NotLikeIgnoreCase(NOT, Like, IGNORE_CASE),
        NotPrefixed(NOT, Prefixed),
        NotPrefixedIgnoreCase(NOT, Prefixed, IGNORE_CASE),
        NotSubstringed(NOT, Substringed),
        NotSubstringedIgnoreCase(NOT, Substringed, IGNORE_CASE),
        NotSuffixed(NOT, Suffixed),
        NotSuffixedIgnoreCase(NOT, Suffixed, IGNORE_CASE);

        // The remaining part of this class is here to help Jakarta Data providers
        // interpret the enumerated constants. It can be removed if it doesn't seem
        // helpful.

        private final Op base;

        private final boolean ignoreCase;

        private final boolean isNegative;

        private Op() {
            this.base = this;
            this.ignoreCase = false;
            this.isNegative = false;
        }

        private Op(int not, Op baseOp) {
            this.base = baseOp;
            this.ignoreCase = false;
            this.isNegative = not == NOT; 
        }

        private Op(int not, Op baseOp, boolean ignoreCase) {
            this.base = baseOp;
            this.ignoreCase = ignoreCase;
            this.isNegative = not == NOT; 
        }

        private Op(Op baseOp, boolean ignoreCase) {
            this.base = baseOp;
            this.ignoreCase = ignoreCase;
            this.isNegative = false;
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
         * Whether to request that case be ignored.
         *
         * @return whether to request that case be ignored.
         */
        public boolean ignoreCase() {
            return ignoreCase;
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
    static final boolean IGNORE_CASE = true;
    static final int NOT = -1;
}
