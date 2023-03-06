/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package test.jakarta.data.template.web;

import java.time.Year;

/**
 * A simple entity without annotations.
 */
public class House {
    public int area;

    public Garage garage;

    public Kitchen kitchen;

    public float lotSize;

    public int numBedrooms;

    public String parcelId;

    public float purchasePrice;

    public Year sold;
}
