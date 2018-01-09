/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package bval.v20.web;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

public class FieldValidatedBean {

    // Basic constraints from Bean Validation

    @NotNull
    public String notNull = "";

    @Size(min = 3, max = 5)
    public String size3to5 = "abc";

    @Min(5)
    public int min5 = 5;

    @Max(100)
    public long max100 = 0;

    // New built-in constraints in Bean Validation 2.0

    @Email(regexp = ".*@example.com")
    public String email = "test@example.com";

    @Positive
    public short positive = 1;

    @PositiveOrZero
    public int positiveOrZero = 0;

    @Negative
    public long negative = -1;

    @NegativeOrZero
    public int negativeOrZero = -1;

    @Past
    public java.time.Year past = Year.of(1990);

    @Future
    public java.time.Instant future = Instant.now().plus(1, ChronoUnit.HOURS);

    @Valid
    public NestedBean validBean = new NestedBean();

    class NestedBean {
        @Positive
        public int positive = 1;
    }

}
