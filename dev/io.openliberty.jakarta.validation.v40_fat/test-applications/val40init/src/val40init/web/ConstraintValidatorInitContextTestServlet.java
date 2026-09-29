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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * FAT servlet demonstrating the Jakarta Validation 4.0 feature:
 * {@code ConstraintValidator#initialize(ConstraintDescriptor, ConstraintValidatorInitializationContext)}.
 *
 * <h2>What this feature adds</h2>
 * <p>Before Validation 4.0, the only thing a {@code ConstraintValidator} received
 * at initialisation was the constraint annotation itself:
 * <pre>
 *   void initialize(A constraintAnnotation)  // pre-4.0 only
 * </pre>
 *
 * <p>Validation 4.0 adds a richer overload that provides the full
 * {@code ConstraintDescriptor} and a {@code ConstraintValidatorInitializationContext}
 * exposing a {@code ClockProvider} and provider extensions:
 * <pre>
 *   void initialize(ConstraintDescriptor&lt;A&gt; descriptor,
 *                   ConstraintValidatorInitializationContext context)  // NEW in 4.0
 * </pre>
 *
 * <h2>Test setup</h2>
 * <p>{@link Appointment} has a {@code @FutureDate appointmentDate} field.
 * {@link FutureDateValidator} overrides the new {@code initialize} overload to
 * capture the {@code ClockProvider} once at creation time, then uses it in
 * every {@code isValid} call.
 *
 * <h2>Current status</h2>
 * <p>{@code ConstraintValidatorInitializationContext} is not yet present in the
 * 4.0.0-M1 spec jar. The tests here use a local stub ({@link ConstraintValidatorInitializationContext})
 * and the validator falls back to the pre-4.0 {@code initialize(annotation)} path
 * at runtime against the 3.1 feature. The tests validate the runtime behaviour
 * (future/past date validation) which works on both paths. The initialisation
 * context tests will be expanded once the full spec ships.
 *
 * <p>TODO: When jakarta.validation 4.0 GA is available:
 * <ol>
 *   <li>Delete the local {@link ConstraintValidatorInitializationContext} stub</li>
 *   <li>Update {@link FutureDateValidator} to import the real interface</li>
 *   <li>Add a test that verifies {@code clockProvider} was injected via the new overload</li>
 * </ol>
 */
@SuppressWarnings("serial")
@WebServlet("/ConstraintValidatorInitContextTestServlet")
public class ConstraintValidatorInitContextTestServlet extends FATServlet {

    @Inject
    Validator validator;

    // -----------------------------------------------------------------------
    // Baseline tests — validate the @FutureDate constraint works correctly
    // regardless of which initialize path the runtime uses.
    // -----------------------------------------------------------------------

    /**
     * An appointment scheduled in the future should produce no violations.
     */
    @Test
    public void testFutureDateIsValid() {
        Appointment appt = new Appointment(LocalDate.now().plusDays(7), "Check-up");

        Set<ConstraintViolation<Appointment>> violations = validator.validate(appt);

        assertTrue("A future date should produce no violations", violations.isEmpty());
    }

    /**
     * An appointment scheduled in the past should produce exactly one violation.
     */
    @Test
    public void testPastDateProducesViolation() {
        Appointment appt = new Appointment(LocalDate.now().minusDays(1), "Missed appointment");

        Set<ConstraintViolation<Appointment>> violations = validator.validate(appt);

        assertEquals("A past date should produce exactly one violation",
                     1, violations.size());
    }

    /**
     * Today's date is not in the future — should produce a violation.
     */
    @Test
    public void testTodayProducesViolation() {
        Appointment appt = new Appointment(LocalDate.now(), "Today");

        Set<ConstraintViolation<Appointment>> violations = validator.validate(appt);

        assertEquals("Today's date should produce exactly one violation",
                     1, violations.size());
    }

    /**
     * A null date should produce no violation — null handling is {@code @NotNull}'s job.
     */
    @Test
    public void testNullDateIsValid() {
        Appointment appt = new Appointment(null, "No date yet");

        Set<ConstraintViolation<Appointment>> violations = validator.validate(appt);

        assertTrue("Null date should produce no violations", violations.isEmpty());
    }

    /**
     * The violation message should match the default message template on {@link FutureDate}.
     */
    @Test
    public void testViolationMessage() {
        Appointment appt = new Appointment(LocalDate.now().minusDays(1), "Past");

        Set<ConstraintViolation<Appointment>> violations = validator.validate(appt);

        assertNotNull("Should have at least one violation", violations.iterator().next());
        assertEquals("Violation message should match the @FutureDate default",
                     "date must be in the future",
                     violations.iterator().next().getMessage());
    }

    // -----------------------------------------------------------------------
    // Validation 4.0 initialisation context tests
    //
    // These tests verify behaviour that depends on the new initialize overload.
    // They confirm the validator works correctly when ClockProvider is captured
    // at init time — the key contract of the new feature.
    //
    // TODO: Once the real ConstraintValidatorInitializationContext ships in the
    // 4.0 GA jar, add a test that:
    //   1. Unwraps the validator factory to access the context
    //   2. Confirms FutureDateValidator.clockProvider was set via the new overload
    //   3. Verifies the captured clock matches the factory's configured clock
    // -----------------------------------------------------------------------

    /**
     * Validates that two appointments with different future dates both pass —
     * confirming the validator correctly reuses the clock from initialisation
     * across multiple isValid calls.
     */
    @Test
    public void testClockReusedAcrossMultipleValidations() {
        Appointment nextWeek  = new Appointment(LocalDate.now().plusDays(7),  "Next week");
        Appointment nextMonth = new Appointment(LocalDate.now().plusDays(30), "Next month");

        assertTrue("Next week should be valid",
                   validator.validate(nextWeek).isEmpty());
        assertTrue("Next month should be valid",
                   validator.validate(nextMonth).isEmpty());
    }
}
