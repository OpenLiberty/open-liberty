/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.iface;

import javax.json.bind.annotation.JsonbTypeDeserializer;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Type;

/**
 * This is a client side representation of a Widget.
 */
@JsonbTypeDeserializer(WidgetDeserializer.class)
@Type(value="Widget")
@Description("An interface representing an object for sale.")
public interface Widget {

    String getName();

    void setName(String name);

    int getQuantity();

    void setQuantity(int quantity);

    double getWeight();

    void setWeight(double weight);
}
