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

/**
 * This qualifier is used on resource definition annotations,
 * but is overridden by a matching deployment descriptor element.
 */
@Qualifier
@Retention(RUNTIME)
@Target(FIELD)
public @interface OverriddenQualifier1 {
    public static class Literal extends AnnotationLiteral<OverriddenQualifier1> implements OverriddenQualifier1 {
        private static final long serialVersionUID = 1410271308588798699L;

        public static final OverriddenQualifier1 INSTANCE = new Literal();

        private Literal() {
        }
    }
}