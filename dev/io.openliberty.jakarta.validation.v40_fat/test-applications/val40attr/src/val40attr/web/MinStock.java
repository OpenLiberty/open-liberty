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
package val40attr.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Field-level constraint that enforces a minimum stock level on a {@link Product}.
 *
 * <p>The {@code min} attribute is the key ingredient for demonstrating
 * {@code ConstraintDescriptor#getAttribute(String, Class)} introduced in
 * Jakarta Validation 4.0. Its value can be read back without an unchecked cast:
 *
 * <pre>
 *   // Validation 4.0 — type-safe
 *   int min = descriptor.getAttribute("min", int.class);
 *
 *   // Pre-4.0 — required an unchecked cast
 *   int min = (int) descriptor.getAttributes().get("min");
 * </pre>
 */
@Documented
@Constraint(validatedBy = { MinStockValidator.class })
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinStock {

    /** Minimum stock level allowed (inclusive). */
    int min() default 1;

    String message() default "stock must be at least {min}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
