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
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Do not list this qualifier on definitions for any of the Concurrency resources.
 * It is used to test what happens when an attempt is made to inject a Concurrency resource
 * into an injection point that has unrecognized qualifiers.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, TYPE })
public @interface Unrecognized {
    public static class Literal extends AnnotationLiteral<Unrecognized> implements Unrecognized {
        private static final long serialVersionUID = 820070466281499464L;

        public static final Unrecognized INSTANCE = new Literal();

        private Literal() {
        }
    }
}