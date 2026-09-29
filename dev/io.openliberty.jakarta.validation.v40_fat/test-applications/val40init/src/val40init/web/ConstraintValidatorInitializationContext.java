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

import jakarta.validation.ClockProvider;

/**
 * Placeholder for the {@code ConstraintValidatorInitializationContext} interface
 * introduced in Jakarta Validation 4.0.
 *
 * <p>This interface is NOT yet present in the 4.0.0-M1 spec jar. It is defined
 * here locally so the sample application compiles and demonstrates the intended
 * programming model. Once the final 4.0 spec jar ships this interface, this file
 * must be deleted and all references updated to use:
 * <pre>
 *   import jakarta.validation.ConstraintValidatorInitializationContext;
 * </pre>
 *
 * <p>The real interface will live in the {@code jakarta.validation} package and
 * will be part of the {@code ConstraintValidator} contract.
 *
 * TODO: Delete this file and update imports when jakarta.validation-api 4.0 GA ships.
 */
public interface ConstraintValidatorInitializationContext {

    /**
     * Returns the {@link ClockProvider} configured for the validator factory.
     *
     * <p>Validators can capture this once at initialisation time instead of
     * fetching it from the {@code ConstraintValidatorContext} on every
     * {@code isValid} call.
     *
     * @return the clock provider, never {@code null}
     */
    ClockProvider getClockProvider();

    /**
     * Unwrap this context to the specified type, allowing access to
     * provider-specific extensions (e.g. Hibernate Validator internals).
     *
     * @param type the type to unwrap to
     * @return the unwrapped object
     * @throws jakarta.validation.ValidationException if the context cannot be
     *         unwrapped to the requested type
     */
    <T> T unwrap(Class<T> type);
}
