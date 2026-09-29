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

/**
 * Simple bean representing a scheduled appointment.
 *
 * <p>The {@code appointmentDate} field carries {@link FutureDate} to demonstrate
 * the new {@code ConstraintValidator#initialize(ConstraintDescriptor, Context)}
 * overload introduced in Jakarta Validation 4.0.
 */
public class Appointment {

    @FutureDate
    private final LocalDate appointmentDate;

    private final String description;

    public Appointment(LocalDate appointmentDate, String description) {
        this.appointmentDate = appointmentDate;
        this.description = description;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public String getDescription() {
        return description;
    }
}
