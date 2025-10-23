/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;

/**
 * Embeddable for ShippingAddress
 */
@Embeddable
public class StreetAddress {

    public ArrayList<String> recipientInfo = new ArrayList<String>();

    public int houseNumber;

    public String streetName;

    public StreetAddress() {
    }

    public StreetAddress(int houseNumber, String streetName) {
        this(houseNumber, streetName, null);
    }

    public StreetAddress(int houseNumber, String streetName, List<String> recipientInfo) {
        this.houseNumber = houseNumber;
        this.streetName = streetName;
        if (recipientInfo != null)
            this.recipientInfo.addAll(recipientInfo);
    }

    @Override
    public String toString() {
        return "StreetAddress@" + Integer.toHexString(hashCode()) + ": " + houseNumber + " " + streetName + (recipientInfo.isEmpty() ? "" : (" " + recipientInfo));
    }
}
