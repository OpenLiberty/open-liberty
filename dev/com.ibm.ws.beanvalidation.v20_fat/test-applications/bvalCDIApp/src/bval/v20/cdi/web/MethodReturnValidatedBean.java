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
package bval.v20.cdi.web;

import java.time.Instant;
import java.time.Year;

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

public class MethodReturnValidatedBean {

    // Basic constraints from Bean Validation

    @NotNull
    public String notNull(String s) {
        return s;
    }

    @Size(min = 3, max = 5)
    public String size3to5(String s) {
        return s;
    }

    @Min(5)
    public int min5(int i) {
        return i;
    }

    @Max(100)
    public long max100(long l) {
        return l;
    }

    // New built-in constraints in Bean Validation 2.0

    @Email(regexp = ".*@example.com")
    public String email(String s) {
        return s;
    }

    @Positive
    public short positive(short s) {
        return s;
    }

    @PositiveOrZero
    public int positiveOrZero(int i) {
        return i;
    }

    @Negative
    public long negative(long l) {
        return l;
    }

    @NegativeOrZero
    public int negativeOrZero(int i) {
        return i;
    }

    @Past
    public java.time.Year past(Year y) {
        return y;
    }

    @Future
    public java.time.Instant future(Instant i) {
        return i;
    }

    @Valid
    public NestedBean validBean(NestedBean b) {
        return b;
    }

    class NestedBean {
        @Positive
        public int positive = 1;

        public NestedBean() {}
    }
}
