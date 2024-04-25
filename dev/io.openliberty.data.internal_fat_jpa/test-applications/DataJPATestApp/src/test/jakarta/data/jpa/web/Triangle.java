/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import java.util.Arrays;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Entity with some less common Jakarta Data basic types
 * that aren't tested anywhere else.
 */
@Entity
public class Triangle {

    @Basic(optional = false)
    @GeneratedValue
    @Id
    public Integer distinctKey;

    @Basic(optional = true)
    Byte hypotenuse;

    @Basic(optional = false)
    short perimeter;

    @Basic(optional = false)
    Short sameLengthSides; // 0, 2, or 3

    @Basic(optional = false)
    byte[] sides;

    public Triangle() {
    }

    Triangle(byte side1, byte side2, byte side3) {
        perimeter = (short) (side1 + side2 + side3);
        sides = new byte[] { side1, side2, side3 };
        sameLengthSides = side1 == side2 && side2 == side3 ? (short) 3 //
                        : side1 == side2 || side2 == side3 || side1 == side3 ? (short) 2 //
                                        : 0;
        byte longest = 0;
        byte[] others = new byte[2];
        int i = 0;

        for (byte s : sides)
            if (s > longest) {
                if (longest > 0)
                    others[i++] = longest;
                longest = s;
            } else if (s <= 0) {
                throw new IllegalArgumentException("side " + s);
            } else {
                others[i++] = s;
            }

        hypotenuse = longest * longest == others[0] * others[0] + others[1] * others[1] ? longest : null;
    }

    @Override
    public String toString() {
        return "Triangle#" + distinctKey + " sides " + Arrays.toString(sides) + ", "
               + sameLengthSides + "of same length, perimeter " + perimeter + ", hypotenuse " + hypotenuse;
    }
}
