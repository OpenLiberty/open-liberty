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

import java.time.LocalDate;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.metadata.ConstraintDescriptor;

/**
 * Validator for {@link FutureDate} demonstrating the Jakarta Validation 4.0
 * {@code ConstraintValidator#initialize(ConstraintDescriptor, ConstraintValidatorInitializationContext)}
 * overload.
 *
 * <h2>Key difference from pre-4.0</h2>
 *
 * <p><b>Pre-4.0 pattern</b> — clock fetched on every {@code isValid} call:
 * <pre>
 *   public boolean isValid(LocalDate value, ConstraintValidatorContext ctx) {
 *       ClockProvider cp = ctx.getClockProvider();   // fetched every time
 *       return value.isAfter(LocalDate.now(cp.getClock()));
 *   }
 * </pre>
 *
 * <p><b>Validation 4.0 pattern</b> — clock captured once at initialisation:
 * <pre>
 *   public void initialize(ConstraintDescriptor&lt;FutureDate&gt; descriptor,
 *                          ConstraintValidatorInitializationContext ctx) {
 *       this.clockProvider = ctx.getClockProvider();  // stored once
 *   }
 * </pre>
 *
 * <p>The new overload also receives the full {@link ConstraintDescriptor}, giving
 * access to all annotation attributes via the type-safe {@code getAttribute} API
 * (Jakarta Validation 4.0 Feature 2) at initialisation time.
 *
 * <h2>Backwards compatibility</h2>
 * <p>The new {@code initialize(ConstraintDescriptor, Context)} method has a
 * {@code default} implementation in the spec that delegates to the existing
 * {@code initialize(annotation)} method, so all existing validators continue
 * to work without changes.
 *
 * <p><b>NOTE:</b> The {@link ConstraintValidatorInitializationContext} used here
 * is a local placeholder stub. Replace with the real
 * {@code jakarta.validation.ConstraintValidatorInitializationContext} import once
 * the 4.0 GA jar is available.
 */
public class FutureDateValidator implements ConstraintValidator<FutureDate, LocalDate> {

    /**
     * Captured once at initialisation via the new 4.0 initialize overload.
     * Null when running against a pre-4.0 runtime (falls back to the legacy
     * initialize method).
     */
    private ClockProvider clockProvider;

    // -----------------------------------------------------------------------
    // Validation 4.0 — new initialize overload
    //
    // Receives the full ConstraintDescriptor (so getAttribute is also available)
    // plus a ConstraintValidatorInitializationContext exposing ClockProvider.
    //
    // TODO: Update the parameter type to jakarta.validation.ConstraintValidatorInitializationContext
    //       once the 4.0 GA jar ships and the local stub is removed.
    // -----------------------------------------------------------------------
    public void initialize(ConstraintDescriptor<FutureDate> descriptor,
                           ConstraintValidatorInitializationContext initializationContext) {
        // Capture the ClockProvider once — used in every isValid call
        this.clockProvider = initializationContext.getClockProvider();
    }

    // -----------------------------------------------------------------------
    // Existing initialize overload — called by pre-4.0 runtimes
    // -----------------------------------------------------------------------
    @Override
    public void initialize(FutureDate annotation) {
        // clockProvider remains null — will be fetched lazily in isValid
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null handling is @NotNull's responsibility
        }

        // Use the pre-captured ClockProvider if available (4.0 path),
        // otherwise fall back to fetching it from the context (pre-4.0 path)
        ClockProvider cp = (clockProvider != null)
                           ? clockProvider
                           : context.getClockProvider();

        return value.isAfter(LocalDate.now(cp.getClock()));
    }
}
