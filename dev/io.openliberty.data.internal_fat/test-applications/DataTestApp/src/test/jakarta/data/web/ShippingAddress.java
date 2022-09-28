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

import jakarta.data.DiscriminatorColumn;
import jakarta.data.DiscriminatorValue;
import jakarta.data.Inheritance;

/**
 *
 */
@Inheritance
@DiscriminatorColumn("ADDRESS_TYPE")
@DiscriminatorValue("Standard")
public class ShippingAddress {

    public Long id;

    public String city;

    public String state;

    public StreetAddress streetAddress; // @Embeddable

    public int zipCode;
}
