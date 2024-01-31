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
package concurrent.cdi.web;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target(FIELD)
public @interface WithTransactionContext {
    /**
     * Array type attribute is allowed on qualifier as non-binding,
     * which means the attribute is ignored when determining whether a bean can inject.
     */
    @Nonbinding
    Class<?>[] classes() default { long.class, int.class, short.class };

    /**
     * Array type attribute is allowed on qualifier as non-binding,
     * which means the attribute is ignored when determining whether a bean can inject.
     */
    @Nonbinding
    int[] numbers() default { 216, 713, 745 };

    public static class Literal extends AnnotationLiteral<WithTransactionContext> implements WithTransactionContext {
        private static final long serialVersionUID = -938635792865549601L;

        public static final WithTransactionContext INSTANCE = new Literal( //
                        new Class<?>[] { long.class, int.class, short.class }, //
                        new int[] { 216, 713, 745 });

        private final Class<?>[] classes;
        private final int[] numbers;

        private Literal(Class<?>[] classes, int[] numbers) {
            this.classes = classes;
            this.numbers = numbers;
        }

        @Override
        public Class<?>[] classes() {
            return classes;
        }

        @Override
        public int[] numbers() {
            return numbers;
        }

        public static final WithTransactionContext of(Class<?>[] classes, int[] numbers) {
            return new Literal(classes, numbers);
        }

    }
}