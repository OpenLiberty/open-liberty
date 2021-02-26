/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.restConsumer.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @author anupag
 *
 */
public class PriceModel {

    @Schema(required = false, example = "BLUEPOINTS, CREDITCARD, PAYPAL", description = "App option for purchase", defaultValue = "BLUEPOINTS")
    PurchaseType purchaseType;

    @Schema(required = false, example = "0.0", description = "Selling price of the app", defaultValue = "0.0")
    double sellingPrice;

    public PurchaseType getPurchaseType() {
        return purchaseType;
    }

    public void setPurchaseType(PurchaseType purchaseType) {
        this.purchaseType = purchaseType;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public enum PurchaseType {

        BLUEPOINTS, // default
        CREDITCARD,
        PAYAPL;
    }
}
