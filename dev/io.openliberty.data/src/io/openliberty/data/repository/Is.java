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
        IgnoreCase(Equal),
        In,
        LessThan,
        LessThanEqual,
        Like,
        LikeIgnoreCase(Like),
        Prefixed,
        PrefixedIgnoreCase(Prefixed),
        Substringed,
        SubstringedIgnoreCase(Substringed),
        Suffixed,
        SuffixedIgnoreCase(Suffixed),
        Not(Equal),
        NotIgnoreCase(Equal),
        NotIn(In),
        NotLike(Like),
        NotLikeIgnoreCase(Like),
        NotPrefixed(Prefixed),
        NotPrefixedIgnoreCase(Prefixed),
        NotSubstringed,
        NotSubstringedIgnoreCase(Substringed),
        NotSuffixed(Suffixed),
        NotSuffixedIgnoreCase(Suffixed);

        private final Op base;
        private final boolean ignoreCase;
        private final boolean negative;

        private Op() {
            String name = name();
            base = this;
            negative = name.startsWith("Not");
            ignoreCase = name.endsWith("IgnoreCase");
        }

        private Op(Op baseOp) {
            String name = name();
            base = baseOp;
            negative = name.startsWith("Not");
            ignoreCase = name.endsWith("IgnoreCase");
        }

        public Op base() {
            return base;
        }

        public boolean ignoreCase() {
            return ignoreCase;
        }

        public boolean isNegative() {
            return negative;
        }
    }
}
