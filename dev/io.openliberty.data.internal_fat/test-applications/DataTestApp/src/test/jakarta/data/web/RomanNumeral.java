/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import java.util.ArrayList;

/**
 * A record to use as a result type for selecting attributes of the Prime entity.
 */
public record RomanNumeral(
                String name,
                String romanNumeral
// , TODO enable once EclipseLink bug #30501 is fixed
// ArrayList<String> romanNumeralSymbols
) {

    /**
     * Format in an easy way for tests to compare results.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name).append(" ");
        s.append(romanNumeral).append(" ( ");
        for (char symbol : romanNumeral.toCharArray())
        // TODO replace the above with the following once EclipseLink bug #30501 is fixed
        //for (String symbol : romanNumeralSymbols)
            s.append(symbol).append(' ');
        s.append(")");
        return s.toString();
    }
}
