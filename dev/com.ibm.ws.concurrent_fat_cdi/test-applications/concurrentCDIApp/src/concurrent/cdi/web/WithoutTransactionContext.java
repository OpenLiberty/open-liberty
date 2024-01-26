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
import jakarta.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target(FIELD)
public @interface WithoutTransactionContext {

    /**
     * A qualifier attribute such as this must have a default value to be usable
     * on a Concurrency resource definition.
     */
    String letter() default "A";

    /**
     * A qualifier attribute such as this must have a default value to be usable
     * on a Concurrency resource definition.
     */
    int number() default 10;

    public static class Literal extends AnnotationLiteral<WithoutTransactionContext> implements WithoutTransactionContext {
        private static final long serialVersionUID = -3978433230176629819L;

        public static final WithoutTransactionContext INSTANCE = new Literal("A", 10);

        private final String letter;
        private final int number;

        private Literal(String letter, int number) {
            this.letter = letter;
            this.number = number;
        }

        @Override
        public String letter() {
            return letter;
        }

        @Override
        public int number() {
            return number;
        }

        public static final WithoutTransactionContext of(String letter, int number) {
            return new Literal(letter, number);
        }

    }
}