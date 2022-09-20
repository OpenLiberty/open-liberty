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

import java.time.OffsetDateTime;

import jakarta.data.Entity;
import jakarta.data.Generated;

/**
 *
 */
@Entity("ORDERS") // overrides the default name Order, which happens to be a keyword in the database
public class Order {

    @Generated(Generated.Type.SEQUENCE)
    public Long id;

    public String purchasedBy;

    public OffsetDateTime purchasedOn;

    public float total;
}
