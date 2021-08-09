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
package mpGraphQL10.types;

/**
 * This is a client side representation of a Widget.
 */
public class Widget {

    private long widgetId;
    private String name;
    private int quantity = -1;
    private double weight = -1.0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String toString() {
        return "Widget(" + name + ", " + quantity + ", " + weight + ")";
    }

    public long getWidgetId() {
        return widgetId;
    }
    
    public void setWidgetId(long widgetId) {
        this.widgetId = widgetId;
    }
}
