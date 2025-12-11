/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.models;

import java.util.ArrayList;

public record RomanNumeral(
                String name,
                String romanNumeral,
                ArrayList<String> romanNumeralSymbols
) {

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name).append(" ");
        s.append(romanNumeral).append(" ( ");
        s.append(romanNumeralSymbols).append(' ');
        s.append(")");
        return s.toString();
    }
}
