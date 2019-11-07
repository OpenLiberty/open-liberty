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
package mpGraphQL10.graphQLInterface;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Interface;

@Interface("IWidget")
@Description("Something to sell")
public interface IWidget {

    public String getName();

    public void setName(String name);

    public int getQuantity();

    public void setQuantity(int quantity);

    public double getWeight();

    public void setWeight(double weight);
}
