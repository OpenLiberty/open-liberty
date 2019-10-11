/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient12.jsonbContext;

import java.text.NumberFormat;

/**
 * Basic model object on the client side. This class is the client side
 * "replica" of the server side's <code>remoteApp.basic.Widget</code>
 * This class has only private fields - thus requiring the private visibility
 * JSON-B strategy for serializing and deserializing.
 */
public class Widget {

    private String name;
    private int quantity;
    private double weight;

    public Widget() {}

    public Widget(String name, int quantity, double weight) {
        this.name = name;
        this.quantity = quantity;
        this.weight = weight;
    }

    @Override
    public String toString() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumIntegerDigits(1);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        nf.setMinimumFractionDigits(1);
        return name + ";" + quantity + ";" + nf.format(weight);
    }
}
