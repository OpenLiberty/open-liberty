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
package test.jakarta.data.experimental.web;

/**
 * A record to use as a result type for selecting attributes of the Prime entity.
 */
public record Hexadecimal(
                String numerals,
                long decimalValue) {
    /**
     * Format in an easy way for tests to compare results.
     */
    @Override
    public String toString() {
        return "Hexadecimal " + numerals + " (" + decimalValue + ")";
    }
}
