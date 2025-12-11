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
package test.jakarta.data.jpa.web;

/**
 * Java class that needs an AttributeConverter to allow for comparisons of
 * order and sorting.
 */
public class ZipCode {
    final int digits;

    private ZipCode(int digits) {
        this.digits = digits;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ZipCode z && digits == z.digits;
    }

    @Override
    public int hashCode() {
        return digits;
    }

    static ZipCode of(int digits) {
        return new ZipCode(digits);
    }

    @Override
    public String toString() {
        return Integer.toString(digits);
    }
}
