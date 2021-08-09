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
package mpGraphQL10.iface;

/**
 * This is an implementation class of the interface entity, Widget.
 */
public class WidgetImpl implements Widget {
    
    private String name;
    private int quantity = -1;
    private double weight = -1.0;

    static WidgetImpl fromWidgetInput(WidgetInput input) {
        return new WidgetImpl(input.getName(),
                              input.getQuantity(),
                              input.getWeight());
    }

    // NOTE that this ensures that JSON-B cannot deserialize this object without
    // a custom deserializer.
    WidgetImpl() {}

    WidgetImpl(String name, int quantity, double weight) {
        this.name = name;
        this.quantity = quantity;
        this.weight = weight;
    }

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
        return "WidgetImpl(" + name + ", " + quantity + ", " + weight + ")";
    }
}
