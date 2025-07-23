/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.util.Objects;

public class ZipCode {

    private final int value;

    private ZipCode(int value) {
        // Optional validation: enforce 5-digit zip codes
        if (value < 0 || value > 99999) {
            throw new IllegalArgumentException("Invalid zip code: must be 5 digits");
        }
        this.value = value;
    }

    public static ZipCode of(int value) {
        return new ZipCode(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        // Pad with leading zeros, e.g., 02115
        return String.format("%05d", value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZipCode)) return false;
        ZipCode zipCode = (ZipCode) o;
        return value == zipCode.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

