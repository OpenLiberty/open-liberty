/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.web;

import jakarta.data.repository.Select;

/**
 * Represents a slope.
 * The rise and run components are directly hard coded to map to
 * Fraction.numerator and Fraction.denominator, respectively,
 * by this record.
 */
public record Slope(
                @Select(_Fraction.NUMERATOR) //
                int rise,
                @Select(_Fraction.DENOMINATOR) //
                int run) {
    @Override
    public String toString() {
        return "Slope of " + rise + "/" + run;
    }
}
