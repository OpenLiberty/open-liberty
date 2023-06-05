/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 * Entity that simulates a Java record, but usable on Java 11. // TODO switch to Java 17+
 */
public class Receipt {
    public Receipt(long purchaseId,
                   String customer,
                   float total) { // TODO more fields

        // TODO The remainder of the class would be unnecessary if it were actually a record
        this.purchaseId = purchaseId;
        this.customer = customer;
        this.total = total;
    }

    private final String customer;
    private final long purchaseId;
    private final float total;

    public long purchaseId() {
        return purchaseId;
    }

    public String customer() {
        return customer;
    }

    public float total() {
        return total;
    }

    @Override
    public String toString() {
        return "Receipt[productId=" + purchaseId + ", customer=" + customer + ", total=" + total + "]";
    }
}
