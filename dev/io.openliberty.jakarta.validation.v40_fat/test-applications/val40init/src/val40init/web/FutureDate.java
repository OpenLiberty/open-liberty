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
package val40init.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Field-level constraint that requires a date to be in the future.
 *
 * <p>Used to demonstrate the Jakarta Validation 4.0 feature:
 * {@code ConstraintValidator#initialize(ConstraintDescriptor, ConstraintValidatorInitializationContext)}.
 *
 * <p>The validator ({@link FutureDateValidator}) overrides the new {@code initialize}
 * overload to capture a {@code ClockProvider} once at initialisation time, rather
 * than fetching it on every {@code isValid} call.
 */
@Documented
@Constraint(validatedBy = { FutureDateValidator.class })
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FutureDate {

    String message() default "date must be in the future";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
