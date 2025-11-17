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

/**
 * A record to use as a result type for selecting attributes of the Prime entity.
 * EclipseLink decided to disallow collection attributes such as
 * ArrayList<String> romanNumeralSymbols
 * so it is omitted from from this record.
 */
public record RomanNumeral(
                String name,
                String romanNumeral) {

    /**
     * Format in an easy way for tests to compare results.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name).append(" ");
        s.append(romanNumeral).append(" ( ");
        // If collection attributes are ever allowed, the following could become
        // for (String symbol : romanNumeralSymbols)
        for (char symbol : romanNumeral.toCharArray())
            s.append(symbol).append(' ');
        s.append(")");
        return s.toString();
    }
}
