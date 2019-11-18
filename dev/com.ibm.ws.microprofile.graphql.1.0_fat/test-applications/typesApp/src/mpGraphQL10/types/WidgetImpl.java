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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Type;

/**
 * This is a server side representation of a Widget.
 */
@Type("Widget")
@Description("An object that is for sale.")
public class WidgetImpl {

    private Set<Long> usedIds = new HashSet<>();
    
    @Id
    private final long widgetId;
    private String name;
    private int quantity = -1;
    private double weight = -1.0;

    static WidgetImpl fromWidgetInput(WidgetInput input) {
        return new WidgetImpl(input.getName(),
                              input.getQuantity(),
                              input.getWeight());
    }

    // NOTE that this ensures that JSON-B cannot de-serialize this object,
    // but it should be able to serialize it.
    private WidgetImpl(String name, int quantity, double weight) {
        this.widgetId = System.currentTimeMillis();
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
        return "Widget(" + name + ", " + quantity + ", " + weight + ")";
    }

    public long getWidgetId() {
        return widgetId;
    }
}
