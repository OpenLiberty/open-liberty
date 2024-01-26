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
public @interface WithLocationContext {
    /**
     * Can include array type attributes on qualifiers if non-binding is specified,
     * in which case the attribute is ignored when determining whether a bean can inject.
     */
    @Nonbinding
    String[] pairedWith() default {};

    public static class Literal extends AnnotationLiteral<WithLocationContext> implements WithLocationContext {
        private static final long serialVersionUID = 2667072598927683578L;

        public static final WithLocationContext INSTANCE = new Literal();

        private final String[] otherTypes;

        private Literal(String... otherContextTypes) {
            otherTypes = otherContextTypes;
        }

        @Override
        public String[] pairedWith() {
            return otherTypes;
        }

        public static WithLocationContext with(String... otherContextTypes) {
            return new Literal(otherContextTypes);
        }
    }
}