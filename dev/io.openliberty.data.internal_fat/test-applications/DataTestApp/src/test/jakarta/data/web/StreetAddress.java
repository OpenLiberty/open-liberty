/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import io.openliberty.data.Embeddable;

/**
 *
 */
@Embeddable
public class StreetAddress {

    public int houseNumber;

    public String streetName;

    public StreetAddress() {
    }

    public StreetAddress(int houseNumber, String streetName) {
        this.houseNumber = houseNumber;
        this.streetName = streetName;
    }

    @Override
    public String toString() {
        return "StreetAddress@" + Integer.toHexString(hashCode()) + ": " + houseNumber + " " + streetName;
    }
}
