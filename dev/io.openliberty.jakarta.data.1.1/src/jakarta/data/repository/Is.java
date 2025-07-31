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
package jakarta.data.repository;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.data.constraint.Constraint;
import jakarta.data.constraint.EqualTo;

/**
 * Method signatures are copied from Jakarta Data.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Is {

    @SuppressWarnings("rawtypes")
    Class<? extends Constraint> value() default EqualTo.class;
}
